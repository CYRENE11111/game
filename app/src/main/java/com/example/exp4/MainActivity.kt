package com.example.exp4

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.exp4.storage.PlayerRepository
import com.example.exp4.ui.ProfileFragment
import com.example.exp4.ui.ThreeCardGameFragment
import com.example.exp4.ui.TwoCardGameFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 主 Activity：只负责承载工具栏与 Fragment 容器。
 *
 * 三个页面均由 Fragment 实现，通过底部导航切换：
 * 1. [ProfileFragment]        个人信息；
 * 2. [TwoCardGameFragment]    2 张牌的翻牌游戏；
 * 3. [ThreeCardGameFragment]  3 张牌的翻牌游戏。
 *
 * 启动时会从应用私有文件恢复个人信息，游戏 Fragment 在创建时各自恢复游戏进度。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 在创建 Fragment 视图之前先恢复玩家信息，保证首次显示的就是文件中的历史数据
        PlayerRepository.load(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        bottomNav = findViewById(R.id.bottomNav)

        val tab = savedInstanceState?.getInt(KEY_TAB, R.id.nav_profile) ?: R.id.nav_profile
        if (savedInstanceState == null) {
            showTab(tab)
            bottomNav.selectedItemId = tab
        } else {
            // 旋转屏幕后 FragmentManager 已恢复 Fragment，这里只同步底部选中状态与标题
            bottomNav.selectedItemId = tab
            updateSubtitle(tab)
        }

        // 初始化完成后再注册监听，避免恢复状态时重复创建 Fragment
        bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }

        // 在游戏页按返回键时先回到个人信息页
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bottomNav.selectedItemId != R.id.nav_profile) {
                    bottomNav.selectedItemId = R.id.nav_profile
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_TAB, bottomNav.selectedItemId)
    }

    /** 切换到指定标签对应的 Fragment；标签相同时不重复创建。 */
    private fun showTab(itemId: Int) {
        updateSubtitle(itemId)
        val tag = tagOf(itemId) ?: return
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer)?.tag == tag) {
            return
        }

        val fragment: Fragment = when (itemId) {
            R.id.nav_profile -> ProfileFragment()
            R.id.nav_game_two -> TwoCardGameFragment()
            R.id.nav_game_three -> ThreeCardGameFragment()
            else -> return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commitNow()
    }

    private fun tagOf(itemId: Int): String? = when (itemId) {
        R.id.nav_profile -> TAG_PROFILE
        R.id.nav_game_two -> TAG_GAME_TWO
        R.id.nav_game_three -> TAG_GAME_THREE
        else -> null
    }

    private fun updateSubtitle(itemId: Int) {
        toolbar.subtitle = when (itemId) {
            R.id.nav_profile -> getString(R.string.tab_profile)
            R.id.nav_game_two -> getString(R.string.tab_game_two)
            R.id.nav_game_three -> getString(R.string.tab_game_three)
            else -> null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                showAboutDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /** 顶部菜单“关于”：弹出版本、作者与玩法说明对话框。 */
    private fun showAboutDialog() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_title)
            .setMessage(
                getString(
                    R.string.about_message,
                    versionName,
                    PlayerRepository.player.name,
                    PlayerRepository.player.studentId
                )
            )
            .setPositiveButton(R.string.btn_ok, null)
            .show()
    }

    companion object {
        private const val KEY_TAB = "selected_tab"
        private const val TAG_PROFILE = "profile"
        private const val TAG_GAME_TWO = "game_two"
        private const val TAG_GAME_THREE = "game_three"
    }
}

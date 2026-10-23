package com.example.exp4

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.exp4.model.Player
import com.example.exp4.model.PlayerRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 主 Activity：显示当前玩家的基本信息
 * （头像、姓名、性别、学号、电话、邮件地址、爱好、游戏最高分），
 * 并提供进入“信息修改”和“翻牌游戏”两个二级 Activity 的入口；
 * 顶部菜单中的“关于”项会弹出关于对话框。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvInfoName: TextView
    private lateinit var tvInfoGender: TextView
    private lateinit var tvInfoStudentId: TextView
    private lateinit var tvInfoPhone: TextView
    private lateinit var tvInfoEmail: TextView
    private lateinit var tvInfoHobbies: TextView
    private lateinit var tvInfoHighScore: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 顶部工具栏 + 菜单（关于）
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        tvWelcome = findViewById(R.id.tvWelcome)
        tvInfoName = findViewById(R.id.tvInfoName)
        tvInfoGender = findViewById(R.id.tvInfoGender)
        tvInfoStudentId = findViewById(R.id.tvInfoStudentId)
        tvInfoPhone = findViewById(R.id.tvInfoPhone)
        tvInfoEmail = findViewById(R.id.tvInfoEmail)
        tvInfoHobbies = findViewById(R.id.tvInfoHobbies)
        tvInfoHighScore = findViewById(R.id.tvInfoHighScore)

        // 修改个人信息
        findViewById<View>(R.id.btnEdit).setOnClickListener {
            startActivity(Intent(this, EditInfoActivity::class.java))
        }

        // 进入翻牌游戏
        findViewById<View>(R.id.btnPlay).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // 从“信息修改”或游戏界面返回时，刷新显示的基本信息
        showPlayer(PlayerRepository.player)
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

    private fun showPlayer(player: Player) {
        tvWelcome.text = getString(R.string.welcome_player, player.name)
        tvInfoName.text = player.name
        tvInfoGender.text = player.gender
        tvInfoStudentId.text = player.studentId
        tvInfoPhone.text = player.phone
        tvInfoEmail.text = player.email
        tvInfoHobbies.text = player.hobbies
        tvInfoHighScore.text = getString(R.string.high_score_value, player.highScore)
    }
}

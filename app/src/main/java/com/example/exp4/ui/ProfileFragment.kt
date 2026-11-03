package com.example.exp4.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.exp4.R
import com.example.exp4.model.Player
import com.example.exp4.storage.PlayerRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * 第 1 个 Fragment：个人信息。
 *
 * 同一个 Fragment 中有两块内容：
 * 1. 信息展示视图：头像、姓名、性别、学号、电话、邮件地址、爱好、游戏最高分；
 * 2. 信息修改视图：点击“修改个人信息”后切换显示，保存时把 [Player] 对象
 *    通过对象流写入应用私有文件 player.dat，退出程序后重新打开即可恢复。
 */
class ProfileFragment : Fragment() {

    private lateinit var displayView: View
    private lateinit var editView: View

    // 信息展示视图
    private lateinit var tvWelcome: TextView
    private lateinit var tvInfoName: TextView
    private lateinit var tvInfoGender: TextView
    private lateinit var tvInfoStudentId: TextView
    private lateinit var tvInfoPhone: TextView
    private lateinit var tvInfoEmail: TextView
    private lateinit var tvInfoHobbies: TextView
    private lateinit var tvInfoHighScore: TextView

    // 信息修改视图
    private lateinit var tilName: TextInputLayout
    private lateinit var tilStudentId: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etStudentId: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var cbTravel: CheckBox
    private lateinit var cbReading: CheckBox
    private lateinit var cbGame: CheckBox
    private lateinit var cbShopping: CheckBox

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayView = view.findViewById(R.id.profileDisplay)
        editView = view.findViewById(R.id.profileEdit)

        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvInfoName = view.findViewById(R.id.tvInfoName)
        tvInfoGender = view.findViewById(R.id.tvInfoGender)
        tvInfoStudentId = view.findViewById(R.id.tvInfoStudentId)
        tvInfoPhone = view.findViewById(R.id.tvInfoPhone)
        tvInfoEmail = view.findViewById(R.id.tvInfoEmail)
        tvInfoHobbies = view.findViewById(R.id.tvInfoHobbies)
        tvInfoHighScore = view.findViewById(R.id.tvInfoHighScore)

        tilName = view.findViewById(R.id.tilName)
        tilStudentId = view.findViewById(R.id.tilStudentId)
        tilPhone = view.findViewById(R.id.tilPhone)
        tilEmail = view.findViewById(R.id.tilEmail)
        etName = view.findViewById(R.id.etName)
        etStudentId = view.findViewById(R.id.etStudentId)
        etPhone = view.findViewById(R.id.etPhone)
        etEmail = view.findViewById(R.id.etEmail)
        rbMale = view.findViewById(R.id.rbMale)
        rbFemale = view.findViewById(R.id.rbFemale)
        cbTravel = view.findViewById(R.id.cbTravel)
        cbReading = view.findViewById(R.id.cbReading)
        cbGame = view.findViewById(R.id.cbGame)
        cbShopping = view.findViewById(R.id.cbShopping)

        view.findViewById<View>(R.id.btnEdit).setOnClickListener { showEditForm() }
        view.findViewById<View>(R.id.btnSave).setOnClickListener { saveInfo() }
        view.findViewById<View>(R.id.btnCancel).setOnClickListener { showDisplay() }

        showDisplay()
    }

    override fun onResume() {
        super.onResume()
        // 从游戏界面刷新了最高分后，回到个人信息页立即更新显示
        if (::displayView.isInitialized && displayView.visibility == View.VISIBLE) {
            renderPlayer()
        }
    }

    /** 展示个人信息，隐藏修改表单。 */
    private fun showDisplay() {
        editView.visibility = View.GONE
        displayView.visibility = View.VISIBLE
        renderPlayer()
    }

    /** 切换到修改表单，并把当前信息填入输入框。 */
    private fun showEditForm() {
        showCurrentInfo()
        displayView.visibility = View.GONE
        editView.visibility = View.VISIBLE
        editView.scrollTo(0, 0)
    }

    private fun renderPlayer() {
        val player = PlayerRepository.player
        tvWelcome.text = getString(R.string.welcome_player, player.name)
        tvInfoName.text = player.name
        tvInfoGender.text = player.gender
        tvInfoStudentId.text = player.studentId
        tvInfoPhone.text = player.phone
        tvInfoEmail.text = player.email
        tvInfoHobbies.text = player.hobbies
        tvInfoHighScore.text = getString(R.string.high_score_value, player.highScore)
    }

    private fun showCurrentInfo() {
        val player = PlayerRepository.player
        clearErrors()
        etName.setText(player.name)
        etStudentId.setText(player.studentId)
        etPhone.setText(player.phone)
        etEmail.setText(player.email)

        if (player.gender == getString(R.string.gender_female)) {
            rbFemale.isChecked = true
        } else {
            rbMale.isChecked = true
        }

        // 爱好：按“、”拆分后勾选对应的复选框
        val hobbies = player.hobbies.split(HOBBY_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
        cbTravel.isChecked = getString(R.string.hobby_travel) in hobbies
        cbReading.isChecked = getString(R.string.hobby_reading) in hobbies
        cbGame.isChecked = getString(R.string.hobby_game) in hobbies
        cbShopping.isChecked = getString(R.string.hobby_shopping) in hobbies
    }

    /** 校验输入并把修改后的玩家信息序列化保存到文件。 */
    private fun saveInfo() {
        clearErrors()

        val name = etName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            tilName.error = getString(R.string.toast_name_empty)
            etName.requestFocus()
            return
        }

        val studentId = etStudentId.text?.toString()?.trim().orEmpty()
        if (studentId.isEmpty()) {
            tilStudentId.error = getString(R.string.toast_student_id_empty)
            etStudentId.requestFocus()
            return
        }

        val phone = etPhone.text?.toString()?.trim().orEmpty()
        if (!isValidPhone(phone)) {
            tilPhone.error = getString(R.string.toast_phone_invalid)
            etPhone.requestFocus()
            return
        }

        val email = etEmail.text?.toString()?.trim().orEmpty()
        if (!isValidEmail(email)) {
            tilEmail.error = getString(R.string.toast_email_invalid)
            etEmail.requestFocus()
            return
        }

        val gender = if (rbFemale.isChecked) {
            getString(R.string.gender_female)
        } else {
            getString(R.string.gender_male)
        }

        val hobbyList = mutableListOf<String>()
        if (cbTravel.isChecked) hobbyList.add(getString(R.string.hobby_travel))
        if (cbReading.isChecked) hobbyList.add(getString(R.string.hobby_reading))
        if (cbGame.isChecked) hobbyList.add(getString(R.string.hobby_game))
        if (cbShopping.isChecked) hobbyList.add(getString(R.string.hobby_shopping))

        val player: Player = PlayerRepository.player
        player.name = name
        player.studentId = studentId
        player.phone = phone
        player.email = email
        player.gender = gender
        player.hobbies = hobbyList.joinToString(HOBBY_SEPARATOR)

        val success = PlayerRepository.save(requireContext())
        val message = if (success) R.string.toast_saved else R.string.toast_save_failed
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        showDisplay()
    }

    private fun clearErrors() {
        tilName.error = null
        tilStudentId.error = null
        tilPhone.error = null
        tilEmail.error = null
    }

    /** 手机号校验：11 位数字且以 1 开头。 */
    private fun isValidPhone(phone: String): Boolean {
        return phone.length == 11 && phone.startsWith("1") && phone.all { it.isDigit() }
    }

    /** 邮箱校验：xxx@yyy.zz 形式。 */
    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && EMAIL_REGEX.matches(email)
    }

    companion object {
        private const val HOBBY_SEPARATOR = "、"
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

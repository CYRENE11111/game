package com.example.exp4

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.exp4.model.PlayerRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * 信息修改 Activity：修改玩家的姓名、学号、电话号码、电子邮箱地址、性别和爱好。
 *
 * 按实验要求，修改后的信息不需要保存到文件中，
 * 因此这里直接改写内存中的玩家信息，返回主界面即可看到更新。
 */
class EditInfoActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_info)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tilName = findViewById(R.id.tilName)
        tilStudentId = findViewById(R.id.tilStudentId)
        tilPhone = findViewById(R.id.tilPhone)
        tilEmail = findViewById(R.id.tilEmail)
        etName = findViewById(R.id.etName)
        etStudentId = findViewById(R.id.etStudentId)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        rbMale = findViewById(R.id.rbMale)
        rbFemale = findViewById(R.id.rbFemale)
        cbTravel = findViewById(R.id.cbTravel)
        cbReading = findViewById(R.id.cbReading)
        cbGame = findViewById(R.id.cbGame)
        cbShopping = findViewById(R.id.cbShopping)

        findViewById<MaterialToolbar>(R.id.toolbarEdit).setNavigationOnClickListener {
            finish()
        }

        // 显示当前已有的信息
        showCurrentInfo()

        findViewById<View>(R.id.btnSave).setOnClickListener { saveInfo() }
        findViewById<View>(R.id.btnCancel).setOnClickListener { finish() }
    }

    private fun showCurrentInfo() {
        val player = PlayerRepository.player
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

        // 只修改内存中的数据，不写入文件
        val player = PlayerRepository.player
        player.name = name
        player.studentId = studentId
        player.phone = phone
        player.email = email
        player.gender = gender
        player.hobbies = hobbyList.joinToString(HOBBY_SEPARATOR)

        Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show()
        finish()
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

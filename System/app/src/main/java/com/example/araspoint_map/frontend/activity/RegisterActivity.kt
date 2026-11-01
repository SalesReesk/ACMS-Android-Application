package com.example.araspoint_map.frontend.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.model.toShortDepartment
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.databinding.ActivityRegisterBinding
import com.example.araspoint_map.frontend.viewModel.AuthViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authViewModel: AuthViewModel
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        authViewModel = AuthViewModel(AuthRepository())
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))

            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                (v as ScrollView).post {
                    val focused = v.findFocus()
                    if (focused != null) {
                        v.smoothScrollTo(0, focused.bottom)
                    } else {
                        v.smoothScrollTo(0, v.getChildAt(0).bottom)
                    }
                }
            }
            insets
        }

        val departments = arrayOf(
            "(CABEIHM) - College of Accountancy, Business, Economics, and International Hospitality Management",
            "(CAS) - College of Arts and Sciences",
            "(CCJE) - College of Criminal Justice Education",
            "(CHS) - College of Health Sciences",
            "(CICS) - College of Informatics & Computing Sciences",
            "(CTE) - College of Teacher Education"
        )

        val deptAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, departments)
        binding.spinnerDepartment.adapter = deptAdapter

        val roles = arrayOf("Student", "Teacher")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        binding.spinnerRole.adapter = roleAdapter

        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                binding.password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility)
            }
            binding.password.setSelection(binding.password.text?.length ?: 0)
        }

        binding.btnToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                binding.confirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                binding.confirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnToggleConfirmPassword.setImageResource(R.drawable.ic_visibility)
            }
            binding.confirmPassword.setSelection(binding.confirmPassword.text?.length ?: 0)
        }

        binding.password.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pwd = s?.toString() ?: ""
                updatePasswordStrength(pwd)
                checkPasswordsMatch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.confirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkPasswordsMatch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.joinUs.setOnClickListener {
            val name = binding.name.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()
            val confirmPassword = binding.confirmPassword.text.toString().trim()
            val roleStr = binding.spinnerRole.selectedItem.toString()

            if (name.isEmpty()) {
                binding.name.error = "Name is required"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                binding.email.error = "Email is required"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.email.error = "Please enter a valid email address"
                binding.email.requestFocus()
                return@setOnClickListener
            }
            if (!email.endsWith("@g.batstate-u.edu.ph") && !email.endsWith("@gmail.com", ignoreCase = true)) {
                binding.email.error = "Only @g.batstate-u.edu.ph and @gmail.com emails are accepted"
                binding.email.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.password.error = "Password is required"
                return@setOnClickListener
            }
            if (password.length < 6) {
                binding.password.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                binding.confirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }
            if (!binding.checkboxTerms.isChecked) {
                Toast.makeText(this, "Please agree to the University ICT Acceptable Use Policy", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val role = when (roleStr) {
                "Teacher" -> UserRole.TEACHER
                else -> UserRole.STUDENT
            }

            val department = (binding.spinnerDepartment.selectedItem?.toString() ?: "").toShortDepartment()
            val idNumber = email.substringBefore("@")

            authViewModel.register(name, email, password, role, department, idNumber) { success, message ->
                if (success) {
                    val intent = Intent(this, EmailVerificationActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, message ?: "Registration failed", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.textLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun updatePasswordStrength(password: String) {
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++
        if (password.any { !it.isLetterOrDigit() } && password.length >= 10) score++

        binding.passwordStrengthProgress.progress = score
        when (score) {
            0 -> {
                binding.passwordStrengthText.text = "Minimum 8 characters with numbers & symbols"
            }
            1 -> {
                binding.passwordStrengthText.text = "Fair - add numbers and special characters"
            }
            2 -> {
                binding.passwordStrengthText.text = "Good - ready for campus asset portal"
            }
            3 -> {
                binding.passwordStrengthText.text = "Strong - compliant with university security policy"
            }
        }
    }

    private fun checkPasswordsMatch() {
        val pwd = binding.password.text?.toString() ?: ""
        val confirm = binding.confirmPassword.text?.toString() ?: ""

        if (confirm.isNotEmpty() && pwd == confirm) {
            binding.matchNotice.visibility = View.VISIBLE
        } else {
            binding.matchNotice.visibility = View.GONE
        }
    }
}

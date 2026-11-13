package com.example.araspoint_map.frontend.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.databinding.ActivityLoginBinding
import com.example.araspoint_map.frontend.viewModel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private var isPasswordVisible = false

    companion object {
        private const val PREFS_NAME = "ArasPointLoginPrefs"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_REMEMBER = "key_remember"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val existingUser = FirebaseAuth.getInstance().currentUser
        if (existingUser != null) {
            if (existingUser.isEmailVerified) {
                val intent = Intent(this, MainDashboardAdminActivity::class.java)
                startActivity(intent)
                finish()
                return
            } else {
                val intent = Intent(this, EmailVerificationActivity::class.java)
                intent.putExtra("email", existingUser.email ?: "")
                startActivity(intent)
                finish()
                return
            }
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
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

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val emailDomains = arrayOf(
            "@g.batstate-u.edu.ph",
            "@gmail.com"
        )
        val emailAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, emailDomains)
        binding.email.setAdapter(emailAdapter)
        binding.email.threshold = 1

        binding.chipBatstateU.setOnClickListener {
            val currentText = binding.email.text.toString().trim()
            val username = currentText.substringBefore("@")
            binding.email.setText("$username@g.batstate-u.edu.ph")
            binding.email.setSelection(binding.email.text?.length ?: 0)
        }

        binding.chipGmail.setOnClickListener {
            val currentText = binding.email.text.toString().trim()
            val username = currentText.substringBefore("@")
            binding.email.setText("$username@gmail.com")
            binding.email.setSelection(binding.email.text?.length ?: 0)
        }

        val rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER, false)
        if (rememberMe) {
            val savedEmail = sharedPreferences.getString(KEY_EMAIL, "")
            binding.email.setText(savedEmail)
            binding.checkboxRememberMe.isChecked = true
        }

        binding.btnClearEmail.setOnClickListener {
            binding.email.text?.clear()
        }

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

        binding.btnDemoFaculty.setOnClickListener {
            binding.email.setText("faculty.arasof@g.batstate-u.edu.ph")
            binding.password.setText("TeacherPortal2024!")
        }

        binding.btnDemoMaintenance.setOnClickListener {
            binding.email.setText("maint.auditor@g.batstate-u.edu.ph")
            binding.password.setText("FieldOpsNasugbu!")
        }

        binding.btnDemoAdmin.setOnClickListener {
            binding.email.setText("ict.admin@g.batstate-u.edu.ph")
            binding.password.setText("RootAdminNasugbu99!")
        }

        binding.loginButton.setOnClickListener {

            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isEmpty()) {
                binding.email.error = "Email is required"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.email.error = "Please enter a valid email address"
                binding.email.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.password.error = "Password is required"
                return@setOnClickListener
            }

            val editor = sharedPreferences.edit()
            if (binding.checkboxRememberMe.isChecked) {
                editor.putBoolean(KEY_REMEMBER, true)
                editor.putString(KEY_EMAIL, email)
            } else {
                editor.putBoolean(KEY_REMEMBER, false)
                editor.remove(KEY_EMAIL)
            }
            editor.apply()

            authViewModel.login(email, password) { success, message ->
                if (success) {
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null && firebaseUser.isEmailVerified) {
                        val intent = Intent(this, MainDashboardAdminActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this, EmailVerificationActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this, message ?: "Login failed", Toast.LENGTH_LONG).show()
                    binding.password.text?.clear()
                }
            }
        }

        binding.textClickable.setOnClickListener {
            Toast.makeText(this, "Forgot Password - Please contact ICT Facilities office", Toast.LENGTH_SHORT).show()
        }

        binding.textRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}

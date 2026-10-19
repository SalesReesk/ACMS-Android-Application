package com.example.araspoint_map.frontend.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.databinding.ActivityLoginBinding
import com.example.araspoint_map.frontend.viewModel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "ArasPointLoginPrefs"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_REMEMBER = "key_remember"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Auto login if already authenticated
        if (FirebaseAuth.getInstance().currentUser != null) {
            val intent = Intent(this, MainDashboardAdminActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        authViewModel = AuthViewModel(AuthRepository())
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Load Remember Me data
        val rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER, false)
        if (rememberMe) {
            val savedEmail = sharedPreferences.getString(KEY_EMAIL, "")
            binding.email.setText(savedEmail)
            binding.checkboxRememberMe.isChecked = true
        }

        binding.loginButton.setOnClickListener {

            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isEmpty()) {
                binding.email.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.password.error = "Password is required"
                return@setOnClickListener
            }

            // Save or clear Remember Me preferences
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
                Toast.makeText(this, message ?: "", Toast.LENGTH_LONG).show()
                if (success) {
                    val intent = Intent(this, MainDashboardAdminActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    binding.password.text.clear()
                }
            }
        }

        binding.textClickable.setOnClickListener {
            Toast.makeText(this, "Forgot Password", Toast.LENGTH_SHORT).show()
        }

        binding.textRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}

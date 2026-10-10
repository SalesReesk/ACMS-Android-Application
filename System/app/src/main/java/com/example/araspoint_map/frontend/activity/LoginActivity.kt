package com.example.araspoint_map.frontend.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.databinding.ActivityLoginBinding
import com.example.araspoint_map.frontend.viewModel.AuthViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        authViewModel = AuthViewModel(AuthRepository())
        setContentView(binding.root)

        binding.signinBtn.setOnClickListener {

            val email = binding.emailAd.text.toString().trim()
            val password = binding.editPass.text.toString().trim()

            if (email.isEmpty()) {
                binding.emailAd.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.editPass.error = "Password is required"
                return@setOnClickListener
            }

            authViewModel.login(email, password) { success, message ->
                Toast.makeText(this, message ?: "", Toast.LENGTH_LONG).show()
                if (success) {
                    // TODO: Navigate to map/dashboard screen on successful login
                } else {
                    binding.emailAd.text.clear()
                    binding.editPass.text.clear()
                }
            }
        }
    }
}
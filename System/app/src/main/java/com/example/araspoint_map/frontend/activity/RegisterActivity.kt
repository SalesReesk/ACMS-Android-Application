package com.example.araspoint_map.frontend.activity

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.databinding.ActivityRegisterBinding
import com.example.araspoint_map.frontend.viewModel.AuthViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        authViewModel = AuthViewModel(AuthRepository())
        setContentView(binding.root)

        // Setup role spinner
        val roles = arrayOf("Student", "Teacher")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        binding.spinnerRole.adapter = roleAdapter

        binding.joinUs.setOnClickListener {
            val name = binding.name.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()
            val roleStr = binding.spinnerRole.selectedItem.toString()

            if (name.isEmpty()) {
                binding.name.error = "Name is required"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                binding.email.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.password.error = "Password is required"
                return@setOnClickListener
            }

            val role = when (roleStr) {
                "Teacher" -> UserRole.TEACHER
                else -> UserRole.STUDENT
            }

            authViewModel.register(name, email, password, role) { success, message ->
                Toast.makeText(this, message ?: "", Toast.LENGTH_LONG).show()
                if (success) {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, message ?: "", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.textLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}

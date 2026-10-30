package com.example.araspoint_map.frontend.activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.repository.AuthRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EmailVerificationActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()
    private var cooldownTimer: CountDownTimer? = null
    private var canResend = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_email_verification)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.emailVerifRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val email = intent.getStringExtra("email") ?: FirebaseAuth.getInstance().currentUser?.email ?: ""
        val labelEmail = findViewById<TextView>(R.id.labelEmail)
        val btnVerified = findViewById<MaterialButton>(R.id.btnIveVerified)
        val btnResend = findViewById<MaterialButton>(R.id.btnResendEmail)
        val labelCooldown = findViewById<TextView>(R.id.labelCooldown)
        val btnBack = findViewById<TextView>(R.id.btnBackToLogin)

        labelEmail.text = email

        btnVerified.setOnClickListener {
            btnVerified.isEnabled = false
            btnVerified.text = "Checking..."
            lifecycleScope.launch {
                val verified = authRepository.reloadUser()
                if (verified) {
                    Toast.makeText(this@EmailVerificationActivity,
                        "Email verified! Welcome to ArasPoint.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@EmailVerificationActivity, MainDashboardAdminActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    btnVerified.isEnabled = true
                    btnVerified.text = "I've Verified My Email"
                    Toast.makeText(this@EmailVerificationActivity,
                        "Email not yet verified. Please click the link in your inbox.", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnResend.setOnClickListener {
            if (!canResend) return@setOnClickListener
            lifecycleScope.launch {
                val result = authRepository.sendEmailVerification()
                if (result.isSuccess) {
                    Toast.makeText(this@EmailVerificationActivity,
                        "Verification email sent to $email", Toast.LENGTH_SHORT).show()
                    startCooldown(btnResend, labelCooldown)
                } else {
                    Toast.makeText(this@EmailVerificationActivity,
                        "Could not send email: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnBack.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun startCooldown(btnResend: MaterialButton, labelCooldown: TextView) {
        canResend = false
        btnResend.isEnabled = false
        labelCooldown.visibility = View.VISIBLE

        cooldownTimer?.cancel()
        cooldownTimer = object : CountDownTimer(60_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val secs = millisUntilFinished / 1000
                labelCooldown.text = "You can resend in ${secs}s"
            }
            override fun onFinish() {
                canResend = true
                btnResend.isEnabled = true
                labelCooldown.visibility = View.GONE
            }
        }.start()
    }

    override fun onDestroy() {
        cooldownTimer?.cancel()
        super.onDestroy()
    }
}

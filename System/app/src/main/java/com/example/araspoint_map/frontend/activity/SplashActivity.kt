package com.example.araspoint_map.frontend.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.araspoint_map.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splashRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        animateSplash()

        lifecycleScope.launch {
            delay(1200)

            val currentUser = FirebaseAuth.getInstance().currentUser
            val intent = if (currentUser != null) {
                Intent(this@SplashActivity, MainDashboardAdminActivity::class.java)
            } else {
                Intent(this@SplashActivity, LoginActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun animateSplash() {
        val logoCard = findViewById<CardView>(R.id.logoCard)
        val centerGroup = findViewById<View>(R.id.centerGroup)
        val bottomGroup = findViewById<View>(R.id.bottomGroup)

        val scaleUp = ScaleAnimation(0.8f, 1f, 0.8f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 500
            fillAfter = true
        }
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 500
            fillAfter = true
        }
        val logoAnim = AnimationSet(true).apply {
            addAnimation(scaleUp)
            addAnimation(fadeIn)
        }
        logoCard?.startAnimation(logoAnim)

        val centerFade = AlphaAnimation(0f, 1f).apply {
            duration = 500
            startOffset = 200
            fillAfter = true
        }
        centerGroup?.startAnimation(centerFade)

        val bottomFade = AlphaAnimation(0f, 1f).apply {
            duration = 400
            startOffset = 400
            fillAfter = true
        }
        bottomGroup?.startAnimation(bottomFade)
    }
}

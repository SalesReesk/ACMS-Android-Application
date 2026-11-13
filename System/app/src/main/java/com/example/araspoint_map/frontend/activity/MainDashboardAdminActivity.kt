package com.example.araspoint_map.frontend.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.databinding.ActivityMainDashboardAdminBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainDashboardAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainDashboardAdminBinding
    private val authRepository = AuthRepository()
    private var currentUserRole: UserRole = UserRole.STUDENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainDashboardAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val headerView = binding.navigationView.getHeaderView(0)
        val statusBarSpacer = headerView.findViewById<View>(R.id.statusBarSpacer)

        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topToolbar.setPadding(0, systemBars.top, 0, 0)

            val params = binding.bottomBarCoordinator.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = (4 * resources.displayMetrics.density).toInt()
            binding.bottomBarCoordinator.layoutParams = params

            val spacerParams = statusBarSpacer?.layoutParams
            if (spacerParams != null) {
                spacerParams.height = maxOf(systemBars.top, (36 * resources.displayMetrics.density).toInt())
                statusBarSpacer.layoutParams = spacerParams
            }
            insets
        }

        binding.topToolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.drawer_reports -> {
                    openFragment(ReportFragment())
                    true
                }
                R.id.drawer_profile -> {
                    openAccountFragment()
                    true
                }
                R.id.drawer_user_mgmt -> {
                    if (currentUserRole == UserRole.ADMIN) {
                        openFragment(UserManagementFragment())
                    } else {
                        Toast.makeText(this, "Access denied. Admin only.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.drawer_settings -> {
                    openFragment(SettingsFragment())
                    true
                }
                R.id.drawer_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        val navTabHome = binding.root.findViewById<View>(R.id.navTabHome)
        val navTabRight = binding.root.findViewById<View>(R.id.navTabRight)
        val fabCenterMap = binding.root.findViewById<View>(R.id.fabCenterMap)

        val imgTabHome = binding.root.findViewById<ImageView>(R.id.imgTabHome)
        val txtTabHome = binding.root.findViewById<TextView>(R.id.txtTabHome)
        val imgTabRight = binding.root.findViewById<ImageView>(R.id.imgTabRight)
        val txtTabRight = binding.root.findViewById<TextView>(R.id.txtTabRight)

        fun selectTab(tab: String) {
            val primaryColor = getColor(R.color.primary)
            val outlineColor = getColor(R.color.outline)

            imgTabHome?.setColorFilter(outlineColor)
            txtTabHome?.setTextColor(outlineColor)
            txtTabHome?.typeface = Typeface.DEFAULT

            imgTabRight?.setColorFilter(outlineColor)
            txtTabRight?.setTextColor(outlineColor)
            txtTabRight?.typeface = Typeface.DEFAULT

            when (tab) {
                "home" -> {
                    imgTabHome?.setColorFilter(primaryColor)
                    txtTabHome?.setTextColor(primaryColor)
                    txtTabHome?.typeface = Typeface.DEFAULT_BOLD
                    openFragment(DashBoardFragment())
                }
                "map" -> {
                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                }
                "right" -> {
                    imgTabRight?.setColorFilter(primaryColor)
                    txtTabRight?.setTextColor(primaryColor)
                    txtTabRight?.typeface = Typeface.DEFAULT_BOLD
                    if (currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.MAINTENANCE) {
                        openFragment(InventoryFragment())
                    } else {
                        openFragment(ReportFragment())
                    }
                }
            }
        }

        navTabHome.setOnClickListener { selectTab("home") }
        fabCenterMap.setOnClickListener { selectTab("map") }
        navTabRight.setOnClickListener { selectTab("right") }

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userEmail = firebaseUser?.email ?: ""
        
        val txtHeaderName = headerView.findViewById<TextView>(R.id.txtHeaderName)
        val txtHeaderEmail = headerView.findViewById<TextView>(R.id.txtHeaderEmail)
        val txtHeaderRole = headerView.findViewById<TextView>(R.id.txtHeaderRole)
        val txtHeaderAvatar = headerView.findViewById<TextView>(R.id.txtHeaderAvatar)
        val imgHeaderProfile = headerView.findViewById<ImageView>(R.id.imgHeaderProfile)

        txtHeaderEmail?.text = if (userEmail.isNotEmpty()) userEmail else "user@araspoint.edu"
        txtHeaderName?.text = firebaseUser?.displayName ?: if (userEmail.isNotEmpty()) userEmail.substringBefore("@").replaceFirstChar { it.uppercase() } else "User"
        txtHeaderAvatar?.text = txtHeaderName?.text?.take(2)?.toString()?.uppercase() ?: "AP"

        lifecycleScope.launch {
            try {
                val fullUser = authRepository.getFullUser()
                if (fullUser != null && fullUser.photo.isNotEmpty() && imgHeaderProfile != null) {
                    Glide.with(this@MainDashboardAdminActivity)
                        .load(fullUser.photo)
                        .into(imgHeaderProfile)
                }
            } catch (_: Exception) {}

            val role = authRepository.getUserRole()
            currentUserRole = when {
                userEmail.contains("admin", ignoreCase = true) -> UserRole.ADMIN
                userEmail.contains("maint", ignoreCase = true) || role == UserRole.MAINTENANCE -> UserRole.MAINTENANCE
                role != null -> role
                else -> UserRole.STUDENT
            }

            txtHeaderRole?.text = currentUserRole.name

            val userMgmtItem = binding.navigationView.menu.findItem(R.id.drawer_user_mgmt)
            userMgmtItem?.isVisible = (currentUserRole == UserRole.ADMIN)

            if (currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.MAINTENANCE) {
                txtTabRight?.text = "Assets"
                imgTabRight?.setImageResource(R.drawable.ic_nav_assets)
            } else {
                txtTabRight?.text = "Reports"
                imgTabRight?.setImageResource(R.drawable.ic_nav_tasks)
            }

            if (savedInstanceState == null) {
                selectTab("home")
            }
        }

        if (savedInstanceState == null) {
            selectTab("home")
        }
    }

    fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .commit()
    }

    fun openReportFragment() {
        openFragment(ReportFragment())
    }

    fun openMapFragment() {
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
    }

    fun openInventoryFragment() {
        openFragment(InventoryFragment())
    }

    fun openAccountFragment() {
        openFragment(AccountFragment())
    }
}

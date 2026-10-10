package com.example.araspoint_map.frontend.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.araspoint_map.R
import com.example.araspoint_map.databinding.ActivityMainDashboardAdminBinding

class MainDashboardAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainDashboardAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainDashboardAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomNavigation = binding.bottomAdminNav

        bottomNavigation.setOnItemSelectedListener { item ->

            val selectedFragment: Fragment? = when (item.itemId) {
                R.id.dashboard -> DashBoardFragment()
                R.id.inventory -> InventoryFragment()
                R.id.report -> ReportFragment()
                R.id.account -> AccontFragment()
                else -> null
            }

            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerView, selectedFragment)
                    .commit()

                true
            } else {
                false
            }
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.dashboard
        }
    }
}
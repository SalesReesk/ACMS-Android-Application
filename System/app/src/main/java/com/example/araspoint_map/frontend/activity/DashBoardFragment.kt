package com.example.araspoint_map.frontend.activity

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.enums.AssetStatus
import com.example.araspoint_map.backend.repository.AssetRepository
import com.example.araspoint_map.backend.repository.MaintenanceRepository
import com.example.araspoint_map.databinding.FragmentDashBoardBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File

class DashBoardFragment : Fragment() {

    private var _binding: FragmentDashBoardBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences
    private val assetRepository = AssetRepository()
    private val maintenanceRepository = MaintenanceRepository()

    companion object {
        private const val PREFS_NAME = "ArasPointProfilePrefs"
        private const val KEY_PHOTO_PATH = "key_photo_path"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val fullName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"
        val firstName = fullName.split(" ").firstOrNull() ?: fullName

        binding.txtStudent.text = "$firstName!"

        loadSavedProfileImage()
        loadDashboardStats()

        binding.imgDashboardProfile.setOnClickListener {
            (activity as? MainDashboardAdminActivity)?.openAccountFragment()
        }

        binding.cardMiniMap.setOnClickListener {
            (activity as? MainDashboardAdminActivity)?.findViewById<BottomNavigationView>(R.id.bottom_admin_nav)?.selectedItemId = R.id.map_nav
        }

        binding.cardQuickInventory.setOnClickListener {
            (activity as? MainDashboardAdminActivity)?.findViewById<BottomNavigationView>(R.id.bottom_admin_nav)?.selectedItemId = R.id.inventory
        }

        binding.cardQuickReport.setOnClickListener {
            (activity as? MainDashboardAdminActivity)?.findViewById<BottomNavigationView>(R.id.bottom_admin_nav)?.selectedItemId = R.id.report
        }

        binding.searchBar.setOnClickListener {
            (activity as? MainDashboardAdminActivity)?.findViewById<BottomNavigationView>(R.id.bottom_admin_nav)?.selectedItemId = R.id.inventory
        }
    }

    override fun onResume() {
        super.onResume()
        loadSavedProfileImage()
        loadDashboardStats()
    }

    private fun loadDashboardStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            val assetsResult = assetRepository.getAssets()
            val reportsResult = maintenanceRepository.getReports()

            if (_binding != null && assetsResult.isSuccess) {
                val assets = assetsResult.getOrNull() ?: emptyList()
                binding.txtStatTotalAssets.text = assets.size.toString()
                val availableCount = assets.count { it.status == AssetStatus.AVAILABLE }
                binding.txtStatAvailable.text = availableCount.toString()
            }

            if (_binding != null && reportsResult.isSuccess) {
                val reports = reportsResult.getOrNull() ?: emptyList()
                binding.txtStatReports.text = reports.size.toString()
            }
        }
    }

    private fun loadSavedProfileImage() {
        val path = sharedPreferences.getString(KEY_PHOTO_PATH, null)
        if (path != null) {
            val file = File(path)
            if (file.exists() && _binding != null) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    binding.imgDashboardProfile.setImageBitmap(bitmap)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

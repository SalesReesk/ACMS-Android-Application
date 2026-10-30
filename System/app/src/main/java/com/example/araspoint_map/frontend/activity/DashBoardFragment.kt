package com.example.araspoint_map.frontend.activity

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.enums.AssetStatus
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.repository.AssetRepository
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.backend.repository.MaintenanceRepository
import com.example.araspoint_map.databinding.FragmentDashBoardBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class DashBoardFragment : Fragment() {

    private var _binding: FragmentDashBoardBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences
    private val assetRepository = AssetRepository()
    private val maintenanceRepository = MaintenanceRepository()
    private val authRepository = AuthRepository()
    private var userRole: UserRole = UserRole.STUDENT

    companion object {
        private const val PREFS_NAME = "ArasPointProfilePrefs"
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
        val userEmail = currentUser?.email ?: ""
        val fullName = currentUser?.displayName ?: userEmail.substringBefore("@")
        val firstName = fullName.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "User"

        binding.txtStudent.text = "$firstName!"

        viewLifecycleOwner.lifecycleScope.launch {
            val role = authRepository.getUserRole()
            userRole = when {
                userEmail.contains("admin", ignoreCase = true) -> UserRole.ADMIN
                userEmail.contains("maint", ignoreCase = true) || role == UserRole.MAINTENANCE -> UserRole.MAINTENANCE
                role != null -> role
                else -> UserRole.STUDENT
            }

            setupRoleBasedDashboard(userRole)
            loadDashboardStats()
        }

        binding.cardMiniMap.setOnClickListener {
            (activity as? MainDashboardAdminActivity)?.openMapFragment()
        }

        binding.cardQuickInventory.setOnClickListener {
            when (userRole) {
                UserRole.ADMIN -> (activity as? MainDashboardAdminActivity)?.openInventoryFragment()
                UserRole.MAINTENANCE -> (activity as? MainDashboardAdminActivity)?.openReportFragment()
                UserRole.TEACHER -> (activity as? MainDashboardAdminActivity)?.openReportFragment()
                UserRole.STUDENT -> (activity as? MainDashboardAdminActivity)?.openReportFragment()
            }
        }

        binding.cardQuickReport.setOnClickListener {
            when (userRole) {
                UserRole.ADMIN -> (activity as? MainDashboardAdminActivity)?.openReportFragment()
                UserRole.MAINTENANCE -> (activity as? MainDashboardAdminActivity)?.openMapFragment()
                UserRole.TEACHER -> (activity as? MainDashboardAdminActivity)?.openInventoryFragment()
                UserRole.STUDENT -> (activity as? MainDashboardAdminActivity)?.openMapFragment()
            }
        }
    }

    private fun setupRoleBasedDashboard(role: UserRole) {
        if (_binding == null) return

        when (role) {
            UserRole.ADMIN -> {
                binding.txtRoleBadge.text = "Administrator"
                binding.txtKpiSectionTitle.text = "Campus System Overview"
                binding.lblStatTotalAssets.text = "Total Assets"
                binding.lblStatAvailable.text = "Available"
                binding.lblStatAttention.text = "Attention"
                binding.lblStatReports.text = "Active Reports"

                binding.txtQuickOp1Title.text = "Manage Asset Inventory"
                binding.imgQuickOp1Icon.setImageResource(R.drawable.ic_inventory_box)

                binding.txtQuickOp2Title.text = "View Incident & Damage Reports"
                binding.imgQuickOp2Icon.setImageResource(R.drawable.ic_warning_shield)
            }
            UserRole.MAINTENANCE -> {
                binding.txtRoleBadge.text = "Maintenance Specialist"
                binding.txtKpiSectionTitle.text = "Work Orders & Repairs Overview"
                binding.lblStatTotalAssets.text = "Campus Equipment"
                binding.lblStatAvailable.text = "Operational"
                binding.lblStatAttention.text = "Under Repair"
                binding.lblStatReports.text = "Pending Work Orders"

                binding.txtQuickOp1Title.text = "View Assigned Work Orders & Reports"
                binding.imgQuickOp1Icon.setImageResource(R.drawable.ic_nav_tasks)

                binding.txtQuickOp2Title.text = "Locate Equipment on Campus Map"
                binding.imgQuickOp2Icon.setImageResource(R.drawable.ic_explore)
            }
            UserRole.TEACHER -> {
                binding.txtRoleBadge.text = "Faculty Instructor"
                binding.txtKpiSectionTitle.text = "Classroom & Lab Equipment Overview"
                binding.lblStatTotalAssets.text = "Lab Equipment"
                binding.lblStatAvailable.text = "Classroom Ready"
                binding.lblStatAttention.text = "Faulty / Missing"
                binding.lblStatReports.text = "Lab Reports"

                binding.txtQuickOp1Title.text = "Report Classroom Equipment Issue"
                binding.imgQuickOp1Icon.setImageResource(R.drawable.ic_warning_shield)

                binding.txtQuickOp2Title.text = "Check Lab Equipment Inventory"
                binding.imgQuickOp2Icon.setImageResource(R.drawable.ic_inventory_box)
            }
            UserRole.STUDENT -> {
                binding.txtRoleBadge.text = "Student"
                binding.txtKpiSectionTitle.text = "Campus Workstation Availability"
                binding.lblStatTotalAssets.text = "Total PCs & Laptops"
                binding.lblStatAvailable.text = "Available For Use"
                binding.lblStatAttention.text = "Under Maintenance"
                binding.lblStatReports.text = "Reported Issues"

                binding.txtQuickOp1Title.text = "Report Broken Desk Equipment"
                binding.imgQuickOp1Icon.setImageResource(R.drawable.ic_warning_shield)

                binding.txtQuickOp2Title.text = "Find Free PCs on Campus Map"
                binding.imgQuickOp2Icon.setImageResource(R.drawable.ic_explore)
            }
        }
    }

    override fun onResume() {
        super.onResume()
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

                val attentionCount = assets.count { it.status == AssetStatus.UNDER_MAINTENANCE || it.status == AssetStatus.MISSING }
                binding.txtStatAttention.text = attentionCount.toString()
            }

            if (_binding != null && reportsResult.isSuccess) {
                val reports = reportsResult.getOrNull() ?: emptyList()
                binding.txtStatReports.text = reports.size.toString()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

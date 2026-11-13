package com.example.araspoint_map.frontend.activity

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.araspoint_map.R
import com.example.araspoint_map.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: SharedPreferences

    companion object {
        private const val PREFS_SETTINGS = "ArasPointSettingsPrefs"
        private const val KEY_NOTIF_REPORTS = "key_notif_reports"
        private const val KEY_NOTIF_URGENT = "key_notif_urgent"
        private const val KEY_AUTO_MAP = "key_auto_map"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)

        val notifReports = prefs.getBoolean(KEY_NOTIF_REPORTS, true)
        val notifUrgent = prefs.getBoolean(KEY_NOTIF_URGENT, true)
        val autoCenterMap = prefs.getBoolean(KEY_AUTO_MAP, true)

        binding.switchReportNotifications.isChecked = notifReports
        binding.switchUrgentAlerts.isChecked = notifUrgent
        binding.switchAutoCenterMap.isChecked = autoCenterMap

        binding.switchReportNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_NOTIF_REPORTS, isChecked).apply()
        }

        binding.switchUrgentAlerts.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_NOTIF_URGENT, isChecked).apply()
        }

        binding.switchAutoCenterMap.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_AUTO_MAP, isChecked).apply()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnClearCache.setOnClickListener {
            clearImageCache()
        }

        binding.btnPlanOfUsePolicy.setOnClickListener {
            showPlanOfUsePolicyDialog()
        }

        binding.btnDataPrivacyNotice.setOnClickListener {
            showDataPrivacyNoticeDialog()
        }

        binding.btnAssetAuditChecklist.setOnClickListener {
            showAssetAuditChecklistDialog()
        }
    }

    private fun showPlanOfUsePolicyDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("ICT Acceptable Plan of Use Policy")
            .setMessage(
                """
                BATANGAS STATE UNIVERSITY - ARASOF NASUGBU
                OOP SEMESTRAL PROJECT - SYSTEM PLAN OF USE POLICY
                
                * ACADEMIC PROJECT DISCLAIMER
                • ArasPoint is an academic demonstration developed for Object-Oriented Programming (OOP) coursework at BatStateU ARASOF-Nasugbu.
                
                1. AUTHORIZED ACCESS & USAGE
                • Access to ArasPoint system is granted to official BatStateU students, faculty, staff, and Property Officers for academic evaluation.
                • System credentials (email & password) are confidential and non-transferable.
                
                2. PROPERTY TRACKING & INTEGRITY
                • All assets, physical hardware, and infrastructure logged in this system represent campus assets of Batangas State University.
                • Tampering with physical asset barcodes, geotags, or system records is strictly prohibited.
                
                3. INCIDENT REPORTING RESPONSIBILITY
                • Users must accurately report damaged, missing, or faulty items with genuine evidence.
                • Falsification of maintenance tickets or submitting misleading information is subject to university academic disciplinary action.
                
                4. SYSTEM AUDITABILITY
                • Every asset addition, update, or deletion is logged with user ID, timestamp, and audit trail records.
                """.trimIndent()
            )
            .setPositiveButton("I Understand", null)
            .show()
    }

    private fun showDataPrivacyNoticeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Data Privacy Notice & Audit Logs")
            .setMessage(
                """
                DATA PRIVACY NOTICE & AUDIT TRAIL GOVERNANCE
                Republic Act No. 10173 (Philippine Data Privacy Act of 2012)
                BatStateU ARASOF-Nasugbu - OOP Semestral Project
                
                1. ACADEMIC PROJECT PURPOSE
                • ArasPoint is developed as an academic demonstration for Object-Oriented Programming (OOP). All information is stored securely in designated Firebase & Cloudinary sandbox environments.
                
                2. PERSONAL INFORMATION COLLECTED
                • User Account Data: Institutional email address, display name, assigned role, and profile picture.
                • Location & Navigation Data: Real-time GPS coordinates utilized strictly for campus wayfinding and location-based asset positioning.
                • Ticket Evidence: Photos captured and uploaded during damage reporting.
                
                3. PURPOSE OF DATA PROCESSING
                • Physical property tracking, maintenance ticket routing, equipment verification, and campus navigation within BatStateU ARASOF-Nasugbu.
                
                4. DATA PROTECTION & STORAGE
                • All user profiles and asset audit logs are secured via Firebase Authentication and Firestore Security Rules.
                • Evidence media uploaded to Cloudinary is restricted to authorized Property Management Officers and academic evaluators.
                
                5. USER RIGHTS
                • You have the right to request information on stored account records, request corrections, or request data deletion upon completion of academic evaluation.
                """.trimIndent()
            )
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showAssetAuditChecklistDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Asset Audit Information Checklist")
            .setMessage(
                """
                CAMPUS PROPERTY AUDIT & ASSET CHECKLIST GUIDELINES
                BatStateU ARASOF-Nasugbu - OOP Semestral Project
                
                When conducting property inventory or auditing equipment, verify all required data points:
                
                ✓ 1. ASSET TAG & IDENTIFIER
                  • Valid AAMS ID code (e.g., AAMS-0001) matching physical barcode label.
                
                ✓ 2. CATEGORY & CLASSIFICATION
                  • Proper categorization (IT Equipment, Electronics & AV, Laboratory, Furniture, Appliances, Safety).
                
                ✓ 3. PHYSICAL LOCATION & DESIGNATION
                  • Specific Campus Building (e.g., HEB, CTEB, CAS, GYM), Floor number, and Room designation.
                
                ✓ 4. OPERATIONAL STATUS
                  • Current status: Available, Under Maintenance, or Missing/Damaged.
                
                ✓ 5. PHYSICAL CONDITION
                  • Rating: Excellent, Good, Fair, or Poor.
                
                ✓ 6. TIMESTAMPD PHOTO EVIDENCE
                  • High-resolution photo verifying physical presence and readable tag.
                
                ✓ 7. AUDITOR VERIFICATION LOG
                  • User ID, date, and timestamp recorded in the audit trail.
                """.trimIndent()
            )
            .setPositiveButton("Got It", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val inputNewPassword = EditText(requireContext()).apply {
            hint = "Enter new password (min. 6 characters)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setMessage("Please enter your new password:")
            .setView(inputNewPassword)
            .setPositiveButton("Update") { dialog, _ ->
                val newPassword = inputNewPassword.text.toString().trim()
                if (newPassword.length < 6) {
                    Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(requireContext(), "Password updated successfully!", Toast.LENGTH_LONG).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(requireContext(), "Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearImageCache() {
        Toast.makeText(requireContext(), "Clearing image cache...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Glide.get(requireContext()).clearDiskCache()
                withContext(Dispatchers.Main) {
                    Glide.get(requireContext()).clearMemory()
                    Toast.makeText(requireContext(), "Image & local cache cleared successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to clear cache: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

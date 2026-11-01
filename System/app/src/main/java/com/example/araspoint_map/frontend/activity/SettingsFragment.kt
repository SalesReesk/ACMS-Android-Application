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
import androidx.appcompat.app.AppCompatDelegate
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

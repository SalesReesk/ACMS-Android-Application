package com.example.araspoint_map.frontend.activity

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.databinding.FragmentAccontBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AccontFragment : Fragment() {

    private var _binding: FragmentAccontBinding? = null
    private val binding get() = _binding!!
    private val authRepository = AuthRepository()

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadProfileToCloudinary(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccontBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val config = HashMap<String, String>()
            config["cloud_name"] = "djyggmhmk"
            MediaManager.init(requireContext(), config)
        } catch (_: Exception) {}

        val currentUser = FirebaseAuth.getInstance().currentUser
        val displayName =
            currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: currentUser?.email?.substringBefore("@")?.replace(".", " ")
                    ?.split(" ")?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                ?: "User"
        val email = currentUser?.email ?: "—"
        val uid = currentUser?.uid?.takeLast(8)?.uppercase() ?: "—"

        binding.profileName.text = displayName
        binding.profileEmail.text = email
        binding.profileUid.text = "UID: $uid"
        binding.profileRole.text = "Loading..."
        binding.profileAccessLevel.text = "Loading..."

        loadCloudinaryProfileImage()
        binding.profileImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnEditProfile.setOnClickListener { showEditProfileDialog() }

        // Fetch dynamic user role & show admin features
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = authRepository.getFullUser()
                if (user != null && _binding != null) {
                    if (user.photo.isNotEmpty()) {
                        Glide.with(requireContext())
                            .load(user.photo)
                            .placeholder(R.drawable.user_icon)
                            .into(binding.profileImage)
                    }
                    if (user.department.isNotEmpty()) {
                        binding.profileDepartment.text = user.department
                    }
                    if (user.idNumber.isNotEmpty()) {
                        binding.profileIdNumber.text = user.idNumber
                    }
                }

                val roleFromAuth = user?.role
                val emailStr = currentUser?.email ?: ""
                val isAdmin = (roleFromAuth == UserRole.ADMIN ||
                        emailStr.contains("admin", ignoreCase = true))
                val isMaintenance = (roleFromAuth == UserRole.MAINTENANCE ||
                        emailStr.contains("maint", ignoreCase = true))

                val roleName = when {
                    isAdmin -> "Admin"
                    isMaintenance -> "Maintenance"
                    roleFromAuth == UserRole.TEACHER -> "Teacher"
                    else -> "Student"
                }

                val accessLevel = when {
                    isAdmin -> "Full Access (Admin)"
                    isMaintenance -> "Maintenance Access"
                    roleFromAuth == UserRole.TEACHER -> "Faculty Read/Write"
                    else -> "Read Only"
                }

                if (_binding == null) return@launch

                binding.profileRole.text = roleName
                binding.profileAccessLevel.text = accessLevel

                if (isAdmin || isMaintenance) {
                    binding.labelAdminActions.visibility = View.VISIBLE
                    if (isAdmin) {
                        binding.btnCreateMaintenanceAccount.visibility = View.VISIBLE
                        binding.labelAdminActions.text = "Administration"
                    } else {
                        binding.btnCreateMaintenanceAccount.visibility = View.GONE
                        binding.labelAdminActions.text = "Maintenance"
                    }
                } else {
                    binding.labelAdminActions.visibility = View.GONE
                    binding.btnCreateMaintenanceAccount.visibility = View.GONE
                }
            } catch (_: Exception) {
                if (_binding != null) {
                    binding.profileRole.text = "Student"
                    binding.profileAccessLevel.text = "Read Only"
                }
            }
        }

        binding.btnCreateMaintenanceAccount.setOnClickListener {
            showCreateMaintenanceDialog()
        }

        binding.btnLogout.setOnClickListener {
            authRepository.logout()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadCloudinaryProfileImage()
    }

    private fun loadCloudinaryProfileImage() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = authRepository.getFullUser()
                if (user != null && user.photo.isNotEmpty() && _binding != null) {
                    Glide.with(requireContext())
                        .load(user.photo)
                        .placeholder(R.drawable.user_icon)
                        .into(binding.profileImage)
                }
            } catch (_: Exception) {}
        }
    }

    private fun uploadProfileToCloudinary(imageUri: Uri) {
        Toast.makeText(requireContext(), "Uploading profile photo to Cloudinary...", Toast.LENGTH_SHORT).show()
        try {
            MediaManager.get().upload(imageUri)
                .unsigned("CampusAssets")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String ?: ""
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null && secureUrl.isNotEmpty()) {
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .update("photo", secureUrl)
                        }
                        activity?.runOnUiThread {
                            if (_binding != null && secureUrl.isNotEmpty()) {
                                Glide.with(requireContext())
                                    .load(secureUrl)
                                    .placeholder(R.drawable.user_icon)
                                    .into(binding.profileImage)
                            }
                            Toast.makeText(requireContext(), "Profile photo updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error initiating upload: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val inputDept = dialogView.findViewById<EditText>(R.id.inputDepartment)
        val inputId = dialogView.findViewById<EditText>(R.id.inputIdNumber)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSaveProfile)

        viewLifecycleOwner.lifecycleScope.launch {
            val user = authRepository.getFullUser()
            if (user != null) {
                inputDept.setText(user.department)
                inputId.setText(user.idNumber)
            }
        }

        btnSubmit.setOnClickListener {
            val dept = inputDept.text.toString().trim()
            val idNum = inputId.text.toString().trim()

            viewLifecycleOwner.lifecycleScope.launch {
                val result = authRepository.updateUserProfile(dept, idNum)
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    if (_binding != null) {
                        binding.profileDepartment.text = dept.ifBlank { "—" }
                        binding.profileIdNumber.text = idNum.ifBlank { "—" }
                    }
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showCreateMaintenanceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_maintenance, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val inputName = dialogView.findViewById<EditText>(R.id.inputMaintName)
        val inputEmail = dialogView.findViewById<EditText>(R.id.inputMaintEmail)
        val inputPassword = dialogView.findViewById<EditText>(R.id.inputMaintPassword)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmitCreateMaint)

        btnSubmit.setOnClickListener {
            val name = inputName.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val result = authRepository.register(name, email, password, UserRole.MAINTENANCE)
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "Maintenance Account created successfully", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

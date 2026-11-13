package com.example.araspoint_map.frontend.activity

import android.app.Activity
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import android.util.Log
import com.example.araspoint_map.BuildConfig
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private val authRepository = AuthRepository()

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { startCrop(it) }
        }

    private val cropActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null) {
                    uploadProfileToCloudinary(resultUri)
                }
            }
        }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(
            File(requireContext().cacheDir, "cropped_profile_${System.currentTimeMillis()}.jpg")
        )
        val intent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(600, 600)
            .getIntent(requireContext())
        cropActivityResultLauncher.launch(intent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        binding.profileImage.setImageResource(R.drawable.user_icon)

        loadCloudinaryProfileImage()
        binding.profileImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnEditProfile.setOnClickListener { showEditProfileDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = authRepository.getFullUser()
                if (user != null && _binding != null) {
                    if (user.name.isNotBlank()) {
                        binding.profileName.text = user.name
                    }
                    if (user.photo.isNotEmpty()) {
                        Glide.with(requireContext())
                            .load(user.photo)
                            .placeholder(R.drawable.user_icon)
                            .error(R.drawable.user_icon)
                            .into(binding.profileImage)
                    } else {
                        binding.profileImage.setImageResource(R.drawable.user_icon)
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


            } catch (_: Exception) {
                if (_binding != null) {
                    binding.profileRole.text = "Student"
                    binding.profileAccessLevel.text = "Read Only"
                }
            }
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
                if (user != null && _binding != null) {
                    if (user.photo.isNotEmpty()) {
                        Glide.with(requireContext())
                            .load(user.photo)
                            .placeholder(R.drawable.user_icon)
                            .error(R.drawable.user_icon)
                            .into(binding.profileImage)
                    } else {
                        binding.profileImage.setImageResource(R.drawable.user_icon)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun uploadProfileToCloudinary(imageUri: Uri) {
        Toast.makeText(requireContext(), "Uploading profile photo to Cloudinary...", Toast.LENGTH_SHORT).show()
        try {
            MediaManager.get().upload(imageUri)
                .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
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
                                    .error(R.drawable.user_icon)
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

        val inputName = dialogView.findViewById<EditText>(R.id.inputFullName)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSaveProfile)

        viewLifecycleOwner.lifecycleScope.launch {
            val user = authRepository.getFullUser()
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                inputName.setText(user.name.ifBlank { firebaseUser?.displayName ?: "" })
            } else {
                inputName.setText(firebaseUser?.displayName ?: "")
            }
        }

        btnSubmit.setOnClickListener {
            val name = inputName.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a valid name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val result = authRepository.updateUserProfile(name)
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    if (_binding != null) {
                        binding.profileName.text = name
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



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

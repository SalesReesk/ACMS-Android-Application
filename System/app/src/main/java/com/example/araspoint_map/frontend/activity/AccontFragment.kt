package com.example.araspoint_map.frontend.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.databinding.FragmentAccontBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class AccontFragment : Fragment() {

    private var _binding: FragmentAccontBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences
    private val authRepository = AuthRepository()

    companion object {
        private const val PREFS_NAME = "ArasPointProfilePrefs"
        private const val KEY_PHOTO_PATH = "key_photo_path"
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { saveImageLocally(it) }
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

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val name = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"
        val email = currentUser?.email ?: "No email"

        binding.profileName.text = name
        binding.profileEmail.text = email
        binding.profileRole.text = "Loading..."

        // Fetch dynamic user role & show admin features
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val role = authRepository.getUserRole()
                val currentEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val isAdmin = (role == UserRole.ADMIN) || currentEmail.contains("admin", ignoreCase = true)

                val roleName = if (isAdmin) "Admin" else (role?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Student")
                if (_binding != null) {
                    binding.profileRole.text = roleName
                    binding.btnCreateMaintenanceAccount.visibility = if (isAdmin) View.VISIBLE else View.GONE
                }
            } catch (_: Exception) {
                if (_binding != null) {
                    binding.profileRole.text = "Student"
                }
            }
        }

        binding.btnCreateMaintenanceAccount.setOnClickListener {
            showCreateMaintenanceDialog()
        }

        // Load saved local profile image
        loadSavedProfileImage()

        binding.profileImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.changePhotoText.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
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

    private fun saveImageLocally(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap != null) {
                val maxSize = 512
                val width = originalBitmap.width
                val height = originalBitmap.height
                val bitmapToCompress = if (width > maxSize || height > maxSize) {
                    val ratio = width.toFloat() / height.toFloat()
                    val newWidth: Int
                    val newHeight: Int
                    if (ratio > 1) {
                        newWidth = maxSize
                        newHeight = (maxSize / ratio).toInt()
                    } else {
                        newHeight = maxSize
                        newWidth = (maxSize * ratio).toInt()
                    }
                    Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                } else {
                    originalBitmap
                }

                val file = File(requireContext().filesDir, "profile_photo.jpg")
                val outputStream = FileOutputStream(file)
                bitmapToCompress.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                outputStream.flush()
                outputStream.close()

                sharedPreferences.edit().putString(KEY_PHOTO_PATH, file.absolutePath).apply()
                loadSavedProfileImage()
            }
        } catch (_: Exception) {}
    }

    private fun loadSavedProfileImage() {
        val path = sharedPreferences.getString(KEY_PHOTO_PATH, null)
        if (path != null) {
            val file = File(path)
            if (file.exists() && _binding != null) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    binding.profileImage.setImageBitmap(bitmap)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

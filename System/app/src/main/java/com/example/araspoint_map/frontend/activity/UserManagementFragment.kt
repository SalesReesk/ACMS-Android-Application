package com.example.araspoint_map.frontend.activity

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.databinding.FragmentUserManagementBinding
import kotlinx.coroutines.launch

class UserManagementFragment : Fragment() {

    private var _binding: FragmentUserManagementBinding? = null
    private val binding get() = _binding!!
    private val authRepository = AuthRepository()
    private var allUsers = listOf<User>()
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userAdapter = UserAdapter(
            users = emptyList(),
            onEditRoleClick = { user -> showChangeRoleDialog(user) },
            onDeleteClick = { user -> confirmDeleteUser(user) }
        )

        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        binding.btnAddUser.setOnClickListener {
            showAddUserDialog()
        }

        binding.searchUserEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadUsers()
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            allUsers = authRepository.getAllUsers()
            if (_binding != null) {
                binding.progressBar.visibility = View.GONE
                filterUsers(binding.searchUserEdit.text.toString())
            }
        }
    }

    private fun filterUsers(query: String) {
        val filtered = if (query.isBlank()) {
            allUsers
        } else {
            allUsers.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true) ||
                        it.role.name.contains(query, ignoreCase = true)
            }
        }
        userAdapter.updateList(filtered)
        binding.emptyStateText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showChangeRoleDialog(user: User) {
        val roles = UserRole.entries.toTypedArray()
        val roleNames = roles.map { it.name }.toTypedArray()
        var selectedRole = user.role

        AlertDialog.Builder(requireContext())
            .setTitle("Change Role for ${user.name.ifBlank { user.email }}")
            .setSingleChoiceItems(roleNames, roles.indexOf(user.role)) { _, which ->
                selectedRole = roles[which]
            }
            .setPositiveButton("Save") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = authRepository.updateUserRole(user.userId, selectedRole)
                    if (result.isSuccess) {
                        Toast.makeText(requireContext(), "Role updated to ${selectedRole.name}", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update role", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteUser(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name.ifBlank { user.email }}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = authRepository.deleteUser(user.userId)
                    if (result.isSuccess) {
                        Toast.makeText(requireContext(), "User deleted successfully", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete user", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_user, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val inputName = dialogView.findViewById<EditText>(R.id.inputUserName)
        val inputEmail = dialogView.findViewById<EditText>(R.id.inputUserEmail)
        val inputPassword = dialogView.findViewById<EditText>(R.id.inputUserPassword)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerUserRole)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmitCreateUser)

        val roles = UserRole.entries.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        btnSubmit.setOnClickListener {
            val name = inputName.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()
            val selectedRole = UserRole.entries[spinnerRole.selectedItemPosition]

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val result = authRepository.register(name, email, password, selectedRole)
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "User account created successfully for $name", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                    loadUsers()
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

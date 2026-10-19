package com.example.araspoint_map.frontend.activity

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.Asset
import com.example.araspoint_map.backend.model.Constants
import com.example.araspoint_map.backend.model.MaintenanceRecord
import com.example.araspoint_map.backend.model.enums.AssetCondition
import com.example.araspoint_map.backend.model.enums.AssetStatus
import com.example.araspoint_map.backend.model.enums.PriorityStatus
import com.example.araspoint_map.backend.model.enums.ReportStatus
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.repository.AssetRepository
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.backend.repository.MaintenanceRepository
import com.example.araspoint_map.databinding.DialogAssetDetailsBinding
import com.example.araspoint_map.databinding.DialogSubmitReportBinding
import com.example.araspoint_map.databinding.FragmentInventoryBinding
import com.example.araspoint_map.databinding.ItemInventoryBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var assetAdapter: AssetAdapter
    private val assetRepository = AssetRepository()
    private val authRepository = AuthRepository()
    private val maintenanceRepository = MaintenanceRepository()
    private var allAssets = listOf<Asset>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewInventory.layoutManager = LinearLayoutManager(requireContext())

        assetAdapter = AssetAdapter(emptyList()) { asset ->
            showAssetDetailsDialog(asset)
        }
        binding.recyclerViewInventory.adapter = assetAdapter

        val buildingAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Constants.FILTER_BUILDINGS)
        binding.filterBuilding.adapter = buildingAdapter

        binding.fabAddAsset.setOnClickListener {
            val intent = Intent(requireContext(), AddEditAssetActivity::class.java)
            startActivity(intent)
        }

        // Check role and hide FAB if student/read-only
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val role = authRepository.getUserRole()
                if (_binding != null) {
                    if (role == UserRole.STUDENT) {
                        binding.fabAddAsset.visibility = View.GONE
                    } else {
                        binding.fabAddAsset.visibility = View.VISIBLE
                    }
                }
            } catch (_: Exception) {}
        }

        binding.searchInventory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAssets()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.filterBuilding.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterAssets()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadAssets()
    }

    override fun onResume() {
        super.onResume()
        loadAssets()
    }

    private fun loadAssets() {
        if (_binding == null) return
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewInventory.visibility = View.GONE
        binding.emptyStateText.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = assetRepository.getAssets()
            if (_binding != null) {
                binding.progressBar.visibility = View.GONE
                if (result.isSuccess) {
                    allAssets = result.getOrNull() ?: emptyList()
                    filterAssets()
                } else {
                    allAssets = emptyList()
                    filterAssets()
                }
            }
        }
    }

    private fun showAssetDetailsDialog(asset: Asset) {
        val dialogBinding = DialogAssetDetailsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.detailAssetName.text = asset.assetName
        dialogBinding.detailAssetId.text = "ID: ${asset.assetId}"
        dialogBinding.detailAssetType.text = "Type: ${asset.assetType}"
        dialogBinding.detailAssetLocation.text = "Location: ${asset.location}"
        dialogBinding.detailAssetStatus.text = "Status: ${asset.status}"
        dialogBinding.detailAssetCondition.text = "Condition: ${asset.condition}"

        // Show Edit & Delete if Admin or Maintenance
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val role = authRepository.getUserRole()
                if (role == UserRole.ADMIN || role == UserRole.MAINTENANCE) {
                    dialogBinding.btnEditAsset.visibility = View.VISIBLE
                    dialogBinding.btnDeleteAsset.visibility = View.VISIBLE
                } else {
                    dialogBinding.btnEditAsset.visibility = View.GONE
                    dialogBinding.btnDeleteAsset.visibility = View.GONE
                }
            } catch (_: Exception) {}
        }

        dialogBinding.btnEditAsset.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(requireContext(), AddEditAssetActivity::class.java).apply {
                putExtra("EXTRA_ASSET_ID", asset.assetId)
                putExtra("EXTRA_ASSET_NAME", asset.assetName)
            }
            startActivity(intent)
        }

        dialogBinding.btnDeleteAsset.setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Asset")
                .setMessage("Are you sure you want to delete ${asset.assetName}?")
                .setPositiveButton("Delete") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val result = assetRepository.removeAsset(asset.assetId)
                        if (result.isSuccess) {
                            Toast.makeText(requireContext(), "Asset deleted successfully", Toast.LENGTH_SHORT).show()
                            loadAssets()
                        } else {
                            Toast.makeText(requireContext(), "Failed to delete asset", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialogBinding.btnReportThisAsset.setOnClickListener {
            dialog.dismiss()
            showSubmitReportDialog(
                assetId = asset.assetId,
                prefillTitle = "Issue with ${asset.assetName}",
                prefillLocation = asset.location
            )
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showSubmitReportDialog(assetId: String = "", prefillTitle: String = "", prefillLocation: String = "") {
        val dialogBinding = DialogSubmitReportBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val priorities = PriorityStatus.values().map { it.name }
        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities)
        dialogBinding.dialogReportPriority.adapter = priorityAdapter
        dialogBinding.dialogReportPriority.setSelection(priorities.indexOf(PriorityStatus.MEDIUM.name))

        if (prefillTitle.isNotEmpty()) dialogBinding.dialogReportTitle.setText(prefillTitle)
        if (prefillLocation.isNotEmpty()) dialogBinding.dialogReportLocation.setText(prefillLocation)

        dialogBinding.dialogBtnSubmitReport.setOnClickListener {
            val title = dialogBinding.dialogReportTitle.text.toString().trim()
            val location = dialogBinding.dialogReportLocation.text.toString().trim()
            val description = dialogBinding.dialogReportDescription.text.toString().trim()
            val priorityStr = dialogBinding.dialogReportPriority.selectedItem?.toString() ?: "MEDIUM"

            if (title.isEmpty() || location.isEmpty() || description.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val priority = try { PriorityStatus.valueOf(priorityStr) } catch (_: Exception) { PriorityStatus.MEDIUM }
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "student@campus.edu"

            val record = MaintenanceRecord(
                assetId = assetId,
                title = title,
                location = location,
                description = description,
                dateReported = dateStr,
                dateUpdated = "Pending Admin Review",
                status = ReportStatus.PENDING,
                priority = priority,
                reporterEmail = userEmail
            )

            viewLifecycleOwner.lifecycleScope.launch {
                val result = maintenanceRepository.addReport(record)
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "Report submitted to maintenance", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed to submit report", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun filterAssets() {
        if (_binding == null) return
        val query = binding.searchInventory.text.toString().lowercase().trim()
        val selectedBuilding = binding.filterBuilding.selectedItem?.toString() ?: "All Buildings"

        val filtered = allAssets.filter { asset ->
            val matchesQuery = query.isEmpty() ||
                    asset.assetName.lowercase().contains(query) ||
                    asset.assetType.lowercase().contains(query) ||
                    asset.location.lowercase().contains(query)

            val matchesBuilding = selectedBuilding == "All Buildings" ||
                    asset.location.startsWith(selectedBuilding, ignoreCase = true)

            matchesQuery && matchesBuilding
        }

        assetAdapter.updateAssets(filtered)
        if (filtered.isEmpty()) {
            binding.recyclerViewInventory.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE
        } else {
            binding.recyclerViewInventory.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
        }
    }

    class AssetAdapter(
        private var assets: List<Asset>,
        private val onItemClick: (Asset) -> Unit
    ) : RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

        fun updateAssets(newAssets: List<Asset>) {
            assets = newAssets
            notifyDataSetChanged()
        }

        class AssetViewHolder(val binding: ItemInventoryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
            val binding = ItemInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AssetViewHolder(binding)
        }

        override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
            val asset = assets[position]
            holder.binding.assetNameText.text = asset.assetName
            holder.binding.assetTypeText.text = "Type: ${asset.assetType}"
            holder.binding.assetLocationText.text = "Location: ${asset.location}"

            holder.binding.assetStatusText.text = "Status: ${asset.status}"
            when (asset.status) {
                AssetStatus.AVAILABLE -> holder.binding.assetStatusText.setTextColor(Color.parseColor("#2E7D32"))
                AssetStatus.IN_USE -> holder.binding.assetStatusText.setTextColor(Color.parseColor("#1565C0"))
                AssetStatus.MISSING -> holder.binding.assetStatusText.setTextColor(Color.parseColor("#EF6C00"))
                AssetStatus.UNDER_MAINTENANCE -> holder.binding.assetStatusText.setTextColor(Color.parseColor("#8E24AA"))
                AssetStatus.DISPOSED -> holder.binding.assetStatusText.setTextColor(Color.parseColor("#C62828"))
            }

            holder.binding.assetConditionText.text = "Condition: ${asset.condition}"
            when (asset.condition) {
                AssetCondition.EXCELLENT -> holder.binding.assetConditionText.setTextColor(Color.parseColor("#00C853"))
                AssetCondition.GOOD -> holder.binding.assetConditionText.setTextColor(Color.parseColor("#4CAF50"))
                AssetCondition.FAIR -> holder.binding.assetConditionText.setTextColor(Color.parseColor("#FFB300"))
                AssetCondition.POOR -> holder.binding.assetConditionText.setTextColor(Color.parseColor("#FF9800"))
                AssetCondition.DAMAGED -> holder.binding.assetConditionText.setTextColor(Color.parseColor("#F44336"))
            }

            holder.itemView.setOnClickListener {
                onItemClick(asset)
            }
        }

        override fun getItemCount(): Int = assets.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.araspoint_map.frontend.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
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
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var assetAdapter: AssetAdapter
    private val assetRepository = AssetRepository()
    private val authRepository = AuthRepository()
    private val maintenanceRepository = MaintenanceRepository()
    private var allAssets = listOf<Asset>()
    private var selectedStatusFilter = "ALL"

    private var capturedPhotoUrl = ""
    private var activePhotoStatusText: TextView? = null
    private var tempCameraUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            startCrop(tempCameraUri!!)
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = File(requireContext().cacheDir, "camera_inv_report_${System.currentTimeMillis()}.jpg")
            tempCameraUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
            takePictureLauncher.launch(tempCameraUri!!)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { startCrop(it) }
    }

    private val cropActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                uploadEvidenceToCloudinary(resultUri)
            }
        }
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_inv_report_${System.currentTimeMillis()}.jpg"))
        val intent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(4f, 3f)
            .withMaxResultSize(1200, 900)
            .getIntent(requireContext())
        cropActivityResultLauncher.launch(intent)
    }

    private fun uploadEvidenceToCloudinary(imageUri: Uri) {
        activePhotoStatusText?.text = "Uploading evidence photo..."
        activePhotoStatusText?.setTextColor(Color.parseColor("#EF6C00"))
        Toast.makeText(requireContext(), "Uploading evidence to Cloudinary...", Toast.LENGTH_SHORT).show()

        MediaManager.get().upload(imageUri)
            .unsigned("CampusAssets")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val secureUrl = resultData["secure_url"] as? String ?: ""
                    capturedPhotoUrl = secureUrl
                    activity?.runOnUiThread {
                        activePhotoStatusText?.text = "Evidence photo verified ✓"
                        activePhotoStatusText?.setTextColor(Color.parseColor("#2E7D32"))
                        Toast.makeText(requireContext(), "Evidence photo verified successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    activity?.runOnUiThread {
                        activePhotoStatusText?.text = "Upload failed"
                        activePhotoStatusText?.setTextColor(Color.parseColor("#C62828"))
                        Toast.makeText(requireContext(), "Upload failed: ${error.description}", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("Take Photo with Camera", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Capture Evidence Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    1 -> {
                        try {
                            pickImageLauncher.launch("image/*")
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Could not open gallery: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

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
                if (s.isNullOrEmpty()) {
                    binding.btnClearSearch.visibility = View.GONE
                } else {
                    binding.btnClearSearch.visibility = View.VISIBLE
                }
                filterAssets()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.searchInventory.text?.clear()
        }

        binding.btnScanBarcode.setOnClickListener {
            Toast.makeText(requireContext(), "Barcode / QR Scanner ready for camera feed", Toast.LENGTH_SHORT).show()
        }

        binding.chipFilterAll.setOnClickListener {
            selectedStatusFilter = "ALL"
            updateChipStyles(binding.chipFilterAll)
            filterAssets()
        }
        binding.chipFilterAvailable.setOnClickListener {
            selectedStatusFilter = "AVAILABLE"
            updateChipStyles(binding.chipFilterAvailable)
            filterAssets()
        }
        binding.chipFilterMaintenance.setOnClickListener {
            selectedStatusFilter = "UNDER_MAINTENANCE"
            updateChipStyles(binding.chipFilterMaintenance)
            filterAssets()
        }
        binding.chipFilterDamaged.setOnClickListener {
            selectedStatusFilter = "DISPOSED"
            updateChipStyles(binding.chipFilterDamaged)
            filterAssets()
        }

        binding.filterBuilding.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterAssets()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadAssets()
    }

    private fun updateChipStyles(selectedChip: TextView) {
        val chips = listOf(
            binding.chipFilterAll,
            binding.chipFilterAvailable,
            binding.chipFilterMaintenance,
            binding.chipFilterDamaged
        )
        for (chip in chips) {
            if (chip == selectedChip) {
                chip.setBackgroundResource(R.drawable.bg_button_primary)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary))
            } else {
                chip.setBackgroundResource(R.drawable.bg_badge_institution)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
            }
        }
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
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.emptyStateText.text = "Failed to load assets: ${result.exceptionOrNull()?.message}"
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

        if (!asset.photo.isNullOrEmpty()) {
            dialogBinding.detailAssetImage.visibility = View.VISIBLE
            com.bumptech.glide.Glide.with(requireContext())
                .load(asset.photo)
                .placeholder(R.drawable.ic_inventory_box)
                .into(dialogBinding.detailAssetImage)
        } else {
            dialogBinding.detailAssetImage.visibility = View.GONE
        }

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
                putExtra("EXTRA_ASSET_TYPE", asset.assetType)
                putExtra("EXTRA_ASSET_LOCATION", asset.location)
                putExtra("EXTRA_ASSET_STATUS", asset.status.name)
                putExtra("EXTRA_ASSET_CONDITION", asset.condition.name)
                putExtra("EXTRA_ASSET_PHOTO", asset.photo)
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
            showReportIssueDialog(asset)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showReportIssueDialog(asset: Asset) {
        capturedPhotoUrl = ""
        val dialogBinding = DialogSubmitReportBinding.inflate(layoutInflater)
        activePhotoStatusText = dialogBinding.dialogTxtPhotoStatus

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.dialogReportTitle.setText("Issue: ${asset.assetName}")
        dialogBinding.dialogReportLocation.setText(asset.location)

        val priorities = arrayOf("LOW", "MEDIUM", "HIGH", "CRITICAL")
        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities)
        dialogBinding.dialogReportPriority.adapter = priorityAdapter
        dialogBinding.dialogReportPriority.setSelection(1) // Default to MEDIUM

        dialogBinding.dialogBtnCapturePhoto.setOnClickListener {
            showPhotoSourceDialog()
        }

        dialogBinding.dialogBtnSubmitReport.setOnClickListener {
            val title = dialogBinding.dialogReportTitle.text.toString().trim()
            val location = dialogBinding.dialogReportLocation.text.toString().trim()
            val description = dialogBinding.dialogReportDescription.text.toString().trim()
            val priorityStr = dialogBinding.dialogReportPriority.selectedItem.toString()

            if (title.isEmpty()) {
                dialogBinding.dialogReportTitle.error = "Title required"
                return@setOnClickListener
            }
            if (location.isEmpty()) {
                dialogBinding.dialogReportLocation.error = "Location required"
                return@setOnClickListener
            }
            if (description.isEmpty()) {
                dialogBinding.dialogReportDescription.error = "Description required"
                return@setOnClickListener
            }

            val priority = try {
                PriorityStatus.valueOf(priorityStr)
            } catch (_: Exception) {
                PriorityStatus.MEDIUM
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            val userEmail = currentUser?.email ?: "Anonymous"
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

            val generatedId = if (asset.assetId.isNotEmpty()) "REP-${System.currentTimeMillis()}-${(1000..9999).random()}" else UUID.randomUUID().toString()
            val record = MaintenanceRecord(
                reportId = generatedId,
                assetId = asset.assetId,
                title = title,
                description = description,
                location = location,
                assignedTo = "Unassigned",
                dateReported = nowStr,
                dateUpdated = "Pending Admin Review",
                status = ReportStatus.PENDING,
                priority = priority,
                reporterEmail = userEmail,
                photo = capturedPhotoUrl
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
                    asset.location.lowercase().contains(query) ||
                    asset.assetId.lowercase().contains(query)

            val matchesBuilding = selectedBuilding == "All Buildings" ||
                    asset.location.startsWith(selectedBuilding, ignoreCase = true)

            val matchesStatus = when (selectedStatusFilter) {
                "AVAILABLE" -> asset.status == AssetStatus.AVAILABLE
                "UNDER_MAINTENANCE" -> asset.status == AssetStatus.UNDER_MAINTENANCE || asset.status == AssetStatus.MISSING
                "DISPOSED" -> asset.status == AssetStatus.DISPOSED
                else -> true
            }

            matchesQuery && matchesBuilding && matchesStatus
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

package com.example.araspoint_map.frontend.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.araspoint_map.BuildConfig
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.MaintenanceRecord
import com.example.araspoint_map.backend.model.enums.AssetCondition
import com.example.araspoint_map.backend.model.enums.AssetStatus
import com.example.araspoint_map.backend.model.enums.PriorityStatus
import com.example.araspoint_map.backend.model.enums.ReportStatus
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.repository.AssetRepository
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.databinding.DialogAssignMaintenanceBinding
import com.example.araspoint_map.databinding.DialogReportStatusDetailsBinding
import com.example.araspoint_map.databinding.DialogSubmitReportBinding
import com.example.araspoint_map.databinding.FragmentReportBinding
import com.example.araspoint_map.databinding.ItemReportBinding
import com.example.araspoint_map.frontend.viewModel.MaintenanceViewModel
import com.google.firebase.auth.FirebaseAuth
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private val maintenanceViewModel: MaintenanceViewModel by viewModels()
    private val authRepository = AuthRepository()
    private val assetRepository = AssetRepository()
    private lateinit var reportAdapter: ReportAdapter

    private var currentUserRole: UserRole? = null
    private var baseReportsList = listOf<MaintenanceRecord>()
    private var selectedStatusFilter = "ALL"
    private var searchQuery = ""

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
            val photoFile = File(requireContext().cacheDir, "camera_report_${System.currentTimeMillis()}.jpg")
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
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_report_${System.currentTimeMillis()}.jpg"))
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
            .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val secureUrl = resultData["secure_url"] as? String ?: ""
                    capturedPhotoUrl = secureUrl
                    activity?.runOnUiThread {
                        activePhotoStatusText?.text = "Evidence photo uploaded ✓"
                        activePhotoStatusText?.setTextColor(Color.parseColor("#2E7D32"))
                        Toast.makeText(requireContext(), "Evidence photo uploaded successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    activity?.runOnUiThread {
                        activePhotoStatusText?.text = "Upload failed"
                        activePhotoStatusText?.setTextColor(Color.parseColor("#C62828"))
                        Toast.makeText(requireContext(), "Cloudinary upload failed: ${error.description}", Toast.LENGTH_LONG).show()
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
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reportAdapter = ReportAdapter(emptyList(), false,
            onItemClick = { record -> showReportStatusDetailsDialog(record) },
            onManageClick = { record -> handleManageReportClick(record) }
        )

        binding.recyclerViewReports.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewReports.adapter = reportAdapter

        binding.fabSubmitReport.setOnClickListener {
            showNewIssueDialog()
        }

        binding.searchReportInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                filterAndDisplayReports()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val chips = listOf(
            Triple(binding.chipFilterAll, "ALL", "All"),
            Triple(binding.chipFilterPending, "PENDING", "Pending"),
            Triple(binding.chipFilterInProgress, "IN_PROGRESS", "In Progress"),
            Triple(binding.chipFilterResolved, "RESOLVED", "Resolved")
        )

        chips.forEach { (view, statusKey, _) ->
            view.setOnClickListener {
                selectedStatusFilter = statusKey
                updateChipStyles()
                filterAndDisplayReports()
            }
        }
        updateChipStyles()

        viewLifecycleOwner.lifecycleScope.launch {
            maintenanceViewModel.reports.collectLatest { reports ->
                if (_binding != null) {
                    if (currentUserRole == null) {
                        currentUserRole = authRepository.getUserRole()
                    }

                    val currentFbUser = FirebaseAuth.getInstance().currentUser
                    val currentEmail = currentFbUser?.email ?: ""
                    val currentName = currentFbUser?.displayName ?: ""

                    val effectiveRole = if (currentEmail.contains("admin", ignoreCase = true)) {
                        UserRole.ADMIN
                    } else {
                        currentUserRole ?: UserRole.STUDENT
                    }
                    currentUserRole = effectiveRole

                    baseReportsList = when (effectiveRole) {
                        UserRole.ADMIN -> reports
                        UserRole.MAINTENANCE -> reports.filter {
                            (it.status == ReportStatus.IN_PROGRESS || it.status == ReportStatus.RESOLVED) &&
                                    (it.assignedTo.isEmpty() || it.assignedTo.contains(currentEmail, ignoreCase = true) || it.assignedTo.contains(currentName, ignoreCase = true))
                        }
                        else -> reports.filter { it.reporterEmail.equals(currentEmail, ignoreCase = true) }
                    }

                    updateChipCounts(baseReportsList)
                    filterAndDisplayReports()
                }
            }
        }

        loadReports()
    }

    private fun updateChipStyles() {
        if (_binding == null) return
        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_button_primary)
        val inactiveBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_institution)
        val activeTextColor = ContextCompat.getColor(requireContext(), R.color.on_primary)
        val inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.on_surface_variant)

        val chips = listOf(
            Pair(binding.chipFilterAll, selectedStatusFilter == "ALL"),
            Pair(binding.chipFilterPending, selectedStatusFilter == "PENDING"),
            Pair(binding.chipFilterInProgress, selectedStatusFilter == "IN_PROGRESS"),
            Pair(binding.chipFilterResolved, selectedStatusFilter == "RESOLVED")
        )

        chips.forEach { (chip, isActive) ->
            chip.background = if (isActive) activeBg else inactiveBg
            chip.setTextColor(if (isActive) activeTextColor else inactiveTextColor)
        }
    }

    private fun loadReports() {
        if (_binding == null) return
        maintenanceViewModel.loadReports()
    }

    private fun updateChipCounts(list: List<MaintenanceRecord>) {
        if (_binding == null) return
        val allCount = list.size
        val pendingCount = list.count { it.status == ReportStatus.PENDING }
        val inProgressCount = list.count { it.status == ReportStatus.IN_PROGRESS }
        val resolvedCount = list.count { it.status == ReportStatus.RESOLVED }

        binding.chipFilterAll.text = "All ($allCount)"
        binding.chipFilterPending.text = "Pending ($pendingCount)"
        binding.chipFilterInProgress.text = "In Progress ($inProgressCount)"
        binding.chipFilterResolved.text = "Resolved ($resolvedCount)"
    }



    private fun filterAndDisplayReports() {
        if (_binding == null) return
        var filtered = when (selectedStatusFilter) {
            "PENDING" -> baseReportsList.filter { it.status == ReportStatus.PENDING }
            "IN_PROGRESS" -> baseReportsList.filter { it.status == ReportStatus.IN_PROGRESS }
            "RESOLVED" -> baseReportsList.filter { it.status == ReportStatus.RESOLVED }
            else -> baseReportsList
        }

        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true) ||
                        it.location.contains(searchQuery, ignoreCase = true) ||
                        it.reportId.contains(searchQuery, ignoreCase = true) ||
                        it.assetId.contains(searchQuery, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerViewReports.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerViewReports.visibility = View.VISIBLE
        }

        val showManage = (currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.MAINTENANCE)
        reportAdapter.updateReports(filtered, showManage)
    }

    private fun showNewIssueDialog() {
        capturedPhotoUrl = ""
        val dialogBinding = DialogSubmitReportBinding.inflate(layoutInflater)
        activePhotoStatusText = dialogBinding.dialogTxtPhotoStatus

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val priorities = arrayOf("LOW", "MEDIUM", "HIGH", "CRITICAL")
        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities)
        dialogBinding.dialogReportPriority.adapter = priorityAdapter
        dialogBinding.dialogReportPriority.setSelection(0)

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
            if (capturedPhotoUrl.isEmpty()) {
                Toast.makeText(requireContext(), "An evidence photo captured via camera is required to submit a report.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val priority = try {
                PriorityStatus.valueOf(priorityStr)
            } catch (_: Exception) {
                PriorityStatus.MEDIUM
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            val userEmail = currentUser?.email ?: "student@batstate-u.edu.ph"
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val generatedId = "REP-${System.currentTimeMillis()}-${(1000..9999).random()}"

            val record = MaintenanceRecord(
                reportId = generatedId,
                assetId = "GENERIC",
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

            maintenanceViewModel.addReport(record)
            Toast.makeText(requireContext(), "Ticket submitted successfully!", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showReportStatusDetailsDialog(record: MaintenanceRecord) {
        val dialogBinding = DialogReportStatusDetailsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.detailReportTitle.text = record.title.ifEmpty { "Maintenance Report" }
        dialogBinding.detailReportLocation.text = "Location: ${record.location}"
        dialogBinding.detailReportDescription.text = record.description

        if (record.photo.isNotEmpty()) {
            dialogBinding.cardReportImage.visibility = View.VISIBLE
            Glide.with(requireContext())
                .load(record.photo)
                .placeholder(R.drawable.ic_inventory_box)
                .into(dialogBinding.detailReportImage)
        } else {
            dialogBinding.cardReportImage.visibility = View.GONE
        }

        dialogBinding.detailReportStatus.text = "Status: ${record.status}"

        when (record.status) {
            ReportStatus.PENDING -> dialogBinding.detailReportStatus.setTextColor(Color.parseColor("#EF6C00"))
            ReportStatus.IN_PROGRESS -> dialogBinding.detailReportStatus.setTextColor(Color.parseColor("#1565C0"))
            ReportStatus.RESOLVED -> dialogBinding.detailReportStatus.setTextColor(Color.parseColor("#2E7D32"))
            ReportStatus.REJECTED -> dialogBinding.detailReportStatus.setTextColor(Color.parseColor("#C62828"))
        }

        dialogBinding.detailReportPriority.text = "Priority: ${record.priority}"
        when (record.priority) {
            PriorityStatus.LOW -> dialogBinding.detailReportPriority.setTextColor(Color.parseColor("#1565C0"))
            PriorityStatus.MEDIUM -> dialogBinding.detailReportPriority.setTextColor(Color.parseColor("#FFB300"))
            PriorityStatus.HIGH -> dialogBinding.detailReportPriority.setTextColor(Color.parseColor("#EF6C00"))
            PriorityStatus.CRITICAL -> dialogBinding.detailReportPriority.setTextColor(Color.parseColor("#C62828"))
        }

        dialogBinding.detailReportAssignedTo.text = "Assigned To: ${record.assignedTo.ifEmpty { "Unassigned (Pending Admin)" }}"
        dialogBinding.detailDateReported.text = "Reported on: ${record.dateReported.ifEmpty { "N/A" }}"
        dialogBinding.detailDateUpdated.text = "Last Updated: ${record.dateUpdated.ifEmpty { "Pending Admin Review" }}"

        dialogBinding.btnCloseReportDetails.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDeleteReportTicket.visibility = if (currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.MAINTENANCE) {
            View.VISIBLE
        } else {
            View.GONE
        }

        dialogBinding.btnDeleteReportTicket.setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Report Ticket")
                .setMessage("Are you sure you want to delete this report ticket?")
                .setPositiveButton("Delete") { _, _ ->
                    maintenanceViewModel.deleteReport(record.reportId)
                    Toast.makeText(requireContext(), "Report ticket deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private suspend fun updateAssetStatus(assetId: String, reportStatus: ReportStatus) {
        if (assetId.isEmpty() || assetId == "GENERIC") return
        val assetResult = assetRepository.findAsset(assetId)
        if (assetResult.isSuccess) {
            val asset = assetResult.getOrNull()
            if (asset != null) {
                when (reportStatus) {
                    ReportStatus.IN_PROGRESS -> {
                        asset.status = AssetStatus.UNDER_MAINTENANCE
                        asset.condition = AssetCondition.FAIR
                    }
                    ReportStatus.RESOLVED -> {
                        asset.status = AssetStatus.AVAILABLE
                        asset.condition = AssetCondition.GOOD
                    }
                    ReportStatus.REJECTED -> {
                        asset.status = AssetStatus.AVAILABLE
                    }
                    ReportStatus.PENDING -> {}
                }
                assetRepository.updateAsset(asset)
            }
        }
    }

    private fun handleManageReportClick(record: MaintenanceRecord) {
        if (record.status == ReportStatus.PENDING && (currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.MAINTENANCE)) {
            showAssignMaintenanceDialog(record)
        } else {
            val nextStatus = when (record.status) {
                ReportStatus.PENDING -> ReportStatus.IN_PROGRESS
                ReportStatus.IN_PROGRESS -> ReportStatus.RESOLVED
                ReportStatus.RESOLVED -> ReportStatus.REJECTED
                ReportStatus.REJECTED -> ReportStatus.PENDING
            }

            record.status = nextStatus
            record.dateUpdated = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

            maintenanceViewModel.updateReport(record)
            viewLifecycleOwner.lifecycleScope.launch {
                updateAssetStatus(record.assetId, nextStatus)
            }
            Toast.makeText(requireContext(), "Status updated to: $nextStatus", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAssignMaintenanceDialog(record: MaintenanceRecord) {
        val dialogBinding = DialogAssignMaintenanceBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        viewLifecycleOwner.lifecycleScope.launch {
            val maintenanceUsers = authRepository.getMaintenanceUsers()
            val staffNames = if (maintenanceUsers.isNotEmpty()) {
                maintenanceUsers.map { it.name.ifEmpty { it.email } }
            } else {
                listOf("Engr. Danilo Mendoza", "Tech. Roberto Cruz", "Campus Electrician", "General Maintenance Team")
            }

            val staffAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, staffNames)
            dialogBinding.spinnerAssignStaff.adapter = staffAdapter

            val priorities = arrayOf("LOW", "MEDIUM", "HIGH", "CRITICAL")
            val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities)
            dialogBinding.spinnerAssignPriority.adapter = priorityAdapter
            val currentIndex = priorities.indexOf(record.priority.name)
            if (currentIndex >= 0) dialogBinding.spinnerAssignPriority.setSelection(currentIndex)

            dialogBinding.btnConfirmAssignment.setOnClickListener {
                val selectedStaff = dialogBinding.spinnerAssignStaff.selectedItem?.toString() ?: "General Maintenance Team"
                val selectedPriorityStr = dialogBinding.spinnerAssignPriority.selectedItem?.toString() ?: record.priority.name
                val finalPriority = try { PriorityStatus.valueOf(selectedPriorityStr) } catch (_: Exception) { record.priority }

                record.assignedTo = selectedStaff
                record.priority = finalPriority
                record.status = ReportStatus.IN_PROGRESS
                record.dateUpdated = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

                maintenanceViewModel.updateReport(record)
                viewLifecycleOwner.lifecycleScope.launch {
                    updateAssetStatus(record.assetId, ReportStatus.IN_PROGRESS)
                }
                Toast.makeText(requireContext(), "Assigned to $selectedStaff ($finalPriority Priority)", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    class ReportAdapter(
        private var reports: List<MaintenanceRecord>,
        private var showManageButton: Boolean,
        private val onItemClick: (MaintenanceRecord) -> Unit,
        private val onManageClick: (MaintenanceRecord) -> Unit
    ) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

        fun updateReports(newReports: List<MaintenanceRecord>, manage: Boolean) {
            reports = newReports
            showManageButton = manage
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
            val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ReportViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
            val record = reports[position]
            holder.bind(record, showManageButton, onItemClick, onManageClick)
        }

        override fun getItemCount() = reports.size

        class ReportViewHolder(private val binding: ItemReportBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(
                record: MaintenanceRecord,
                showManage: Boolean,
                onItemClick: (MaintenanceRecord) -> Unit,
                onManageClick: (MaintenanceRecord) -> Unit
            ) {
                binding.reportTitle.text = record.title.ifEmpty { "Maintenance Report" }
                binding.reportLocation.text = record.location
                binding.reportDateText.text = record.dateReported
                binding.reportStatus.text = record.status.name

                when (record.status) {
                    ReportStatus.PENDING -> binding.reportStatus.setTextColor(Color.parseColor("#EF6C00"))
                    ReportStatus.IN_PROGRESS -> binding.reportStatus.setTextColor(Color.parseColor("#1565C0"))
                    ReportStatus.RESOLVED -> binding.reportStatus.setTextColor(Color.parseColor("#2E7D32"))
                    ReportStatus.REJECTED -> binding.reportStatus.setTextColor(Color.parseColor("#C62828"))
                }

                binding.btnActionManage.visibility = if (showManage) View.VISIBLE else View.GONE
                if (showManage) {
                    binding.btnActionManage.text = if (record.status == ReportStatus.PENDING) "Assign" else "Advance"
                }

                binding.root.setOnClickListener { onItemClick(record) }
                binding.btnActionManage.setOnClickListener { onManageClick(record) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

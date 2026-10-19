package com.example.araspoint_map.frontend.activity

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
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
import com.example.araspoint_map.backend.model.MaintenanceRecord
import com.example.araspoint_map.backend.model.enums.AssetCondition
import com.example.araspoint_map.backend.model.enums.AssetStatus
import com.example.araspoint_map.backend.model.enums.PriorityStatus
import com.example.araspoint_map.backend.model.enums.ReportStatus
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.repository.AssetRepository
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.backend.repository.MaintenanceRepository
import com.example.araspoint_map.databinding.DialogAssignMaintenanceBinding
import com.example.araspoint_map.databinding.DialogReportStatusDetailsBinding
import com.example.araspoint_map.databinding.FragmentReportBinding
import com.example.araspoint_map.databinding.ItemReportBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private val maintenanceRepository = MaintenanceRepository()
    private val authRepository = AuthRepository()
    private val assetRepository = AssetRepository()
    private lateinit var reportAdapter: ReportAdapter

    private var currentUserRole: UserRole? = UserRole.STUDENT
    private var baseReportsList = listOf<MaintenanceRecord>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewReports.layoutManager = LinearLayoutManager(requireContext())
        
        reportAdapter = ReportAdapter(emptyList(), false, { record ->
            showReportStatusDetailsDialog(record)
        }, { record ->
            handleManageReportClick(record)
        })
        binding.recyclerViewReports.adapter = reportAdapter

        // Setup Priority Filter Spinner
        val priorityOptions = arrayOf("All Priorities", "CRITICAL", "HIGH", "MEDIUM", "LOW")
        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorityOptions)
        binding.spinnerPriorityFilter.adapter = priorityAdapter

        binding.spinnerPriorityFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyPriorityFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Check user role
        viewLifecycleOwner.lifecycleScope.launch {
            currentUserRole = authRepository.getUserRole()
            if (_binding != null) {
                if (currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.MAINTENANCE) {
                    binding.maintenanceSummaryLayout.visibility = View.VISIBLE
                } else {
                    binding.maintenanceSummaryLayout.visibility = View.GONE
                }
                loadReports()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadReports()
    }

    private fun loadReports() {
        if (_binding == null) return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = maintenanceRepository.getReports()
            if (_binding != null && result.isSuccess) {
                val reports = result.getOrNull() ?: emptyList()
                val myEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val myName = FirebaseAuth.getInstance().currentUser?.displayName ?: ""

                baseReportsList = when (currentUserRole) {
                    UserRole.ADMIN -> reports
                    UserRole.MAINTENANCE -> reports.filter {
                        (it.status == ReportStatus.IN_PROGRESS || it.status == ReportStatus.RESOLVED) &&
                                (it.assignedTo.isEmpty() || it.assignedTo.contains(myEmail, ignoreCase = true) || it.assignedTo.contains(myName, ignoreCase = true))
                    }
                    else -> reports.filter { it.reporterEmail == myEmail }
                }

                updateSummaryBoard(baseReportsList)
                applyPriorityFilter()
            } else if (_binding != null) {
                baseReportsList = emptyList()
                updateSummaryBoard(emptyList())
                reportAdapter.updateReports(emptyList(), currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.MAINTENANCE)
            }
        }
    }

    private fun updateSummaryBoard(list: List<MaintenanceRecord>) {
        if (_binding == null) return
        val total = list.size
        val inProgress = list.count { it.status == ReportStatus.IN_PROGRESS }
        val criticalOrHigh = list.count { it.priority == PriorityStatus.CRITICAL || it.priority == PriorityStatus.HIGH }

        binding.txtSummaryTotal.text = total.toString()
        binding.txtSummaryInProgress.text = inProgress.toString()
        binding.txtSummaryCritical.text = criticalOrHigh.toString()
    }

    private fun applyPriorityFilter() {
        if (_binding == null) return
        val selectedPriority = binding.spinnerPriorityFilter.selectedItem?.toString() ?: "All Priorities"
        val filteredList = if (selectedPriority == "All Priorities") {
            baseReportsList
        } else {
            baseReportsList.filter { it.priority.name == selectedPriority }
        }

        val showManage = (currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.MAINTENANCE)
        reportAdapter.updateReports(filteredList, showManage)
    }

    private fun showReportStatusDetailsDialog(record: MaintenanceRecord) {
        val dialogBinding = DialogReportStatusDetailsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.detailReportTitle.text = record.title.ifEmpty { "Maintenance Report" }
        dialogBinding.detailReportLocation.text = "Location: ${record.location}"
        dialogBinding.detailReportDescription.text = record.description
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

        dialogBinding.btnDeleteReportTicket.setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Report Ticket")
                .setMessage("Are you sure you want to delete this report ticket?")
                .setPositiveButton("Delete") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val result = maintenanceRepository.deleteReport(record.reportId)
                        if (result.isSuccess) {
                            Toast.makeText(requireContext(), "Report ticket deleted successfully", Toast.LENGTH_SHORT).show()
                            loadReports()
                        } else {
                            Toast.makeText(requireContext(), "Failed to delete report ticket", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private suspend fun updateAssetStatus(assetId: String, reportStatus: ReportStatus) {
        if (assetId.isEmpty()) return
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
        if (record.status == ReportStatus.PENDING && currentUserRole == UserRole.ADMIN) {
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

            viewLifecycleOwner.lifecycleScope.launch {
                maintenanceRepository.updateReport(record)
                updateAssetStatus(record.assetId, nextStatus)
                loadReports()
            }
        }
    }

    private fun showAssignMaintenanceDialog(record: MaintenanceRecord) {
        val dialogBinding = DialogAssignMaintenanceBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val priorities = PriorityStatus.values().map { it.name }
        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities)
        dialogBinding.spinnerAssignPriority.adapter = priorityAdapter
        val initialPriorityIndex = priorities.indexOf(record.priority.name).let { if (it >= 0) it else priorities.indexOf(PriorityStatus.MEDIUM.name) }
        dialogBinding.spinnerAssignPriority.setSelection(initialPriorityIndex)

        viewLifecycleOwner.lifecycleScope.launch {
            val maintUsers = authRepository.getMaintenanceUsers()
            val staffOptions = if (maintUsers.isNotEmpty()) {
                maintUsers.map { it.name.ifEmpty { it.email.substringBefore("@") } }
            } else {
                listOf("Maintenance Staff 1", "Maintenance Staff 2", "General Maintenance Team")
            }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, staffOptions)
            dialogBinding.spinnerAssignStaff.adapter = adapter

            dialogBinding.btnConfirmAssignment.setOnClickListener {
                val selectedStaff = dialogBinding.spinnerAssignStaff.selectedItem?.toString() ?: "General Maintenance Team"
                val selectedPriorityStr = dialogBinding.spinnerAssignPriority.selectedItem?.toString() ?: record.priority.name
                val finalPriority = try { PriorityStatus.valueOf(selectedPriorityStr) } catch (_: Exception) { record.priority }

                record.assignedTo = selectedStaff
                record.priority = finalPriority
                record.status = ReportStatus.IN_PROGRESS
                record.dateUpdated = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

                viewLifecycleOwner.lifecycleScope.launch {
                    maintenanceRepository.updateReport(record)
                    updateAssetStatus(record.assetId, ReportStatus.IN_PROGRESS)
                    Toast.makeText(requireContext(), "Assigned to $selectedStaff ($finalPriority Priority)", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                    loadReports()
                }
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

        class ReportViewHolder(val binding: ItemReportBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
            val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ReportViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
            val record = reports[position]
            holder.binding.reportTitle.text = record.title.ifEmpty { "Maintenance Report" }
            holder.binding.reportLocation.text = "Location: ${record.location}"
            holder.binding.reportAssignedTo.text = "Assigned: ${record.assignedTo.ifEmpty { "Pending Admin" }}"
            holder.binding.reportDescription.text = record.description
            holder.binding.reportStatus.text = "Status: ${record.status}"

            when (record.status) {
                ReportStatus.PENDING -> holder.binding.reportStatus.setTextColor(Color.parseColor("#EF6C00"))
                ReportStatus.IN_PROGRESS -> holder.binding.reportStatus.setTextColor(Color.parseColor("#1565C0"))
                ReportStatus.RESOLVED -> holder.binding.reportStatus.setTextColor(Color.parseColor("#2E7D32"))
                ReportStatus.REJECTED -> holder.binding.reportStatus.setTextColor(Color.parseColor("#C62828"))
            }

            holder.binding.reportPriority.text = "Priority: ${record.priority}"
            when (record.priority) {
                PriorityStatus.LOW -> holder.binding.reportPriority.setTextColor(Color.parseColor("#1565C0"))
                PriorityStatus.MEDIUM -> holder.binding.reportPriority.setTextColor(Color.parseColor("#FFB300"))
                PriorityStatus.HIGH -> holder.binding.reportPriority.setTextColor(Color.parseColor("#EF6C00"))
                PriorityStatus.CRITICAL -> holder.binding.reportPriority.setTextColor(Color.parseColor("#C62828"))
            }

            holder.itemView.setOnClickListener {
                onItemClick(record)
            }

            if (showManageButton) {
                holder.binding.btnActionManage.visibility = View.VISIBLE
                holder.binding.btnActionManage.text = when (record.status) {
                    ReportStatus.PENDING -> "Pass to Maintenance"
                    ReportStatus.IN_PROGRESS -> "Mark Resolved"
                    ReportStatus.RESOLVED -> "Mark Rejected"
                    ReportStatus.REJECTED -> "Reopen"
                }
                holder.binding.btnActionManage.setOnClickListener {
                    onManageClick(record)
                }
            } else {
                holder.binding.btnActionManage.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = reports.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

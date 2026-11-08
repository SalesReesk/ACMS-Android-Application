package com.example.araspoint_map.frontend.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.Asset
import com.example.araspoint_map.backend.repository.AssetRepository
import com.example.araspoint_map.databinding.DialogAssetDetailsBinding
import com.example.araspoint_map.databinding.DialogBuildingAssetsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BuildingAssetsBottomSheetDialogFragment(
    private val buildingTitle: String,
    private val buildingSubtitle: String
) : BottomSheetDialogFragment() {

    private var _binding: DialogBuildingAssetsBinding? = null
    private val binding get() = _binding!!
    private val assetRepository = AssetRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBuildingAssetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtSheetBuildingTitle.text = buildingTitle
        binding.txtSheetBuildingSubtitle.text = buildingSubtitle

        binding.btnCloseSheet.setOnClickListener {
            dismiss()
        }

        binding.rvBuildingAssets.layoutManager = LinearLayoutManager(requireContext())

        loadAndGroupAssetsByFloorAndRoom()
    }

    private fun loadAndGroupAssetsByFloorAndRoom() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = assetRepository.getAssets()
            val allAssets = if (result.isSuccess) result.getOrNull() ?: emptyList() else emptyList()

            val keywords = when {
                buildingTitle.contains("ADMIN", ignoreCase = true) -> listOf("admin", "registrar", "executive")
                buildingTitle.contains("CICS", ignoreCase = true) -> listOf("cics", "lab", "computer")
                buildingTitle.contains("HEB", ignoreCase = true) || buildingTitle.contains("LIBRARY", ignoreCase = true) -> listOf("heb", "library", "audio")
                buildingTitle.contains("CET", ignoreCase = true) || buildingTitle.contains("CSB", ignoreCase = true) -> listOf("csb", "cet", "mechatronics")
                buildingTitle.contains("GYM", ignoreCase = true) -> listOf("gym", "grandstand", "athletics")
                buildingTitle.contains("TAAL", ignoreCase = true) -> listOf("taal")
                buildingTitle.contains("LEMERY", ignoreCase = true) -> listOf("lemery")
                buildingTitle.contains("IBAAN", ignoreCase = true) -> listOf("ibaan")
                buildingTitle.contains("HOSTEL", ignoreCase = true) -> listOf("hostel", "nasugbu hostel")
                buildingTitle.contains("IMT", ignoreCase = true) -> listOf("imt")
                buildingTitle.contains("LIAN", ignoreCase = true) -> listOf("lian", "cafeteria")
                buildingTitle.contains("ROSARIO", ignoreCase = true) -> listOf("rosario", "canteen")
                buildingTitle.contains("INFIRMARY", ignoreCase = true) -> listOf("infirmary", "clinic")
                buildingTitle.contains("APACIBLE", ignoreCase = true) -> listOf("apacible", "museum")
                buildingTitle.contains("CAS", ignoreCase = true) -> listOf("cas")
                buildingTitle.contains("ERMITA", ignoreCase = true) -> listOf("ermita")
                else -> listOf(buildingTitle.lowercase())
            }

            val filteredAssets = allAssets.filter { asset ->
                keywords.any { kw ->
                    asset.location.contains(kw, ignoreCase = true) ||
                    asset.assetName.contains(kw, ignoreCase = true) ||
                    asset.remarks.contains(kw, ignoreCase = true) ||
                    asset.assetType.contains(kw, ignoreCase = true)
                }
            }

            val floorRoomMap = mutableMapOf<String, MutableMap<String, MutableList<Asset>>>()

            for (asset in filteredAssets) {
                val loc = asset.location
                val locLower = loc.lowercase()

                val floorName = when {
                    locLower.contains("3rd") || locLower.contains("third") || locLower.contains(" floor 3") -> "Third Floor"
                    locLower.contains("2nd") || locLower.contains("second") || locLower.contains(" floor 2") -> "Second Floor"
                    locLower.contains("1st") || locLower.contains("first") || locLower.contains(" floor 1") -> "First Floor"
                    else -> "Ground Floor"
                }

                val roomName = when {
                    locLower.contains("room") -> {
                        val idx = locLower.indexOf("room")
                        loc.substring(idx).trim()
                    }
                    locLower.contains("lab") -> {
                        val idx = locLower.indexOf("lab")
                        loc.substring(idx).trim()
                    }
                    else -> "General Room"
                }

                floorRoomMap.getOrPut(floorName) { mutableMapOf() }
                    .getOrPut(roomName) { mutableListOf() }
                    .add(asset)
            }

            val displayItems = mutableListOf<BuildingDisplayItem>()
            val floorsOrder = listOf("Ground Floor", "First Floor", "Second Floor", "Third Floor")

            for (floor in floorsOrder) {
                val roomMap = floorRoomMap[floor]
                if (!roomMap.isNullOrEmpty()) {
                    displayItems.add(BuildingDisplayItem.FloorHeader(floor))
                    for ((roomName, assetsInRoom) in roomMap) {
                        displayItems.add(BuildingDisplayItem.RoomHeader(roomName))
                        for (asset in assetsInRoom) {
                            displayItems.add(BuildingDisplayItem.AssetRow(asset))
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    if (displayItems.isEmpty()) {
                        binding.txtEmptyBuildingAssets.visibility = View.VISIBLE
                        binding.rvBuildingAssets.visibility = View.GONE
                    } else {
                        binding.txtEmptyBuildingAssets.visibility = View.GONE
                        binding.rvBuildingAssets.visibility = View.VISIBLE
                        binding.rvBuildingAssets.adapter = BuildingAssetsAdapter(displayItems) { asset ->
                            showAssetDetailsDialog(asset)
                        }
                    }
                }
            }
        }
    }

    private fun showAssetDetailsDialog(asset: Asset) {
        val dialogBinding = DialogAssetDetailsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.detailAssetName.text = asset.assetName.ifBlank { asset.assetType }
        dialogBinding.detailAssetId.text = "ID: ${asset.assetId}"
        dialogBinding.detailAssetType.text = "Type: ${asset.assetType}"
        dialogBinding.detailAssetLocation.text = "Location: ${asset.location}"
        dialogBinding.detailAssetStatus.text = "Status: ${asset.status.name.replace("_", " ")}"
        dialogBinding.detailAssetCondition.text = "Condition: ${asset.condition}"

        if (asset.photo.isNotEmpty()) {
            dialogBinding.detailAssetImage.visibility = View.VISIBLE
            Glide.with(requireContext())
                .load(asset.photo)
                .placeholder(R.drawable.user_icon)
                .into(dialogBinding.detailAssetImage)
        }

        dialogBinding.btnReportThisAsset.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

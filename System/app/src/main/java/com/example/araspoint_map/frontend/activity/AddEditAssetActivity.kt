package com.example.araspoint_map.frontend.activity

import android.R
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.araspoint_map.backend.model.Asset
import com.example.araspoint_map.backend.model.Constants
import com.example.araspoint_map.backend.model.enums.AssetCondition
import com.example.araspoint_map.backend.model.enums.AssetStatus
import com.example.araspoint_map.backend.service.AssetService
import com.example.araspoint_map.databinding.ActivityAddEditAssetBinding
import kotlinx.coroutines.launch

class AddEditAssetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditAssetBinding
    private val assetService = AssetService()
    private var editingAssetId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddEditAssetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        // Setup Spinners for Asset Type, Status, Condition, Building, and Floor
        val assetTypes = arrayOf("Electronics", "Furniture", "Appliance", "IT Equipment", "Scientific", "Facility")
        val typeAdapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, assetTypes)
        binding.spinnerAssetType.adapter = typeAdapter

        val statuses = AssetStatus.values().map { it.name }
        val statusAdapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, statuses)
        binding.spinnerAssetStatus.adapter = statusAdapter

        val conditions = AssetCondition.values().map { it.name }
        val conditionAdapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, conditions)
        binding.spinnerAssetCondition.adapter = conditionAdapter

        val buildingAdapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, Constants.BUILDINGS)
        binding.spinnerBuilding.adapter = buildingAdapter

        val floors = arrayOf("1", "2", "3", "4")
        val floorAdapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, floors)
        binding.spinnerFloor.adapter = floorAdapter

        // Check if editing existing asset
        editingAssetId = intent.getStringExtra("EXTRA_ASSET_ID")
        if (editingAssetId != null) {
            binding.titleText.text = "Edit Asset"
            binding.btnSaveAsset.text = "Update Asset"
            binding.inputAssetName.setText(intent.getStringExtra("EXTRA_ASSET_NAME") ?: "")
        }

        binding.btnSaveAsset.setOnClickListener {
            val name = binding.inputAssetName.text.toString().trim()
            val type = binding.spinnerAssetType.selectedItem.toString()
            val statusStr = binding.spinnerAssetStatus.selectedItem.toString()
            val conditionStr = binding.spinnerAssetCondition.selectedItem.toString()
            val building = binding.spinnerBuilding.selectedItem.toString()
            val floor = binding.spinnerFloor.selectedItem.toString()
            val room = binding.inputRoomNumber.text.toString().trim()

            if (name.isEmpty() || room.isEmpty()) {
                Toast.makeText(this, "Please fill in Asset Name and Room Number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val location = "$building $floor$room" // e.g., HEB 204

            val status = try { AssetStatus.valueOf(statusStr) } catch (_: Exception) { AssetStatus.AVAILABLE }
            val condition = try { AssetCondition.valueOf(conditionStr) } catch (_: Exception) { AssetCondition.GOOD }

            val asset = Asset(
                assetId = editingAssetId ?: Asset().assetId,
                assetName = name,
                assetType = type,
                location = location,
                status = status,
                condition = condition
            )

            lifecycleScope.launch {
                val result = if (editingAssetId != null) {
                    assetService.updateAsset(asset)
                } else {
                    assetService.createAsset(asset)
                }

                if (result.isSuccess) {
                    Toast.makeText(this@AddEditAssetActivity, "Asset saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddEditAssetActivity, "Failed to save asset: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

package com.example.araspoint_map.frontend.activity

import android.Manifest
import android.R
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.araspoint_map.BuildConfig
import com.example.araspoint_map.backend.model.Asset
import com.example.araspoint_map.backend.model.Constants
import com.example.araspoint_map.backend.model.enums.AssetCondition
import com.example.araspoint_map.backend.model.enums.AssetStatus
import com.example.araspoint_map.databinding.ActivityAddEditAssetBinding
import com.example.araspoint_map.frontend.viewModel.AssetViewModel
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class AddEditAssetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditAssetBinding
    private val assetViewModel: AssetViewModel by viewModels()
    private var editingAssetId: String? = null
    private var uploadedPhotoUrl: String = ""
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
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = File(cacheDir, "camera_asset_${System.currentTimeMillis()}.jpg")
            tempCameraUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
            takePictureLauncher.launch(tempCameraUri!!)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { startCrop(it) }
    }

    private val cropActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                uploadImageToCloudinary(resultUri)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR && result.data != null) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(this, "Crop failed: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        val intent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(4f, 3f)
            .withMaxResultSize(1200, 900)
            .getIntent(this)
        cropActivityResultLauncher.launch(intent)
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo with Camera", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Asset Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    1 -> {
                        pickImageLauncher.launch("image/*")
                    }
                }
            }
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddEditAssetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))

            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                (v as ScrollView).post {
                    val focused = v.findFocus()
                    if (focused != null) {
                        v.smoothScrollTo(0, focused.bottom)
                    } else {
                        v.smoothScrollTo(0, v.getChildAt(0).bottom)
                    }
                }
            }
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSelectPhoto.setOnClickListener {
            showImageSourceDialog()
        }

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

        editingAssetId = intent.getStringExtra("EXTRA_ASSET_ID")
        if (editingAssetId != null) {
            binding.titleText.text = "Edit Asset"
            binding.btnSaveAsset.text = "Update Asset"
            binding.inputAssetName.setText(intent.getStringExtra("EXTRA_ASSET_NAME") ?: "")

            uploadedPhotoUrl = intent.getStringExtra("EXTRA_ASSET_PHOTO") ?: ""
            if (uploadedPhotoUrl.isNotEmpty()) {
                binding.txtPhotoStatus.text = "Existing photo loaded ✓"
            }

            val assetType = intent.getStringExtra("EXTRA_ASSET_TYPE") ?: ""
            binding.spinnerAssetType.selectValue(assetType)

            val status = intent.getStringExtra("EXTRA_ASSET_STATUS") ?: ""
            binding.spinnerAssetStatus.selectValue(status)

            val condition = intent.getStringExtra("EXTRA_ASSET_CONDITION") ?: ""
            binding.spinnerAssetCondition.selectValue(condition)

            val location = intent.getStringExtra("EXTRA_ASSET_LOCATION") ?: ""
            if (location.isNotEmpty()) {
                if (location.contains("Floor") && location.contains("Room")) {
                    val buildingPart = location.substringBefore("Floor").trim()
                    val afterFloor = location.substringAfter("Floor").trim()
                    val floorPart = afterFloor.substringBefore("Room").trim()
                    val roomPart = afterFloor.substringAfter("Room").trim()

                    if (buildingPart.isNotEmpty()) binding.spinnerBuilding.selectValue(buildingPart)
                    if (floorPart.isNotEmpty()) binding.spinnerFloor.selectValue(floorPart)
                    if (roomPart.isNotEmpty()) binding.inputRoomNumber.setText(roomPart)
                } else {
                    val parts = location.split(" ")
                    if (parts.isNotEmpty()) {
                        binding.spinnerBuilding.selectValue(parts[0])
                    }
                    if (parts.size > 1) {
                        val roomStr = parts.drop(1).joinToString(" ")
                        binding.inputRoomNumber.setText(roomStr)
                        if (roomStr.isNotEmpty() && roomStr[0].isDigit()) {
                            val floorChar = roomStr[0].toString()
                            binding.spinnerFloor.selectValue(floorChar)
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            assetViewModel.operationStatus.collectLatest { result ->
                if (result != null) {
                    if (result.isSuccess) {
                        Toast.makeText(this@AddEditAssetActivity, "Asset saved successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddEditAssetActivity, "Failed to save asset: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
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

            val location = "$building Floor $floor Room $room"

            val status = try { AssetStatus.valueOf(statusStr) } catch (_: Exception) { AssetStatus.AVAILABLE }
            val condition = try { AssetCondition.valueOf(conditionStr) } catch (_: Exception) { AssetCondition.GOOD }

            val asset = Asset(
                assetId = editingAssetId ?: Asset().assetId,
                assetName = name,
                assetType = type,
                location = location,
                status = status,
                condition = condition,
                photo = uploadedPhotoUrl
            )

            assetViewModel.saveAsset(asset, editingAssetId != null)
        }
    }

    private fun Spinner.selectValue(value: String) {
        val adapter = this.adapter as? ArrayAdapter<*> ?: return
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString().equals(value, ignoreCase = true)) {
                this.setSelection(i)
                return
            }
        }
    }

    private fun uploadImageToCloudinary(imageUri: Uri) {
        binding.txtPhotoStatus.text = "Uploading cropped photo..."
        Toast.makeText(this, "Uploading photo to Cloudinary...", Toast.LENGTH_SHORT).show()

        MediaManager.get().upload(imageUri)
            .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val secureUrl = resultData["secure_url"] as? String ?: ""
                    uploadedPhotoUrl = secureUrl
                    runOnUiThread {
                        binding.txtPhotoStatus.text = "Photo cropped & uploaded ✓"
                        Toast.makeText(this@AddEditAssetActivity, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    runOnUiThread {
                        binding.txtPhotoStatus.text = "Upload failed"
                        Toast.makeText(this@AddEditAssetActivity, "Cloudinary upload failed: ${error.description}", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }
}

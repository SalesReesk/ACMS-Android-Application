package com.example.admin_side

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.admin_side.databinding.ActivityUploadBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadBinding
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)

        setContentView(binding.root)


        binding.saveButton.setOnClickListener {
            val assetName = binding.uploadName.text.toString()
            val assetID = binding.uploadID.text.toString()
            val assetStatus = binding.uploadStatus.text.toString()
            val assetCondition = binding.uploadCondition.text.toString()

            databaseReference = FirebaseDatabase.getInstance().getReference("Assets")

            val assetData = AssetData(assetName, assetID, assetStatus, assetCondition)

            databaseReference.child(assetID).setValue(assetData).addOnSuccessListener {
                binding.uploadName.text.clear()
                binding.uploadID.text.clear()
                binding.uploadStatus.text.clear()
                binding.uploadCondition.text.clear()

                Toast.makeText(this, "Saved Asset Information", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@UploadActivity, admin::class.java)
                startActivity(intent)
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to Save Asset Information", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
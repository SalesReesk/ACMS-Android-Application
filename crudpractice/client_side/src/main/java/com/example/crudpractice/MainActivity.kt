package com.example.crudpractice

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.crudpractice.databinding.ActivityMainBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchButton.setOnClickListener {
            val searchAssetID: String = binding.searchPhone.text.toString()
            if (searchAssetID.isNotEmpty()) {
                readData(searchAssetID)
            } else {
                Toast.makeText(this, "Please Enter Asset ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readData(assetID: String) {
        databaseReference = FirebaseDatabase.getInstance().getReference("Assets")
        databaseReference.child(assetID).get().addOnSuccessListener {
            if (it.exists()) {
                val assetName = it.child("assetName").value
                val assetStatus = it.child("assetStatus").value
                val assetCondition = it.child("assetCondition").value
                Toast.makeText(this, "Asset Found!", Toast.LENGTH_SHORT).show()
                binding.searchPhone.text.clear()
                binding.readName.text = assetName.toString()
                binding.readStatus.text = assetStatus.toString()
                binding.readCondition.text = assetCondition.toString()
            } else {
                Toast.makeText(this, "Asset Not Exists.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Someting Went Wrong", Toast.LENGTH_SHORT).show()
        }
    }
}
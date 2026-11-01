package com.example.araspoint_map.frontend.activity

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.Asset
import com.example.araspoint_map.backend.model.enums.AssetStatus
import com.example.araspoint_map.databinding.ItemBuildingAssetRowBinding
import com.example.araspoint_map.databinding.ItemBuildingFloorHeaderBinding
import com.example.araspoint_map.databinding.ItemBuildingRoomHeaderBinding

sealed class BuildingDisplayItem {
    data class FloorHeader(val floorName: String) : BuildingDisplayItem()
    data class RoomHeader(val roomName: String) : BuildingDisplayItem()
    data class AssetRow(val asset: Asset) : BuildingDisplayItem()
}

class BuildingAssetsAdapter(
    private val items: List<BuildingDisplayItem>,
    private val onItemClick: (Asset) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_FLOOR_HEADER = 0
        private const val TYPE_ROOM_HEADER = 1
        private const val TYPE_ASSET = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is BuildingDisplayItem.FloorHeader -> TYPE_FLOOR_HEADER
            is BuildingDisplayItem.RoomHeader -> TYPE_ROOM_HEADER
            is BuildingDisplayItem.AssetRow -> TYPE_ASSET
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_FLOOR_HEADER -> {
                val binding = ItemBuildingFloorHeaderBinding.inflate(inflater, parent, false)
                FloorHeaderViewHolder(binding)
            }
            TYPE_ROOM_HEADER -> {
                val binding = ItemBuildingRoomHeaderBinding.inflate(inflater, parent, false)
                RoomHeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemBuildingAssetRowBinding.inflate(inflater, parent, false)
                AssetViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is BuildingDisplayItem.FloorHeader -> {
                (holder as FloorHeaderViewHolder).binding.txtFloorHeaderName.text = item.floorName
            }
            is BuildingDisplayItem.RoomHeader -> {
                (holder as RoomHeaderViewHolder).binding.txtRoomHeaderName.text = item.roomName
            }
            is BuildingDisplayItem.AssetRow -> {
                val asset = item.asset
                val assetHolder = holder as AssetViewHolder
                val b = assetHolder.binding
                b.txtRowAssetName.text = asset.assetName.ifBlank { asset.assetType.ifBlank { "Campus Asset" } }
                b.txtRowAssetTag.text = asset.assetId
                b.txtRowAssetLocation.text = "• ${asset.location.ifBlank { "Main Facility" }}"
                b.txtRowAssetStatus.text = asset.status.name.replace("_", " ")

                when (asset.status) {
                    AssetStatus.AVAILABLE -> {
                        b.txtRowAssetStatus.setBackgroundResource(R.drawable.bg_status_pill_available)
                        b.txtRowAssetStatus.setTextColor(Color.parseColor("#15803D"))
                    }
                    AssetStatus.UNDER_MAINTENANCE -> {
                        b.txtRowAssetStatus.setBackgroundResource(R.drawable.bg_status_pill_maintenance)
                        b.txtRowAssetStatus.setTextColor(Color.parseColor("#D97706"))
                    }
                    AssetStatus.MISSING, AssetStatus.DISPOSED -> {
                        b.txtRowAssetStatus.setBackgroundResource(R.drawable.bg_status_pill_damaged)
                        b.txtRowAssetStatus.setTextColor(Color.parseColor("#DC2626"))
                    }
                    else -> {
                        b.txtRowAssetStatus.setBackgroundResource(R.drawable.bg_status_pill_available)
                        b.txtRowAssetStatus.setTextColor(Color.parseColor("#5421AC"))
                    }
                }

                assetHolder.itemView.setOnClickListener {
                    onItemClick(asset)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class FloorHeaderViewHolder(val binding: ItemBuildingFloorHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class RoomHeaderViewHolder(val binding: ItemBuildingRoomHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class AssetViewHolder(val binding: ItemBuildingAssetRowBinding) : RecyclerView.ViewHolder(binding.root)
}

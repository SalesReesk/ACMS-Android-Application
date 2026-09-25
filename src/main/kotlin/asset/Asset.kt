package asset

open class Asset {

    companion object {
        private var nextId = 1
    }

    var assetId: String = "AAMS-${nextId++.toString().padStart(4, '0')}"
    var assetName: String = ""
    var assetType: String = ""

    var latitude: Double = 0.0
    var longitude: Double = 0.0

    var location: String = ""
    var status: AssetStatus = AssetStatus.AVAILABLE
    var condition: AssetCondition = AssetCondition.GOOD
    var photo: String = ""
    var remarks: String = ""

}
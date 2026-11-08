package com.example.araspoint_map.backend.navigation

import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoNode(
    val id: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

enum class TurnType {
    HEAD_STRAIGHT,
    TURN_SLIGHT_LEFT,
    TURN_LEFT,
    TURN_SHARP_LEFT,
    TURN_SLIGHT_RIGHT,
    TURN_RIGHT,
    TURN_SHARP_RIGHT,
    ARRIVED
}

data class NavigationStep(
    val instruction: String = "",
    val distanceMeters: Double = 0.0,
    val turnType: TurnType = TurnType.HEAD_STRAIGHT,
    val targetNode: GeoNode = GeoNode()
)

class CampusNavigationEngine(
    val nodes: List<GeoNode>,
    val adjacencyList: Map<String, List<GeoNode>>
) {

    private var activePath: MutableList<GeoNode> = mutableListOf()
    private var destinationNode: GeoNode? = null
    val offRouteThresholdMeters = 12.0

    fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLambda = Math.toRadians(lon2 - lon1)

        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun findPathAStar(startNodeId: String, targetNodeId: String): List<GeoNode> {
        val start = nodes.find { it.id == startNodeId } ?: return emptyList()
        val target = nodes.find { it.id == targetNodeId } ?: return emptyList()
        destinationNode = target

        val gScore = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
        val fScore = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
        val cameFrom = mutableMapOf<String, String>()

        gScore[start.id] = 0.0
        fScore[start.id] = distanceBetween(start.lat, start.lng, target.lat, target.lng)

        val openSet = PriorityQueue<Pair<String, Double>>(compareBy { it.second })
        openSet.add(Pair(start.id, fScore[start.id]!!))

        while (openSet.isNotEmpty()) {
            val (currentId, _) = openSet.poll()!!

            if (currentId == target.id) {
                val path = mutableListOf<GeoNode>()
                var curr: String? = currentId
                while (curr != null) {
                    nodes.find { it.id == curr }?.let { path.add(0, it) }
                    curr = cameFrom[curr]
                }
                activePath = path
                return path
            }

            val current = nodes.find { it.id == currentId } ?: continue
            val neighbors = adjacencyList[currentId] ?: emptyList()

            for (neighbor in neighbors) {
                val tentativeGScore = gScore.getValue(currentId) + distanceBetween(current.lat, current.lng, neighbor.lat, neighbor.lng)
                if (tentativeGScore < gScore.getValue(neighbor.id)) {
                    cameFrom[neighbor.id] = currentId
                    gScore[neighbor.id] = tentativeGScore
                    val f = tentativeGScore + distanceBetween(neighbor.lat, neighbor.lng, target.lat, target.lng)
                    fScore[neighbor.id] = f
                    openSet.add(Pair(neighbor.id, f))
                }
            }
        }
        return emptyList()
    }

    fun generateTurnInstructions(path: List<GeoNode>): List<NavigationStep> {
        if (path.size < 2) return emptyList()
        val steps = mutableListOf<NavigationStep>()

        var previousBearing = calculateBearing(path[0].lat, path[0].lng, path[1].lat, path[1].lng)

        for (i in 0 until path.size - 1) {
            val curr = path[i]
            val next = path[i + 1]
            val stepDist = distanceBetween(curr.lat, curr.lng, next.lat, next.lng)
            val currentBearing = calculateBearing(curr.lat, curr.lng, next.lat, next.lng)

            val turnAngle = (currentBearing - previousBearing + 540) % 360 - 180
            val turnType = when {
                i == path.size - 2 -> TurnType.ARRIVED
                abs(turnAngle) < 20.0 -> TurnType.HEAD_STRAIGHT
                turnAngle in 20.0..65.0 -> TurnType.TURN_SLIGHT_RIGHT
                turnAngle in 65.0..115.0 -> TurnType.TURN_RIGHT
                turnAngle > 115.0 -> TurnType.TURN_SHARP_RIGHT
                turnAngle in -65.0..-20.0 -> TurnType.TURN_SLIGHT_LEFT
                turnAngle in -115.0..-65.0 -> TurnType.TURN_LEFT
                else -> TurnType.TURN_SHARP_LEFT
            }

            val instructionText = when (turnType) {
                TurnType.ARRIVED -> "Arriving at ${next.name}"
                TurnType.HEAD_STRAIGHT -> "Head straight towards ${next.name}"
                TurnType.TURN_SLIGHT_RIGHT -> "Turn slightly right towards ${next.name}"
                TurnType.TURN_RIGHT -> "Turn right towards ${next.name}"
                TurnType.TURN_SHARP_RIGHT -> "Make a sharp right towards ${next.name}"
                TurnType.TURN_SLIGHT_LEFT -> "Turn slightly left towards ${next.name}"
                TurnType.TURN_LEFT -> "Turn left towards ${next.name}"
                TurnType.TURN_SHARP_LEFT -> "Make a sharp left towards ${next.name}"
            }

            steps.add(NavigationStep(instructionText, stepDist, turnType, next))
            previousBearing = currentBearing
        }
        return steps
    }

    fun updateLiveLocation(userLat: Double, userLng: Double, onRerouteNeeded: (GeoNode) -> Unit): List<GeoNode> {
        if (activePath.size < 2) return activePath

        val minDistanceToPath = activePath.zipWithNext().minOf { (a, b) ->
            distanceToSegment(userLat, userLng, a.lat, a.lng, b.lat, b.lng)
        }

        if (minDistanceToPath > offRouteThresholdMeters) {
            val nearestNode = nodes.minByOrNull { distanceBetween(userLat, userLng, it.lat, it.lng) }
            if (nearestNode != null && destinationNode != null) {
                onRerouteNeeded(nearestNode)
                return activePath
            }
        }

        val nextTargetNode = activePath[1]
        val distToNextNode = distanceBetween(userLat, userLng, nextTargetNode.lat, nextTargetNode.lng)
        if (distToNextNode < 5.0 && activePath.size > 2) {
            activePath.removeAt(0)
        }

        return activePath
    }

    private fun distanceToSegment(pLat: Double, pLng: Double, aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val abDistSq = (bLat - aLat).pow(2) + (bLng - aLng).pow(2)
        if (abDistSq == 0.0) return distanceBetween(pLat, pLng, aLat, aLng)

        val t = ((pLat - aLat) * (bLat - aLat) + (pLng - aLng) * (bLng - aLng)) / abDistSq
        val clampedT = t.coerceIn(0.0, 1.0)
        val projLat = aLat + clampedT * (bLat - aLat)
        val projLng = aLng + clampedT * (bLng - aLng)
        return distanceBetween(pLat, pLng, projLat, projLng)
    }
}

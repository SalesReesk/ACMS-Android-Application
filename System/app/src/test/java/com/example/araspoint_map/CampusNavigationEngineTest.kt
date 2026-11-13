package com.example.araspoint_map

import com.example.araspoint_map.backend.navigation.CampusNavigationEngine
import com.example.araspoint_map.backend.navigation.GeoNode
import com.example.araspoint_map.backend.navigation.TurnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampusNavigationEngineTest {

    private val nodes = listOf(
        GeoNode("A", "Building A", 14.0665, 120.6260),
        GeoNode("B", "Building B", 14.0670, 120.6265),
        GeoNode("C", "Building C", 14.0675, 120.6270)
    )

    private val adjacencyList = mapOf(
        "A" to listOf(nodes[1]),
        "B" to listOf(nodes[0], nodes[2]),
        "C" to listOf(nodes[1])
    )

    private val engine = CampusNavigationEngine(nodes, adjacencyList)

    @Test
    fun testHaversineDistance() {
        val dist = engine.distanceBetween(14.0665, 120.6260, 14.0670, 120.6265)
        assertTrue(dist > 0.0)
    }

    @Test
    fun testAStarPathfinding() {
        val path = engine.findPathAStar("A", "C")
        assertEquals(3, path.size)
        assertEquals("A", path[0].id)
        assertEquals("B", path[1].id)
        assertEquals("C", path[2].id)
    }

    @Test
    fun testTurnInstructions() {
        val path = engine.findPathAStar("A", "C")
        val steps = engine.generateTurnInstructions(path)
        assertEquals(2, steps.size)
        assertEquals(TurnType.HEAD_STRAIGHT, steps[0].turnType)
        assertEquals(TurnType.ARRIVED, steps[1].turnType)
    }
}

package com.example.tsu_navigator

import java.util.PriorityQueue
import kotlin.math.*

data class Point(val x: Int, val y: Int)

fun findPath(
    grid: Array<IntArray>,
    start: Point,
    finish: Point
): List<Point>? {
    val height = grid.size
    val width = grid[0].size

    fun heuristic(p: Point) =
        sqrt((p.x - finish.x).toDouble().pow(2) + (p.y - finish.y).toDouble().pow(2))

    val gScore = Array(height) { DoubleArray(width) { Double.POSITIVE_INFINITY } }
    gScore[start.y][start.x] = 0.0

    val fScore = Array(height) { DoubleArray(width) { Double.POSITIVE_INFINITY } }
    fScore[start.y][start.x] = heuristic(start)

    val cameFrom = Array(height) { arrayOfNulls<Point>(width) }

    val openSet = PriorityQueue<Point>(compareBy { fScore[it.y][it.x] })
    openSet.add(start)

    val directions = listOf(
        Point(1, 0), Point(-1, 0), Point(0, 1), Point(0, -1),
        Point(1, 1), Point(1, -1), Point(-1, 1), Point(-1, -1)
    )

    while (openSet.isNotEmpty()) {
        val current = openSet.poll()

        if (fScore[current.y][current.x] < gScore[current.y][current.x] + heuristic(current)) {
            continue
        }

        if (current == finish) {
            return reconstructPath(cameFrom, current)
        }

        for (dir in directions) {
            val nx = current.x + dir.x
            val ny = current.y + dir.y

            if (nx !in 0 until width || ny !in 0 until height) continue
            if (grid[ny][nx] == 0) continue

            val stepCost = if (dir.x != 0 && dir.y != 0) 1.41421356237 else 1.0
            val tentativeGScore = gScore[current.y][current.x] + stepCost

            if (tentativeGScore < gScore[ny][nx]) {
                cameFrom[ny][nx] = current
                gScore[ny][nx] = tentativeGScore
                fScore[ny][nx] = tentativeGScore + heuristic(Point(nx, ny))

                openSet.add(Point(nx, ny))
            }
        }
    }
    return null
}

private fun reconstructPath(cameFrom: Array<Array<Point?>>, current: Point): List<Point> {
    val path = mutableListOf<Point>()
    var node: Point? = current
    while (node != null) {
        path.add(node)
        node = cameFrom[node.y][node.x]
    }
    return path.asReversed()
}
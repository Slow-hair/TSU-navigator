@file:Suppress("DEPRECATION")

package ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tsu_navigator.Point
import com.example.tsu_navigator.findPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import kotlin.math.*

@Composable
fun Grid() {
    val context = LocalContext.current

    var navigationGrid by remember { mutableStateOf<Array<IntArray>?>(null) }
    var gridRows by remember { mutableIntStateOf(0) }
    var gridCols by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    val south = 56.4631200
    val west = 84.9387400
    val north = 56.4746400
    val east = 84.9585000

    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val grid = loadGridFromAssets(context)
            if (grid != null) {
                navigationGrid = grid
                gridRows = grid.size
                gridCols = grid[0].size
                println("Загружена матрица ${gridRows}x${gridCols}")
            }
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        MapViewWithGrid(
            context = context,
            navigationGrid = navigationGrid,
            gridRows = gridRows,
            gridCols = gridCols,
            south = south,
            west = west,
            north = north,
            east = east
        )
    }
}

private fun loadGridFromAssets(context: Context): Array<IntArray>? {
    return try {
        val jsonString = context.assets.open("walkability_grid.json").bufferedReader().use { it.readText() }
        val json = JSONObject(jsonString)
        val rows = json.getInt("rows")
        val cols = json.getInt("cols")
        val gridArray = json.getJSONArray("grid")
        Array(rows) { i ->
            IntArray(cols) { j ->
                gridArray.getJSONArray(i).getInt(j)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapViewWithGrid(
    context: Context,
    navigationGrid: Array<IntArray>?,
    gridRows: Int,
    gridCols: Int,
    south: Double,
    west: Double,
    north: Double,
    east: Double
) {
    Configuration.getInstance().load(
        context,
        context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    )

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setBuiltInZoomControls(true)
            setMultiTouchControls(true)
            val bounds = BoundingBox(north, east, south, west)
            setScrollableAreaLimitDouble(bounds)
            minZoomLevel = 17.0
            maxZoomLevel = 19.0
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(56.466, 84.948))
        }
    }

    var startPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var endPoint by remember { mutableStateOf<GeoPoint?>(null) }

    fun geoToIndex(lat: Double, lon: Double): Point? {
        if (gridRows == 0 || gridCols == 0) return null
        val x = ((lon - west) / (east - west) * gridCols).toInt().coerceIn(0, gridCols - 1)
        val y = ((lat - south) / (north - south) * gridRows).toInt().coerceIn(0, gridRows - 1)
        return Point(x, y)
    }

    fun indexToGeo(x: Int, y: Int): GeoPoint {
        val lon = west + (x + 0.5) * (east - west) / gridCols
        val lat = south + (y + 0.5) * (north - south) / gridRows
        return GeoPoint(lat, lon)
    }

    fun findNearestWalkable(grid: Array<IntArray>, startX: Int, startY: Int): Point {
        val maxRadius = max(grid.size, grid[0].size)
        for (radius in 0..maxRadius) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val x = startX + dx
                    val y = startY + dy
                    if (x in 0 until grid[0].size && y in 0 until grid.size && grid[y][x] == 1) {
                        return Point(x, y)
                    }
                }
            }
        }
        return Point(startX, startY)
    }

    fun buildAndShowRoute() {
        if (navigationGrid == null || startPoint == null || endPoint == null) {
            println("Данные не готовы")
            return
        }
        val startIdxRaw = geoToIndex(startPoint!!.latitude, startPoint!!.longitude) ?: return
        val endIdxRaw = geoToIndex(endPoint!!.latitude, endPoint!!.longitude) ?: return

        val startIdx = if (navigationGrid[startIdxRaw.y][startIdxRaw.x] == 1) startIdxRaw
        else findNearestWalkable(navigationGrid, startIdxRaw.x, startIdxRaw.y)
        val endIdx = if (navigationGrid[endIdxRaw.y][endIdxRaw.x] == 1) endIdxRaw
        else findNearestWalkable(navigationGrid, endIdxRaw.x, endIdxRaw.y)

        val pathIndices = findPath(navigationGrid, startIdx, endIdx)
        if (pathIndices == null) {
            println("Путь не найден")
            return
        }
        println("Путь найден точек ${pathIndices.size}")

        val geoPath = pathIndices.map { indexToGeo(it.x, it.y) }

        mapView.overlays.removeAll(mapView.overlays.filterIsInstance<Polyline>())
        val polyline = Polyline().apply {
            setPoints(geoPath)
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = 8f
        }
        mapView.overlays.add(polyline)
        mapView.invalidate()

        startPoint = null
        endPoint = null
    }

    mapView.setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            val proj = mapView.projection
            val geoPoint = proj.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
            println("Клик ${geoPoint.latitude}, ${geoPoint.longitude}")
            when {
                startPoint == null -> {
                    startPoint = geoPoint
                    mapView.overlays.removeAll(mapView.overlays.filterIsInstance<Polyline>())
                    mapView.invalidate()
                    mapView.performClick()
                }
                endPoint == null -> {
                    endPoint = geoPoint
                    buildAndShowRoute()
                    mapView.performClick()
                }
                else -> {
                    startPoint = geoPoint
                    endPoint = null
                    mapView.overlays.removeAll(mapView.overlays.filterIsInstance<Polyline>())
                    mapView.invalidate()
                    mapView.performClick()
                }
            }
        }
        false
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}
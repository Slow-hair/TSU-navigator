@file:Suppress("DEPRECATION")

package ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color as ComposeColor
import com.example.tsu_navigator.EatPlace
import com.example.tsu_navigator.EatPlacesData
enum class SelectionMode { NONE, START, FINISH }

@Composable
fun Grid(selectedPlace: EatPlace? = null) {
    val context = LocalContext.current

    var navigationGrid by remember { mutableStateOf<Array<IntArray>?>(null) }
    var gridRows by remember { mutableIntStateOf(0) }
    var gridCols by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    var startPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var endPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var selectionMode by remember { mutableStateOf(SelectionMode.NONE) }

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
            east = east,
            startPoint = startPoint,
            endPoint = endPoint,
            onStartPointChange = { startPoint = it },
            onEndPointChange = { endPoint = it },
            selectionMode = selectionMode,
            onSelectionModeChange = { selectionMode = it },
            selectedPlace = selectedPlace
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 10.dp)
        ) {
            Button(
                onClick = { selectionMode = SelectionMode.START },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectionMode == SelectionMode.START) ComposeColor.Green else ComposeColor.DarkGray
                ),modifier = Modifier.width(90.dp)

            ) {
                Text("Старт", fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.width(5.dp))
            Button(
                onClick = { selectionMode = SelectionMode.FINISH },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectionMode == SelectionMode.FINISH) ComposeColor.Red else ComposeColor.DarkGray
                ),modifier = Modifier.width(90.dp)
            ) {
                Text("Финиш", fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.width(5.dp))
            Button(
                onClick = {
                    startPoint = null
                    endPoint = null
                    selectionMode = SelectionMode.NONE
                },modifier = Modifier.width(90.dp)
            ) {
                Text("Сброс", fontSize = 10.sp)
            }
        }
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

private fun createCircleMarker(context: Context, color: Int): Drawable {
    val size = (20 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    return BitmapDrawable(context.resources, bitmap)
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
    east: Double,
    startPoint: GeoPoint?,
    endPoint: GeoPoint?,
    onStartPointChange: (GeoPoint?) -> Unit,
    onEndPointChange: (GeoPoint?) -> Unit,
    selectionMode: SelectionMode,
    onSelectionModeChange: (SelectionMode) -> Unit,
    selectedPlace: EatPlace? = null
) {
    Configuration.getInstance().load(
        context,
        context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    )

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setBuiltInZoomControls(false)
            setMultiTouchControls(true)
            val bounds = BoundingBox(north, east, south, west)
            setScrollableAreaLimitDouble(bounds)
            minZoomLevel = 17.0
            maxZoomLevel = 19.0
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(56.466, 84.948))
        }
    }
    LaunchedEffect(selectedPlace) {
        if (selectedPlace != null) {
            mapView.controller.animateTo(GeoPoint(
                selectedPlace.latitude,
                selectedPlace.longitude
            ))
            mapView.controller.setZoom(18.0)

            val oldMarkers = mapView.overlays.filter { it is Marker && it.title != "Старт" && it.title != "Финиш" }
            mapView.overlays.removeAll(oldMarkers)

            val marker = Marker(mapView).apply {
                position = GeoPoint(selectedPlace.latitude, selectedPlace.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = selectedPlace.name
                subDescription = selectedPlace.cuisine ?: ""
                icon = createCircleMarker(context, android.graphics.Color.RED)
            }
            mapView.overlays.add(marker)
            mapView.invalidate()
        }
    }
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
        val grid = navigationGrid
        val start = startPoint
        val end = endPoint
        if (grid == null || start == null || end == null) {
            println("buildAndShowRoute: данные не готовы")
            return
        }
        val startIdxRaw = geoToIndex(start.latitude, start.longitude) ?: return
        val endIdxRaw = geoToIndex(end.latitude, end.longitude) ?: return

        val startIdx = if (grid[startIdxRaw.y][startIdxRaw.x] == 1) startIdxRaw
        else findNearestWalkable(grid, startIdxRaw.x, startIdxRaw.y)
        val endIdx = if (grid[endIdxRaw.y][endIdxRaw.x] == 1) endIdxRaw
        else findNearestWalkable(grid, endIdxRaw.x, endIdxRaw.y)

        val pathIndices = findPath(grid, startIdx, endIdx)
        if (pathIndices == null) {
            println("Путь не найден")
            return
        }
        println("Путь найден точек ${pathIndices.size}")

        val geoPath = pathIndices.map { indexToGeo(it.x, it.y) }

        mapView.overlays.removeAll(mapView.overlays.filterIsInstance<Polyline>())
        val polyline = Polyline().apply {
            setPoints(geoPath)
            outlinePaint.color = android.graphics.Color.BLUE
            outlinePaint.strokeWidth = 8f
        }
        mapView.overlays.add(polyline)
        mapView.invalidate()
    }

    fun updateMarkers() {
        mapView.overlays.removeAll(mapView.overlays.filterIsInstance<Marker>())
        startPoint?.let {
            Marker(mapView).apply {
                position = it
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Старт"
                icon = createCircleMarker(context, android.graphics.Color.rgb(0, 150, 255))
                mapView.overlays.add(this)
            }
        }
        endPoint?.let {
            Marker(mapView).apply {
                position = it
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Финиш"
                icon = createCircleMarker(context, android.graphics.Color.rgb(0, 200, 0))
                mapView.overlays.add(this)
            }
        }
        mapView.invalidate()
    }

    LaunchedEffect(startPoint, endPoint) {
        updateMarkers()
        if (startPoint != null && endPoint != null) {
            buildAndShowRoute()
        }else {
            mapView.overlays.removeAll(mapView.overlays.filterIsInstance<Polyline>())
            mapView.invalidate()
        }
    }

    mapView.setOnTouchListener { _: View?, event: MotionEvent ->
        if (event.action == MotionEvent.ACTION_UP) {
            val proj = mapView.projection
            val geoPoint = proj.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
            println("Клик ${geoPoint.latitude}, ${geoPoint.longitude}")
            when (selectionMode) {
                SelectionMode.START -> {
                    onStartPointChange(geoPoint)
                    onSelectionModeChange(SelectionMode.NONE)
                    mapView.overlays.removeAll(mapView.overlays.filterIsInstance<Polyline>())
                    mapView.invalidate()
                    mapView.performClick()
                }
                SelectionMode.FINISH -> {
                    onEndPointChange(geoPoint)
                    onSelectionModeChange(SelectionMode.NONE)
                    mapView.performClick()
                }
                SelectionMode.NONE -> { }
            }
        }
        false
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        ) {
            Button(
                onClick = { mapView.controller.zoomIn() },
                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.DarkGray),
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { mapView.controller.zoomOut() },
                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.DarkGray),
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("-", fontSize = 24.sp)
            }
        }
    }
}
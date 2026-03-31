package com.example.tsu_navigator.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.*
import org.osmdroid.util.BoundingBox

@Composable
fun Grid() {
    val context = androidx.compose.ui.platform.LocalContext.current

    Configuration.getInstance().load(
        context,
        context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    )

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setBuiltInZoomControls(true)
            setMultiTouchControls(true)
            val bounds = BoundingBox(
                56.4746400,
                84.9585000,
                56.4631200,
                84.9387400
            )
            setScrollableAreaLimitDouble(bounds)
            minZoomLevel = 16.0
            maxZoomLevel = 19.0
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(56.466, 84.948))
        }
    }

    val fixedGridOverlay = remember {
        object : Overlay() {
            private val stepMeters = 5.0
            private val stepLat = stepMeters / 111_000.0
            private val lonFactor = cos(Math.toRadians(56.47))
            private val stepLon = stepLat / lonFactor

            override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
                if (shadow) return

                val paint = Paint().apply {
                    color = Color.RED
                    strokeWidth = 2f
                    style = Paint.Style.STROKE
                }

                val visibleArea = mapView.boundingBox
                val coordConverter = mapView.projection

                val firstLat = floor(visibleArea.latSouth / stepLat) * stepLat
                val firstLon = floor(visibleArea.lonWest / stepLon) * stepLon

                var lon = firstLon
                while (lon <= visibleArea.lonEast) {
                    val top = coordConverter.toPixels(GeoPoint(visibleArea.latNorth, lon), null)
                    val bottom = coordConverter.toPixels(GeoPoint(visibleArea.latSouth, lon), null)
                    canvas.drawLine(
                        top.x.toFloat(), top.y.toFloat(),
                        bottom.x.toFloat(), bottom.y.toFloat(),
                        paint
                    )
                    lon += stepLon
                }

                var lat = firstLat
                while (lat <= visibleArea.latNorth) {
                    val left = coordConverter.toPixels(GeoPoint(lat, visibleArea.lonWest), null)
                    val right = coordConverter.toPixels(GeoPoint(lat, visibleArea.lonEast), null)
                    canvas.drawLine(
                        left.x.toFloat(), left.y.toFloat(),
                        right.x.toFloat(), right.y.toFloat(),
                        paint
                    )
                    lat += stepLat
                }
            }
        }
    }

    //mapView.overlays.add(fixedGridOverlay)

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}
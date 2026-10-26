package com.example.runner.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.runner.R
import com.example.runner.domain.model.TrackPoint
import kotlin.math.max
import kotlin.math.min
import org.maplibre.android.annotations.Icon
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.HeatmapLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

const val STYLE_LIGHT = "https://tiles.openfreemap.org/styles/liberty"
const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"

data class MapOverlay(
    val track: List<TrackPoint> = emptyList(),
    val ghostTrack: List<TrackPoint> = emptyList(),
    val ghostPoint: TrackPoint? = null,
    val userPoint: TrackPoint? = null,
    val showStartFinish: Boolean = false,
    val followUser: Boolean = true,
    val trackColor: Int = Color.parseColor("#1FA89A"),
    val ghostColor: Int = Color.parseColor("#7C3AED"),
)

class MapController {
    var map: MapLibreMap? = null
    var mapView: MapView? = null
    var onUserGesture: () -> Unit = {}
    private var trackLine: Polyline? = null
    private var ghostLine: Polyline? = null
    private var userMarker: Marker? = null
    private var ghostMarker: Marker? = null
    private var startMarker: Marker? = null
    private var finishMarker: Marker? = null
    private var fittedKey: Int = 0

    fun applyOverlay(overlay: MapOverlay, animateCamera: Boolean) {
        runCatching {
            val map = map ?: return
            val context = mapView?.context ?: return

            trackLine?.remove()
            ghostLine?.remove()
            trackLine = null
            ghostLine = null

            if (overlay.track.size >= 2) {
                trackLine = map.addPolyline(
                    PolylineOptions()
                        .addAll(overlay.track.map { LatLng(it.latitude, it.longitude) })
                        .color(overlay.trackColor)
                        .width(6f),
                )
            }
            if (overlay.ghostTrack.size >= 2) {
                ghostLine = map.addPolyline(
                    PolylineOptions()
                        .addAll(overlay.ghostTrack.map { LatLng(it.latitude, it.longitude) })
                        .color(overlay.ghostColor)
                        .width(4f),
                )
            }

            userMarker?.remove()
            userMarker = overlay.userPoint?.let {
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(it.latitude, it.longitude))
                        .title("Я"),
                )
            }
            ghostMarker?.remove()
            ghostMarker = overlay.ghostPoint?.let { point ->
                val options = MarkerOptions()
                    .position(LatLng(point.latitude, point.longitude))
                    .title("Призрак")
                markerIcon(context, R.drawable.ic_ghost_marker)?.let { options.icon(it) }
                map.addMarker(options)
            }

            if (overlay.showStartFinish && overlay.track.isNotEmpty()) {
                startMarker?.remove()
                finishMarker?.remove()
                val startOptions = MarkerOptions()
                    .position(LatLng(overlay.track.first().latitude, overlay.track.first().longitude))
                    .title("Старт")
                markerIcon(context, R.drawable.ic_start_marker)?.let { startOptions.icon(it) }
                startMarker = map.addMarker(startOptions)
                if (overlay.track.size > 1) {
                    val finishOptions = MarkerOptions()
                        .position(LatLng(overlay.track.last().latitude, overlay.track.last().longitude))
                        .title("Финиш")
                    markerIcon(context, R.drawable.ic_finish_marker)?.let { finishOptions.icon(it) }
                    finishMarker = map.addMarker(finishOptions)
                }
            }

            when {
                overlay.showStartFinish && overlay.track.size >= 2 -> {
                    val key = overlay.track.size xor
                        overlay.track.first().latitude.hashCode() xor
                        overlay.track.last().longitude.hashCode()
                    if (key != fittedKey) {
                        fittedKey = key
                        fitToTrack(overlay.track)
                    }
                }
                overlay.followUser && overlay.userPoint != null && animateCamera -> {
                    map.easeCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(overlay.userPoint.latitude, overlay.userPoint.longitude),
                            16.5,
                        ),
                        400,
                    )
                }
            }
        }
    }

    private fun fitToTrack(track: List<TrackPoint>) {
        val map = map ?: return
        val view = mapView ?: return
        if (track.size < 2) return
        val apply = {
            runCatching {
                val lats = track.map { it.latitude }
                val lons = track.map { it.longitude }
                val south = lats.min()
                val north = lats.max()
                val west = lons.min()
                val east = lons.max()
                val latSpan = north - south
                val lonSpan = east - west
                if (latSpan < 1e-6 && lonSpan < 1e-6) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(south, west), 16.0))
                } else {
                    val padLat = max(latSpan * 0.12, 0.0004)
                    val padLon = max(lonSpan * 0.12, 0.0004)
                    val bounds = LatLngBounds.Builder()
                        .include(LatLng(south - padLat, west - padLon))
                        .include(LatLng(north + padLat, east + padLon))
                        .build()
                    val padding = min(72, min(view.width, view.height) / 6).coerceAtLeast(24)
                    if (view.width > padding * 2 && view.height > padding * 2) {
                        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } else {
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng((south + north) / 2, (west + east) / 2),
                                15.0,
                            ),
                        )
                    }
                }
            }
        }
        if (view.width == 0 || view.height == 0) {
            view.post { apply() }
        } else {
            apply()
        }
    }

    private fun markerIcon(context: Context, @DrawableRes resId: Int): Icon? {
        return runCatching {
            val drawable = ContextCompat.getDrawable(context, resId) ?: return@runCatching null
            val width = drawable.intrinsicWidth.coerceAtLeast(48)
            val height = drawable.intrinsicHeight.coerceAtLeast(48)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            IconFactory.getInstance(context).fromBitmap(bitmap)
        }.getOrNull()
    }

    fun moveTo(lat: Double, lon: Double, zoom: Double = 16.0) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom))
    }

    fun snapshot(onReady: (android.graphics.Bitmap?) -> Unit) {
        map?.snapshot { onReady(it) } ?: onReady(null)
    }

    fun showHeatmap(points: List<TrackPoint>) {
        val map = map ?: return
        map.getStyle { style ->
            val features = points.map { Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)) }
            val source = style.getSource("all-points") as? GeoJsonSource
            if (source == null) {
                style.addSource(GeoJsonSource("all-points", FeatureCollection.fromFeatures(features)))
            } else {
                source.setGeoJson(FeatureCollection.fromFeatures(features))
            }
            if (style.getLayer("heat") == null) {
                style.addLayer(
                    HeatmapLayer("heat", "all-points").apply {
                        setProperties(
                            PropertyFactory.heatmapRadius(22f),
                            PropertyFactory.heatmapIntensity(0.9f),
                            PropertyFactory.heatmapOpacity(0.85f),
                            PropertyFactory.heatmapColor(
                                Expression.interpolate(
                                    Expression.linear(),
                                    Expression.heatmapDensity(),
                                    Expression.literal(0),
                                    Expression.rgba(33, 102, 172, 0),
                                    Expression.literal(0.3),
                                    Expression.rgb(103, 169, 207),
                                    Expression.literal(0.6),
                                    Expression.rgb(209, 229, 240),
                                    Expression.literal(0.8),
                                    Expression.rgb(253, 141, 60),
                                    Expression.literal(1),
                                    Expression.rgb(239, 59, 44),
                                ),
                            ),
                        )
                    },
                )
                style.addLayer(
                    CircleLayer("heat-points", "all-points").apply {
                        setProperties(
                            PropertyFactory.circleRadius(2.2f),
                            PropertyFactory.circleColor(Color.parseColor("#F97316")),
                            PropertyFactory.circleOpacity(0.15f),
                        )
                    },
                )
            }
            if (points.size >= 2) {
                fitToTrack(points)
            } else if (points.size == 1) {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(points.first().latitude, points.first().longitude),
                        14.0,
                    ),
                )
            }
        }
    }
}

@Composable
fun MapLibreContainer(
    modifier: Modifier = Modifier,
    styleUri: String,
    overlay: MapOverlay,
    controller: MapController = remember { MapController() },
    onMapReady: (MapLibreMap) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val overlayState = rememberUpdatedState(overlay)
    val onReady = rememberUpdatedState(onMapReady)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).also { mapView ->
                mapView.onCreate(null)
                mapView.onStart()
                mapView.onResume()
                controller.mapView = mapView
                mapView.getMapAsync { map ->
                    controller.map = map
                    map.uiSettings.isAttributionEnabled = true
                    map.uiSettings.isLogoEnabled = false
                    map.addOnMoveListener(
                        object : MapLibreMap.OnMoveListener {
                            override fun onMoveBegin(detector: org.maplibre.android.gestures.MoveGestureDetector) {
                                controller.onUserGesture()
                            }

                            override fun onMove(detector: org.maplibre.android.gestures.MoveGestureDetector) = Unit

                            override fun onMoveEnd(detector: org.maplibre.android.gestures.MoveGestureDetector) = Unit
                        },
                    )
                    map.setStyle(Style.Builder().fromUri(styleUri)) {
                        onReady.value(map)
                        controller.applyOverlay(overlayState.value, animateCamera = true)
                    }
                }
            }
        },
        update = {
            if (controller.map?.style != null) {
                controller.applyOverlay(overlay, animateCamera = overlay.followUser)
            }
        },
        onRelease = { mapView ->
            controller.map = null
            controller.mapView = null
            runCatching {
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            }
        },
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val mapView = controller.mapView ?: return@LifecycleEventObserver
            runCatching {
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    else -> Unit
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

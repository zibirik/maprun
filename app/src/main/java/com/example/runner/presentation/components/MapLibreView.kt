package com.example.runner.presentation.components

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.runner.R
import com.example.runner.domain.model.TrackPoint
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

    fun applyOverlay(overlay: MapOverlay, animateCamera: Boolean) {
        val map = map ?: return
        val context = mapView?.context ?: return
        val icons = IconFactory.getInstance(context)

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
        ghostMarker = overlay.ghostPoint?.let {
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(it.latitude, it.longitude))
                    .title("Призрак")
                    .icon(icons.fromResource(R.drawable.ic_ghost_marker)),
            )
        }

        if (overlay.showStartFinish && overlay.track.isNotEmpty()) {
            startMarker?.remove()
            finishMarker?.remove()
            startMarker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(overlay.track.first().latitude, overlay.track.first().longitude))
                    .title("Старт")
                    .icon(icons.fromResource(R.drawable.ic_start_marker)),
            )
            if (overlay.track.size > 1) {
                finishMarker = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(overlay.track.last().latitude, overlay.track.last().longitude))
                        .title("Финиш")
                        .icon(icons.fromResource(R.drawable.ic_finish_marker)),
                )
            }
        }

        when {
            overlay.showStartFinish && overlay.track.size >= 2 -> {
                val bounds = LatLngBounds.Builder().apply {
                    overlay.track.forEach { include(LatLng(it.latitude, it.longitude)) }
                }.build()
                map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80), 600)
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
            if (points.isNotEmpty()) {
                val bounds = LatLngBounds.Builder().apply {
                    points.forEach { include(LatLng(it.latitude, it.longitude)) }
                }.build()
                map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80), 800)
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

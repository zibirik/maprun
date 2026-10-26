package com.example.runner.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.runner.MainActivity
import com.example.runner.R
import com.example.runner.data.location.LocationClient
import com.example.runner.domain.model.TrackingStatus
import com.example.runner.domain.repository.SettingsRepository
import com.example.runner.domain.usecase.CheckAchievementsUseCase
import com.example.runner.domain.usecase.GetDailyChallengeUseCase
import com.example.runner.domain.usecase.GetGhostRunUseCase
import com.example.runner.domain.usecase.SaveWorkoutUseCase
import com.example.runner.domain.util.FormatUtils
import com.example.runner.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    @Inject lateinit var locationClient: LocationClient
    @Inject lateinit var trackingManager: TrackingManager
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var voiceCoach: VoiceCoach
    @Inject lateinit var getGhostRun: GetGhostRunUseCase
    @Inject lateinit var saveWorkout: SaveWorkoutUseCase
    @Inject lateinit var checkAchievements: CheckAchievementsUseCase
    @Inject lateinit var dailyChallenge: GetDailyChallengeUseCase
    @Inject lateinit var widgetUpdater: WidgetUpdater

    private var locationJob: Job? = null
    private var clockJob: Job? = null
    private var ghostLoaded = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        observeVoiceCues()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                startSession()
                return START_STICKY
            }
            ACTION_PAUSE -> {
                trackingManager.pause()
                return START_STICKY
            }
            ACTION_RESUME -> {
                trackingManager.resume()
                return START_STICKY
            }
            ACTION_STOP -> {
                // Если стоп пришёл через startForegroundService, сначала закрываем контракт FGS.
                runCatching { startAsForeground() }
                stopSession()
                return START_NOT_STICKY
            }
            else -> {
                val status = trackingManager.state.value.status
                if (status == TrackingStatus.RUNNING || status == TrackingStatus.PAUSED) {
                    startSession()
                    return START_STICKY
                }
                stopSelf()
                return START_NOT_STICKY
            }
        }
    }

    private fun startSession() {
        startAsForeground()
        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
            trackingManager.configure(settings.weightKg, settings.autoPauseEnabled)
            trackingManager.startRunning()
            if (settings.voiceEveryKm > 0f) voiceCoach.speakStart()
        }
        ghostLoaded = false
        locationJob?.cancel()
        locationJob = lifecycleScope.launch {
            runCatching {
                locationClient.locationUpdates().collect { location ->
                    trackingManager.onLocation(location)
                    maybeLoadGhost()
                }
            }
        }
        clockJob?.cancel()
        clockJob = lifecycleScope.launch {
            while (isActive) {
                trackingManager.tickClock()
                updateNotification()
                delay(1_000)
            }
        }
    }

    private suspend fun maybeLoadGhost() {
        if (ghostLoaded) return
        val first = trackingManager.state.value.points.firstOrNull() ?: return
        ghostLoaded = true
        trackingManager.setGhost(getGhostRun(first))
    }

    private fun stopSession() {
        trackingManager.stop()
        locationJob?.cancel()
        clockJob?.cancel()
        lifecycleScope.launch {
            val snapshot = trackingManager.peekFinished()
            runCatching {
                withContext(NonCancellable) {
                    val id = trackingManager.persistIfNeeded { saveWorkout(it) }
                    val saved = trackingManager.peekFinished()
                    if (id != null && saved != null) {
                        val workout = saved.copy(id = id)
                        checkAchievements(workout)
                        dailyChallenge.completeIfMatched(workout)
                        widgetUpdater.refresh()
                    }
                }
            }
            runCatching {
                val settings = settingsRepository.settings.first()
                if (settings.voiceEveryKm > 0f && snapshot != null) {
                    voiceCoach.speakFinish(snapshot.distanceMeters, snapshot.durationMillis, settings.unit)
                }
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun observeVoiceCues() {
        lifecycleScope.launch {
            trackingManager.state
                .map { it.distanceMeters to it.durationMillis }
                .distinctUntilChanged()
                .collectLatest { (distance, duration) ->
                    val settings = settingsRepository.settings.first()
                    val stepMeters = settings.voiceEveryKm * 1000f
                    if (stepMeters <= 0f) return@collectLatest
                    val km = (distance / stepMeters).toInt()
                    val last = trackingManager.state.value.lastAnnouncedKm
                    if (km > last && km > 0) {
                        trackingManager.markAnnouncedKm(km)
                        voiceCoach.speakSplit(km * stepMeters.toDouble(), duration, settings.unit)
                    }
                }
        }
    }

    private fun startAsForeground() {
        val notification = buildNotification("Запись началась")
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun updateNotification() {
        val live = trackingManager.state.value
        val text = "${FormatUtils.duration(live.durationMillis)} · " +
            FormatUtils.distance(live.distanceMeters, com.example.runner.domain.model.DistanceUnit.KILOMETERS)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text, live.status))
    }

    private fun buildNotification(text: String, status: TrackingStatus = TrackingStatus.RUNNING): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val pauseAction = if (status == TrackingStatus.PAUSED) {
            NotificationCompat.Action(
                0,
                getString(R.string.tracking_service_resume),
                servicePending(ACTION_RESUME, 1),
            )
        } else {
            NotificationCompat.Action(
                0,
                getString(R.string.tracking_service_pause),
                servicePending(ACTION_PAUSE, 2),
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_run)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(pauseAction)
            .addAction(
                NotificationCompat.Action(
                    0,
                    getString(R.string.tracking_service_stop),
                    servicePending(ACTION_STOP, 3),
                ),
            )
            .build()
    }

    private fun servicePending(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TrackingService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.notification_channel_desc) }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.example.runner.action.START"
        const val ACTION_PAUSE = "com.example.runner.action.PAUSE"
        const val ACTION_RESUME = "com.example.runner.action.RESUME"
        const val ACTION_STOP = "com.example.runner.action.STOP"
        private const val CHANNEL_ID = "tracking"
        private const val NOTIFICATION_ID = 42
    }
}

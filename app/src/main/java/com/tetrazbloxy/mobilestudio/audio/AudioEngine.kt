package com.tetrazbloxy.mobilestudio.audio

import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-process audio engine (SPEC 7.6): a bounded pool of 28 voices on a
 * HandlerThread plus a single preview player for the audio browser.
 */
class AudioEngine {
    companion object {
        const val MAX_VOICES = 28
    }

    data class Voice(
        val key: Long,
        val player: MediaPlayer,
        var loop: Boolean = false,
        var wantPlay: Boolean = true
    )

    data class PreviewState(
        val url: String? = null,
        val loading: Boolean = false,
        val playing: Boolean = false,
        val positionMs: Int = 0,
        val durationMs: Int = 0,
        val error: String? = null
    )

    private val thread = HandlerThread("MobileStudioAudio").apply { start() }
    private val handler = Handler(thread.looper)
    private val voices = ConcurrentHashMap<Long, Voice>()
    private val missing = ConcurrentHashMap.newKeySet<Long>()
    private val previewListeners = CopyOnWriteArrayList<(PreviewState) -> Unit>()

    private var preview: MediaPlayer? = null
    private var previewUrl: String? = null
    private var previewPrepared = false
    private var previewLoading = false
    private var previewState = PreviewState()

    @Volatile
    private var masterVolume: Float = 1f

    fun setMasterVolume(value: Float) {
        masterVolume = value.coerceIn(0f, 1f)
        handler.post {
            for (voice in voices.values) {
                try {
                    voice.player.setVolume(masterVolume, masterVolume)
                } catch (_: Throwable) {
                }
            }
            try {
                preview?.setVolume(masterVolume, masterVolume)
            } catch (_: Throwable) {
            }
        }
    }

    fun create(soundId: Long, url: String, loop: Boolean = false) {
        if (soundId == 0L || url.isBlank()) return
        handler.post {
            val existing = voices[soundId]
            if (existing != null) {
                existing.wantPlay = true
                applyVoice(existing, 1f, 1f, 1f, loop, true)
                return@post
            }
            if (voices.size >= MAX_VOICES) {
                voices.entries.firstOrNull()?.let { evict(it.value) }
            }
            try {
                val player = MediaPlayer()
                val voice = Voice(soundId, player, loop)
                player.setDataSource(url)
                player.setOnPreparedListener { prepared ->
                    if (!voice.wantPlay) {
                        prepared.release()
                        return@setOnPreparedListener
                    }
                    prepared.isLooping = voice.loop
                    try {
                        prepared.setVolume(masterVolume, masterVolume)
                        prepared.start()
                    } catch (_: Throwable) {
                    }
                }
                player.setOnErrorListener { _, _, _ ->
                    missing.add(soundId)
                    voices.remove(soundId)
                    true
                }
                voices[soundId] = voice
                player.prepareAsync()
            } catch (_: Throwable) {
                missing.add(soundId)
            }
        }
    }

    fun apply(
        soundId: Long,
        left: Float = 1f,
        right: Float = 1f,
        speed: Float = 1f,
        loop: Boolean = false,
        restart: Boolean = false
    ) {
        handler.post {
            val voice = voices[soundId] ?: return@post
            applyVoice(voice, left, right, speed, loop, restart)
        }
    }

    fun stop(soundId: Long) {
        handler.post { voices.remove(soundId)?.let { evict(it) } }
    }

    fun stopAll() {
        handler.post {
            for (voice in voices.values) evict(voice)
            voices.clear()
            missing.clear()
        }
    }

    fun isMissing(soundId: Long): Boolean = missing.contains(soundId)

    private fun applyVoice(
        voice: Voice,
        left: Float,
        right: Float,
        speed: Float,
        loop: Boolean,
        restart: Boolean
    ) {
        voice.loop = loop
        try {
            val player = voice.player
            player.setVolume(
                (left.coerceIn(0f, 1f) * masterVolume),
                (right.coerceIn(0f, 1f) * masterVolume)
            )
            player.isLooping = loop
            setSpeed(player, speed)
            if (restart || !player.isPlaying) {
                player.seekTo(0)
                player.start()
            }
        } catch (_: Throwable) {
        }
    }

    private fun setSpeed(player: MediaPlayer, speed: Float) {
        if (speed <= 0f || kotlin.math.abs(speed - 1f) < 0.001f) return
        try {
            player.playbackParams = player.playbackParams.setSpeed(speed).setPitch(speed)
        } catch (_: Throwable) {
        }
    }

    private fun evict(voice: Voice) {
        voice.wantPlay = false
        try {
            voice.player.stop()
        } catch (_: Throwable) {
        }
        try {
            voice.player.release()
        } catch (_: Throwable) {
        }
    }

    // ---- Preview player -------------------------------------------------

    fun addPreviewListener(listener: (PreviewState) -> Unit): () -> Unit {
        previewListeners.add(listener)
        listener(previewState)
        return { previewListeners.remove(listener) }
    }

    fun currentPreviewState(): PreviewState = previewState

    fun preview(url: String?) {
        handler.post {
            stopPreviewLocked()
            if (url.isNullOrBlank()) {
                publish(PreviewState())
                return@post
            }
            previewUrl = url
            previewLoading = true
            previewPrepared = false
            publish(PreviewState(url = url, loading = true))
            try {
                val player = MediaPlayer()
                preview = player
                player.setDataSource(url)
                player.setOnPreparedListener {
                    previewPrepared = true
                    previewLoading = false
                    try {
                        player.setVolume(masterVolume, masterVolume)
                    } catch (_: Throwable) {
                    }
                    publish(
                        PreviewState(
                            url = url,
                            playing = false,
                            durationMs = it.duration.coerceAtLeast(0)
                        )
                    )
                }
                player.setOnCompletionListener {
                    publish(
                        PreviewState(
                            url = url,
                            playing = false,
                            durationMs = it.duration.coerceAtLeast(0)
                        )
                    )
                }
                player.setOnErrorListener { _, _, _ ->
                    previewLoading = false
                    publish(PreviewState(url = url, error = "Could not play this audio."))
                    true
                }
                player.prepareAsync()
            } catch (exception: Throwable) {
                previewLoading = false
                publish(PreviewState(url = url, error = exception.message))
            }
        }
    }

    fun playPreview() {
        handler.post {
            val player = preview ?: return@post
            if (!previewPrepared) return@post
            try {
                player.start()
                publish(currentPreview(playing = true))
            } catch (_: Throwable) {
            }
        }
    }

    fun pausePreview() {
        handler.post {
            val player = preview ?: return@post
            if (!previewPrepared) return@post
            try {
                player.pause()
                publish(currentPreview(playing = false))
            } catch (_: Throwable) {
            }
        }
    }

    fun togglePreview() {
        handler.post {
            val player = preview ?: return@post
            if (!previewPrepared) return@post
            try {
                if (player.isPlaying) {
                    player.pause()
                    publish(currentPreview(playing = false))
                } else {
                    player.start()
                    publish(currentPreview(playing = true))
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun stopPreview() {
        handler.post { stopPreviewLocked() }
    }

    fun seekPreview(positionMs: Int) {
        handler.post {
            val player = preview ?: return@post
            if (!previewPrepared) return@post
            try {
                player.seekTo(positionMs.coerceAtLeast(0))
                publish(currentPreview())
            } catch (_: Throwable) {
            }
        }
    }

    fun setPreviewSpeed(speed: Float) {
        handler.post {
            val player = preview ?: return@post
            setSpeed(player, speed)
        }
    }

    private fun stopPreviewLocked() {
        previewLoading = false
        previewPrepared = false
        try {
            preview?.stop()
        } catch (_: Throwable) {
        }
        try {
            preview?.release()
        } catch (_: Throwable) {
        }
        preview = null
        previewUrl = null
        publish(PreviewState())
    }

    private fun currentPreview(playing: Boolean = false): PreviewState {
        val player = preview
        return PreviewState(
            url = previewUrl,
            loading = previewLoading,
            playing = playing,
            positionMs = try {
                player?.currentPosition ?: 0
            } catch (_: Throwable) {
                0
            },
            durationMs = try {
                player?.duration ?: 0
            } catch (_: Throwable) {
                0
            }
        )
    }

    private fun publish(state: PreviewState) {
        previewState = state
        for (listener in previewListeners) {
            listener(state)
        }
    }

    fun release() {
        stopAll()
        handler.post { stopPreviewLocked() }
        thread.quitSafely()
    }
}

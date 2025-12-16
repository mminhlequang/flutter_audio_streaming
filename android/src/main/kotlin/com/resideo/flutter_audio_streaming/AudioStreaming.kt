package com.resideo.flutter_audio_streaming

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import com.pedro.rtplibrary.rtsp.RtspOnlyAudio
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import io.flutter.plugin.common.MethodChannel
import java.io.IOException
import android.app.Application
import android.os.Bundle


class AudioStreaming(
    private var activity: Activity? = null,
    private var dartMessenger: DartMessenger? = null
) : ConnectCheckerRtsp, Application.ActivityLifecycleCallbacks, AudioManager.OnAudioFocusChangeListener {

    private val application: Application?
        get() = activity?.application

    private var isInForeground: Boolean = false
    private var pendingReconnect: Boolean = false


    private val rtspAudio: RtspOnlyAudio = RtspOnlyAudio(this)
    private var prepared: Boolean = false
    private var bitrate: Int? = null
    private var sampleRate: Int? = null
    private var isStereo: Boolean? = false
    private var echoCanceler: Boolean? = false
    private var noiseSuppressor: Boolean? = false
    private var phoneStateListener: PhoneStateListener? = null

    // Interruption handling properties
    private val interruptionHandler = Handler(Looper.getMainLooper())
    private var interruptionRunnable: Runnable? = null
    private var isInterrupted: Boolean = false
    private var savedUrl: String? = null
    private val interruptionTimeout: Long = 30000  // 30 seconds

    // Audio focus state tracking
    private var hasAudioFocus: Boolean = false

    fun prepare(
        bitrate: Int?, sampleRate: Int?, isStereo: Boolean?, echoCanceler: Boolean?,
        noiseSuppressor: Boolean?
    ): Boolean {
        this.bitrate = bitrate
        this.sampleRate = sampleRate
        this.isStereo = isStereo
        this.echoCanceler = echoCanceler
        this.noiseSuppressor = noiseSuppressor
        prepared = true
        return rtspAudio.prepareAudio(
            bitrate ?: (128 * 1024),
            sampleRate ?: 44100,
            isStereo ?: true,
            echoCanceler ?: false,
            (noiseSuppressor ?: true)
        )
    }

    private fun prepare(): Boolean {
        prepared = true
        return rtspAudio.prepareAudio(
            this.bitrate ?: (128 * 1024),
            this.sampleRate ?: 44100,
            this.isStereo ?: true,
            this.echoCanceler ?: false,
            this.noiseSuppressor ?: true
        )
    }

    fun getStatistics(result: MethodChannel.Result) {
        val ret = hashMapOf<String, Any>()
        result.success(ret)
    }

    fun startStreaming(url: String?, result: MethodChannel.Result) {
        println("AudioStreaming: StartAudioStreaming url: $url")
        if (url == null) {
            result.error("StartAudioStreaming", "Must specify a url.", null)
            return
        }

        // Save URL for potential reconnection
        savedUrl = url

        // Check if there's an active phone call
        if (isPhoneCallActive()) {
            result.error(
                "PHONE_CALL_ACTIVE",
                "Cannot start streaming during an active phone call",
                null
            )
            return
        }

        // Request Audio Focus FIRST (before acquiring microphone)
        if (!requestAudioFocus()) {
            result.error(
                "AUDIO_FOCUS_DENIED",
                "Cannot acquire audio focus - another app may be using audio",
                null
            )
            return
        }

        try {
            if (!rtspAudio.isStreaming) {
                if (prepared || prepare()) {
                    // ready to start streaming
                    rtspAudio.startStream(url)

                    // Register phone state listener AFTER stream starts
                    registerPhoneStateListener()

                    // Register lifecycle callbacks
                    application?.registerActivityLifecycleCallbacks(this)
                    isInForeground = true // Assume we are foregrounded when starting

                    val ret = hashMapOf<String, Any>()
                    ret["url"] = url
                    result.success(ret)
                } else {
                    // Prepare failed - abandon audio focus
                    abandonAudioFocus()
                    result.error(
                        "AudioStreamingFailed",
                        "Error preparing stream, This device cant do it",
                        null
                    )
                    return
                }
            }
        } catch (e: IOException) {
            // Clean up audio focus on failure
            abandonAudioFocus()
            result.error("AudioStreamingFailed", e.message, null)
        }
    }

    fun muteStreaming(result: MethodChannel.Result) {
        try {
            rtspAudio.disableAudio()
            result.success(null)
        } catch (e: IllegalStateException) {
            result.error("MuteAudioStreamingFailed", e.message, null)
        }
    }

    fun unMuteStreaming(result: MethodChannel.Result) {
        try {
            rtspAudio.enableAudio()
            result.success(null)
        } catch (e: IllegalStateException) {
            result.error("UnMuteAudioStreamingFailed", e.message, null)
        }
    }

    fun stopStreaming(result: MethodChannel.Result) {
        println("AudioStreaming: === stopStreaming START ===")
        try {
            // Cancel any pending interruption timeout
            println("AudioStreaming: Cancelling interruption timeout")
            cancelInterruptionTimeout()

            println("AudioStreaming: Resetting state: isInterrupted = false, savedUrl = null")
            isInterrupted = false
            savedUrl = null

            // Unregister phone state listener
            phoneStateListener?.let {
                println("AudioStreaming: Unregistering PhoneStateListener")
                val telephonyManager = activity?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
                phoneStateListener = null
                println("AudioStreaming: PhoneStateListener unregistered")
            }

            // Unregister lifecycle callbacks
            application?.unregisterActivityLifecycleCallbacks(this)

            // Abandon Audio Focus
            abandonAudioFocus()

            println("AudioStreaming: Calling rtspAudio.stopStream()")
            rtspAudio.stopStream()
            println("AudioStreaming: rtspAudio.stopStream() completed")

            result.success(null)
            println("AudioStreaming: === stopStreaming END SUCCESS ===")
        } catch (e: IllegalStateException) {
            println("AudioStreaming ERROR: IllegalStateException in stopStreaming: ${e.message}")
            e.printStackTrace()
            result.error("StopAudioStreamingFailed", e.message, null)
            println("AudioStreaming: === stopStreaming END FAILURE ===")
        }
    }

    /*   override fun onConnectionSuccessRtmp() {
           println("AudioStreaming: onConnectionSuccessRtmp")
       }

       override fun onNewBitrateRtmp(bitrate: Long) {
           println("AudioStreaming: onNewBitrateRtmp: $bitrate")
       }

       override fun onDisconnectRtmp() {
           println("AudioStreaming: onDisconnectRtmp")
           activity?.runOnUiThread {
               dartMessenger?.send(DartMessenger.EventType.RTMP_STOPPED, "Disconnected")
           }
       }

       override fun onAuthErrorRtmp() {
           println("AudioStreaming: onAuthErrorRtmp")
           activity?.runOnUiThread {
               dartMessenger?.send(DartMessenger.EventType.ERROR, "Auth error")
           }
       }

       override fun onAuthSuccessRtmp() {
           println("AudioStreaming: onAuthSuccessRtmp")
       }

       override fun onConnectionFailedRtmp(reason: String) {
           println("AudioStreaming: onConnectionFailedRtmp")
           activity?.runOnUiThread { //Wait 5s and retry connect stream
               if (rtmpAudio.reTry(5000, reason)) {
                   dartMessenger?.send(DartMessenger.EventType.RTMP_RETRY, reason)
               } else {
                   dartMessenger?.send(DartMessenger.EventType.RTMP_STOPPED, "Failed retry")
                   rtmpAudio.stopStream()
               }
           }
       }

       override fun onConnectionStartedRtmp(rtmpUrl: String) {
           println("AudioStreaming: onConnectionStartedRtmp")
       }*/

    private fun isPhoneCallActive(): Boolean {
        val telephonyManager = activity?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val audioManager = activity?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        // Check telephony manager call state
        val callState = telephonyManager?.callState ?: TelephonyManager.CALL_STATE_IDLE
        println("AudioStreaming: Checking phone call - TelephonyManager state: $callState")
        if (callState != TelephonyManager.CALL_STATE_IDLE) {
            println("AudioStreaming: Phone call detected via TelephonyManager: state=$callState")
            return true
        }

        // Check if audio is being used for communication (call mode)
        val audioMode = audioManager?.mode ?: AudioManager.MODE_NORMAL
        println("AudioStreaming: Checking phone call - AudioManager mode: $audioMode")
        if (audioMode == AudioManager.MODE_IN_CALL || audioMode == AudioManager.MODE_IN_COMMUNICATION) {
            println("AudioStreaming: Phone call detected via AudioManager: mode=$audioMode")
            return true
        }

        println("AudioStreaming: No phone call detected")
        return false
    }

    private fun registerPhoneStateListener() {
        if (phoneStateListener != null) {
            println("AudioStreaming: PhoneStateListener already registered")
            return
        }

        val telephonyManager = activity?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        if (telephonyManager == null) {
            println("AudioStreaming ERROR: TelephonyManager not available, cannot monitor phone calls")
            return
        }

        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)

                val stateString = when(state) {
                    TelephonyManager.CALL_STATE_IDLE -> "IDLE"
                    TelephonyManager.CALL_STATE_RINGING -> "RINGING"
                    TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
                    else -> "UNKNOWN($state)"
                }

                println("AudioStreaming: === PhoneStateListener.onCallStateChanged ===")
                println("AudioStreaming: New state: $stateString")
                println("AudioStreaming: isInterrupted: $isInterrupted")
                println("AudioStreaming: isStreaming: ${rtspAudio.isStreaming}")

                when (state) {
                    TelephonyManager.CALL_STATE_RINGING,
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        println("AudioStreaming: Phone call active - calling handleInterruptionBegan()")
                        handleInterruptionBegan()
                    }

                    TelephonyManager.CALL_STATE_IDLE -> {
                        println("AudioStreaming: Phone call ended - calling handleInterruptionEnded()")
                        handleInterruptionEnded()
                    }
                }
                println("AudioStreaming: === PhoneStateListener.onCallStateChanged END ===")
            }
        }

        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            println("AudioStreaming: PhoneStateListener registered successfully")
        } catch (e: Exception) {
            println("AudioStreaming ERROR: Failed to register PhoneStateListener: ${e.message}")
            phoneStateListener = null
        }
    }

    private fun handleInterruptionBegan() {
        println("AudioStreaming: === handleInterruptionBegan START ===")
        println("AudioStreaming: isInterrupted: $isInterrupted")
        println("AudioStreaming: rtspAudio.isStreaming: ${rtspAudio.isStreaming}")

        if (isInterrupted) {
            println("AudioStreaming: Already handling interruption - returning")
            return
        }

        if (!rtspAudio.isStreaming) {
            println("AudioStreaming: Not streaming, ignoring interruption - returning")
            return
        }

        // Safety check: ensure activity exists
        if (activity == null) {
            println("AudioStreaming ERROR: Activity is null in handleInterruptionBegan")
            return
        }

        isInterrupted = true
        println("AudioStreaming: Set isInterrupted = true")
        println("AudioStreaming: Phone call detected - gracefully stopping stream")

        // 1. Fully stop the stream (releases microphone)
        try {
            println("AudioStreaming: Calling rtspAudio.stopStream()...")
            rtspAudio.stopStream()
            println("AudioStreaming: rtspAudio.stopStream() completed successfully")
            println("AudioStreaming: After stop - isStreaming: ${rtspAudio.isStreaming}")
        } catch (e: Exception) {
            println("AudioStreaming ERROR: Error stopping stream: ${e.message}")
            e.printStackTrace()
        }

        // Abandon Audio Focus (temporarily)
        abandonAudioFocus()

        // 2. Send AUDIO_INTERRUPTED event (NOT RTMP_STOPPED)
        println("AudioStreaming: Sending AUDIO_INTERRUPTED event to Flutter")
        activity?.runOnUiThread {
            dartMessenger?.send(
                DartMessenger.EventType.AUDIO_INTERRUPTED,
                "Phone call detected - stream paused temporarily"
            )
        }

        // 3. Start timeout (30 seconds)
        println("AudioStreaming: Starting interruption timeout timer")
        startInterruptionTimeout()
        println("AudioStreaming: === handleInterruptionBegan END ===")
    }

    private fun handleInterruptionEnded() {
        println("AudioStreaming: === handleInterruptionEnded START ===")
        println("AudioStreaming: isInterrupted: $isInterrupted")

        if (!isInterrupted) {
            println("AudioStreaming: Not in interrupted state - returning")
            return
        }

        println("AudioStreaming: Phone call ended - attempting reconnection")

        // Cancel timeout
        println("AudioStreaming: Cancelling interruption timeout")
        cancelInterruptionTimeout()

        // REMOVED DELAY - reconnect immediately like iOS
        println("AudioStreaming: Checking foreground status before reconnection")

        // CHECK FOREGROUND STATE
        if (isInForeground && activity != null) {
             println("AudioStreaming: App is in foreground - reconnecting now")
             reconnectStream()
        } else {
             println("AudioStreaming: App is in BACKGROUND or activity null - setting pendingReconnect = true")
             pendingReconnect = true
             isInterrupted = false  // Clear flag since we're deferring
        }

        println("AudioStreaming: === handleInterruptionEnded END ===")
    }

    private fun startInterruptionTimeout() {
        cancelInterruptionTimeout()  // Cancel any existing timeout

        interruptionRunnable = Runnable {
            handleInterruptionTimeout()
        }

        interruptionHandler.postDelayed(interruptionRunnable!!, interruptionTimeout)
        println("AudioStreaming: Interruption timeout started (30 seconds)")
    }

    private fun cancelInterruptionTimeout() {
        interruptionRunnable?.let {
            interruptionHandler.removeCallbacks(it)
            interruptionRunnable = null
        }
    }

    private fun handleInterruptionTimeout() {
        println("AudioStreaming: Interruption timeout expired - giving up on reconnection")

        isInterrupted = false
        savedUrl = null

        // Send RTMP_STOPPED event (timeout expired)
        activity?.runOnUiThread {
            dartMessenger?.send(
                DartMessenger.EventType.RTMP_STOPPED,
                "Stream stopped due to prolonged interruption"
            )
        }
    }

    private fun reconnectStream() {
        println("AudioStreaming: === reconnectStream START ===")

        // Validate state BEFORE attempting reconnection
        val url = savedUrl
        if (url == null) {
            println("AudioStreaming ERROR: No saved URL for reconnection")
            isInterrupted = false
            return
        }

        if (activity == null) {
            println("AudioStreaming ERROR: Activity is null, cannot reconnect")
            isInterrupted = false
            savedUrl = null
            return
        }

        if (!isInForeground) {
            println("AudioStreaming: App in background, will reconnect when resumed")
            pendingReconnect = true
            return
        }

        println("AudioStreaming: Starting reconnection background thread for: $url")
        
        // Run reconnection on background thread to avoid blocking UI or NetworkOnMainThreadException
        Thread {
            try {
                 println("AudioStreaming: [BG] Waiting 500ms before reconnecting...")
                 Thread.sleep(500) // Stability delay
                 
                // CRITICAL: Ensure stream is fully stopped before restarting
                if (rtspAudio.isStreaming) {
                    println("AudioStreaming: [BG] Stream still active during reconnect, forcing stop first")
                    try {
                        rtspAudio.stopStream()
                        Thread.sleep(300)  // Wait for cleanup
                        println("AudioStreaming: [BG] Forced stop complete")
                    } catch (e: Exception) {
                        println("AudioStreaming ERROR: [BG] Failed to stop active stream: ${e.message}")
                    }
                }

                // Force re-prepare
                println("AudioStreaming: [BG] Force re-preparing stream...")
                prepared = false
                // Note: prepare() might need to run on specific thread? 
                // Usually prepare() is just setting params.
                if (!rtspAudio.prepareAudio()) { 
                    println("AudioStreaming ERROR: [BG] Failed to prepare stream")
                    activity?.runOnUiThread { handleReconnectionFailure("Failed to prepare stream") }
                    return@Thread
                }
                prepared = true
                println("AudioStreaming: [BG] Stream prepared successfully")

                // Request audio focus on Main Thread
                var focusGranted = false
                val countdown = java.util.concurrent.CountDownLatch(1)
                activity?.runOnUiThread {
                    println("AudioStreaming: [UI] Requesting audio focus...")
                    focusGranted = requestAudioFocus()
                    countdown.countDown()
                }
                countdown.await(2, java.util.concurrent.TimeUnit.SECONDS)

                if (!focusGranted) {
                    println("AudioStreaming ERROR: [BG] Failed to acquire audio focus")
                    activity?.runOnUiThread { handleReconnectionFailure("Cannot acquire audio focus") }
                    return@Thread
                }

                // Restart stream
                println("AudioStreaming: [BG] Calling rtspAudio.startStream($url)...")
                rtspAudio.startStream(url)
                println("AudioStreaming: [BG] rtspAudio.startStream() completed")

                // Update UI/State on Main Thread
                activity?.runOnUiThread {
                    println("AudioStreaming: [UI] Re-registering phone state listener...")
                    registerPhoneStateListener()

                    isInterrupted = false
                    pendingReconnect = false
                    println("AudioStreaming: [UI] Reconnection process finished (waiting for callback)")
                }

            } catch (e: Exception) {
                println("AudioStreaming ERROR: [BG] Exception during reconnection: ${e.message}")
                e.printStackTrace()
                activity?.runOnUiThread {
                    handleReconnectionFailure("Reconnection failed: ${e.message}")
                }
            }
        }.start()
    }

    private fun handleReconnectionFailure(error: String) {
        println("AudioStreaming ERROR: $error")

        isInterrupted = false
        savedUrl = null

        // Clean up audio focus state
        abandonAudioFocus()

        activity?.runOnUiThread {
            dartMessenger?.send(
                DartMessenger.EventType.RTMP_STOPPED,
                error
            )
        }
    }

    companion object {
        const val TAG = "AudioStreaming"
    }

    override fun onAuthErrorRtsp() {
        println("AudioStreaming: onAuthErrorRtsp")
        if (isInterrupted) {
             println("AudioStreaming: Ignoring onAuthErrorRtsp because isInterrupted = true")
             return
        }
        activity?.runOnUiThread {
            dartMessenger?.send(DartMessenger.EventType.ERROR, "Auth error")
        }
    }

    override fun onAuthSuccessRtsp() {
        println("AudioStreaming: onAuthSuccessRtsp")
    }

    override fun onConnectionFailedRtsp(reason: String) {
        println("AudioStreaming: onConnectionFailedRtsp")
        if (isInterrupted) {
             println("AudioStreaming: Ignoring onConnectionFailedRtsp because isInterrupted = true")
             return
        }
        activity?.runOnUiThread { //Wait 5s and retry connect stream
            if (rtspAudio.reTry(5000, reason)) {
                dartMessenger?.send(DartMessenger.EventType.RTMP_RETRY, reason)
            } else {
                dartMessenger?.send(DartMessenger.EventType.RTMP_STOPPED, "Failed retry")
                rtspAudio.stopStream()
            }
        }
    }

    override fun onConnectionStartedRtsp(rtspUrl: String) {
        println("AudioStreaming: onConnectionStartedRtsp")
    }

    override fun onConnectionSuccessRtsp() {
        println("AudioStreaming: onConnectionSuccessRtsp")

        // Always log connection success
        println("AudioStreaming: RTSP connection successful")

        // Check if this was a reconnection after interruption
        if (savedUrl != null) {
            println("AudioStreaming: Reconnection successful (savedUrl set) - sending AUDIO_RESUMED")

            // Additional safety check
            if (activity != null) {
                activity?.runOnUiThread {
                    dartMessenger?.send(
                        DartMessenger.EventType.AUDIO_RESUMED,
                        "Stream resumed after interruption"
                    )
                }
                savedUrl = null
            } else {
                println("AudioStreaming ERROR: Activity is null in onConnectionSuccessRtsp, cannot send event")
                savedUrl = null  // Clear it anyway
            }
        }
    }

    override fun onDisconnectRtsp() {
        println("AudioStreaming: onDisconnectRtsp")
        if (isInterrupted) {
             println("AudioStreaming: Ignoring onDisconnectRtsp because isInterrupted = true")
             return
        }
        activity?.runOnUiThread {
            dartMessenger?.send(DartMessenger.EventType.RTMP_STOPPED, "Disconnected")
        }
    }

    override fun onNewBitrateRtsp(bitrate: Long) {
        println("AudioStreaming: onNewBitrateRtsp: $bitrate")
    }

    // --- Lifecycle Callbacks ---
    override fun onActivityResumed(activity: Activity) {
        if (activity === this.activity) {
            println("AudioStreaming: onActivityResumed")
            isInForeground = true

            if (pendingReconnect && savedUrl != null) {
                println("AudioStreaming: Resumed with pendingReconnect=true and savedUrl set - attempting reconnection")
                pendingReconnect = false

                // Reconnect immediately (no delay needed - resources should be ready)
                reconnectStream()
            } else if (pendingReconnect && savedUrl == null) {
                // Edge case: pending reconnect but URL was cleared (user stopped manually?)
                println("AudioStreaming: Resumed with pendingReconnect=true but savedUrl=null, clearing flag")
                pendingReconnect = false
                isInterrupted = false
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity === this.activity) {
            println("AudioStreaming: onActivityPaused")
            isInForeground = false
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
         if (activity === this.activity) {
             isInForeground = true
         }
    }
    override fun onActivityStopped(activity: Activity) {
         if (activity === this.activity) {
             isInForeground = false
         }
    }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (activity === this.activity) {
            isInForeground = false
            application?.unregisterActivityLifecycleCallbacks(this)
        }
    }

    // --- Audio Focus ---
    private fun requestAudioFocus(): Boolean {
        val audioManager = activity?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            println("AudioStreaming ERROR: AudioManager is null, cannot request audio focus")
            return false
        }

        val result = audioManager.requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )

        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)

        if (hasAudioFocus) {
            println("AudioStreaming: Audio focus GRANTED")
        } else {
            Log.w(TAG, "AudioStreaming: Audio focus REJECTED")
        }

        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) {
            println("AudioStreaming: Audio focus not held, nothing to abandon")
            return
        }

        val audioManager = activity?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.abandonAudioFocus(this)
        hasAudioFocus = false
        println("AudioStreaming: Audio focus abandoned")
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss - another app took focus
                println("AudioStreaming: Audio focus LOSS - stopping stream")
                hasAudioFocus = false

                // Stop streaming gracefully
                if (rtspAudio.isStreaming && !isInterrupted) {
                    activity?.runOnUiThread {
                        try {
                            rtspAudio.stopStream()
                            dartMessenger?.send(
                                DartMessenger.EventType.RTMP_STOPPED,
                                "Audio focus lost - stopped by system"
                            )
                        } catch (e: Exception) {
                            println("AudioStreaming ERROR: Failed to stop on focus loss: ${e.message}")
                        }
                    }
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss (e.g., notification sound)
                println("AudioStreaming: Audio focus LOSS_TRANSIENT - handled by phone state listener")
                hasAudioFocus = false
                // Phone call interruptions are already handled by PhoneStateListener
                // This catches other temporary interruptions
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                // Focus regained
                println("AudioStreaming: Audio focus GAIN")
                hasAudioFocus = true
                // Reconnection is handled by phone state listener
            }
        }
    }
}
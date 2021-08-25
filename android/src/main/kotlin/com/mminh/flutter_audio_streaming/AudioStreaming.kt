package com.mminh.flutter_audio_streaming

import android.app.Activity
import android.util.Log
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import com.pedro.rtplibrary.rtmp.RtmpOnlyAudio
import io.flutter.plugin.common.MethodChannel
import java.io.IOException

class AudioStreaming(
    private var activity: Activity? = null,
    private var dartMessenger: DartMessenger? = null
) : ConnectCheckerRtmp {

    private val rtmpAudio: RtmpOnlyAudio = RtmpOnlyAudio(this)
    private var prepared: Boolean = false

    fun prepare(
        bitrate: Int?, sampleRate: Int?, isStereo: Boolean?, echoCanceler: Boolean?,
        noiseSuppressor: Boolean?
    ): Boolean {
        prepared = true
        return rtmpAudio.prepareAudio(
            bitrate ?: (24 * 1024),
            sampleRate ?: 16000,
            isStereo ?: true,
            echoCanceler ?: true,
            (noiseSuppressor ?: true)
        )
    }

    private fun prepare(): Boolean {
        prepared = true
        return rtmpAudio.prepareAudio(
            24 * 1024,
            16000,
            true,
            true,
            true
        )
    }

    fun getStatistics(result: MethodChannel.Result) {
        val ret = hashMapOf<String, Any>()
        result.success(ret)
    }

    fun startStreaming(url: String?, result: MethodChannel.Result) {
        Log.d(TAG, "StartAudioStreaming url: $url")
        if (url == null) {
            result.error("StartAudioStreaming", "Must specify a url.", null)
            return
        }
        try {
            if (!rtmpAudio.isStreaming) {
                if (prepared || prepare()) {
                    // ready to start streaming
                    rtmpAudio.startStream(url)
                    val ret = hashMapOf<String, Any>()
                    ret["url"] = url
                    result.success(ret)
                } else {
                    result.error(
                        "AudioStreamingFailed",
                        "Error preparing stream, This device cant do it",
                        null
                    )
                    return
                }
            }
        } catch (e: IOException) {
            result.error("AudioStreamingFailed", e.message, null)
        }
    }

    fun muteStreaming(result: MethodChannel.Result) {
        try {
            rtmpAudio.disableAudio()
            result.success(null)
        } catch (e: IllegalStateException) {
            result.error("MuteAudioStreamingFailed", e.message, null)
        }
    }

    fun unMuteStreaming(result: MethodChannel.Result) {
        try {
            rtmpAudio.enableAudio()
            result.success(null)
        } catch (e: IllegalStateException) {
            result.error("UnMuteAudioStreamingFailed", e.message, null)
        }
    }

    fun stopStreaming(result: MethodChannel.Result) {
        try {
            rtmpAudio.stopStream()
            result.success(null)
        } catch (e: IllegalStateException) {
            result.error("StopAudioStreamingFailed", e.message, null)
        }
    }

    override fun onConnectionSuccessRtmp() {
        Log.d(TAG, "onConnectionSuccessRtmp")
    }

    override fun onNewBitrateRtmp(bitrate: Long) {
        Log.d(TAG, "onNewBitrateRtmp: $bitrate")
    }

    override fun onDisconnectRtmp() {
        Log.d(TAG, "onDisconnectRtmp")
        activity?.runOnUiThread {
            dartMessenger?.send(DartMessenger.EventType.RTMP_STOPPED, "Disconnected")
        }
    }

    override fun onAuthErrorRtmp() {
        Log.d(TAG, "onAuthErrorRtmp")
        activity?.runOnUiThread {
            dartMessenger?.send(DartMessenger.EventType.ERROR, "Auth error")
        }
    }

    override fun onAuthSuccessRtmp() {
        Log.d(TAG, "onAuthSuccessRtmp")
    }

    override fun onConnectionFailedRtmp(reason: String) {
        Log.d(TAG, "onConnectionFailedRtmp")
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
        Log.d(TAG, "onConnectionStartedRtmp")
    }

    companion object {
        const val TAG = "AudioStreaming"
    }
}
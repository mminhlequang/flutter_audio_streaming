package com.resideo.flutter_audio_streaming

import android.app.Activity
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import io.flutter.plugin.common.MethodChannel
import java.io.IOException


class AudioRecording(
    private var activity: Activity? = null,
    private var dartMessenger: DartMessenger? = null
) {

    private var recorder: MediaRecorder = MediaRecorder()
    private var path: String? = ""

    fun init(_path: String?) {
        path = _path
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
        recorder.setOutputFile(_path)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
    }

    fun start(result: MethodChannel.Result) {
        Log.d(TAG, "StartAudioRecording path: $path")
        try {
            recorder.prepare()
            recorder.start()
            val ret = hashMapOf<String, Any>()
            ret["path"] = path ?: ""
            result.success(ret)
        } catch (e: IOException) {
            result.error("AudioRecordingFailed", e.message, null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun pause(result: MethodChannel.Result) {
        try {
            recorder.pause()
            result.success(null)
        } catch (e: IOException) {
            result.error("AudioRecordingFailed", e.message, null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun resume(result: MethodChannel.Result) {
        try {
            recorder.resume()
            result.success(null)
        } catch (e: IOException) {
            result.error("AudioRecordingFailed", e.message, null)
        }
    }

    fun stop(result: MethodChannel.Result) {
        try {
            recorder.stop()
            val ret = hashMapOf<String, Any>()
            ret["path"] = path ?: ""
            result.success(ret)
        } catch (e: IllegalStateException) {
            result.error("StopAudioRecordingFailed", e.message, null)
        }
    }

    companion object {
        const val TAG = "AudioRecording"
    }
}
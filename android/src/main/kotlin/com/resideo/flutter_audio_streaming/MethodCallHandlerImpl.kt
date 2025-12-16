package com.resideo.flutter_audio_streaming

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import io.flutter.plugin.common.*
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

class MethodCallHandlerImpl(
    private val activity: Activity,
    private val messenger: BinaryMessenger,
    private val permissions: HandlerPermissions,
    private val permissionsRegistry: HandlerPermissions.PermissionStuff
) : MethodCallHandler {

    private val methodChannel: MethodChannel =
        MethodChannel(messenger, "plugins.flutter.io/flutter_audio_streaming")
    private var streamingMessenger: DartMessenger? = null
    private var recordingMessenger: DartMessenger? = null
    private var audioStreaming: AudioStreaming? = null
    private var audioRecording: AudioRecording? = null

    init {
        methodChannel.setMethodCallHandler(this)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {

            "prepare" -> {
                Log.i("AudioStreaming", "prepareStreaming")
                audioStreaming?.prepare(
                    call.argument("bitrate"),
                    call.argument("sampleRate"),
                    call.argument("isStereo"),
                    call.argument("echoCanceler"),
                    call.argument("noiseSuppressor")
                )
                result.success(null)
            }
            "getStatistics" -> {
                Log.i("AudioStreaming", "getStreamStatisticsAudio")
                audioStreaming?.getStatistics(result)
            }

            //Audio streaming
            "initializeStreaming" -> {
                Log.i("AudioStreaming", "initializeAudio")
                permissions.requestPermissions(
                    activity,
                    permissionsRegistry,
                    object : HandlerPermissions.ResultCallback {
                        override fun onResult(errorCode: String?, errorDescription: String?) {
                            if (errorCode == null) {
                                streamingMessenger = DartMessenger(messenger, "streaming_event")
                                audioStreaming = AudioStreaming(activity, streamingMessenger)
                                result.success(null)

                            } else {
                                result.error(errorCode, errorDescription, null)
                            }
                        }
                    })
            }
            "startStreaming" -> {
                Log.i("AudioStreaming", "startAudioStreaming")
                audioStreaming?.startStreaming(call.argument("url"), result)
            }
            "stopStreaming" -> {
                Log.i("AudioStreaming", "stopStreaming")
                audioStreaming?.stopStreaming(result)
            }
            "muteStreaming" -> {
                Log.i("AudioStreaming", "muteStreaming")
                audioStreaming?.muteStreaming(result)
            }
            "unMuteStreaming" -> {
                Log.i("AudioStreaming", "unMuteStreaming")
                audioStreaming?.unMuteStreaming(result)
            }
            "disposeStreaming" -> {
                Log.i("AudioStreaming", "disposeAudio")
                // Native camera view handles the view lifecircle by themselves
                result.success(null)
            }

            //Audio recording
            "initializeRecording" -> {
                Log.i("AudioRecording", "initializeAudio")
                Log.i("AudioRecording", call.argument("path") ?: "")
                permissions.requestPermissions(
                    activity,
                    permissionsRegistry,
                    object : HandlerPermissions.ResultCallback {
                        override fun onResult(errorCode: String?, errorDescription: String?) {
                            if (errorCode == null) {
                                recordingMessenger = DartMessenger(messenger, "recording_event")
                                audioRecording = AudioRecording(activity, recordingMessenger)
                                audioRecording?.init(call.argument("path"))
                                result.success(null)
                            } else {
                                result.error(errorCode, errorDescription, null)
                            }
                        }
                    })
            }
            "startRecording" -> {
                Log.i("AudioRecording", "startAudioRecording")
                audioRecording?.start(result)
            }
            "pauseRecording" -> {
                Log.i("AudioRecording", "pauseRecording")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    audioRecording?.pause(result)
                }
            }
            "resumeRecording" -> {
                Log.i("AudioRecording", "resumeRecording")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    audioRecording?.resume(result)
                }
            }
            "stopRecording" -> {
                Log.i("AudioRecording", "stopRecordingOrStreamingAudio")
                audioRecording?.stop(result)
            }
            "disposeRecording" -> {
                Log.i("AudioRecording", "disposeAudio")
                // Native camera view handles the view lifecircle by themselves
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    fun stopListening() {
        methodChannel.setMethodCallHandler(null)
    }
}
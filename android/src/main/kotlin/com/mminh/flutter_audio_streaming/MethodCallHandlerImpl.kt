package com.mminh.flutter_audio_streaming

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
    private var dartMicroMessenger: DartMessenger? = null

    init {
        methodChannel.setMethodCallHandler(this)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {

            "prepare" -> {
                Log.i("AudioStreaming", "prepareStreaming")
                audioController().prepare(
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
                audioController().getStatistics(result)
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
                                dartMicroMessenger = DartMessenger(messenger)
                                result.success(null)

                            } else {
                                result.error(errorCode, errorDescription, null)
                            }
                        }
                    })
            }
            "startStreaming" -> {
                Log.i("AudioStreaming", "startAudioStreaming")
                audioController().startStreaming(call.argument("url"), result)
            }
            "stopStreaming" -> {
                Log.i("AudioStreaming", "stopRecordingOrStreamingAudio")
                audioController().stopStreaming(result)
            }
            "disposeStreaming" -> {
                Log.i("AudioStreaming", "disposeAudio")
                // Native camera view handles the view lifecircle by themselves
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    fun stopListening() {
        methodChannel.setMethodCallHandler(null)
    }

    private fun audioController(): AudioStreaming = AudioStreaming(activity, dartMicroMessenger)
}
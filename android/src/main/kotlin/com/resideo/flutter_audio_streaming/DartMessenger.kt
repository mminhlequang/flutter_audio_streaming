package com.resideo.flutter_audio_streaming

import android.text.TextUtils
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import java.util.*

class DartMessenger(messenger: BinaryMessenger, id: String) {
    private var eventSink: EventSink? = null

    enum class EventType {
        ERROR, CAMERA_CLOSING, RTMP_STOPPED, RTMP_RETRY, ROTATION_UPDATE,
        AUDIO_INTERRUPTED, AUDIO_RESUMED
    }

    fun send(eventType: EventType, description: String?) {
        if (eventSink == null) {
            return
        }
        val event: MutableMap<String, String?> = HashMap()
        // Fixed deprecated toLowerCase() usage
        event["eventType"] = eventType.toString().lowercase(Locale.ROOT)
        // Only errors have a description.
        if (!TextUtils.isEmpty(description)) {
            event["errorDescription"] = description
        }
        eventSink!!.success(event)
    }

    init {
        EventChannel(messenger, "plugins.flutter.io/flutter_audio_streaming/$id")
            .setStreamHandler(
                object : EventChannel.StreamHandler {
                    override fun onListen(arguments: Any?, sink: EventSink) {
                        eventSink = sink
                    }

                    override fun onCancel(arguments: Any?) {
                        eventSink = null
                    }
                }
            )
    }
}

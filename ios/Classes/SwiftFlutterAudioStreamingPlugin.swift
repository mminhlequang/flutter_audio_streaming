import Flutter
import UIKit

public class SwiftFlutterAudioStreamingPlugin: NSObject, FlutterPlugin {
  static var eventSink : FlutterEventSink? = nil
  static var eventChannel : FlutterEventChannel? = nil
  var audioStreaming: AudioStreaming? = nil
    
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "plugins.flutter.io/flutter_audio_streaming", binaryMessenger: registrar.messenger())
    eventChannel = FlutterEventChannel(name: "plugins.flutter.io/flutter_audio_streaming/streaming_event", binaryMessenger: registrar.messenger())
    eventChannel!.setStreamHandler(StreamHandlerEvent())
    let instance = SwiftFlutterAudioStreamingPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
    print("SwiftFlutterAudioStreamingPlugin: register")
  }
    
  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "getStatistics":
        let reply: NSDictionary = [
            "enable": true
        ]
        result(reply)
    case "prepare":
        result(nil)
    case "initializeStreaming":
        audioStreaming = AudioStreaming()
        if let sink = SwiftFlutterAudioStreamingPlugin.eventSink {
            audioStreaming?.setEventSink(sink)
        }
        audioStreaming?.setup(result: result)
    case "startStreaming":
        // Makes sure arguments exists and is a Map
        guard let args = call.arguments as? [String: Any] else {
            result(toError(code: "Missing argument", message: "url", details: nil))
            return
        }

        guard let url = args["url"] as? String else {
            result( toError(code: "Missing argument", message: "url", details: nil) )
            return
        }
        audioStreaming?.start(url: url, result: result)
    case "stopStreaming":
        audioStreaming?.stop()
        result(nil)
    case "disposeStreaming":
        audioStreaming?.dispose()
        result(nil)
    case "muteStreaming":
        audioStreaming?.mute()
        result(nil)
    case "unMuteStreaming":
        audioStreaming?.unmute()
        result(nil)
    default:
        result(nil)
    }
  }
    
    private func toError(code : String, message : String, details : String?) -> FlutterError {
        return FlutterError(
                        code: code,
                        message: message,
                        details: details
                    )
    }
}

class StreamHandlerEvent: NSObject, FlutterStreamHandler {
    func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        SwiftFlutterAudioStreamingPlugin.eventSink = events
        print("StreamHandlerEvent: onListen")
        return nil
    }
    
    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        SwiftFlutterAudioStreamingPlugin.eventSink = nil
        //checking remioving it to fix MissingPluginException(No implementation found for method cancel on channel plugins.flutter.io/flutter_audio_streaming/streaming_event
        //SwiftFlutterAudioStreamingPlugin.eventChannel?.setStreamHandler(nil)
        print("StreamHandlerEvent: onCancel")
        return nil
    }
    
    
}

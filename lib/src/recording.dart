// import 'dart:async';
//
// import 'package:flutter/foundation.dart';
// import 'package:flutter/services.dart';
// import 'package:flutter_audio_streaming/src/exception.dart';
//
// import 'channel.dart';

part 'recording_value.dart';

// class RecordingController extends ValueNotifier<RecordingValue> {
//   //ERROR, RTMP_STOPPED, RTMP_RETRY
//   static const String ERROR = "error";
//   static const String RTMP_STOPPED = "rtmp_stopped";
//   static const String RTMP_RETRY = "rtmp_retry";
//
//   RecordingController() : super(const RecordingValue.uninitialized());
//
//   final RecordingBaseChannel channel = RecordingChannel();
//
//   bool _isDisposed = false;
//   StreamSubscription<dynamic> _eventSubscription;
//   Completer<void> _creatingCompleter;
//
//   Future<void> initialize(String url) async {
//     if (_isDisposed) {
//       return Future<void>.value();
//     }
//     try {
//       _creatingCompleter = Completer<void>();
//       await channel.initialize(url);
//       value = value.copyWith(
//         isInitialized: true,
//       );
//     } on PlatformException catch (e) {
//       throw AudioStreamingException(e.code, e.message);
//     }
//     _eventSubscription = EventChannel(
//         'plugins.flutter.io/flutter_audio_streaming/recording_event')
//         .receiveBroadcastStream()
//         .listen(_listener);
//     _creatingCompleter.complete();
//     return _creatingCompleter.future;
//   }
//
//   /// Listen to events from the native plugins.
//   void _listener(dynamic event) {
//     final Map<dynamic, dynamic> map = event;
//     if (_isDisposed || event == null) {
//       return;
//     }
//     // Android: Event {eventType: rtmp_retry, errorDescription: BadName received}
//     // iOS: Event {event: rtmp_retry, errorDescription: connection failed rtmpStatus}
//     final String eventType =
//         map['eventType'] as String ?? map['event'] as String;
//     final String errorDescription = map['errorDescription'];
//     final Map<String, dynamic> uniEvent = <String, dynamic>{
//       'eventType': eventType,
//       'errorDescription': errorDescription
//     };
//     switch (eventType) {
//       default:
//         value = value.copyWith(event: uniEvent);
//         break;
//     }
//   }
//
//   Future<void> start() async {
//     if (!value.isInitialized || _isDisposed) {
//       throw AudioStreamingException(
//         'Uninitialized AudioController',
//         'startAudioStreaming was called on uninitialized AudioController',
//       );
//     }
//     if (value.isRecording) {
//       throw AudioStreamingException(
//         'A audio streaming is already started.',
//         'startAudioStreaming was called when a recording is already started.',
//       );
//     }
//
//     try {
//       await channel.start();
//       value = value.copyWith(isRecording: true);
//     } on PlatformException catch (e) {
//       throw AudioStreamingException(e.code, e.message);
//     }
//   }
//
//   /// Pause.
//   Future<void> pause() async {
//     if (!value.isInitialized || _isDisposed) {
//       throw AudioStreamingException(
//         'Uninitialized CameraController',
//         'stopVideoStreaming was called on uninitialized CameraController',
//       );
//     }
//     if (!value.isRecording) {
//       throw AudioStreamingException(
//         'No video is recording',
//         'stopVideoStreaming was called when no video is streaming.',
//       );
//     }
//     try {
//       value = value.copyWith(isRecording: false, isPause: true);
//       await channel.pause();
//     } on PlatformException catch (e) {
//       throw AudioStreamingException(e.code, e.message);
//     }
//   }
//
//   /// Resume record
//   Future<void> resume() async {
//     if (!value.isInitialized || _isDisposed) {
//       throw AudioStreamingException(
//         'Uninitialized CameraController',
//         'stopVideoStreaming was called on uninitialized CameraController',
//       );
//     }
//     if (!value.isPause) {
//       throw AudioStreamingException(
//         'No video is recording',
//         'stopVideoStreaming was called when no video is streaming.',
//       );
//     }
//     try {
//       value = value.copyWith(isRecording: true, isPause: false);
//       await channel.resume();
//     } on PlatformException catch (e) {
//       throw AudioStreamingException(e.code, e.message);
//     }
//   }
//
//   /// Stop streaming.
//   Future<String> stop() async {
//     if (!value.isInitialized || _isDisposed) {
//       throw AudioStreamingException(
//         'Uninitialized CameraController',
//         'stopVideoStreaming was called on uninitialized CameraController',
//       );
//     }
//     if (!value.isRecording) {
//       throw AudioStreamingException(
//         'No video is recording',
//         'stopVideoStreaming was called when no video is streaming.',
//       );
//     }
//     try {
//       value = value.copyWith(isRecording: false);
//       final reply = await channel.stop();
//       return reply is Map ? reply['path'] : null;
//     } on PlatformException catch (e) {
//       throw AudioStreamingException(e.code, e.message);
//     }
//   }
//
//   /// Releases the resources of this camera.
//   @override
//   Future<void> dispose() async {
//     if (_isDisposed) {
//       return;
//     }
//     _isDisposed = true;
//     super.dispose();
//     if (_creatingCompleter != null) {
//       await _creatingCompleter.future;
//       await channel.dispose();
//       await _eventSubscription?.cancel();
//     }
//   }
// }

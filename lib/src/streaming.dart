import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_audio_streaming/src/exception.dart';

import 'channel.dart';

part 'streaming_value.dart';

class StreamingController extends ValueNotifier<AudioValue> {
  //ERROR, RTMP_STOPPED, RTMP_RETRY, AUDIO_INTERRUPTED, AUDIO_RESUMED
  static const String ERROR = "error";
  static const String RTMP_STOPPED = "rtmp_stopped";
  static const String RTMP_RETRY = "rtmp_retry";
  static const String AUDIO_INTERRUPTED = "audio_interrupted";
  static const String AUDIO_RESUMED = "audio_resumed";

  StreamingController() : super(const AudioValue.uninitialized());

  final StreamingBaseChannel channel = StreamingChannel();

  bool _isDisposed = false;
  StreamSubscription<dynamic>? _eventSubscription;
  Completer<void>? _creatingCompleter;

  Future<void> initialize() async {
    if (_isDisposed) {
      return Future<void>.value();
    }
    try {
      _creatingCompleter = Completer<void>();
      await channel.initialize();
      value = value.copyWith(
        isInitialized: true,
      );
    } on PlatformException catch (e) {
      throw AudioStreamingException(e.code, e.message);
    }
    _eventSubscription = EventChannel('plugins.flutter.io/flutter_audio_streaming/streaming_event')
        .receiveBroadcastStream()
        .listen(_listener);
    _creatingCompleter!.complete();
    return _creatingCompleter!.future;
  }

  Future<void> prepare(
      {int? bitrate,
      int? sampleRate,
      bool? isStereo,
      bool? echoCanceler,
      bool? noiseSuppressor = true}) async {
    await channel.prepare(
        bitrate: bitrate,
        sampleRate: sampleRate,
        isStereo: isStereo,
        echoCanceler: echoCanceler,
        noiseSuppressor: noiseSuppressor);
  }

  /// Listen to events from the native plugins.
  void _listener(dynamic event) {
    final Map<dynamic, dynamic> map = event;
    if (_isDisposed || event == null) {
      return;
    }
    // Android: Event {eventType: rtmp_retry, errorDescription: BadName received}
    // iOS: Event {event: rtmp_retry, errorDescription: connection failed rtmpStatus}
    final String eventType = map['eventType'] as String? ?? map['event'] as String;
    final String errorDescription = map['errorDescription'];
    final Map<String, dynamic> uniEvent = <String, dynamic>{
      'eventType': eventType,
      'errorDescription': errorDescription
    };
    switch (eventType) {
      case 'error':
        value = value.copyWith(errorDescription: errorDescription, event: uniEvent);
        break;
      case 'rtmp_connected':
        value = value.copyWith(event: uniEvent);
        break;
      case 'rtmp_retry':
        value = value.copyWith(event: uniEvent);
        break;
      case 'rtmp_stopped':
        value = value.copyWith(isStreaming: false, event: uniEvent);
        break;
      case 'audio_interrupted':
        value = value.copyWith(event: uniEvent);
        break;
      case 'audio_resumed':
        value = value.copyWith(event: uniEvent);
        break;
      default:
        value = value.copyWith(event: uniEvent);
        break;
    }
  }

  Future<void> start(String url, {bool noiseSuppressor = true}) async {
    if (!value.isInitialized! || _isDisposed) {
      throw AudioStreamingException(
        'Uninitialized AudioController',
        'startAudioStreaming was called on uninitialized AudioController',
      );
    }
    if (value.isStreaming!) {
      throw AudioStreamingException(
        'A audio streaming is already started.',
        'startAudioStreaming was called when a recording is already started.',
      );
    }

    try {
      await channel.start(url);
      value = value.copyWith(isStreaming: true);
    } on PlatformException catch (e) {
      throw AudioStreamingException(e.code, e.message);
    }
  }

  /// Mute streaming.
  Future<void> mute() async {
    if (!value.isInitialized! || _isDisposed) {
      throw AudioStreamingException(
        'Uninitialized CameraController',
        'stopVideoStreaming was called on uninitialized CameraController',
      );
    }
    if (!value.isStreaming!) {
      throw AudioStreamingException(
        'No video is recording',
        'stopVideoStreaming was called when no video is streaming.',
      );
    }
    try {
      value = value.copyWith(isMuted: true);
      await channel.mute();
    } on PlatformException catch (e) {
      throw AudioStreamingException(e.code, e.message);
    }
  }

  /// Stop streaming.
  Future<void> unMute() async {
    if (!value.isInitialized! || _isDisposed) {
      throw AudioStreamingException(
        'Uninitialized CameraController',
        'stopVideoStreaming was called on uninitialized CameraController',
      );
    }
    if (!value.isStreaming!) {
      throw AudioStreamingException(
        'No video is recording',
        'stopVideoStreaming was called when no video is streaming.',
      );
    }
    try {
      value = value.copyWith(isMuted: false);
      await channel.unMute();
    } on PlatformException catch (e) {
      throw AudioStreamingException(e.code, e.message);
    }
  }

  /// Stop streaming.
  Future<void> stop() async {
    if (!value.isInitialized! || _isDisposed) {
      throw AudioStreamingException(
        'Uninitialized CameraController',
        'stopVideoStreaming was called on uninitialized CameraController',
      );
    }
    if (!value.isStreaming!) {
      throw AudioStreamingException(
        'No video is recording',
        'stopVideoStreaming was called when no video is streaming.',
      );
    }
    try {
      value = value.copyWith(isStreaming: false);
      await channel.stopStreaming();
    } on PlatformException catch (e) {
      throw AudioStreamingException(e.code, e.message);
    }
  }

  /// Releases the resources of this camera.
  @override
  Future<void> dispose() async {
    if (_isDisposed) {
      return;
    }
    _isDisposed = true;
    super.dispose();
    if (_creatingCompleter != null) {
      await _creatingCompleter!.future;
      await channel.dispose();
      await _eventSubscription?.cancel();
    }
  }
}

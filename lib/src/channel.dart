import 'package:flutter/services.dart';

final MethodChannel _channel =
    const MethodChannel('plugins.flutter.io/flutter_audio_streaming');

class StreamingChannel extends StreamingBaseChannel {
  @override
  initialize() async {
    await _channel.invokeMapMethod<String, dynamic>('initializeStreaming');
  }

  @override
  getStatistics() async {
    await _channel.invokeMapMethod<String, dynamic>(
      'getStatistics',
      <String, dynamic>{},
    );
  }

  @override
  prepare(
      {int bitrate,
      int sampleRate,
      bool isStereo,
      bool echoCanceler,
      noiseSuppressor}) async {
    await _channel.invokeMapMethod<String, dynamic>(
      'prepare',
      <String, dynamic>{},
    );
  }

  @override
  start(String url) async {
    await _channel.invokeMapMethod<String, dynamic>(
      'startStreaming',
      <String, dynamic>{
        "url" : url
      },
    );
  }

  @override
  stopStreaming() async {
    await _channel.invokeMapMethod<String, dynamic>(
      'stopStreaming',
      <String, dynamic>{},
    );
  }

  @override
  dispose() async {
    await _channel.invokeMapMethod<String, dynamic>(
      'disposeStreaming',
      <String, dynamic>{},
    );
  }
}

abstract class StreamingBaseChannel extends _BaseChannel {
  initialize();

  start(String url);

  stopStreaming();

  dispose();
}

abstract class RecordingBaseChannel extends _BaseChannel {
  initialize();

  start(String url);

  stopStreaming();

  dispose();
}

abstract class _BaseChannel {
  prepare(
      {int bitrate,
      int sampleRate,
      bool isStereo,
      bool echoCanceler,
      noiseSuppressor});

  getStatistics();
}

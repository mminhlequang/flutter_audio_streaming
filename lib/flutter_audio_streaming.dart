
import 'dart:async';

import 'package:flutter/services.dart';

class FlutterAudioStreaming {
  static const MethodChannel _channel =
      const MethodChannel('flutter_audio_streaming');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}

# flutter_audio_streaming

A Flutter plugin for record or streaming audio by RTMP

## Getting Started

For android I use [rtmp-rtsp-stream-client-java](https://github.com/pedroSG94/rtmp-rtsp-stream-client-java)
and for iOS I use
[HaishinKit.swift](https://github.com/shogo4405/HaishinKit.swift)

## Features:

* Push RTMP audio
* Record audio (Develop mode)

## Installation

First, add `flutter_audio_streaming` as a [dependency in your pubspec.yaml file](https://flutter.io/using-packages/).

### iOS

Add two rows to the `ios/Runner/Info.plist`:

* one with the key `Privacy - Camera Usage Description` and a usage description.
* and one with the key `Privacy - Microphone Usage Description` and a usage description.

Or in text format add the key:

```
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsArbitraryLoads</key>
    <true/>
</dict>
<key>UIBackgroundModes</key>
<array>
    <string>processing</string>
</array>
<key>NSMicrophoneUsageDescription</key>
<string>App requires access to the microphone for live streaming feature.</string>
```

### Android

Change the minimum Android sdk version to 21 (or higher) in your `android/app/build.gradle` file.

```
minSdkVersion 21
```

Add the following permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
```

**Note**: The `READ_PHONE_STATE` permission is required for phone call detection. Without it, the plugin cannot detect active phone calls and prevent streaming conflicts.

Need to add in a section to the packaging options to exclude a file, or gradle will error on building.

```
packagingOptions {
   exclude 'project.clj'
}
```
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

First, add `camera` as a [dependency in your pubspec.yaml file](https://flutter.io/using-packages/).

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
<key>NSPhotoLibraryUsageDescription</key>
<string></string>
<key>UIBackgroundModes</key>
<array>
    <string>processing</string>
</array>
<key>NSCameraUsageDescription</key>
<string>App requires access to the camera for live streaming feature.</string>
<key>NSMicrophoneUsageDescription</key>
<string>App requires access to the microphone for live streaming feature.</string>
```

### Android

Change the minimum Android sdk version to 21 (or higher) in your `android/app/build.gradle` file.

```
minSdkVersion 21
```

Need to add in a section to the packaging options to exclude a file, or gradle will error on building.

```
packagingOptions {
   exclude 'project.clj'
}
```
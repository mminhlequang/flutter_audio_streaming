#import "FlutterAudioStreamingPlugin.h"
#if __has_include(<flutter_audio_streaming/flutter_audio_streaming-Swift.h>)
#import <flutter_audio_streaming/flutter_audio_streaming-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "flutter_audio_streaming-Swift.h"
#endif

@implementation FlutterAudioStreamingPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterAudioStreamingPlugin registerWithRegistrar:registrar];
}
@end

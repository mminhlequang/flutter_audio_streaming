part of './streaming.dart';

/// The state of a [StreamingController].
class AudioValue {
  const AudioValue({
    this.isInitialized,
    this.isStreaming,
    this.errorDescription,
    this.isMuted,
    this.event,
  });

  const AudioValue.uninitialized()
      : this(
          isInitialized: false,
          isStreaming: false,
          isMuted: false,
        );

  /// True after [StreamingController.initialize] has completed successfully.
  final bool isInitialized;

  /// True when the streaming is true
  final bool isStreaming;

  /// True when the micro muted is true
  final bool isMuted;

  /// Raw event info
  final dynamic event;

  final String errorDescription;

  bool get hasError => errorDescription != null;

  AudioValue copyWith({
    bool isInitialized,
    bool isStreaming,
    String errorDescription,
    bool isMuted,
    dynamic event,
  }) {
    return AudioValue(
        isInitialized: isInitialized ?? this.isInitialized,
        isStreaming: isStreaming ?? this.isStreaming,
        errorDescription: errorDescription ?? this.errorDescription,
        event: event ?? this.event,
        isMuted: isMuted ?? this.isMuted);
  }

  @override
  String toString() {
    return '$runtimeType('
        'isStreaming: $isStreaming, '
        'event: $event, '
        'isInitialized: $isInitialized, '
        'isMuted: $isMuted, '
        'errorDescription: $errorDescription)';
  }
}

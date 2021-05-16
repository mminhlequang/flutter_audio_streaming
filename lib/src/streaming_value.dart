part of './streaming.dart';

/// The state of a [StreamingController].
class AudioValue {
  const AudioValue({
    this.isInitialized,
    this.isStreaming,
    this.errorDescription,
    this.event,
  });

  const AudioValue.uninitialized()
      : this(
          isInitialized: false,
          isStreaming: false,
        );

  /// True after [StreamingController.initialize] has completed successfully.
  final bool isInitialized;

  /// True when the streaming is true
  final bool isStreaming;

  /// Raw event info
  final dynamic event;

  final String errorDescription;

  bool get hasError => errorDescription != null;

  AudioValue copyWith({
    bool isInitialized,
    bool isStreaming,
    String errorDescription,
    dynamic event,
  }) {
    return AudioValue(
      isInitialized: isInitialized ?? this.isInitialized,
      isStreaming: isStreaming ?? this.isStreaming,
      errorDescription: errorDescription ?? this.errorDescription,
      event: event ?? this.event,
    );
  }

  @override
  String toString() {
    return '$runtimeType('
        'isStreaming: $isStreaming, '
        'event: $event, '
        'isInitialized: $isInitialized, '
        'errorDescription: $errorDescription)';
  }
}

part of './recording.dart';

/// The state of a [StreamingController].
class RecordingValue {
  const RecordingValue({
    this.isInitialized,
    this.isPause,
    this.isRecording,
    this.errorDescription,
    this.event,
  });

  const RecordingValue.uninitialized()
      : this(isInitialized: false, isRecording: false, isPause: false);

  /// True after [StreamingController.initialize] has completed successfully.
  final bool? isInitialized;

  /// True when the streaming is true
  final bool? isRecording;

  /// True when pause record
  final bool? isPause;

  /// Raw event info
  final dynamic event;

  final String? errorDescription;

  bool get hasError => errorDescription != null;

  RecordingValue copyWith({
    bool? isInitialized,
    bool? isRecording,
    bool? isPause,
    String? errorDescription,
    dynamic event,
  }) {
    return RecordingValue(
        isInitialized: isInitialized ?? this.isInitialized,
        isRecording: isRecording ?? this.isRecording,
        errorDescription: errorDescription ?? this.errorDescription,
        event: event ?? this.event,
        isPause: isPause ?? this.isPause);
  }

  @override
  String toString() {
    return '$runtimeType('
        'isRecording: $isRecording, '
        'event: $event, '
        'isPause: $isPause, '
        'isInitialized: $isInitialized, '
        'errorDescription: $errorDescription)';
  }
}

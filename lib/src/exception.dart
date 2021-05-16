/// This is thrown when the plugin reports an error.
class AudioStreamingException implements Exception {
  AudioStreamingException(this.code, this.description);

  String code;
  String description;

  @override
  String toString() => '$runtimeType($code, $description)';
}

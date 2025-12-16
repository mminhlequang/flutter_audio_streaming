import 'dart:async';
import 'dart:developer';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_audio_streaming/flutter_audio_streaming.dart';
import 'package:path_provider/path_provider.dart';

class StreamingAndRecordExample extends StatefulWidget {
  const StreamingAndRecordExample({Key? key}) : super(key: key);

  @override
  _StreamingAndRecordExampleState createState() =>
      _StreamingAndRecordExampleState();
}

class _StreamingAndRecordExampleState extends State<StreamingAndRecordExample>
    with WidgetsBindingObserver {
  StreamingController streamingController = StreamingController();
  RecordingController recordingController = RecordingController();

  TextEditingController _textFieldController = TextEditingController(
      text:
          "rtmp://voip.bdata.vn/LiveApp/7ri7u5yK2raV4hs4t50tL8fxzS021625623648");
  late String _localPath;
  bool isVisible = true;
  Timer? _timer;
  int seconds = 0;
  int minutes = 0;
  int hours = 0;

  String get textDateStream =>
      "${_min(hours)} : ${_min(minutes)} : ${_min(seconds)}";

  bool get isStreaming => streamingController.value.isStreaming ?? false;

  @override
  void initState() {
    super.initState();
    initialize();
  }

  @override
  void dispose() async {
    _timer?.cancel();
    _timer = null;
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
    if (isStreaming) await streamingController.stop();
    streamingController.dispose();
  }

  @override
  Future<void> didChangeAppLifecycleState(AppLifecycleState state) async {
    // App state changed before we got the chance to initialize.
    // if (controller == null || !controller.value.isInitialized) {
    //   return;
    // }
    // if (state == AppLifecycleState.paused) {
    //   isVisible = false;
    //   if (isStreaming) {
    //     await pauseStreaming();
    //   }
    // } else if (state == AppLifecycleState.resumed) {
    //   isVisible = true;
    //   if (controller != null) {
    //     if (isStreaming) {
    //       await resumeStreaming();
    //     }
    //   }
    // }
  }

  void initialize() async {
    streamingController.addListener(() async {
      if (streamingController.value.hasError) {
        showInSnackBar(
            'Camera error ${streamingController.value.errorDescription}');
        await stopStreaming();
      } else
        try {
          if (streamingController.value.event == null) return;
          final Map<dynamic, dynamic> event =
              streamingController.value.event as Map<dynamic, dynamic>;
          print('Event: $event');
          final String eventType = event['eventType'] as String;
          switch (eventType) {
            case StreamingController.ERROR:
              break;
            case StreamingController.RTMP_STOPPED:
              break;
            case StreamingController.RTMP_RETRY:
              if (isVisible && isStreaming) {
                await stopStreaming();
              }
              break;
          }
        } catch (e) {
          print('initialize streaming: $e');
        }
    });
    await streamingController.initialize();
    streamingController.prepare();

    recordingController.addListener(() async {
      if (streamingController.value.hasError) {
        showInSnackBar(
            'Camera error ${streamingController.value.errorDescription}');
        await stopStreaming();
      } else
        try {
          if (recordingController.value.event == null) return;
          final Map<dynamic, dynamic> event =
              recordingController.value.event as Map<dynamic, dynamic>;
          print('Event recordingController: $event');
          final String eventType = event['eventType'] as String;
          switch (eventType) {
            case StreamingController.ERROR:
              break;
            case StreamingController.RTMP_STOPPED:
              break;
            case StreamingController.RTMP_RETRY:
              if (isVisible && isStreaming) {
                await stopStreaming();
              }
              break;
          }
        } catch (e) {
          print('initialize record: $e');
        }
    });
    await _prepareSaveDir();
    await recordingController.initialize(
        pathFile(DateTime.now().millisecondsSinceEpoch.toString() + '.aac'));
  }

  String pathFile(String name) => _localPath + Platform.pathSeparator + name;

  Future<void> _prepareSaveDir() async {
    _localPath =
        (await _findLocalPath())! + Platform.pathSeparator + 'RecordAudio';

    final savedDir = Directory(_localPath);
    bool hasExisted = await savedDir.exists();
    if (!hasExisted) {
      savedDir.create();
    }
  }

  Future<String?> _findLocalPath() async {
    final directory = Platform.isAndroid
        ? await getExternalStorageDirectory()
        : await getApplicationDocumentsDirectory();
    return directory?.path;
  }

  Future<String> startStreaming() async {
    if (!streamingController.value.isInitialized!) {
      showInSnackBar('Error: is not Initialized.');
      return '';
    }
    if (isStreaming) return '';
    // Open up a dialog for the url
    String? url = await _getUrl();
    if (url == null || url.isEmpty) return '';
    try {
      await streamingController.start(url);
      await recordingController.start();
      hours = 0;
      minutes = 0;
      seconds = 0;
      _timer = new Timer.periodic(
        Duration(seconds: 1),
        (Timer timer) => setState(
          () {
            seconds = seconds + 1;
            if (seconds > 59) {
              minutes += 1;
              seconds = 0;
              if (minutes > 59) {
                hours += 1;
                minutes = 0;
              }
            }
          },
        ),
      );
    } on AudioStreamingException catch (e) {
      _showException("startStreaming", e);
      return '';
    }
    return url;
  }

  Future<String?> _getUrl() async {
    // Open up a dialog for the url
    String result = _textFieldController.text;

    return await showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: Text('Url to Stream to'),
            content: TextField(
              controller: _textFieldController,
              decoration: InputDecoration(hintText: "Url to Stream to"),
              onChanged: (String str) => result = str,
            ),
            actions: <Widget>[
              new TextButton(
                child: new Text(
                    MaterialLocalizations.of(context).cancelButtonLabel),
                onPressed: () {
                  Navigator.of(context).pop();
                },
              ),
              TextButton(
                child: Text(MaterialLocalizations.of(context).okButtonLabel),
                onPressed: () {
                  Navigator.pop(context, result);
                },
              )
            ],
          );
        });
  }

  Future<void> stopStreaming() async {
    if (!streamingController.value.isInitialized!) {
      return;
    }
    if (!isStreaming) {
      return;
    }
    try {
      await streamingController.stop();
      final path = await recordingController.stop();
      print('path: $path');
      if (path != null && path.isNotEmpty) {
        //TODO: add ne wplayer here
        //AudioPlayer audioPlayer = AudioPlayer();
        //await audioPlayer.play(path, isLocal: true);
      }
      _timer?.cancel();
      _timer = null;
      setState(() {});
    } on AudioStreamingException catch (e) {
      _showException("stopStreaming", e);
      return null;
    }
  }

  void _showException(String at, AudioStreamingException e) {
    log("AudioStreaming: Error at $at \n${e.code}\n${e.description}");
    showInSnackBar('$at Error: ${e.code}\n${e.description}');
  }

  String _min(int _) {
    if (_ < 10) return "0$_";
    return _.toString();
  }

  void showInSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(message),
      duration: const Duration(seconds: 1),
      action: SnackBarAction(
        label: 'OK',
        onPressed: () => ScaffoldMessenger.of(context).hideCurrentSnackBar(),
      ),
    ));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Audio streaming and record example'),
      ),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              textDateStream,
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 32),
            ),
            const SizedBox(
              height: 32,
            ),
            ElevatedButton(
                onPressed: isStreaming ? stopStreaming : startStreaming,
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(isStreaming
                        ? Icons.stop_circle_outlined
                        : Icons.play_circle_outline_outlined),
                    const SizedBox(
                      width: 4,
                    ),
                    Text(
                      isStreaming ? "Stop" : "Start" + "Stream",
                      style:
                          TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                    )
                  ],
                ))
          ],
        ),
      ),
    );
  }
}

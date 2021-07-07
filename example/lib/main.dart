import 'package:flutter/material.dart';

import 'streaming.dart';
import 'streaming_and_record.dart';

Future<void> main() async {
  runApp(App());
}

class App extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: true,
      home: Home(),
    );
  }
}

class Home extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
        body: Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ElevatedButton(
              onPressed: () {},
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.multitrack_audio_outlined),
                  const SizedBox(
                    width: 4,
                  ),
                  Text(
                    "Recording",
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  )
                ],
              )),
          const SizedBox(
            height: 24,
          ),
          ElevatedButton(
              onPressed: () => Navigator.push(context,
                  MaterialPageRoute(builder: (_) => StreamingExample())),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.audiotrack_rounded),
                  const SizedBox(
                    width: 4,
                  ),
                  Text(
                    "Streaming",
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  )
                ],
              )),
          const SizedBox(
            height: 24,
          ),
          ElevatedButton(
              onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (_) => StreamingAndRecordExample())),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.audiotrack_rounded),
                  Icon(Icons.multitrack_audio_outlined),
                  const SizedBox(
                    width: 4,
                  ),
                  Text(
                    "Streaming And Record",
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  )
                ],
              ))
        ],
      ),
    ));
  }
}

import 'package:flutter/material.dart';

import 'streaming.dart';

Future<void> main() async {
  runApp(App());
}

class App extends StatelessWidget {
  const App({Key key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: true,
      home: Home(),
    );
  }
}

class Home extends StatelessWidget {
  const Home({Key key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        body: Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ElevatedButton(
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
              ))
        ],
      ),
    ));
  }
}

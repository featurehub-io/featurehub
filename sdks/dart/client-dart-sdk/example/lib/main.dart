import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:flutter/material.dart';
import 'package:logging/logging.dart';

ClientFeatureRepository? featurehub;
FeatureHubSimpleApi? featurehubApi;

void main() {
  Logger.root.level = Level.ALL; // defaults to Level.INFO
  Logger.root.onRecord.listen((record) {
    // ignore: avoid_print
    print('${record.level.name}: ${record.time}: ${record.message}');
    if (record.object != null) {
      // ignore: avoid_print
      print('exception:${record.object}');
    }
    if (record.stackTrace != null) {
      // ignore: avoid_print
      print('stackTrace:${record.stackTrace}');
    }
  });

  featurehub = ClientFeatureRepository();

  // if you use this one (GET based, which is geneally preferred in Mobile) then
  // you should uncomment the refresh down below which causes the GET to happen again.
  // this next step can be delayed based on environment loading, etc
  featurehubApi = FeatureHubSimpleApi(
      'http://localhost:8903',
      [
        'default/82afd7ae-e7de-4567-817b-dd684315adf7/SJXBRyGCe1dZwnL7OQYUiJ5J8VcoMrrHP3iKCrkpYovhNIuwuIPNYGy7iOFeKE4Kaqp5sT7g5X2qETsW'
      ],
      featurehub!);
  featurehubApi!.request();

  // final rs = EventSourceRepositoryListener(
  //     'http://localhost:8903',
  //     'default/82afd7ae-e7de-4567-817b-dd684315adf7/SJXBRyGCe1dZwnL7OQYUiJ5J8VcoMrrHP3iKCrkpYovhNIuwuIPNYGy7iOFeKE4Kaqp5sT7g5X2qETsW',
  //     featurehub!);

  featurehub!.clientContext
      .userKey('susanna')
      .device(StrategyAttributeDeviceName.desktop)
      .platform(StrategyAttributePlatformName.macos)
      .attr('sausage', 'cumberlands')
      .build();
  // featurehubApi.request();
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  final String title;

  MyHomePage({Key? key, required this.title}) : super(key: key);

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _counter = 0;

  void _incrementCounter() {
    setState(() {
      _counter++;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: StreamBuilder<FeatureStateHolder>(
          stream: featurehub!.feature('FLUTTER_COLOUR').featureUpdateStream,
          builder: (context, snapshot) {
            return RefreshIndicator(
              onRefresh: () {
                // return Future.value();
                return featurehubApi!.request();
              },
              child: ListView(
                children: [
                  Container(
                    color: determineColour(snapshot.data),
                    child: Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: <Widget>[
                          Text(
                            'You have pushed the button this many times:',
                          ),
                          Text(
                            '$_counter',
                            style: Theme.of(context).textTheme.headline4,
                          ),
                        ],
                      ),
                    ),
                  )
                ],
              ),
            );
          }),
      floatingActionButton: FloatingActionButton(
        onPressed: _incrementCounter,
        tooltip: 'Increment',
        child: Icon(Icons.add),
      ),
    );
  }

  Color determineColour(FeatureStateHolder? data) {
    // ignore: avoid_print
    print('colour changed? $data');
    if (data == null || !data.exists) {
      return Colors.white;
    }
    // ignore: avoid_print
    print('colour is ${data.stringValue}');
    switch (data.stringValue) {
      case 'blue':
        return Colors.blue;
      case 'yellow':
        return Colors.yellow;
      case 'purple':
        return Colors.purple;
      case 'green':
        return Colors.green;
      default:
        return Colors.white;
    }
  }
}

import 'dart:async';
import 'dart:html';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:fh_userstate/repository_loader.dart';
import 'package:flutter/material.dart';
import 'package:rxdart/rxdart.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'FeatureHub User State',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: BlocProvider(
          creator: (_b, _c) => RepositoryLoaderBloc(),
          child: Builder(
            builder: (context) {
              return BlocProvider(
                  creator: (_b, _c) => HostingBloc(
                      BlocProvider.of<RepositoryLoaderBloc>(context)),
                  child: HostingPage());
            },
          )),
    );
  }
}

enum HostingPageState { Loading, RequestUrl, Loaded }

class HostingBloc implements Bloc {
  final RepositoryLoaderBloc repositoryLoaderBloc;
  final hostingPageSource = BehaviorSubject<HostingPageState>();

  StreamSubscription<Readyness> _readynessSubscriber;

  HostingBloc(this.repositoryLoaderBloc) {
    _readynessSubscriber =
        repositoryLoaderBloc.repository.readynessStream.listen((event) {
      if (event == Readyness.Ready) {
        hostingPageSource.add(HostingPageState.Loaded);
      }
      if (event == Readyness.Failed) {
        hostingPageSource.add(HostingPageState.RequestUrl);
      }
      if (event == Readyness.NotReady &&
          repositoryLoaderBloc.hasStoredEnvironment()) {
        hostingPageSource.add(HostingPageState.Loading);
      }
    });

    if (repositoryLoaderBloc.hasStoredEnvironment()) {
      repositoryLoaderBloc.init();
    } else {
      hostingPageSource.add(HostingPageState.RequestUrl);
    }
  }

  void changeEnvironments() {
    hostingPageSource.add(HostingPageState.RequestUrl);
  }

  @override
  void dispose() {
    if (_readynessSubscriber != null) {
      _readynessSubscriber.cancel();
      _readynessSubscriber = null;
    }
  }
}

class HostingPage extends StatefulWidget {
  HostingPage({Key key}) : super(key: key);

  @override
  _HostingPageState createState() => _HostingPageState();
}

class _HostingPageState extends State<HostingPage> {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<HostingBloc>(context);
    return Scaffold(
      appBar: AppBar(
          actions: [
            RaisedButton(
              onPressed: () => bloc.changeEnvironments(),
              child: Text('Logout'),
            ),
            RaisedButton(
                onPressed: () => bloc.repositoryLoaderBloc.reset(),
                child: Text('Reset'))
          ],
          title: StreamBuilder<HostingPageState>(
            stream: bloc.hostingPageSource,
            builder: (context, snapshot) {
              if (!snapshot.hasData) {
                return Text('Loading...');
              }

              switch (snapshot.data) {
                case HostingPageState.Loading:
                  return Text('Loading...');
                  break;
                case HostingPageState.RequestUrl:
                  return Text('Specify SDKAPI Url');
                  break;
                case HostingPageState.Loaded:
                  return Text('User Overrides');
                  break;
              }
            },
          )),
      body: StreamBuilder<HostingPageState>(
          stream: bloc.hostingPageSource,
          builder: (context, snapshot) {
            if (!snapshot.hasData) {
              return SizedBox.shrink();
            }

            switch (snapshot.data) {
              case HostingPageState.Loading:
                return LoadingPage();
                break;
              case HostingPageState.RequestUrl:
                return RequestUrlPage();
                break;
              case HostingPageState.Loaded:
                return BlocProvider(
                    creator: (_c, _b) =>
                        EditingBloc(bloc.repositoryLoaderBloc.repository),
                    child: EditingPage());
                break;
            }
          }),
    );
  }
}

class LoadingPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Center(
      child: CircularProgressIndicator(),
    );
  }
}

class RequestUrlPage extends StatefulWidget {
  @override
  _RequestUrlPageState createState() => _RequestUrlPageState();
}

class _RequestUrlPageState extends State<RequestUrlPage> {
  final _url = new TextEditingController();
  GlobalKey<FormState> formKey;
  RepositoryLoaderBloc bloc;

  @override
  void initState() {
    super.initState();

    formKey = GlobalKey<FormState>();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    bloc = BlocProvider.of<RepositoryLoaderBloc>(context);
    _url.text = bloc.previousStoredEnvironmentHost();
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: formKey,
      child: Column(
        children: [
          TextFormField(
            controller: _url,
            decoration: InputDecoration(
                contentPadding:
                    EdgeInsets.symmetric(vertical: 12.0, horizontal: 10.0),
                filled: true,
                labelText: 'SDK URL'),
            validator: (v) {
              if (v.isEmpty) {
                return 'Please enter SDK URL';
              }
              return null;
            },
          ),
          FlatButton(
              onPressed: () {
                if (formKey.currentState.validate()) {
                  bloc.sdkUrl = _url.text.trim();
                }
              },
              child: Text('Load'))
        ],
      ),
    );
  }
}

class Editing {
  final String key;
  bool editing = false;

  Editing(this.key);
}

class EditingBloc implements Bloc {
  final ClientFeatureRepository repository;
  BehaviorSubject<List<Editing>> _editingSource;

  Stream<List<Editing>> get editingStream => _editingSource.stream;

  EditingBloc(this.repository) {
    _editingSource = BehaviorSubject<List<Editing>>.seeded(
        repository.availableFeatures.map((k) => Editing(k)).toList());
  }

  @override
  void dispose() {}

  editing(String keyField) {
    final data = _editingSource.value;
    final key = data.firstWhere((e) => e.key == keyField);
    key.editing = !key.editing;
    _editingSource.add(data);
  }
}

class EditingPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<EditingBloc>(context);

    return RefreshIndicator(
      onRefresh: () => BlocProvider.of<RepositoryLoaderBloc>(context).refresh(),
      child: StreamBuilder<List<Editing>>(
          stream: bloc.editingStream,
          builder: (context, snapshot) {
            if (!snapshot.hasData) {
              return SizedBox.shrink();
            }

            final children = <Widget>[
              for (var e in snapshot.data)
                if (e.editing)
                  EditFeature(keyField: e.key)
                else
                  ShowEditingFeature(keyField: e.key)
            ];

            return ListView(
              children: children,
            );
          }),
    );
  }
}

class ShowEditingFeature extends StatelessWidget {
  final String keyField;

  const ShowEditingFeature({Key key, this.keyField}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<EditingBloc>(context);
    final rLoader = BlocProvider.of<RepositoryLoaderBloc>(context);

    return GestureDetector(
      onTap: () => bloc.editing(keyField),
      child: Row(children: [
        Flexible(
            flex: 1,
            fit: FlexFit.tight,
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Text(keyField),
            )),
        Flexible(
            flex: 1,
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Text(value(rLoader, keyField)),
            ))
      ]),
    );
  }

  String value(RepositoryLoaderBloc bloc, String key) {
    final fs = bloc.repository.getFeatureState(key);
    final val = bloc.getDisplayValue(key, nullReturn: true);
    switch (fs.type) {
      case FeatureValueType.BOOLEAN:
        return val;
      case FeatureValueType.STRING:
      case FeatureValueType.NUMBER:
      case FeatureValueType.JSON:
        return val == null ? '(not set)' : val;
    }
  }
}

class EditFeature extends StatefulWidget {
  final String keyField;

  const EditFeature({Key key, this.keyField}) : super(key: key);

  @override
  _EditFeatureState createState() => _EditFeatureState();
}

class _EditFeatureState extends State<EditFeature> {
  final tec = TextEditingController();
  EditingBloc bloc;
  RepositoryLoaderBloc repositoryBloc;
  FeatureStateHolder fs;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    bloc = BlocProvider.of<EditingBloc>(context);
    repositoryBloc = BlocProvider.of<RepositoryLoaderBloc>(context);
    fs = bloc.repository.getFeatureState(widget.keyField);

    if (fs.type != FeatureValueType.BOOLEAN) {
      tec.text = repositoryBloc.getDisplayValue(widget.keyField);
    }
  }

  @override
  Widget build(BuildContext context) {
    Widget edit;

    switch (fs.type) {
      case FeatureValueType.BOOLEAN:
        edit = DropdownButtonHideUnderline(
          child: DropdownButton(
            isDense: true,
            isExpanded: false,
            items: <String>['On', 'Off']
                .map<DropdownMenuItem<String>>((String value) {
              return DropdownMenuItem<String>(
                value: value,
                child: Text(
                  value,
                  style: Theme.of(context).textTheme.bodyText2,
                ),
              );
            }).toList(),
            value: repositoryBloc.getDisplayValue(widget.keyField),
            onChanged: (value) {
              final replacementBoolean = (value == 'On');
              setState(() {
                repositoryBloc.setValue(widget.keyField, replacementBoolean);
              });
            },
            disabledHint: Text(repositoryBloc.getDisplayValue(widget.keyField),
                style: Theme.of(context).textTheme.caption),
          ),
        );
        break;
      case FeatureValueType.STRING:
        edit = TextField(
          style: Theme.of(context).textTheme.bodyText1,
          controller: tec,
          decoration: InputDecoration(
              contentPadding:
                  EdgeInsets.only(left: 4.0, right: 4.0, bottom: 8.0),
              enabledBorder: OutlineInputBorder(
                  borderSide: BorderSide(
                color: Theme.of(context).buttonColor,
              )),
              disabledBorder: OutlineInputBorder(
                  borderSide: BorderSide(
                color: Colors.grey,
              )),
              hintText: 'Enter string value',
              hintStyle: Theme.of(context).textTheme.caption),
          onChanged: (value) {},
          onSubmitted: (value) {
            final replacementValue = value.isEmpty ? null : tec.text?.trim();
            repositoryBloc.setValue(widget.keyField, replacementValue);
            bloc.editing(widget.keyField);
          },
        );
        break;
      case FeatureValueType.NUMBER:
        edit = Text('no dice');
        break;
      case FeatureValueType.JSON:
        edit = Text('no dice');
        break;
    }

    return GestureDetector(
      onTap: () => bloc.editing(widget.keyField),
      child: Row(
        children: [
          Flexible(
              flex: 1,
              fit: FlexFit.tight,
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Text(widget.keyField),
              )),
          Flexible(
              flex: 1,
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: edit,
              ))
        ],
      ),
    );
  }
}

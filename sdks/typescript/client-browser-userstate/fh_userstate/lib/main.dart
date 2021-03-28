import 'dart:async';
import 'dart:html';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:fh_userstate/repository_loader.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: ElevatedButton(
                onPressed: () => bloc.changeEnvironments(),
                child: Text('Swap Env'),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: ElevatedButton(
                  onPressed: () => bloc.repositoryLoaderBloc.reset(),
                  child: Text('Reset')),
            )
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

              return SizedBox.shrink();
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

            return SizedBox.shrink();
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
          ElevatedButton(
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
    final ds = rLoader.getDisplayValue(keyField);

    Widget child = Container(
      color: ds.overridden ? Colors.lime : Colors.white,
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
              child: Text(value(ds)),
            ))
      ]),
    );

    if (ds.type == FeatureValueType.JSON) {
      return child;
    }

    return GestureDetector(
      onTap: () => bloc.editing(keyField),
      child: child,
    );
  }

  String value(DisplayValue ds) {
    switch (ds.type) {
      case FeatureValueType.BOOLEAN:
        return ds.value;
      case FeatureValueType.STRING:
      case FeatureValueType.NUMBER:
      case FeatureValueType.JSON:
        return ds.value == null ? '(not set)' : ds.value;
    }

    return '';
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
    fs = bloc.repository.feature(widget.keyField);

    if (fs.type != FeatureValueType.BOOLEAN) {
      tec.text = repositoryBloc.getDisplayValue(widget.keyField).value;
    }
  }

  @override
  Widget build(BuildContext context) {
    Widget edit;

    final ds = repositoryBloc.getDisplayValue(widget.keyField);

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
            value: ds.value,
            onChanged: (value) {
              final replacementBoolean = (value == 'On');
              setState(() {
                repositoryBloc.setValue(widget.keyField, replacementBoolean);
              });
            },
            disabledHint:
                Text(ds.value, style: Theme.of(context).textTheme.caption),
          ),
        );
        break;
      case FeatureValueType.STRING:
      case FeatureValueType.NUMBER:
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
              hintText: 'Enter value',
              hintStyle: Theme.of(context).textTheme.caption),
          onChanged: (value) {},
          onSubmitted: (value) {
            final replacementValue = value.isEmpty ? null : tec.text?.trim();
            repositoryBloc.setValue(widget.keyField, replacementValue);
            bloc.editing(widget.keyField);
          },
          inputFormatters: [
            if (fs.type == FeatureValueType.NUMBER)
              DecimalTextInputFormatter(
                  decimalRange: 5, activatedNegativeValues: true)
          ],
        );
        break;
      case FeatureValueType.JSON:
        edit = Text('no editing supported');
        break;
    }

    return GestureDetector(
      onTap: () => bloc.editing(widget.keyField),
      child: Container(
        color: ds.overridden ? Colors.lime : Colors.white,
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
      ),
    );
  }
}

class DecimalTextInputFormatter extends TextInputFormatter {
  DecimalTextInputFormatter(
      {int decimalRange, bool activatedNegativeValues = true})
      : assert(decimalRange == null || decimalRange >= 0,
            'DecimalTextInputFormatter declaration error') {
    final dp = (decimalRange != null && decimalRange > 0)
        ? '([.][0-9]{0,$decimalRange}){0,1}'
        : '';
    final num = '[0-9]*$dp';

    if (activatedNegativeValues) {
      _exp = RegExp('^((((-){0,1})|((-){0,1}[0-9]$num))){0,1}\$');
    } else {
      _exp = RegExp('^($num){0,1}\$');
    }
  }

  RegExp _exp;

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    if (_exp.hasMatch(newValue.text)) {
      return newValue;
    }
    return oldValue;
  }
}

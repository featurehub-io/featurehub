import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_card.dart';
import 'package:app_singleapp/widgets/common/fh_outline_button.dart';
import 'package:app_singleapp/widgets/setup/setup_bloc.dart';
import 'package:flutter/material.dart';

class SetupPage2Widget extends StatefulWidget {
  final SetupBloc bloc;

  const SetupPage2Widget({Key key, @required this.bloc})
      : assert(bloc != null),
        super(key: key);

  @override
  State<StatefulWidget> createState() {
    // TODO: implement createState
    return _SetupPage2State();
  }
}

class _SetupPage2State extends State<SetupPage2Widget> {
  final _org = TextEditingController();
  final _portfolio = TextEditingController();
  final _formKey = GlobalKey<FormState>();

  @override
  Widget build(BuildContext context) {
    return _dataEntry(context);
  }

  @override
  void initState() {
    super.initState();
    copyIn();
  }

  Widget _dataEntry(BuildContext context) {
    return Form(
      key: _formKey,
      child: FHCardWidget(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.only(bottom: 26.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Image.asset('assets/logo/FeatureHub-icon.png',
                      width: 40, height: 40),
                ],
              ),
            ),
            Text(
              'Set organization and a portfolio',
              style: Theme.of(context).textTheme.headline6,
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(0, 10, 0, 10),
              child: Text(
                  'Nice work. Next step, before we can add an application we need a "Portfolio" to give your applications a home.',
                  style: Theme.of(context).textTheme.bodyText1),
            ),
            TextFormField(
              controller: _org,
              autofocus: true,
              onFieldSubmitted: (_) => _handleSubmitted(),
              textInputAction: TextInputAction.next,
              decoration: InputDecoration(
                  hintText: 'The name of your organization',
                  hintStyle: Theme.of(context).textTheme.caption,
                  labelText: 'Organization Name'),
              validator: (v) =>
                  v.isEmpty ? "Please enter your organization's name" : null,
            ),
            TextFormField(
              controller: _portfolio,
              onFieldSubmitted: (_) => _handleSubmitted(),
              decoration: InputDecoration(
                  hintText: 'The name of your first grouping of applications',
                  hintStyle: Theme.of(context).textTheme.caption,
                  labelText: 'Portfolio'),
              textInputAction: TextInputAction.done,
              validator: (v) => v.isEmpty
                  ? 'Please enter the name of your first portfolio'
                  : null,
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: <Widget>[
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: FHOutlineButton(
                    onPressed: () {
                      copyState();
                      widget.bloc.priorPage();
                    },
                    title: 'Back',
                    keepCase: true,
                  ),
                ),
                FHFlatButton(
                  onPressed: () => _handleSubmitted(),
                  title: 'Submit',
                )
              ],
            )
          ],
        ),
      ),
    );
  }

  void _handleSubmitted() {
    if (_formKey.currentState.validate()) {
      copyState();
      widget.bloc.nextPage();
    }
  }

  @override
  void dispose() {
    _org.dispose();
    _portfolio.dispose();
    super.dispose();
  }

  void copyIn() {
    _org.text = widget.bloc.orgName ?? '';
    _portfolio.text = widget.bloc.portfolio ?? '';
  }

  void copyState() {
    widget.bloc.orgName = _org.text;
    widget.bloc.portfolio = _portfolio.text;
  }
}

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_card.dart';
import 'package:app_singleapp/widgets/user/register/register_url_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:openapi_dart_common/openapi.dart';
import 'package:zxcvbn/zxcvbn.dart';

class RegisterURLRoute extends StatefulWidget {
  final String token;

  RegisterURLRoute(this.token, {Key? key}) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return RegisterURLState();
  }
}

class RegisterURLState extends State<RegisterURLRoute> {
  final _formKey = GlobalKey<FormState>(debugLabel: 'registration url');
  final _name = TextEditingController();
  final _pw1 = TextEditingController();
  final _pw2 = TextEditingController();
  final _PASSWORD_SCORE_THRESHOLD = 1;
  Text _passwordStrength = Text('');

  @override
  Widget build(BuildContext context) {
    var bloc = BlocProvider.of<RegisterBloc>(context);

    return FHCardWidget(
        child: Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: <Widget>[
        Container(
            width: 500,
            //  color: Colors.yellow,
            child: StreamBuilder(
                stream: bloc.formState,
                builder: (context, snapshot) {
                  if (snapshot.hasData) {
                    if (snapshot.data == RegisterUrlForm.initialState) {
                      return initialState(context, bloc);
                    } else if (snapshot.data == RegisterUrlForm.successState) {
                      ManagementRepositoryClientBloc.router
                          .navigateTo(context, '/');
                      return SizedBox.shrink();
                    } else if (snapshot.data ==
                        RegisterUrlForm.alreadyLoggedIn) {
                      // go back to login
                      ManagementRepositoryClientBloc.router
                          .navigateTo(context, '/');
                      BlocProvider.of<ManagementRepositoryClientBloc>(context)
                          .resetInitialized();
                      return SizedBox.shrink();
                    }
                  }
                  if (snapshot.hasError) {
                    String humanErrorMessage;
                    humanErrorMessage = 'Unexpected error occured\n.'
                        'Please contact your FeatureHub administrator.';

                    if (snapshot.error is ApiException &&
                        !snapshot.error.toString().contains('500')) {
                      humanErrorMessage =
                          'This Register URL is either expired or invalid.\n\n'
                          'Check your URL is correct or contact your FeatureHub administrator.';
                    }
                    return Text(humanErrorMessage);
                  }
                  return Text('Validating your invitation URL');
                })),
      ],
    ));
  }

  Widget initialState(BuildContext context, RegisterBloc bloc) {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Text(
            'Welcome to FeatureHub',
            style: Theme.of(context).textTheme.headline6,
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(0, 10, 0, 10),
            child: Text('To register please complete the following details',
                style: Theme.of(context).textTheme.subtitle2),
          ),
          TextFormField(
            enabled: false,
            decoration: InputDecoration(labelText: 'Email'),
            initialValue: bloc.person.email,
          ),
          TextFormField(
            controller: _name,
            autofocus: true,
            decoration: InputDecoration(labelText: 'Name'),
            textInputAction: TextInputAction.next,
            validator: (v) =>
                v?.isEmpty == false ? null : 'Please enter your name',
          ),
          TextFormField(
              controller: _pw1,
              obscureText: true,
              textInputAction: TextInputAction.next,
              decoration: InputDecoration(labelText: 'Password'),
              validator: (v) {
                if (v == null) {
                  return 'Please enter your password';
                }
                if (v.isEmpty) {
                  return 'Please enter your password';
                }
                if (v.length < 7) {
                  return 'Password must be at least 7 characters!';
                }
                if (_pw2.text.isNotEmpty && v != _pw2.text) {
                  return "Passwords don't match";
                }
                return null;
              }),
          Padding(
            padding: const EdgeInsets.fromLTRB(0, 5, 0, 10),
            child: _passwordStrength,
          ),
          TextFormField(
              controller: _pw2,
              obscureText: true,
              autovalidateMode: AutovalidateMode.onUserInteraction,
              textInputAction: TextInputAction.next,
              decoration: InputDecoration(labelText: 'Confirm Password'),
              validator: (v) {
                if (v == null || v.isEmpty) {
                  return 'Please confirm your password';
                }
                if (v != _pw1.text) {
                  return "Passwords don't match";
                }
                return null;
              }),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: <Widget>[
              Padding(
                padding: const EdgeInsets.only(top: 16.0),
                child: FHFlatButton(
                    onPressed: () {
                      if (_formKey.currentState!.validate()) {
                        bloc.completeRegistration(
                            widget.token,
                            bloc.person.email!,
                            _name.text,
                            _pw1.text,
                            _pw2.text);
                        ManagementRepositoryClientBloc.router
                            .navigateTo(context, '/');
                      }
                    },
                    title: 'Register'),
              )
            ],
          )
        ],
      ),
    );
  }

  void setPasswordStrength() {
    final result = Zxcvbn().evaluate(_pw1.text);
    var state = 'Weak';
    if (result.score == 1) {
      state = 'Below average';
    } else if (result.score == 2) {
      state = 'Good';
    } else if (result.score == 3) {
      state = 'Strong';
    }
    Color stateColor =
        (result.score == null || result.score! < _PASSWORD_SCORE_THRESHOLD)
            ? Colors.red
            : Colors.green;

    if (result.score == 1) {
      stateColor = Colors.orange;
    }

    final stateText = Text(state, style: TextStyle(color: stateColor));
    setState(() {
      _passwordStrength = stateText;
    });
  }
}

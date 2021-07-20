import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/FHFlatButton.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';

class ResetPasswordWidget extends StatefulWidget {
  const ResetPasswordWidget();

  @override
  State<StatefulWidget> createState() {
    return _ResetPasswordState();
  }
}

class _ResetPasswordState extends State<ResetPasswordWidget> {
  final _confirmPassword = TextEditingController();
  final _password = TextEditingController();
  final _formKey = GlobalKey<FormState>(debugLabel: 'reset_password_widget');

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return Form(
      key: _formKey,
      child: FHCardWidget(
        child: Container(
          padding: const EdgeInsets.fromLTRB(40, 8, 40, 40),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.max,
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
                'Reset your temporary password\n\n',
                style: Theme.of(context).textTheme.headline5,
              ),
              Column(
                children: <Widget>[
                  const Text(
                      'It looks like you tried to sign in with a temporary password, please reset your password below before proceeding.'),
                  TextFormField(
                      controller: _password,
                      obscureText: true,
                      autofocus: true,
                      textInputAction: TextInputAction.next,
                      validator: (v) => v == null || v.isEmpty
                          ? 'Please enter your new password'
                          : null,
                      decoration: const InputDecoration(labelText: 'Password')),
                  TextFormField(
                      controller: _confirmPassword,
                      obscureText: true,
                      textInputAction: TextInputAction.next,
                      validator: (v) => v == null || v.isEmpty
                          ? 'Please confirm your password'
                          : null,
                      decoration:
                          const InputDecoration(labelText: 'Confirm password')),
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: const <Widget>[],
              ),
              Container(
                padding: const EdgeInsets.only(top: 20),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: <Widget>[
                    FHFlatButton(
                        title: 'Save',
                        onPressed: () async {
                          if (_formKey.currentState!.validate()) {
                            try {
                              await bloc.replaceTempPassword(_password.text);
                            } catch (e, s) {
                              await bloc.dialogError(e, s);
                            }
                          }
                        })
                  ],
                ),
              )
            ],
          ),
        ),
      ),
    );
  }
}

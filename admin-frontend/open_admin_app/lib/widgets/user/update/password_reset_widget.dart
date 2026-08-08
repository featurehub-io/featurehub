import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/utils/password_policy_validator.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/password_requirements_widget.dart';

class ResetPasswordWidget extends StatefulWidget {
  final String personId;

  const ResetPasswordWidget(this.personId, {super.key});

  @override
  State<StatefulWidget> createState() {
    return _ResetPasswordState();
  }
}

class _ResetPasswordState extends State<ResetPasswordWidget> {
  final _confirmPassword = TextEditingController();
  final _password = TextEditingController();
  final _formKey = GlobalKey<FormState>(debugLabel: 'reset_password_widget');

  /// Populated when the server rejects a password this form accepted - the server is authoritative.
  List<PasswordPolicyRule>? _serverRejectedRules;

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    final l10n = AppLocalizations.of(context)!;

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
                '${l10n.resetTempPasswordTitle}\n\n',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(l10n.resetTempPasswordMessage),
                  TextFormField(
                      controller: _password,
                      obscureText: true,
                      autofocus: true,
                      textInputAction: TextInputAction.next,
                      onChanged: (_) => setState(() {}),
                      validator: (v) {
                        if (v == null || v.isEmpty) {
                          return l10n.newPasswordRequired;
                        }
                        return passwordValidationMessage(
                            v, bloc.passwordPolicy, l10n);
                      },
                      decoration:
                          InputDecoration(labelText: l10n.passwordLabel)),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(0, 8, 0, 4),
                    child: PasswordRequirementsWidget(
                      policy: bloc.passwordPolicy,
                      password: _password.text,
                      serverRejectedRules: _serverRejectedRules,
                    ),
                  ),
                  TextFormField(
                      controller: _confirmPassword,
                      obscureText: true,
                      textInputAction: TextInputAction.next,
                      validator: (v) {
                        if (v == null || v.isEmpty) {
                          return l10n.confirmPasswordRequired;
                        }
                        if (v != _password.text) {
                          return l10n.passwordsDoNotMatch;
                        }
                        return null;
                      },
                      decoration: InputDecoration(
                          labelText: l10n.confirmPasswordLabel)),
                ],
              ),
              const Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: <Widget>[],
              ),
              Container(
                padding: const EdgeInsets.only(top: 20),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: <Widget>[
                    FHFlatButton(
                        title: l10n.save,
                        onPressed: () async {
                          if (_formKey.currentState!.validate()) {
                            try {
                              await bloc.replaceTempPassword(
                                  widget.personId, _password.text);
                              setState(() => _serverRejectedRules = null);
                            } catch (e, s) {
                              final rejected = passwordPolicyRejection(e);
                              if (rejected != null) {
                                // the server disagreed with us about the password - it wins
                                setState(() => _serverRejectedRules = rejected);
                                return;
                              }
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

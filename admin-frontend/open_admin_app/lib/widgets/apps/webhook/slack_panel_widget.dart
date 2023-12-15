import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_env_bloc.dart';

class SlackPanelWidget extends StatefulWidget {
  final WebhookEnvironmentBloc bloc;
  final EnvironmentAndWebhookType env;

  SlackPanelWidget(this.env, this.bloc);

  @override
  State<StatefulWidget> createState() {
    return SlackPanelWidgetState();
  }
}

class SlackPanelWidgetState extends State<SlackPanelWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _apiKey = TextEditingController();
  final TextEditingController _channelName = TextEditingController();
  bool _enabled = false;

  @override
  void initState() {
    super.initState();

    _setup();
  }

  @override
  void didUpdateWidget(SlackPanelWidget oldWidget) {
    super.didUpdateWidget(oldWidget);

    _setup();
  }

  _setup() {
    final env = widget.env.environment?.webhookEnvironmentInfo ?? {};
    final prefix = widget.env.type!.envPrefix;
    _enabled = env['${prefix}.enabled'] == 'true';
    _apiKey.text = env['${prefix}.api_key'] ?? '';
    _channelName.text = env['${prefix}.channel_name'] ?? '';
  }

  @override
  Widget build(BuildContext context) {
    return Card(
        margin: const EdgeInsets.all(8.0),
        child: Padding(
            padding: const EdgeInsets.all(8.0),
            child: Column(children: [
              const Row(
                mainAxisAlignment: MainAxisAlignment.start,
                children: [
                  Text('Slack Configuration',
                      style: TextStyle(fontWeight: FontWeight.bold)),
                ],
              ),
              Form(
                  key: _formKey,
                  child:
                      Column(mainAxisSize: MainAxisSize.min, children: <Widget>[
                        Row(children: [
                          const Text('Enabled'),
                          Checkbox(autofocus: true,
                            value: _enabled, onChanged: (value) => setState(() {
                            _enabled = value ?? false;
                          }),)
                        ],),
                    Row(children: [
                      Expanded(
                          child: TextFormField(
                              controller: _apiKey,
                              autofocus: true,
                              textInputAction: TextInputAction.next,
                              obscureText: _apiKey.text == 'ENCRYPTED-TEXT',
                              obscuringCharacter: '*',
                              readOnly: _apiKey.text == 'ENCRYPTED-TEXT',
                              decoration: const InputDecoration(
                                  labelText: 'Slack API Key'),
                              validator: ((v) {
                                if (v == null || v.isEmpty) {
                                  return 'Please enter a Slack API Key';
                                }
                                return null;
                              }))),
                    ]),
                    Row(children: [
                      Expanded(
                          child: TextFormField(
                              controller: _channelName,
                              autofocus: true,
                              textInputAction: TextInputAction.next,
                              decoration: const InputDecoration(
                                  labelText: 'Slack channel code (e.g. @C0150T7AF25)'),
                              validator: ((v) {
                                if (v == null || v.isEmpty) {
                                  return 'Please enter a Slack Channel';
                                }
                                return null;
                              }))),
                    ]),
                    const SizedBox(
                      height: 24.0,
                    ),
                    Row(children: [
                      if (widget.bloc.mrBloc.identityProviders.capabilityWebhookEncryption &&
                          widget.bloc.mrBloc.identityProviders.capabilityWebhookDecryption)
                      Column(children: [
                        if (_apiKey.text == 'ENCRYPTED-TEXT')
                          FilledButton(
                              onPressed: () => widget.bloc
                                  .decryptEncryptedFields(
                                      widget.env.environment!),
                              child: const Text('Reveal hidden values')),
                      ]),
                      Expanded(
                        child: Column(
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                FilledButton(
                                    onPressed: () async {
                                      if ( _formKey.currentState!.validate()) {
                                        _formKey.currentState!.save();
                                        await _updateData();
                                      }
                                    },
                                    child: const Text('Save')),
                              ],
                            )
                          ],
                        ),
                      )
                    ]),
                  ])),
            ])));
  }

  bool _saveable() {
    return _apiKey.text.trim().isNotEmpty && _channelName.text.trim().isNotEmpty;
  }

  Future<void> _updateData() async {
    final env = widget.env.environment?.webhookEnvironmentInfo ?? {};
    final prefix = widget.env.type!.envPrefix;
    env['${prefix}.enabled'] = _enabled.toString();
    env['${prefix}.api_key'] = _apiKey.text;
    env['${prefix}.channel_name'] = _channelName.text;
    env['${prefix}.encrypt'] = '${prefix}.api_key';
    widget.bloc.mrBloc.addSnackbar(Text("Saving Slack webhook"));
    await widget.bloc.updateEnvironmentWithWebhookData(
        widget.env.environment!, env, "$prefix.");
  }
}

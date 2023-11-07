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
              Row(children: [
                Text("Slack is currently ${_enabled ? 'enabled' : 'disabled'}")
              ],),
              Form(
                  key: _formKey,
                  child:
                      Column(mainAxisSize: MainAxisSize.min, children: <Widget>[
                    Row(children: [
                      Expanded(
                          child: TextFormField(
                              controller: _apiKey,
                              autofocus: true,
                              textInputAction: TextInputAction.next,
                              obscureText: _channelName.text == 'ENCRYPTED-TEXT',
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
                              obscureText: _channelName.text == 'ENCRYPTED-TEXT',
                              obscuringCharacter: '*',
                              readOnly: _channelName.text == 'ENCRYPTED-TEXT',
                              decoration: const InputDecoration(
                                  labelText: 'Slack Channel (use @XXXX)'),
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
                                    onPressed: () => _updateData(false),
                                    child: const Text('Disable')),
                                FilledButton(
                                    onPressed: () => _updateData(true),
                                    child: const Text('Save/Enable')),
                              ],
                            )
                          ],
                        ),
                      )
                    ]),
                  ])),
            ])));
  }

  Future<void> _updateData(bool enabled) async {
    final env = widget.env.environment?.webhookEnvironmentInfo ?? {};
    final prefix = widget.env.type!.envPrefix;
    env['${prefix}.enabled'] = enabled.toString();
    env['${prefix}.api_key'] = _apiKey.text;
    env['${prefix}.channel_name'] = _channelName.text;
    env['${prefix}.encrypt'] = '${prefix}.api_key,${prefix}.channel_name';
    await widget.bloc.updateEnvironmentWithWebhookData(
        widget.env.environment!, env, prefix + ".");
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/systemconfig/system_config_encrypted_text_field.dart';

/// hosts the field, plus a button that rebuilds the parent with the *same* field (as ticking a checkbox on the
/// Slack form does), and one that hands the field a brand new config (as a save + refresh does)
class _Host extends StatefulWidget {
  final SystemConfig initial;
  final VoidCallback? onCleared;
  final Future<String?> Function()? onDecrypt;

  const _Host({required this.initial, this.onCleared, this.onDecrypt});

  @override
  State<_Host> createState() => _HostState();
}

class _HostState extends State<_Host> {
  late SystemConfig field = widget.initial;
  final formKey = GlobalKey<FormState>();
  int rebuilds = 0;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(
        body: Form(
          key: formKey,
          child: Column(children: [
            SystemConfigEncryptableTextField(
                field: field,
                onCleared: widget.onCleared,
                onDecrypt: widget.onDecrypt),
            Text('rebuilds $rebuilds'),
            TextButton(
                onPressed: () => setState(() => rebuilds++),
                child: const Text('rebuild-parent')),
            TextButton(
                onPressed: () => setState(() => field = SystemConfig(
                    key: field.key,
                    version: field.version + 1,
                    encrypted: true,
                    value: encryptedTextPlaceholder)),
                child: const Text('replace-field')),
          ]),
        ),
      ),
    );
  }
}

SystemConfig _storedToken() => SystemConfig(
    key: 'slack.bearerToken',
    version: 1,
    encrypted: true,
    value: encryptedTextPlaceholder);

SystemConfig _emptyToken() =>
    SystemConfig(key: 'slack.bearerToken', version: -1, encrypted: true);

TextField _textField(WidgetTester tester) =>
    tester.widget<TextField>(find.byType(TextField));

void main() {
  testWidgets('a stored value is read only and can be cleared',
      (tester) async {
    final token = _storedToken();
    var cleared = 0;
    await tester.pumpWidget(_Host(initial: token, onCleared: () => cleared++));

    expect(_textField(tester).readOnly, isTrue);
    expect(find.text('Clear'), findsOneWidget);

    await tester.tap(find.text('Clear'));
    await tester.pump();

    expect(token.value, isNull);
    expect(cleared, 1);
    expect(_textField(tester).readOnly, isFalse);
    expect(_textField(tester).controller!.text, isEmpty);
    expect(find.text('Clear'), findsNothing);
  });

  testWidgets('a new value typed after clearing survives a parent rebuild',
      (tester) async {
    final token = _storedToken();
    await tester.pumpWidget(_Host(initial: token));

    await tester.tap(find.text('Clear'));
    await tester.pump();
    await tester.enterText(find.byType(TextFormField), 'xoxb-new-token');
    expect(token.value, 'xoxb-new-token');

    await tester.tap(find.text('rebuild-parent'));
    await tester.pump();

    expect(find.text('rebuilds 1'), findsOneWidget);
    expect(_textField(tester).controller!.text, 'xoxb-new-token');
    expect(token.value, 'xoxb-new-token');
  });

  testWidgets('blank input is treated as no value', (tester) async {
    final token = _emptyToken();
    await tester.pumpWidget(_Host(initial: token));

    await tester.enterText(find.byType(TextFormField), '   ');
    expect(token.value, isNull);

    await tester.enterText(find.byType(TextFormField), 'abc');
    expect(token.value, 'abc');

    await tester.enterText(find.byType(TextFormField), '');
    expect(token.value, isNull);
  });

  testWidgets('being handed a different config reloads the field',
      (tester) async {
    await tester.pumpWidget(_Host(initial: _emptyToken()));

    await tester.enterText(find.byType(TextFormField), 'xoxb-new-token');
    await tester.tap(find.text('replace-field'));
    await tester.pumpAndSettle();

    expect(_textField(tester).controller!.text, encryptedTextPlaceholder);
    expect(_textField(tester).readOnly, isTrue);
  });

  testWidgets('show value is only offered when decryption is available',
      (tester) async {
    await tester.pumpWidget(_Host(initial: _storedToken()));
    expect(find.text('Show value'), findsNothing);
  });

  testWidgets('show value displays the decrypted value', (tester) async {
    final token = _storedToken();
    await tester
        .pumpWidget(_Host(initial: token, onDecrypt: () async => 'xoxb-real'));

    await tester.tap(find.text('Show value'));
    await tester.pumpAndSettle();

    expect(_textField(tester).controller!.text, 'xoxb-real');
    expect(token.value, 'xoxb-real');
    expect(_textField(tester).readOnly, isFalse);
  });
}

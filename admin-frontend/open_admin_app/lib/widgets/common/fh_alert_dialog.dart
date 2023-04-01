import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:open_admin_app/api/client_api.dart';

class FHAlertDialog extends StatefulWidget {
  final Widget title;
  final Widget content;
  final List<Widget> actions;
  final Function? escKey;

  const FHAlertDialog(
      {Key? key,
      required this.title,
      required this.content,
      required this.actions,
      this.escKey
      })
      : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _FHAlertDialogState();
  }
}

class _FHAlertDialogState extends State<FHAlertDialog> {
  final FocusNode _alertDialog = FocusNode();

  @override
  Widget build(BuildContext context) {
    return Stack(children: [
      const ModalBarrier(dismissible: true, color: Colors.black54),
      RawKeyboardListener(
        focusNode: _alertDialog,
        onKey: (key) {
          if (key.logicalKey == LogicalKeyboardKey.escape) {
            if (widget.escKey != null) {
              widget.escKey!();
            } else {
              final bloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
              if (bloc != null) {
                bloc.removeOverlay();
              }
            }
          }
        },
        child: AlertDialog(
          scrollable: true,
          title: widget.title,
          content: widget.content,
          actions: widget.actions,
          buttonPadding: const EdgeInsets.symmetric(horizontal: 16.0),
        ),
      )
    ]);
  }
}

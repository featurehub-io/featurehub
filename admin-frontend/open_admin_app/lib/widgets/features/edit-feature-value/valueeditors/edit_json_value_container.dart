import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_json_editor.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/valueeditors/edit_feature_value_widget.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/valueeditors/json_viewer_field.dart';

class EditJsonValueContainer extends EditFeatureValueWidget {
  const EditJsonValueContainer({
    super.key,
    required super.unlocked,
    required super.canEdit,
    super.rolloutStrategy,
    super.groupRolloutStrategy,
    super.applicationRolloutStrategy,
    super.portfolioRolloutStrategy,
    required super.strBloc,
  });

  @override
  EditJsonValueContainerState createState() => EditJsonValueContainerState();
}

class EditJsonValueContainerState
    extends EditFeatureValueState<EditJsonValueContainer> {
  TextEditingController tec = TextEditingController();
  final _formKey = GlobalKey<FormState>();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final valueSource =
        resolveStrategyValue() ?? widget.strBloc.featureValue.valueJson;
    if (valueSource != null) {
      try {
        tec.text = const JsonEncoder.withIndent('  ')
            .convert(json.decode(valueSource))
            .toString();
      } catch (e) {
        tec.text = valueSource.toString();
      }
    } else {
      tec.text = '';
    }
  }

  @override
  Widget build(BuildContext context) {
    BoxDecoration myBoxDecoration() {
      return BoxDecoration(
        border: Border.all(
          width: 1.0,
          color: Theme.of(context).colorScheme.primary,
        ),
        borderRadius: const BorderRadius.all(
          Radius.circular(6.0),
        ),
      );
    }

    return SizedBox(
      height: 30,
      child: InkWell(
        canRequestFocus: false,
        mouseCursor: SystemMouseCursors.click,
        customBorder: const RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(6.0)),
        ),
        hoverColor: Colors.black12,
        onTap: () => _viewJsonEditor(context, widget.canEdit),
        child: Container(
            padding: const EdgeInsets.all(4.0),
            decoration: myBoxDecoration(),
            child: Align(
              child: JsonViewerField(
                  text: tec.text,
                  canEdit: widget.canEdit,
                  unlocked: widget.unlocked),
            )),
      ),
    );
  }

  void _viewJsonEditor(BuildContext context, bool enabled) {
    final l10n = AppLocalizations.of(context)!;
    var initialValue = tec.text;
    showDialog(
        context: context,
        builder: (_) {
          return AlertDialog(
            content: FHJsonEditorWidget(
              controller: tec,
              formKey: _formKey,
              onlyJsonValidation: true,
            ),
            title: Text(l10n.setFeatureValue),
            actions: [
              FHFlatButtonTransparent(
                onPressed: () {
                  tec.text = initialValue;
                  Navigator.pop(context);
                },
                title: l10n.cancel,
                keepCase: true,
              ),
              enabled
                  ? FHFlatButton(
                      title: l10n.setValue,
                      onPressed: (() {
                        if (_formKey.currentState!.validate()) {
                          _valueChanged();
                          Navigator.pop(context);
                        }
                      }))
                  : Container(),
            ],
          );
        });
  }

  void _valueChanged() {
    updateValue(tec.text.isEmpty
        ? null
        : json.encode(json.decode(tec.text.trim())).toString());
  }
}

import 'dart:async';

import 'package:open_admin_app/utils/utils.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_json_editor.dart';
import 'per_application_features_bloc.dart';

class SetFeatureMetadataWidget extends StatefulWidget {
  final PerApplicationFeaturesBloc bloc;

  const SetFeatureMetadataWidget({Key? key, required this.bloc})
      : super(key: key);

  @override
  State<SetFeatureMetadataWidget> createState() =>
      _SetFeatureMetadataWidgetState();
}

class _SetFeatureMetadataWidgetState extends State<SetFeatureMetadataWidget> {
  TextEditingController tec = TextEditingController();
  Feature? _feature;
  late StreamSubscription<Feature?> streamSubscription;
  @override
  void dispose() {
    super.dispose();
    tec.dispose();
    streamSubscription.cancel();
  }

  @override
  void initState() {
    super.initState();
    streamSubscription = widget.bloc.featureMetadataStream.listen((feature) {
      _feature = feature;
      if (feature != null && feature.metaData != null && mounted) {
        setState(() {
          tec.text = feature.metaData!;
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final isReadOnly = !widget.bloc.mrClient.userIsFeatureAdminOfCurrentApplication;

    return SizedBox(
      height: 500,
      child: (AlertDialog(
          title: Text(" ${isReadOnly ? 'View' : 'Edit'} metadata for '${_feature != null ? _feature!.name : ''}'"),
          actions: [
            FHFlatButtonTransparent(
              onPressed: () {
                widget.bloc.mrClient.removeOverlay();
              },
              title: 'Cancel',
              keepCase: true,
            ),
            !isReadOnly
                ? FHFlatButton(
                    title: 'Set value',
                    onPressed: (() {
                      if (validateJson(tec.text) != null) {
                        widget.bloc.mrClient.customError(
                            messageTitle: 'JSON not valid!',
                            messageBody:
                                'Make sure your keys and values are in double quotes.');
                      } else {
                        _changeValue();
                        widget.bloc.mrClient.removeOverlay();
                      }
                    }))
                : Container(),
          ],
          content: FHJsonEditorWidget(controller: tec))),
    );
  }

  Future<void> _changeValue() async {
    try {
      await widget.bloc.updateFeatureMetadata(_feature!, tec.text);
      widget.bloc.mrClient
          .addSnackbar(Text('Feature ${_feature!.name} metadata has been updated!'));
    }
    catch (e, s) {
        widget.bloc.mrClient.dialogError(e, s);
      }
  }
}

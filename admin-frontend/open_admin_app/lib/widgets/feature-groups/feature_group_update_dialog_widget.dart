import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_groups_bloc.dart';
import 'package:openapi_dart_common/openapi.dart';

class FeatureGroupUpdateDialogWidget extends StatefulWidget {
  final FeatureGroupListGroup? featureGroup;
  final FeatureGroupsBloc bloc;

  const FeatureGroupUpdateDialogWidget({
    Key? key,
    required this.bloc,
    this.featureGroup,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _FeatureGroupUpdateDialogWidgetState();
  }
}

class _FeatureGroupUpdateDialogWidgetState
    extends State<FeatureGroupUpdateDialogWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _featureGroupName = TextEditingController();
  final TextEditingController _featureGroupDescription =
      TextEditingController();
  bool isUpdate = false;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    if (widget.featureGroup != null) {
      _featureGroupName.text = widget.featureGroup!.name;
      _featureGroupDescription.text = widget.featureGroup!.description;
      isUpdate = true;
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_busy) {
      return const FHLoadingIndicator();
    } else {
      return FHAlertDialog(
        title: Text(widget.featureGroup == null
            ? 'Create new Feature Group'
            : 'Edit Feature Group'),
        content: Form(
            key: _formKey,
            child: SizedBox(
              width: 500,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  TextFormField(
                      controller: _featureGroupName,
                      autofocus: true,
                      textInputAction: TextInputAction.next,
                      decoration: const InputDecoration(
                          labelText: 'Feature group name'),
                      validator: ((v) {
                        if (v == null || v.isEmpty) {
                          return 'Please enter feature group name';
                        }
                        if (v.length < 4) {
                          return 'Group name needs to be at least 4 characters long';
                        }
                        return null;
                      })),
                  TextFormField(
                      controller: _featureGroupDescription,
                      textInputAction: TextInputAction.done,
                      decoration: const InputDecoration(
                          labelText: 'Feature group description'),
                      validator: ((v) {
                        if (v == null || v.isEmpty) {
                          return 'Please enter feature group description';
                        }
                        if (v.length < 4) {
                          return 'Description needs to be at least 4 characters long';
                        }
                        return null;
                      })),
                ],
              ),
            )),
        actions: [
          FHFlatButtonTransparent(
            title: 'Cancel',
            keepCase: true,
            onPressed: () {
              widget.bloc.mrClient.removeOverlay();
            },
          ),
          FHFlatButton(
              title: isUpdate ? 'Update' : 'Create',
              keepCase: true,
              onPressed: () => _handleValidation()),
        ],
      );
    }
  }

  void _handleValidation() async {
    if (_formKey.currentState!.validate()) {
      try {
        setState(() {
          _busy = true;
        });
        if (isUpdate) {
          await widget.bloc.updateFeatureGroup(widget.featureGroup!,
              name: _featureGroupName.text,
              description: _featureGroupDescription.text);
          widget.bloc.mrClient.removeOverlay();
          widget.bloc.mrClient.addSnackbar(
              Text('Feature group ${_featureGroupName.text} updated!'));
        } else {
          await widget.bloc.createFeatureGroup(
              _featureGroupName.text, _featureGroupDescription.text);
          widget.bloc.mrClient.removeOverlay();
          widget.bloc.mrClient.addSnackbar(
              Text('Feature group ${_featureGroupName.text} created!'));
        }
      } catch (e, s) {
        if (e is ApiException && e.code == 409) {
          widget.bloc.mrClient.customError(
              messageTitle:
                  "Feature group '${_featureGroupName.text}' already exists");
        } else {
          await widget.bloc.mrClient.dialogError(e, s);
        }
      } finally {
        setState(() {
          _busy = false;
        });
      }
    }
  }
}

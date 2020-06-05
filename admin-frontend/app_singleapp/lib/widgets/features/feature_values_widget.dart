
//class FeatureValuesWidget extends StatefulWidget {
//  final Feature feature;
//  final FeatureValuesBloc bloc;
//
//  const FeatureValuesWidget({
//    Key key,
//    @required this.bloc,
//    this.feature,
//  }) : super(key: key);
//
//  @override
//  State<StatefulWidget> createState() {
//    return _FeatureValuesWidgetEditingState();
//  }
//}
//
//class _FeatureValuesWidgetEditingState extends State<FeatureValuesWidget> {
//  Map<String, TextEditingController> stringControllers = Map();
//
//  @override
//  void dispose() {
//    stringControllers.forEach((key, TextEditingController controller) {
//      controller.dispose();
//    });
//    super.dispose();
//  }
//
//  @override
//  Widget build(BuildContext context) {
//    return StreamBuilder<List<FeatureEnvironment>>(
//        stream: widget.bloc.featureValues,
//        builder: (context, snapshot) {
//          if (!snapshot.hasData) {
//            return Container();
//          }
//          return Column(
//            children: <Widget>[
//              Container(
//                color: Colors.white,
//                child: Table(
//                  columnWidths: {
//                    0: FlexColumnWidth(1.25),
//                    1: FlexColumnWidth(1),
//                    2: FlexColumnWidth(2),
//                    3: FlexColumnWidth(3),
//                    4: FlexColumnWidth(1.5),
//                  },
//                  children: _getFeatureEnvironments(snapshot.data),
//                ),
//              ),
//              FHButtonBar(children: <Widget>[
//                FHFlatButtonTransparent(
//                    onPressed: () {
//                      ManagementRepositoryClientBloc.router
//                          .navigateTo(context, "/feature-status");
//                    },
//                    title: 'Cancel'),
//                Padding(
//                    padding: const EdgeInsets.only(left: 8.0),
//                    child: FHFlatButton(
//                        onPressed: () {
//                          List<FeatureValue> fvList = List();
//                          if (widget.feature.valueType !=
//                              FeatureValueType.BOOLEAN) {
//                            _getFeatureValues(widget.feature.valueType);
//                          }
//                          widget.bloc.dirty.forEach((key, value) {
//                            if (value == true) {
//                              fvList.add(widget.bloc.newFeatureValues[key]);
//                            }
//                          });
//                          try {
//                            widget.bloc.updateFeatures(fvList);
//                            widget.bloc.mrClient
//                                .addSnackbar(Text("Features updated!"));
//                            ManagementRepositoryClientBloc.router
//                                .navigateTo(context, "/feature-status");
//                          } catch (e, s) {
//                            widget.bloc.mrClient.dialogError(e, s);
//                          }
//                        },
//                        title: 'Save'))
//              ]),
//            ],
//          );
//        });
//  }
//
//  _getFeatureValues(FeatureValueType type) {
//    this.stringControllers.forEach((key, TextEditingController controller) {
//      if (widget.bloc.newFeatureValues[key] == null) {
//        widget.bloc.newFeatureValues[key] = FeatureValue()
//          ..key = widget.feature.key
//          ..environmentId = key
//          ..locked = false;
//      }
//      if (type == FeatureValueType.STRING) {
//        widget.bloc.newFeatureValues[key].valueString = controller.value.text;
//      } else if (type == FeatureValueType.NUMBER &&
//          controller.value.text.isNotEmpty) {
//        widget.bloc.newFeatureValues[key].valueNumber =
//            double.parse(controller.value.text);
//      } else {
//        widget.bloc.newFeatureValues[key].valueJson = controller.value.text;
//      }
//    });
//  }
//
//  List<TableRow> _getFeatureEnvironments(List<FeatureEnvironment> fes) {
//    final BorderSide bs = BorderSide(color: Theme.of(context).dividerColor);
//    List<TableRow> rows = [];
//    rows.add(_getTableHeader());
//    List<Widget> row;
//    fes.forEach((item) {
//      row = [];
//      row.add(_getCell(
//          child: Container(
//              padding: EdgeInsets.only(top: 10),
//              child: Text(
//                item.environment.name,
//                style: TextStyle(
//                    color: item.roles.isNotEmpty
//                        ? null
//                        : Theme.of(context).dividerColor),
//              ))));
//      row.add(_getCell(child: _buildLockedWidget(item)));
//      row.add(_getCell(
//          child: Row(
//        children: <Widget>[
//          Container(
//            padding: EdgeInsets.only(top: 12, bottom: 12),
//            child: _buildStatusLabel(item, context),
//          ),
//          _buildResetButton(item, context)
//        ],
//      )));
//      row.add(_getCell(child: _buildValueWidget(item)));
//      row.add(SDKDetailsWidget(bloc: widget.bloc, envId: item.environment.id));
//
//      row.add(_getCell(child: _buildUpdatedByWidget(item)));
//      rows.add(TableRow(
//          decoration: BoxDecoration(
//            border: Border(
//              bottom: bs,
//              left: bs,
//              right: bs,
//            ),
//            color: item.roles.isEmpty
//                ? Theme.of(context).secondaryHeaderColor
//                : null,
//          ),
//          children: row));
//    });
//    return rows;
//  }
//
//  TableRow _getTableHeader() {
//    final BorderSide bs = BorderSide(color: Theme.of(context).dividerColor);
//    return TableRow(
//        decoration: BoxDecoration(
//            border: Border(
//          top: bs,
//          bottom: bs,
//          left: bs,
//          right: bs,
//        )),
//        children: [
//          Container(
//              padding: EdgeInsets.only(left: 10, top: 15, bottom: 15),
//              child: Text(
//                "Environment",
//                style: Theme.of(context).textTheme.subtitle2,
//              )),
//          Container(
//              padding: EdgeInsets.only(left: 10, top: 15, bottom: 15),
//              child: Text(
//                "Locked",
//                style: Theme.of(context).textTheme.subtitle2,
//              )),
//          Container(
//              padding: EdgeInsets.only(left: 10, top: 15, bottom: 15),
//              child: Text(
//                "Status",
//                style: Theme.of(context).textTheme.subtitle2,
//              )),
//          Container(
//              padding: EdgeInsets.only(left: 10, top: 15, bottom: 15),
//              child: Text(
//                "Value",
//                style: Theme.of(context).textTheme.subtitle2,
//              )),
//          Container(
//              padding: EdgeInsets.only(left: 10, top: 15, bottom: 15),
//              child: Text(
//                "SDK settings",
//                style: Theme.of(context).textTheme.subtitle2,
//              )),
//          Container(
//              padding: EdgeInsets.only(left: 10, top: 15, bottom: 15),
//              child: Text(
//                "Updated by",
//                style: Theme.of(context).textTheme.subtitle2,
//              )),
//        ]);
//  }
//
//  TableCell _getCell({@required Widget child}) {
//    return TableCell(
//        child: Container(
//            child: child,
//            padding: EdgeInsets.only(left: 10, top: 3, bottom: 2)));
//  }
//
//  Widget _buildValueWidget(FeatureEnvironment fe) {
//    if (this.widget.bloc.getLatestFeature().valueType ==
//        FeatureValueType.BOOLEAN) {
//      return FHBooleanValueWidget(
//          bloc: widget.bloc, fe: fe, feature: widget.feature);
//    } else {
//      stringControllers[fe.environment.id] = TextEditingController();
//    }
//    if (this.widget.bloc.getLatestFeature().valueType ==
//        FeatureValueType.STRING) {
//      return _buildValueString(fe);
//    }
//    if (this.widget.bloc.getLatestFeature().valueType ==
//        FeatureValueType.NUMBER) {
//      return _buildValueNumber(fe);
//    }
//    return _buildValueJson(fe);
//  }
//
//  Widget _buildValueString(FeatureEnvironment fe) {
//    if (fe.featureValue?.valueString != null) {
//      stringControllers[fe.environment.id].value =
//          TextEditingValue(text: fe.featureValue.valueString);
//    }
//    return Visibility(
//        visible: fe.roles.isNotEmpty,
//        child: Container(
//          width: 100,
//          child: TextField(
//              readOnly:
//                  widget.bloc.newFeatureValues[fe.environment.id]?.locked ==
//                          true ||
//                      !fe.roles.contains(RoleType.EDIT),
//              controller: stringControllers[fe.environment.id],
//              onChanged: (value) {
//                widget.bloc.dirty[fe.environment.id] = true;
//                widget.bloc.newFeatureValues[fe.environment.id].valueString =
//                    value;
//              }),
//        ));
//  }
//
//  Widget _buildValueJson(FeatureEnvironment fe) {
//    if (fe.featureValue?.valueJson != null) {
//      stringControllers[fe.environment.id].value =
//          TextEditingValue(text: fe.featureValue.valueJson);
//    }
//    return Visibility(
//        visible: fe.roles.isNotEmpty,
//        child: Row(
//          children: <Widget>[
//            Container(
//              padding: EdgeInsets.only(top: 17, bottom: 16),
//              width: 180,
//              child: Text(
//                  condenseJson(stringControllers[fe.environment.id].text),
//                  overflow: TextOverflow.ellipsis,
//                  style: TextStyle(fontFamily: 'Source', fontSize: 12)),
//            ),
//            Visibility(
//              visible: fe.roles.contains(RoleType.EDIT),
//              child: FHFlatButtonTransparent(
//                onPressed: () => _viewJsonEditor(context, fe),
//                title: "Edit",
//              ),
//            ),
//          ],
//        ));
//  }
//
//  Widget _buildValueNumber(FeatureEnvironment fe) {
//    if (fe.featureValue?.valueNumber != null) {
//      stringControllers[fe.environment.id].value =
//          TextEditingValue(text: fe.featureValue.valueNumber.toString());
//    }
//
//    return Visibility(
//        visible: fe.roles.isNotEmpty,
//        child: Container(
//          width: 300,
//          child: TextField(
//              readOnly:
//                  widget.bloc.newFeatureValues[fe.environment.id]?.locked ==
//                          true ||
//                      !fe.roles.contains(RoleType.EDIT),
//              controller: stringControllers[fe.environment.id],
//              decoration: InputDecoration(
//                labelText: 'Number value',
//                errorText:
//                    validateNumber(stringControllers[fe.environment.id].text) !=
//                            null
//                        ? 'Not a valid number'
//                        : null,
//              ),
//              onChanged: (value) {
//                widget.bloc.dirty[fe.environment.id] = true;
//                if (validateNumber(stringControllers[fe.environment.id].text) ==
//                    null) {
//                  widget.bloc.newFeatureValues[fe.environment.id].valueNumber =
//                      double.parse(stringControllers[fe.environment.id].text);
//                }
//              }),
//        ));
//  }
//
//  void _viewJsonEditor(BuildContext context, FeatureEnvironment fe) {
//    String initialValue = fe.featureValue?.valueJson;
//    widget.bloc.mrClient.addOverlay((BuildContext context) => AlertDialog(
//            content: Container(
//          height: 575,
//          child: Column(
//            children: <Widget>[
//              FHJsonEditorWidget(
//                controller: stringControllers[fe.environment.id],
//              ),
//              FHButtonBar(
//                children: <Widget>[
//                  FHFlatButtonTransparent(
//                      onPressed: () {
//                        stringControllers[fe.environment.id].text =
//                            initialValue;
//                        widget.bloc.mrClient.removeOverlay();
//                      },
//                      title: 'Cancel'),
//                  FHOutlineButton(
//                      title: "set value",
//                      onPressed: (() {
//                        if (validateJson(
//                                stringControllers[fe.environment.id].text) !=
//                            null) {
//                          widget.bloc.mrClient.customError(
//                              messageTitle: "JSON not valid!",
//                              messageBody:
//                                  "Make sure your keys and values are in double quotes.");
//                        } else {
//                          if (fe.featureValue == null) {
//                            fe.featureValue = FeatureValue();
//                          }
//                          setState(() {
//                            fe.featureValue.valueJson =
//                                stringControllers[fe.environment.id].text;
//                          });
//                          if (stringControllers[fe.environment.id].text !=
//                              initialValue) {
//                            widget.bloc.dirty[fe.environment.id] = true;
//                          }
//                          widget.bloc.mrClient.removeOverlay();
//                        }
//                      })),
//                ],
//              ),
//            ],
//          ),
//        )));
//  }
//
//  Widget _buildUpdatedByWidget(FeatureEnvironment fe) {
//    return Container(
//      padding: EdgeInsets.only(top: 5),
//      child: Column(
//        crossAxisAlignment: CrossAxisAlignment.start,
//        children: <Widget>[
//          Text(fe.featureValue?.whoUpdated == null
//              ? ""
//              : fe.featureValue.whoUpdated.name),
//          Text(
//              fe.featureValue?.whenUpdated == null
//                  ? ""
//                  : timeago.format(fe.featureValue.whenUpdated.toLocal()),
//              style: Theme.of(context).textTheme.caption)
//        ],
//      ),
//    );
//  }
//
//  Widget _buildLockedWidget(FeatureEnvironment fe) {
//    bool disabled = (!fe.roles.contains(RoleType.UNLOCK) &&
//            widget.bloc.newFeatureValues[fe.environment.id]?.locked == true) ||
//        (widget.bloc.newFeatureValues[fe.environment.id]?.locked != true &&
//            !fe.roles.contains(RoleType.LOCK));
//    return Visibility(
//      visible: fe.roles.isNotEmpty,
//      child: Row(
//        children: <Widget>[
//          Checkbox(
//              value: widget.bloc.newFeatureValues[fe.environment.id]?.locked ==
//                  true,
//              onChanged: disabled
//                  ? null
//                  : (value) {
//                      widget.bloc.dirty[fe.environment.id] = true;
//                      setState(() {
//                        if (widget.bloc.newFeatureValues[fe.environment.id] ==
//                            null) {
//                          widget.bloc.newFeatureValues[fe.environment.id] =
//                              FeatureValue()
//                                ..key = widget.feature.key
//                                ..environmentId = fe.environment.id
//                                ..locked = false;
//                        }
//                        widget.bloc.newFeatureValues[fe.environment.id].locked =
//                            value;
//                      });
//                    }),
//          Icon(
//            widget.bloc.newFeatureValues[fe.environment.id]?.locked == true
//                ? Icons.lock
//                : Icons.lock_open,
//            color: Theme.of(context).hintColor,
//            size: 24.0,
//            semanticLabel: 'Text to announce in accessibility modes',
//          ),
//        ],
//      ),
//    );
//  }
//
//  Widget _buildResetButton(FeatureEnvironment fe, BuildContext context) {
//    return Container(
//      padding: EdgeInsets.only(left: 5, right: 5),
//      child: Visibility(
//          visible: widget.bloc.hasValue(fe) &&
//                  widget.bloc.newFeatureValues[fe.environment.id]?.locked !=
//                      true
//              ? true
//              : false,
//          child: FHFlatButtonTransparent(
//            title: "RESET",
//            onPressed: () {
//              widget.bloc.dirty[fe.environment.id] = true;
//              setState(() {
//                widget.bloc.resetValue(fe);
//              });
//            },
//          )),
//    );
//  }
//
//  Widget _buildStatusLabel(FeatureEnvironment fe, BuildContext context) {
//    if (widget.bloc.hasValue(fe)) {
//      return FHTagWidget(
//        text: "SET",
//        state: TagStatus.active,
//      );
//    }
//    if (fe.roles.isEmpty) {
//      return FHTagWidget(
//        text: "NO ACCESS",
//        state: TagStatus.disabled,
//      );
//    }
//    return FHTagWidget(
//      text: "NOT SET",
//      state: TagStatus.inactive,
//    );
//  }
//}


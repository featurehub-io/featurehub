import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/fhos_logger.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_error.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/portfolio/feature_filter_bloc.dart';
import 'package:openapi_dart_common/openapi.dart';
import 'package:bloc_provider/bloc_provider.dart';

import '../per_application_features_bloc.dart';

Widget createFeatureDialog(PerApplicationFeaturesBloc bloc, Feature? feature) {
  return BlocProvider<FeatureFilterBloc>.builder(creator: (c,b) => FeatureFilterBloc(bloc.mrClient),
      builder: (c,b) {
        if (feature == null) {
          return CreateFeatureDialogWidget(bloc: bloc);
        }

        return FutureBuilder(
            future: bloc.loadFreshFeature(feature.key),
            builder: (context, asyncSnapshot) {
              if (asyncSnapshot.hasError) {
                return FHErrorWidget(error: FHError(
                    AppLocalizations.of(context)!.errorNotFound));
              }

              if (asyncSnapshot.hasData) {
                return CreateFeatureDialogWidget(
                    bloc: bloc, feature: asyncSnapshot.data);
              }

              return FHLoadingIndicator();
            }
        );
      }
  );
}

class CreateFeatureDialogWidget extends StatefulWidget {
  final Feature? feature;
  final PerApplicationFeaturesBloc bloc;

  const CreateFeatureDialogWidget({
    super.key,
    required this.bloc,
    this.feature,
  });

  @override
  State<StatefulWidget> createState() {
    return _CreateFeatureDialogWidgetState();
  }
}

class _CreateFeatureDialogWidgetState extends State<CreateFeatureDialogWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _featureName = TextEditingController();
  final TextEditingController _featureKey = TextEditingController();
  final TextEditingController _featureAlias = TextEditingController();
  final TextEditingController _featureLink = TextEditingController();
  final TextEditingController _featureDesc = TextEditingController();

  bool isUpdate = false;
  bool isError = false;
  FeatureValueType? _dropDownFeatureTypeValue;
  final List<SearchFeatureFilterItem> _selectedFilters = [];
  late FeatureFilterBloc _filterBloc;

  @override
  void initState() {
    super.initState();
    _filterBloc = BlocProvider.of<FeatureFilterBloc>(context);
    if (widget.feature != null) {
      _featureName.text = widget.feature!.name;
      _featureKey.text = widget.feature!.key;
      _featureAlias.text = widget.feature!.alias ?? '';
      _featureLink.text = widget.feature!.link ?? '';
      _featureDesc.text = widget.feature!.description ?? '';
      _dropDownFeatureTypeValue = widget.feature!.valueType;
      isUpdate = true;

      // Initialize filters
      fhosLogger.fine("features are ${widget.feature}");
      if (widget.feature!.featureFilter != null && widget.feature!.featureFilter!.isNotEmpty) {
        fhosLogger.fine("checking for filter stream");
        _filterBloc.filterResultStream.take(1).listen((result) {
          fhosLogger.fine("filter stream is ${result}");
          if (result != null) {
            setState(() {
              _selectedFilters.addAll(result.filters.where((f) => widget.feature!.featureFilter!.contains(f.id)));
              fhosLogger.fine("found matching ${_selectedFilters}");
              _updateMatching();
            });
          }
        });
      }
    }
  }

  void _updateMatching() {
    _filterBloc.findMatchingResults(
      _selectedFilters.map((f) => f.id).toList(),
      MatchTypeEnum.serviceaccounts,
    );
  }

  @override
  Widget build(BuildContext context) {
    var isReadOnly = false;

    // only let this screen to be editable if right permissions exist
    if (isUpdate) {
      isReadOnly =
          !widget.bloc.mrClient.userHasFeatureEditRoleInCurrentApplication;
    } else {
      isReadOnly =
          !widget.bloc.mrClient.userHasFeatureCreationRoleInCurrentApplication;
    }

    final l10n = AppLocalizations.of(context)!;
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(widget.feature == null
            ? l10n.createNewFeature
            : (isReadOnly ? l10n.viewFeature : l10n.editFeature)),
        content: SizedBox(
          width: 500,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                TextFormField(
                    controller: _featureName,
                    decoration: InputDecoration(labelText: l10n.featureNameLabel),
                    readOnly: isReadOnly,
                    autofocus: true,
                    onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                    validator: ((v) {
                      if (v == null || v.isEmpty) {
                        return l10n.featureNameRequired;
                      }
                      if (v.length < 4) {
                        return l10n.featureNameTooShort;
                      }
                      return null;
                    })),
                TextFormField(
                    controller: _featureKey,
                    readOnly: isReadOnly,
                    decoration: InputDecoration(
                        labelText: l10n.featureKeyLabel,
                        hintText: l10n.featureKeyHint,
                        hintStyle: Theme.of(context).textTheme.bodySmall),
                    onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                    validator: ((v) {
                      if (v == null || v.isEmpty) {
                        return l10n.featureKeyRequired;
                      }
                      if (!validateFeatureKey(v)) {
                        return l10n.featureKeyNoWhitespace;
                      }
                      return null;
                    })),
                TextFormField(
                  readOnly: isReadOnly,
                  controller: _featureDesc,
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  decoration: InputDecoration(
                      labelText: l10n.featureDescriptionLabel,
                      hintText: l10n.featureDescriptionHint,
                      hintStyle: Theme.of(context).textTheme.bodySmall),
                ),
                TextFormField(
                  readOnly: isReadOnly,
                  controller: _featureLink,
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  decoration: InputDecoration(
                      labelText: l10n.featureLinkLabel,
                      hintText: l10n.featureLinkHint,
                      hintStyle: Theme.of(context).textTheme.bodySmall),
                ),
                if (!isUpdate)
                  Padding(
                    padding: const EdgeInsets.only(top: 14.0),
                    child: InkWell(
                      mouseCursor: SystemMouseCursors.click,
                      child: DropdownButton(
                        icon: const Padding(
                          padding: EdgeInsets.only(left: 8.0),
                          child: Icon(
                            Icons.keyboard_arrow_down,
                            size: 18,
                          ),
                        ),
                        isExpanded: false,
                        items: FeatureValueType.values
                            .map((FeatureValueType dropDownStringItem) {
                          return DropdownMenuItem<FeatureValueType>(
                              value: dropDownStringItem,
                              child: Text(
                                  _transformValuesToString(dropDownStringItem, l10n),
                                  style: Theme.of(context).textTheme.bodyMedium));
                        }).toList(),
                        hint: Text(l10n.selectFeatureType,
                            style: Theme.of(context).textTheme.titleSmall),
                        onChanged: (Object? value) {
                          if (!isReadOnly) {
                            setState(() {
                              _dropDownFeatureTypeValue =
                                  value as FeatureValueType?;
                            });
                          }
                        },
                        value: _dropDownFeatureTypeValue,
                      ),
                    ),
                  ),
                if (isError)
                  Text(
                    l10n.selectFeatureType,
                    style: Theme.of(context)
                        .textTheme
                        .bodyMedium!
                        .copyWith(color: Theme.of(context).colorScheme.error),
                  ),
                const SizedBox(height: 24),
                Text(l10n.selectFiltersToApply, style: Theme.of(context).textTheme.titleSmall),
                _filtersChips(l10n, isReadOnly),
                const SizedBox(height: 16),
                _buildMatchingPreview(l10n),
              ],
            ),
          ),
        ),
        actions: <Widget>[
          FHFlatButtonTransparent(
            title: l10n.cancel,
            keepCase: true,
            onPressed: () {
              widget.bloc.mrClient.removeOverlay();
            },
          ),
          if (!isReadOnly)
            FHFlatButton(
                title: isUpdate ? l10n.update : l10n.create,
                keepCase: true,
                onPressed: (() async {
                  if (_formKey.currentState!.validate()) {
                    try {
                      final filterIds = _selectedFilters.map((f) => f.id).toList();
                      if (isUpdate) {
                        await widget.bloc.updateFeature(
                            widget.feature!,
                            _featureName.text,
                            _featureKey.text,
                            _featureAlias.text,
                            _featureLink.text,
                            _featureDesc.text,
                            featureFilterIds: filterIds);
                        widget.bloc.mrClient.removeOverlay();
                        await widget.bloc
                            .updateApplicationFeatureValuesStream();
                        widget.bloc.mrClient.addSnackbar(
                            Text(l10n.featureUpdated(_featureName.text)));
                      } else {
                        if (_dropDownFeatureTypeValue != null) {
                          await widget.bloc.createFeature(
                              _featureName.text,
                              _featureKey.text,
                              _dropDownFeatureTypeValue!,
                              _featureAlias.text,
                              _featureLink.text,
                              _featureDesc.text,
                              featureFilterIds: filterIds);
                          widget.bloc.mrClient.removeOverlay();
                          widget.bloc.updateApplicationFeatureValuesStream();
                          widget.bloc.mrClient.addSnackbar(
                              Text(l10n.featureCreated(_featureName.text)));
                        } else {
                          setState(() {
                            isError = true;
                          });
                        }
                      }
                    } catch (e, s) {
                      if (e is ApiException && e.code == 409) {
                        widget.bloc.mrClient.customError(
                            messageTitle: l10n.featureKeyAlreadyExists(_featureKey.text));
                      } else {
                        await widget.bloc.mrClient.dialogError(e, s);
                      }
                    }
                  }
                }))
        ],
      ),
    );
  }

  Widget _filtersChips(AppLocalizations l10n, bool isReadOnly) {
    return StreamBuilder<SearchFeatureFilterResult?>(
      stream: _filterBloc.filterResultStream,
      builder: (context, snapshot) {
        if (!snapshot.hasData) return const SizedBox.shrink();

        final allFilters = snapshot.data!.filters;

        if (allFilters.isEmpty) {
          return Text(
            l10n.noFeatureFiltersFound,
            style: Theme.of(context)
                .textTheme
                .bodySmall!
                .copyWith(color: Theme.of(context).disabledColor),
          );
        }

        return Wrap(
          spacing: 8,
          runSpacing: 4,
          children: allFilters.map((filter) {
            final isSelected = _selectedFilters.any((s) => s.id == filter.id);
            return FilterChip(
              label: Text(filter.name),
              selected: isSelected,
              selectedColor: Theme.of(context).colorScheme.primaryContainer,
              checkmarkColor: Theme.of(context).colorScheme.onPrimaryContainer,
              labelStyle: TextStyle(
                color: isSelected
                    ? Theme.of(context).colorScheme.onPrimaryContainer
                    : Theme.of(context).colorScheme.onSurface,
              ),
              onSelected: isReadOnly
                  ? null
                  : (selected) {
                      setState(() {
                        if (selected) {
                          _selectedFilters.add(filter);
                        } else {
                          _selectedFilters.removeWhere((s) => s.id == filter.id);
                        }
                        _updateMatching();
                      });
                    },
            );
          }).toList(),
        );
      },
    );
  }

  Widget _buildMatchingPreview(AppLocalizations l10n) {
    return StreamBuilder<MatchingFilterResults?>(
      stream: _filterBloc.matchingResultsStream,
      builder: (context, snapshot) {
        if (!snapshot.hasData || snapshot.data!.matchingResults.isEmpty) {
          return const SizedBox.shrink();
        }

        final res = snapshot.data!;
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.matchingServiceAccounts, style: Theme.of(context).textTheme.bodySmall),
            Wrap(
              spacing: 8,
              children: res.matchingResults.map((m) => Chip(label: Text(m.name))).toList(),
            ),
            const SizedBox(height: 8),
          ],
        );
      },
    );
  }

  String _transformValuesToString(FeatureValueType featureValueType, AppLocalizations l10n) {
    switch (featureValueType) {
      case FeatureValueType.STRING:
        return l10n.featureTypeString;
      case FeatureValueType.NUMBER:
        return l10n.featureTypeNumber;
      case FeatureValueType.BOOLEAN:
        return l10n.featureTypeBoolean;
      case FeatureValueType.JSON:
        return l10n.featureTypeJson;
    }
  }
}

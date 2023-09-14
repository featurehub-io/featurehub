import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_group_bloc.dart';

class FeaturesDropDown extends StatefulWidget {
  final List<FeatureGroupFeature> features;
  final FeatureGroupBloc bloc;

  const FeaturesDropDown({Key? key, required this.features, required this.bloc})
      : super(key: key);

  @override
  _FeaturesDropDownState createState() => _FeaturesDropDownState();
}

class _FeaturesDropDownState extends State<FeaturesDropDown> {
  String? currentValue;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
        onPressed: () => {},
        child: Container(
            constraints: const BoxConstraints(maxWidth: 200),
            child: DropdownButtonHideUnderline(
                child: DropdownButton(
              icon: const Padding(
                padding: EdgeInsets.only(left: 8.0),
                child: Icon(
                  Icons.keyboard_arrow_down,
                  size: 18,
                ),
              ),
              isExpanded: true,
              isDense: true,
              items: widget.features.isNotEmpty
                  ? widget.features.map((FeatureGroupFeature feature) {
                      return DropdownMenuItem<String>(
                          value: feature.id,
                          child: Text(
                            feature.name,
                            style: Theme.of(context).textTheme.bodyMedium,
                            overflow: TextOverflow.ellipsis,
                          ));
                    }).toList()
                  : null,
              hint: Text('Select feature to add',
                  style: Theme.of(context).textTheme.titleSmall),
              onChanged: (String? value) async {
                setState(() {
                  currentValue = value;
                  widget.bloc.selectedFeatureToAdd = value;
                });
              },
              value: currentValue,
            ))));
  }
}

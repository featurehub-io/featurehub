

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';

class FeaturePaginationWidget extends StatelessWidget {
  final FeatureGrouping grouping;

  const FeaturePaginationWidget({Key? key, required this.grouping}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);

    return StreamBuilder<FeaturesByType>(
        stream: bloc.appFeatures(grouping),
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.data!.isEmpty) {
            return const SizedBox.shrink();
          }

          final features = snapshot.data!;

          return Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              Expanded(child: _FeatureSearch(bloc: bloc, features: features, grouping: grouping)),
              _Pagination(bloc: bloc, features: features, grouping: grouping)
            ],
          );
        });
  }
}

class _FeatureSearch extends StatelessWidget {
  final PerApplicationFeaturesBloc bloc;
  final FeaturesByType features;
  final FeatureGrouping grouping;

  _FeatureSearch({required this.bloc, required this.features, required this.grouping});

  @override
  Widget build(BuildContext context) {
    return Container();
  }
}

class _Pagination extends StatelessWidget {
  final PerApplicationFeaturesBloc bloc;
  final FeaturesByType features;
  final FeatureGrouping grouping;

  _Pagination({required this.bloc, required this.features, required this.grouping});

  @override
  Widget build(BuildContext context) {
    if (features.applicationFeatureValues.maxFeatures <= bloc.itemsPerPage) {
      return const SizedBox.shrink();
    }

    int maxPages = (features.applicationFeatureValues.maxFeatures / bloc.itemsPerPage).truncate() - 1;

    return Row(children: [
      if (features.pageNumber > 0)
        FHFlatButton(title: "<<", onPressed: () {
          bloc.updateFeatureGrouping(grouping, features.filter, 0);
        },),

      if (features.pageNumber > 0)
        FHFlatButton(title: "<", onPressed: () {
          bloc.updateFeatureGrouping(grouping, features.filter, features.pageNumber - 1);
        },),

      if (features.pageNumber < maxPages)
        FHFlatButton(title: ">", onPressed: () {
          bloc.updateFeatureGrouping(grouping, features.filter, features.pageNumber + 1);
        },),

      if (features.pageNumber < maxPages)
        FHFlatButton(title: ">>", onPressed: () {
          bloc.updateFeatureGrouping(grouping, features.filter, maxPages);
        },),
    ],);
  }
}

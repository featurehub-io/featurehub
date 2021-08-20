import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/widgets/apps/manage_service_accounts_bloc.dart';
import 'package:open_admin_app/widgets/apps/service_account_list_widget.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';

/// Every user has access to portfolios, they can only see the ones they have access to
/// and their access will be limited based on whether they are a super admin.
class ManageServiceAccountsRoute extends StatelessWidget {
  const ManageServiceAccountsRoute({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return _ServiceAccountSearchWidget();
  }
}

class _ServiceAccountSearchWidget extends StatefulWidget {
  @override
  _ServiceAccountSearchState createState() => _ServiceAccountSearchState();
}

class _ServiceAccountSearchState extends State<_ServiceAccountSearchWidget> {
  String? selectedPortfolio;

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManageServiceAccountsBloc>(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        _headerRow(),
        _filterRow(context, bloc),
        const ServiceAccountsListWidget(),
      ],
    );
  }

  Widget _headerRow() {
    return Container(
        padding: const EdgeInsets.fromLTRB(0, 8, 30, 10),
        child: const FHHeader(
          title: 'Manage service accounts',
        ));
  }

  Widget _filterRow(BuildContext context, ManageServiceAccountsBloc bloc) {
    final bs = BorderSide(color: Theme.of(context).dividerColor);
    return Column(
      children: <Widget>[
        Container(
          padding: const EdgeInsets.fromLTRB(10, 10, 30, 10),
          decoration: BoxDecoration(
              color: Theme.of(context).cardColor,
              border: Border(bottom: bs, left: bs, right: bs, top: bs)),
          child: StreamBuilder<ReleasedPortfolio?>(
              stream: bloc.mrClient.personState.isCurrentPortfolioOrSuperAdmin,
              builder: (context, snapshot) {
                if (snapshot.hasData &&
                    snapshot.data!.currentPortfolioOrSuperAdmin) {
                  return Row(
                    children: <Widget>[
                      TextButton.icon(
                        icon: const Icon(Icons.add),
                        label: const Text('Create new service account'),
                        onPressed: () =>
                            bloc.mrClient.addOverlay((BuildContext context) {
                          return ServiceAccountUpdateDialogWidget(
                            bloc: bloc,
                          );
                        }),
                      ),
                    ],
                  );
                } else {
                  return Container();
                }
              }),
        ),
      ],
    );
  }
}

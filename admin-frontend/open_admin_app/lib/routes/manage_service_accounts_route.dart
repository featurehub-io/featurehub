import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/widgets/apps/manage_service_accounts_bloc.dart';
import 'package:open_admin_app/widgets/apps/service_account_list_widget.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';

/// Every user has access to portfolios, they can only see the ones they have access to
/// and their access will be limited based on whether they are a super admin.
class ManageServiceAccountsRoute extends StatefulWidget {
  final bool createServiceAccount;

  const ManageServiceAccountsRoute({Key? key, required this.createServiceAccount})
      :super(key: key);

  @override
  _ServiceAccountSearchState createState() => _ServiceAccountSearchState();
}

class _ServiceAccountSearchState extends State<ManageServiceAccountsRoute> {
  String? selectedPortfolio;
  ManageServiceAccountsBloc? bloc;

  @override
  void initState() {
    super.initState();

    bloc = BlocProvider.of<ManageServiceAccountsBloc>(context);
  }

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
    return Column(
      children: <Widget>[
        StreamBuilder<ReleasedPortfolio?>(
            stream: bloc.mrClient.streamValley.currentPortfolioStream,
            builder: (context, snapshot) {
              if (snapshot.hasData &&
                  snapshot.data!.currentPortfolioOrSuperAdmin) {
                return Row(
                  children: <Widget>[
                    TextButton.icon(
                      icon: const Icon(Icons.add),
                      label: const Text('Create new service account'),
                      onPressed: () => _createServiceAccount(bloc),
                    ),
                  ],
                );
              } else {
                return Container();
              }
            }),
      ],
    );
  }

  @override
  void didUpdateWidget(ManageServiceAccountsRoute oldWidget) {
    super.didUpdateWidget(oldWidget);
    _createServiceAccountCheck();
  }

  void _createServiceAccountCheck() {
    if (widget.createServiceAccount && bloc != null) {
      WidgetsBinding.instance.addPostFrameCallback((timeStamp) {
        _createServiceAccount(bloc!);
      });
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _createServiceAccountCheck();
  }

  _createServiceAccount(ManageServiceAccountsBloc bloc) {
    bloc.mrClient.addOverlay((BuildContext context) {
      return ServiceAccountUpdateDialogWidget(
        bloc: bloc,
      );
    });
  }
}

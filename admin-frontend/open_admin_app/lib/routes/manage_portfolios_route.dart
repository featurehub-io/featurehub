import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/portfolio/portfolio_bloc.dart';
import 'package:open_admin_app/widgets/portfolio/portfolio_widget.dart';

/// Every user has access to portfolios, they can only see the ones they have access to
/// and their access will be limited based on whether they are a super admin.

class PortfolioRoute extends StatelessWidget {
  const PortfolioRoute({Key? key}) : super(key: key);
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PortfolioBloc>(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.only(top: 8.0),
          child: Wrap(
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              const FHHeader(
                title: 'Manage portfolios',
              ),
              if (bloc.mrClient.userIsSuperAdmin == true)
                Padding(
                  padding: const EdgeInsets.only(top: 12.0),
                  child: TextButton.icon(
                    icon: const Icon(Icons.add),
                    label: const Text('Create new portfolio'),
                    onPressed: () => bloc.mrClient.addOverlay((BuildContext context) {
                      return PortfolioUpdateDialogWidget(
                        bloc: bloc,
                      );
                    }),
                  ),
                )
            ],
          )
        ),
        const SizedBox(height: 16.0),
        Container(
          constraints: const BoxConstraints(maxWidth: 300),
          child: TextField(
            decoration: const InputDecoration(hintText: 'Search portfolios',
                icon: Icon(Icons.search)),
            onChanged: (val) => bloc.triggerSearch(val),
          ),
        ),
        const SizedBox(height: 16.0),
        const PortfolioListWidget(),
      ],
    );
  }
}

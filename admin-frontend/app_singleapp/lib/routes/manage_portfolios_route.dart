import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:app_singleapp/widgets/common/fh_icon_text_button.dart';
import 'package:app_singleapp/widgets/portfolio/portfolio_bloc.dart';
import 'package:app_singleapp/widgets/portfolio/portfolio_widget.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

/// Every user has access to portfolios, they can only see the ones they have access to
/// and their access will be limited based on whether they are a super admin.
class PortfolioRoute extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(child: _PortfolioSearchWidget());
  }
}

class _PortfolioSearchWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PortfolioBloc>(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        _headerRow(context, bloc),
        _filterRow(context, bloc),
        PortfolioListWidget(),
      ],
    );
  }

  Widget _headerRow(BuildContext context, PortfolioBloc bloc) {
    return Container(
        padding: const EdgeInsets.fromLTRB(0, 8, 30, 0),
        child: FHHeader(
          title: 'Manage portfolios',
          children: [
            if (bloc.mrClient.userIsSuperAdmin == true)
              FHIconTextButton(
                iconData: Icons.add,
                label: 'Create new portfolio',
                onPressed: () =>
                    bloc.mrClient.addOverlay((BuildContext context) {
                  return PortfolioUpdateDialogWidget(
                    bloc: bloc,
                  );
                }),
                keepCase: true,
              )
          ],
        ));
  }

  Widget _filterRow(BuildContext context, PortfolioBloc bloc) {
    final bs = BorderSide(color: Theme.of(context).dividerColor);
    return Container(
      padding: const EdgeInsets.fromLTRB(30, 10, 30, 10),
      decoration: BoxDecoration(
          color: Theme.of(context).cardColor,
          border: Border(bottom: bs, left: bs, right: bs, top: bs)),
      child: Row(
        children: <Widget>[
          Container(
            width: 300,
            child: TextField(
              decoration: InputDecoration(
                  //   border: InputBorder.,
                  hintText: 'Filter portfolios'),
              onChanged: (val) => bloc.triggerSearch(val),
            ),
          ),
        ],
      ),
    );
  }
}

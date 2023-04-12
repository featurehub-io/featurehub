import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/portfolio/portfolio_bloc.dart';
import 'package:openapi_dart_common/openapi.dart';

import '../common/fh_loading_indicator.dart';

class PortfolioListWidget extends StatelessWidget {
  const PortfolioListWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PortfolioBloc>(context);
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return StreamBuilder<List<Portfolio>>(
        stream: bloc.portfolioSearch,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const FHLoadingIndicator();
          } else if (snapshot.connectionState == ConnectionState.active ||
              snapshot.connectionState == ConnectionState.done) {
            if (snapshot.hasError) {
              return const FHLoadingError();
            } else if (snapshot.hasData) {
              return ListView.builder(
                  shrinkWrap: true,
                  itemCount: snapshot.data!.length,
                  itemBuilder: (context, index) {
                    final portfolio = snapshot.data![index];
                    return _PortfolioWidget(
                      portfolio: portfolio,
                      mr: mrBloc,
                      bloc: bloc,
                    );
                  });
            }
          }
          return const SizedBox.shrink();
        });
  }
}

class _PortfolioWidget extends StatelessWidget {
  final Portfolio portfolio;
  final ManagementRepositoryClientBloc mr;
  final PortfolioBloc bloc;

  const _PortfolioWidget(
      {Key? key, required this.portfolio, required this.mr, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Row(
          children: <Widget>[
            Expanded(
                child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(portfolio.name),
                Text(
                  portfolio.description ?? '',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            )),
            mr.userIsSuperAdmin
                ? Row(
                    children: <Widget>[
                      FHIconButton(
                          icon: const Icon(Icons.edit),
                          onPressed: () =>
                              bloc.mrClient.addOverlay((BuildContext context) {
                                return PortfolioUpdateDialogWidget(
                                    bloc: bloc, portfolio: portfolio);
                              })),
                      if (mr.userIsSuperAdmin)
                        FHIconButton(
                            icon: const Icon(Icons.delete),
                            onPressed: () => bloc.mrClient
                                    .addOverlay((BuildContext context) {
                                  return PortfolioDeleteDialogWidget(
                                    portfolio: portfolio,
                                    bloc: bloc,
                                  );
                                }))
                    ],
                  )
                : Container(),
          ],
        ),
      ),
    );
  }
}

class PortfolioDeleteDialogWidget extends StatelessWidget {
  final Portfolio portfolio;
  final PortfolioBloc bloc;

  const PortfolioDeleteDialogWidget(
      {Key? key, required this.portfolio, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      content:
          'All Groups, Features, Environments and Applications belonging to this portfolio will be deleted \n\nThis cannot be undone!',
      thing: "portfolio '${portfolio.name}'",
      deleteSelected: () async {
        try {
          await bloc.deletePortfolio(portfolio.id!, true, true);
          bloc.triggerSearch('');
          bloc.mrClient
              .addSnackbar(Text("Portfolio '${portfolio.name}' deleted!"));
          return true;
        } catch (e, s) {
          await bloc.mrClient.dialogError(e, s);
          return false;
        }
      },
    );
  }
}

class PortfolioUpdateDialogWidget extends StatefulWidget {
  final Portfolio? portfolio;
  final PortfolioBloc bloc;

  const PortfolioUpdateDialogWidget(
      {Key? key, required this.bloc, this.portfolio})
      : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _PortfolioUpdateDialogWidgetState();
  }
}

class _PortfolioUpdateDialogWidgetState
    extends State<PortfolioUpdateDialogWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _portfolioName = TextEditingController();
  final TextEditingController _portfolioDescription = TextEditingController();

  @override
  Widget build(BuildContext context) {
    if (widget.portfolio != null) {
      _portfolioName.text = widget.portfolio!.name;
      _portfolioDescription.text = widget.portfolio!.description ?? '';
    }
    return Form(
        key: _formKey,
        child: FHAlertDialog(
            title: Text(widget.portfolio == null
                ? 'Create new portfolio'
                : 'Edit portfolio'),
            content: SizedBox(
              width: 500,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  TextFormField(
                      controller: _portfolioName,
                      decoration:
                          const InputDecoration(labelText: 'Portfolio name'),
                      autofocus: true,
                      validator: ((v) {
                        if (v == null || v.isEmpty) {
                          return 'Please enter a portfolio name';
                        }
                        if (v.length < 4) {
                          return 'Portfolio name needs to be at least 4 characters long';
                        }
                        return null;
                      })),
                  TextFormField(
                      controller: _portfolioDescription,
                      decoration: const InputDecoration(
                          labelText: 'Portfolio description'),
                      validator: ((v) {
                        if (v == null || v.isEmpty) {
                          return 'Please enter a portfolio description';
                        }
                        if (v.length < 4) {
                          return 'Portfolio description needs to be at least 4 characters long';
                        }
                        return null;
                      })),
                ],
              ),
            ),
            actions: <Widget>[
              FHFlatButtonTransparent(
                title: 'Cancel',
                keepCase: true,
                onPressed: () {
                  widget.bloc.mrClient.removeOverlay();
                },
              ),
              FHFlatButton(
                  title: widget.portfolio == null ? 'Create' : 'Update',
                  onPressed: (() async {
                    if (_formKey.currentState!.validate()) {
                      try {
                        await _callUpdatePortfolio(
                            _portfolioName.text, _portfolioDescription.text);
                        await widget.bloc.refreshPortfolios();
                        // force list update
                        widget.bloc.mrClient.removeOverlay();
                        widget.bloc.triggerSearch('');
                        widget.bloc.mrClient.addSnackbar(Text(
                            "Portfolio '${_portfolioName.text}' ${widget.portfolio == null ? " created" : " updated"}!"));
                      } catch (e, s) {
                        if (e is ApiException && e.code == 409) {
                          widget.bloc.mrClient.customError(
                            messageTitle:
                                "Portfolio '${_portfolioName.text}' already exists",
                          );
                        } else {
                          await widget.bloc.mrClient.dialogError(e, s);
                        }
                      }
                    }
                  }))
            ]));
  }

  Future _callUpdatePortfolio(String name, String desc) {
    final portfolio = widget.portfolio ?? Portfolio(name: '', description: '');
    portfolio.name = name.trim();
    portfolio.description = desc.trim();
    return widget.portfolio == null
        ? widget.bloc.createPortfolio(portfolio)
        : widget.bloc.updatePortfolio(portfolio);
  }
}

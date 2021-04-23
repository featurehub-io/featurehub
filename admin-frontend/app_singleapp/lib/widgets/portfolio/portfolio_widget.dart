import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_alert_dialog.dart';
import 'package:app_singleapp/widgets/common/fh_delete_thing.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/fh_icon_button.dart';
import 'package:app_singleapp/widgets/portfolio/portfolio_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

class PortfolioListWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PortfolioBloc>(context);
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return StreamBuilder<List<Portfolio>>(
        stream: bloc.portfolioSearch,
        builder: (context, snapshot) {
          if (snapshot.hasError || snapshot.data == null) {
            return Container(
                padding: EdgeInsets.all(30), child: Text('Loading...'));
          }

          return Column(
            children: <Widget>[
              for (Portfolio p in snapshot.data)
                _PortfolioWidget(
                  portfolio: p,
                  mr: mrBloc,
                  bloc: bloc,
                )
            ],
          );
        });
  }
}

class _PortfolioWidget extends StatelessWidget {
  final Portfolio portfolio;
  final ManagementRepositoryClientBloc mr;
  final PortfolioBloc bloc;

  const _PortfolioWidget(
      {Key? key, required this.portfolio, required this.mr, required this.bloc})
      : assert(portfolio != null),
        assert(mr != null),
        assert(bloc != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    final bs = BorderSide(color: Theme.of(context).dividerColor);

    return Container(
      padding: const EdgeInsets.fromLTRB(30, 5, 30, 5),
      decoration: BoxDecoration(
          color: Theme.of(context).cardColor,
          border: Border(bottom: bs, left: bs, right: bs)),
      child: Row(
        children: <Widget>[
          Expanded(
              child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text('${portfolio.name}'),
              Text(
                portfolio.description != null ? '${portfolio.description}' : '',
                style: Theme.of(context).textTheme.caption,
              ),
            ],
          )),
          mr.userIsSuperAdmin
              ? Row(
                  children: <Widget>[
                    FHIconButton(
                        icon: Icon(Icons.edit),
                        onPressed: () =>
                            bloc.mrClient.addOverlay((BuildContext context) {
                              return PortfolioUpdateDialogWidget(
                                  bloc: bloc, portfolio: portfolio);
                            })),
                    if (mr.userIsSuperAdmin)
                      FHIconButton(
                          icon: Icon(Icons.delete),
                          onPressed: () =>
                              bloc.mrClient.addOverlay((BuildContext context) {
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
    );
  }
}

class PortfolioDeleteDialogWidget extends StatelessWidget {
  final Portfolio portfolio;
  final PortfolioBloc bloc;

  const PortfolioDeleteDialogWidget(
      {Key? key, required this.portfolio, required this.bloc})
      : assert(portfolio != null),
        assert(bloc != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      content:
          'All Groups, Features, Environments and Applications belonging to this portfolio will be deleted \n\nThis cannot be undone!',
      thing: "portfolio '${portfolio.name}'",
      deleteSelected: () async {
        try {
          await bloc.deletePortfolio(portfolio.id, true, true);
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
  final Portfolio portfolio;
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
      _portfolioName.text = widget.portfolio.name;
      _portfolioDescription.text = widget.portfolio.description;
    }
    return Form(
        key: _formKey,
        child: FHAlertDialog(
            title: Text(widget.portfolio == null
                ? 'Create new portfolio'
                : 'Edit portfolio'),
            content: Container(
              width: 500,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  TextFormField(
                      controller: _portfolioName,
                      decoration: InputDecoration(labelText: 'Portfolio name'),
                      validator: ((v) {
                        if (v.isEmpty) {
                          return 'Please enter a portfolio name';
                        }
                        if (v.length < 4) {
                          return 'Portfolio name needs to be at least 4 characters long';
                        }
                        return null;
                      })),
                  TextFormField(
                      controller: _portfolioDescription,
                      decoration:
                          InputDecoration(labelText: 'Portfolio description'),
                      validator: ((v) {
                        if (v.isEmpty) {
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
                    if (_formKey.currentState.validate()) {
                      try {
                        await _callUpdatePortfolio(
                            _portfolioName.text, _portfolioDescription.text);
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
    Portfolio portfolio;
    widget.portfolio != null
        ? portfolio = widget.portfolio
        : portfolio = Portfolio();
    portfolio.name = name.trim();
    portfolio.description = desc.trim();
    return widget.portfolio == null
        ? widget.bloc.createPortfolio(portfolio)
        : widget.bloc.updatePortfolio(portfolio);
  }
}

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/admin_sdk_service_account/admin_sa_access_key_display_widget.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';
import 'package:open_admin_app/widgets/common/fh_filled_input_decoration.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_footer_button_bar.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/user/common/admin_checkbox.dart';
import 'package:open_admin_app/widgets/user/common/portfolio_group_selector_widget.dart';
import 'package:open_admin_app/widgets/user/create/create_user_bloc.dart';
import 'package:openapi_dart_common/openapi.dart';

class CreateAdminServiceAccountsRoute extends StatelessWidget {
  final String title;

  const CreateAdminServiceAccountsRoute({super.key, required this.title});

  @override
  Widget build(BuildContext context) {
    return const FHCardWidget(
        width: 800, child: AddAdminServiceAccountFormWidget());
  }
}

class AddAdminServiceAccountFormWidget extends StatelessWidget {
  const AddAdminServiceAccountFormWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return const Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[TopAdminSAWidget(), BottomAdminSAWidget()],
    );
  }
}

class TopAdminSAWidget extends StatelessWidget {
  const TopAdminSAWidget({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);

    return StreamBuilder<CreateUserForm>(
        stream: bloc.formState,
        builder: (context, AsyncSnapshot<CreateUserForm> snapshot) {
          if (snapshot.hasData &&
              snapshot.data == CreateUserForm.successState) {
            return const TopAdminSAWidgetSuccess();
          }
          // ignore: prefer_const_constructors
          return TopAdminSAWidgetDefault();
        });
  }
}

class TopAdminSAWidgetDefault extends StatefulWidget {
  const TopAdminSAWidgetDefault({super.key});

  @override
  TopAdminSAWidgetDefaultState createState() => TopAdminSAWidgetDefaultState();
}

class TopAdminSAWidgetDefaultState extends State<TopAdminSAWidgetDefault> {
  final _name = TextEditingController();
  bool isAddButtonDisabled = true;

  @override
  void initState() {
    super.initState();
    final bloc = BlocProvider.of<CreateUserBloc>(context);
    bloc.formKey = GlobalKey<FormState>();
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);
    final l10n = AppLocalizations.of(context)!;

    return Form(
        key: bloc.formKey,
        child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              FHHeader(title: l10n.createAdminServiceAccount),
              Container(
                constraints: const BoxConstraints(maxWidth: 300),
                child: Column(
                  children: [
                    const SizedBox(height: 16.0),
                    TextFormField(
                      autofocus: true,
                      controller: _name,
                      decoration: fhFilledInputDecoration(
                        labelText: l10n.nameLabel,
                      ),
                      validator: (v) {
                        if (v?.isEmpty == true) {
                          return l10n.adminSaNameRequired;
                        }
                        return null;
                      },
                      onSaved: (v) => bloc.name = v,
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(top: 30.0),
                child: Text(
                  l10n.adminSaGroupsHint,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
              const PortfolioGroupSelector(),
              const AdminCheckboxWidget()
            ]));
  }
}

class TopAdminSAWidgetSuccess extends StatelessWidget {
  const TopAdminSAWidgetSuccess({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);
    final l10n = AppLocalizations.of(context)!;

    return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(l10n.adminSaCreated(bloc.name!),
              style: Theme.of(context).textTheme.titleLarge),
          AdminAccessKeyDisplayWidget(token: bloc.registrationUrl!.token),
          FHButtonBar(children: [
            FHFlatButtonTransparent(
                onPressed: () {
                  bloc.backToDefault();
                  ManagementRepositoryClientBloc.router
                      .navigateTo(context, '/admin-service-accounts');
                },
                title: l10n.close),
            FHFlatButton(
                onPressed: () {
                  bloc.backToDefault();
                },
                title: l10n.createAnotherServiceAccount,
                keepCase: true),
          ])
        ]);
  }
}

class BottomAdminSAWidget extends StatelessWidget {
  const BottomAdminSAWidget({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);

    return StreamBuilder<CreateUserForm>(
        stream: bloc.formState,
        builder: (context, AsyncSnapshot<CreateUserForm> snapshot) {
          if (snapshot.hasData &&
              snapshot.data == CreateUserForm.successState) {
            return Container();
          }
          return const CreateAdminSAFormButtons();
        });
  }
}

class CreateAdminSAFormButtons extends StatelessWidget {
  const CreateAdminSAFormButtons({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);
    final l10n = AppLocalizations.of(context)!;

    return FHButtonBar(children: <Widget>[
      FHFlatButtonTransparent(
        onPressed: () {
          if (bloc.formKey != null) {
            bloc.formKey!.currentState!.reset;
          }
          ManagementRepositoryClientBloc.router
              .navigateTo(context, '/admin-service-accounts');
        },
        title: l10n.cancel,
        keepCase: true,
      ),
      Padding(
          padding: const EdgeInsets.only(left: 8.0),
          child: FHFlatButton(
              onPressed: () async {
                if (bloc.formKey!.currentState!.validate()) {
                  bloc.formKey!.currentState!.save();
                  try {
                    await bloc.createUser(null, bloc.name!);
                  } catch (e, s) {
                    if (e is ApiException && e.code == 409) {
                      await bloc.client.dialogError(e, s,
                          messageTitle:
                              l10n.adminSaAlreadyExists(bloc.name!));
                    } else {
                      await bloc.client.dialogError(e, s);
                    }
                  }
                }
              },
              title: l10n.create))
    ]);
  }
}

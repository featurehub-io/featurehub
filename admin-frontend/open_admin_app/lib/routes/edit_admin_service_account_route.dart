import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';
import 'package:open_admin_app/widgets/common/fh_filled_input_decoration.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_footer_button_bar.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/user/common/admin_checkbox.dart';
import 'package:open_admin_app/widgets/user/common/portfolio_group_selector_widget.dart';
import 'package:open_admin_app/widgets/user/edit/edit_user_bloc.dart';

class EditAdminServiceAccountRoute extends StatelessWidget {
  const EditAdminServiceAccountRoute({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<EditUserBloc>(context);

    return FHCardWidget(
        width: 800,
        child: StreamBuilder(
            stream: bloc.formState,
            builder: (context, snapshot) {
              if (snapshot.data == EditUserForm.initialState) {
                return EditAdminServiceAccountFormWidget(person: bloc.person!);
              }
              return Container();
            }));
  }
}

class EditAdminServiceAccountFormWidget extends StatefulWidget {
  final Person person;

  const EditAdminServiceAccountFormWidget({Key? key, required this.person})
      : super(key: key);

  @override
  EditUserFormState createState() => EditUserFormState();
}

class EditUserFormState extends State<EditAdminServiceAccountFormWidget> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();

  bool isAddButtonDisabled = true;

  @override
  void initState() {
    super.initState();
    if (_name.text == '') {
      _name.text = widget.person.name!;
    }
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<EditUserBloc>(context);

    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          const Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              FHHeader(
                title: 'Edit admin SDK service account',
                children: <Widget>[],
              ),
            ],
          ),
          const SizedBox(height: 8.0),
          Container(
            constraints: const BoxConstraints(maxWidth: 400),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Padding(
                  padding: const EdgeInsets.only(top: 30),
                  child: TextFormField(
                    controller: _name,
                    decoration: fhFilledInputDecoration(labelText: 'Name'),
                    //  initialValue: bloc.person !=null ? bloc.person.name : '',
                    validator: (v) => (v?.isEmpty == true) ? 'Edit name' : null,
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(top: 30.0),
                  child: Text(
                    'Remove Admin Service Account from a group or add a new one',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ),
              ],
            ),
          ),
          const PortfolioGroupSelector(),
          if (bloc.mrClient.personState.userIsSuperAdmin)
            AdminCheckboxWidget(person: bloc.person),
          FHButtonBar(children: <Widget>[
            FHFlatButtonTransparent(
                onPressed: () {
                  _formKey.currentState!.reset;
                  ManagementRepositoryClientBloc.router
                      .navigateTo(context, '/admin-service-accounts');
                },
                title: 'Cancel',
                keepCase: true),
            Padding(
                padding: const EdgeInsets.only(left: 8.0),
                child: FHFlatButton(
                    onPressed: () {
                      if (_formKey.currentState!.validate()) {
                        _formKey.currentState!.save();
                        try {
                          bloc.updateApiKeyDetails(_name.text);
                          bloc.mrClient.addSnackbar(Text(
                              'Admin Service Account ${bloc.person!.name!} has been updated'));
                          ManagementRepositoryClientBloc.router
                              .navigateTo(context, '/admin-service-accounts');
                        } catch (e, s) {
                          bloc.mrClient.dialogError(e, s);
                        }
                      }
                    },
                    title: 'Save and close',
                    keepCase: true))
          ]),
        ],
      ),
    );
  }
}

import 'package:app_singleapp/widgets/user/common/select_portfolio_group_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class AdminCheckboxWidget extends StatefulWidget {
  final Person person;
  const AdminCheckboxWidget({Key key, this.person}) : super(key: key);

  @override
  AdminCheckboxWidgetState createState() {
    return AdminCheckboxWidgetState(person);
  }
}

class AdminCheckboxWidgetState extends State<AdminCheckboxWidget> {
  bool createUser = true;
  final Person person;
  bool isAdmin = false;

  @override
  void initState() {
    final bloc = BlocProvider.of<SelectPortfolioGroupBloc>(context);
    super.initState();
    if (widget.person != null) {
      createUser = false;
      isAdmin = bloc.mrClient.personState.isSuperAdminGroupFound(person.groups);
    }
  }

  AdminCheckboxWidgetState(this.person);
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<SelectPortfolioGroupBloc>(context);

    return bloc.mrClient.userIsSuperAdmin
        ? Container(
            constraints: BoxConstraints(maxWidth: 300),
            child: CheckboxListTile(
                title: Text(
                  'Set this user as a FeatureHub site admin.',
                  style: Theme.of(context).textTheme.caption,
                ),
                value: isAdmin,
                onChanged: (bool value) {
                  setState(() {
                    isAdmin = value;
                    if (value) {
                      bloc.pushAdminGroupToStream();
                    } else {
                      bloc.removeAdminGroupFromStream();
                    }
                  });
                }),
          )
        : Container();
  }
}

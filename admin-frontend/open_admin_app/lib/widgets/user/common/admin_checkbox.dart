import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/user/common/select_portfolio_group_bloc.dart';

class AdminCheckboxWidget extends StatefulWidget {
  final Person? person;
  const AdminCheckboxWidget({Key? key, this.person}) : super(key: key);

  @override
  AdminCheckboxWidgetState createState() {
    return AdminCheckboxWidgetState();
  }
}

class AdminCheckboxWidgetState extends State<AdminCheckboxWidget> {
  bool createUser = true;
  bool isAdmin = false;

  @override
  void initState() {
    final bloc = BlocProvider.of<SelectPortfolioGroupBloc>(context);
    super.initState();
    if (widget.person != null) {
      createUser = false;
      isAdmin = bloc.mrClient.personState
          .isSuperAdminGroupFound(widget.person!.groups);
    }
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<SelectPortfolioGroupBloc>(context);

    return bloc.mrClient.userIsSuperAdmin
        ? Container(
            constraints: const BoxConstraints(maxWidth: 300),
            child: CheckboxListTile(
                title: Text(
                  'Set this user as a FeatureHub site admin.',
                  style: Theme.of(context).textTheme.caption,
                ),
                value: isAdmin,
                onChanged: (bool? value) {
                  setState(() {
                    isAdmin = value == true;
                    if (value == true) {
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

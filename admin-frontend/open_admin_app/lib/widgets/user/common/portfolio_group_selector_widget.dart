import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/user/common/portfolio_group.dart';
import 'package:open_admin_app/widgets/user/common/select_portfolio_group_bloc.dart';

import '../../common/fh_filled_input_decoration.dart';

class PortfolioGroupSelector extends StatefulWidget {
  const PortfolioGroupSelector({Key? key}) : super(key: key);

  @override
  _PortfolioGroupSelectorState createState() => _PortfolioGroupSelectorState();
}

class _PortfolioGroupSelectorState extends State<PortfolioGroupSelector> {
  String? selectedPortfolio;
  String? selectedGroupID;

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<SelectPortfolioGroupBloc>(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Container(
          padding: const EdgeInsets.only(top: 8.0),
          child: LayoutBuilder(
            builder: (context, constraints) {
              if (constraints.maxWidth < 600) {
                return Column(children: <Widget>[
                  buildPortfolioDropDown(bloc),
                  const SizedBox(height: 16.0),
                  buildGroupDropDown(bloc)
                ]);
              } else {
                return Row(children: <Widget>[
                  buildPortfolioDropDown(bloc),
                  Container(
                      padding: const EdgeInsets.only(left: 14.0),
                      child: buildGroupDropDown(bloc))
                ]);
              }
            },
          ),
        ),
        buildPortfolioGroupChips(bloc),
      ],
    );
  }

  Widget buildPortfolioGroupChips(SelectPortfolioGroupBloc bloc) {
    return Padding(
      padding: const EdgeInsets.only(top: 16.0),
      child: StreamBuilder<Set<PortfolioGroup>?>(
          stream: bloc.addedGroupsStream,
          builder: (context, AsyncSnapshot<Set<PortfolioGroup>?> snapshot) {
            if (snapshot.hasData) {
              return Wrap(
                  spacing: 8.0,
                  runSpacing: 4.0,
                  children:
                      //check if portfolio is null - it is site admin group that we don't display as a chip
                      snapshot.data!
                          .map((item) => Padding(
                              padding: const EdgeInsets.all(6.0),
                              child: InputChip(
                                deleteIcon: const InkWell(
                                    mouseCursor: SystemMouseCursors.click,
                                    child: Icon(
                                      Icons.cancel,
                                      size: 18.0,
                                    )),
                                key: ObjectKey(item),
                                label: item.portfolio == null
                                    ? const Text('FeatureHub Administrators')
                                    : Text(
                                        '${item.portfolio?.name}: ${item.group.name}'),
                                onDeleted: () =>
                                    bloc.removeGroupFromStream(item),
                                materialTapTargetSize:
                                    MaterialTapTargetSize.shrinkWrap,
                              )))
                          .toList());
            }
            return Container();
          }),
    );
  }

  Widget buildGroupDropDown(SelectPortfolioGroupBloc bloc) {
    return StreamBuilder<List<Group>?>(
        stream: bloc.groups,
        builder: (context, AsyncSnapshot<List<Group>?> snapshot) {
          return Theme(
            data: Theme.of(context).copyWith(brightness: Brightness.light),
            child: Container(
              constraints: const BoxConstraints(maxWidth: 300),
              child: InputDecorator(
                decoration: fhFilledInputDecoration(labelText: 'Group'),
                child: InkWell(
                  mouseCursor: SystemMouseCursors.click,
                  child: DropdownButton(
                    autofocus: true,
                    icon: const Padding(
                      padding: EdgeInsets.only(left: 8.0),
                      child: Icon(
                        Icons.keyboard_arrow_down,
                        size: 18,
                      ),
                    ),
                    isExpanded: true,
                    isDense: true,
                    underline: Container(),
                    items: snapshot.data != null
                        ? snapshot.data!.map((Group dropDownStringItem) {
                            return DropdownMenuItem<String>(
                                value: dropDownStringItem.id,
                                child: Text(dropDownStringItem.name));
                          }).toList()
                        : null,
                    hint: Text('Select group',
                        style: Theme.of(context).textTheme.bodyText1),
                    onChanged: (String? value) {
                      if (value != null) {
                        setState(() {
                          selectedGroupID = value;
                          bloc.pushAddedGroupToStream(value);
                        });
                      }
                    },
                    value: snapshot.data != null ? selectedGroupID : null,
                  ),
                ),
              ),
            ),
          );
        });
  }

  StreamBuilder<List<Portfolio>?> buildPortfolioDropDown(
      SelectPortfolioGroupBloc bloc) {
    return StreamBuilder<List<Portfolio>?>(
        stream: bloc.portfolios,
        builder: (context, AsyncSnapshot<List<Portfolio>?> snapshot) {
          if (snapshot.hasData) {
            return Theme(
              data: Theme.of(context).copyWith(
//                canvasColor: Colors.blue,
                  brightness: Brightness.light),
              child: Container(
                constraints: const BoxConstraints(maxWidth: 300),
                child: InputDecorator(
                  decoration: fhFilledInputDecoration(labelText: 'Portfolio'),
                  child: InkWell(
                    mouseCursor: SystemMouseCursors.click,
                    child: DropdownButton<String?>(
                      autofocus: true,
                      icon: const Padding(
                        padding: EdgeInsets.only(left: 8.0),
                        child: Icon(
                          Icons.keyboard_arrow_down,
                          size: 18,
                        ),
                      ),
                      isExpanded: true,
                      isDense: true,
                      underline: Container(),
                      items: snapshot.data!.map((Portfolio dropDownStringItem) {
                        return DropdownMenuItem<String>(
                            value: dropDownStringItem.id,
                            child: Text(dropDownStringItem.name));
                      }).toList(),
                      hint: Text('Select portfolio',
                          style: Theme.of(context).textTheme.bodyText1),
                      onChanged: (String? value) {
                        if (value != null) {
                          setState(() {
                            selectedPortfolio = value;
                            selectedGroupID = null;
                          });
                          bloc.setCurrentPortfolioAndGroups(value);
                        }
                      },
                      value: selectedPortfolio,
                    ),
                  ),
                ),
              ),
            );
          }
          return const Text('no data');
        });
  }
}

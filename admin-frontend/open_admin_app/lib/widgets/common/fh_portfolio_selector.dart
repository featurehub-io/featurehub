import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';

class PortfolioSelectorWidget extends StatefulWidget {
  const PortfolioSelectorWidget({Key? key}) : super(key: key);

  @override
  _PortfolioSelectorWidgetState createState() =>
      _PortfolioSelectorWidgetState();
}

class _PortfolioSelectorWidgetState extends State<PortfolioSelectorWidget> {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return StreamBuilder<List<Portfolio>>(
        stream: bloc.streamValley.portfolioListStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.data!.isEmpty) {
            return const SizedBox.shrink();
          }

          return StreamBuilder<Portfolio>(
              stream: bloc.streamValley.currentPortfolioStream,
              builder: (context, currentPortfolioSnap) {
                return Flexible(
                  fit: FlexFit.loose,
                  child: Padding(
                    padding: const EdgeInsets.only(
                        left: 16.0, right: 32.0, top: 24.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Your current portfolio',
                            style: Theme.of(context).textTheme.caption),
                        const SizedBox(height: 4.0),
                        InkWell(
                          mouseCursor: SystemMouseCursors.click,
                          child: OutlinedButton(
                            onPressed: () => {},
                            child: DropdownButtonHideUnderline(
                              child: DropdownButton(
                                icon: const Padding(
                                  padding: EdgeInsets.only(left: 8.0),
                                  child: Icon(
                                    Icons.keyboard_arrow_down,
                                    size: 18,
                                  ),
                                ),
                                style: Theme.of(context).textTheme.bodyText1,
                                isDense: true,
                                isExpanded: true,
                                items:
                                    snapshot.data!.map((Portfolio portfolio) {
                                  return DropdownMenuItem<String>(
                                      value: portfolio.id,
                                      child: Text(
                                        portfolio.name,
                                        // style: GoogleFonts.poppins(
                                        //     textStyle: Theme.of(context)
                                        //         .textTheme
                                        //         .bodyText2,
                                        //     fontWeight: FontWeight.w600),
                                        overflow: TextOverflow.ellipsis,
                                      ));
                                }).toList(),
                                hint: Text('Select portfolio',
                                    style:
                                        Theme.of(context).textTheme.bodyText2),
                                onChanged: (String? value) {
                                  setState(() {
                                    bloc.setCurrentPid(value);
                                    bloc.setCurrentAid(null);
                                  });
                                },
                                value: currentPortfolioSnap.hasData
                                    ? currentPortfolioSnap.data!.id
                                    : null,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              });
        });
  }
}

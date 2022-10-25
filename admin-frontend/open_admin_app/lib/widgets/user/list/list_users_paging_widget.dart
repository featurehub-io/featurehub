

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';

import 'list_users_bloc.dart';

class PagingUsersWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ListPersonBloc>(context);

    return StreamBuilder<SearchPersonPagination>(
        stream: bloc.searchResult,
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.hasError) {
            return SizedBox.shrink();
          }

          final buttons = <FHFlatButton>[];
          final data = snapshot.data!;
          if (data.startPos != 0) {
            buttons.add(
                FHFlatButton(onPressed: () => bloc.first(), title: "|<"));
            buttons.add(FHFlatButton(onPressed: () => bloc.prev(), title: "<"));
          }

          final pageCount = data.result.max ~/ data.pageSize;
          for(int count = 1; count <= pageCount; count ++) {
            buttons.add(FHFlatButton(onPressed: () => bloc.page(count), title: "${count}"));
          }

          final lastPos = (data.result.max - (data.result.max % data.pageSize));

          if (data.startPos < lastPos) {
            buttons.add(
                FHFlatButton(onPressed: () => bloc.next(), title: ">"));
            buttons.add(FHFlatButton(onPressed: () => bloc.last(), title: ">|"));
          }

          return Container(
              height: 50, color: Colors.red,
              child: Row(children: buttons,));
        });
  }

}

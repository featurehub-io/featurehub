import 'package:flutter/cupertino.dart';
import 'package:flutter/widgets.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/external_docs_links_widget.dart';
import 'package:open_admin_app/widgets/common/fh_error_message_details_widget.dart';
import 'package:open_admin_app/widgets/user/common/select_portfolio_group_bloc.dart';
import 'package:open_admin_app/widgets/user/edit/edit_user_bloc.dart';
import 'package:open_admin_app/widgets/user/signin/signin_widget.dart';
import 'package:open_admin_app/widgets/version-check/version-check-widget.dart';

import 'api/client_api.dart';

class WidgetCreator {
  bool canSeeOrganisationMenuDrawer(ManagementRepositoryClientBloc client) =>
      client.userIsSuperAdmin;
  Widget createSigninWidget(ManagementRepositoryClientBloc client) =>
      SigninWidget(client);
  Widget orgNameContainer(ManagementRepositoryClientBloc client) =>
      const SizedBox.shrink();
  Widget setBillingAdminCheckbox() => const SizedBox.shrink();
  Widget externalDocsLinksWidget() => const ExternalDocsLinksWidget();
  List<Widget> extraApplicationMenuItems(
          ManagementRepositoryClientBloc client) =>
      [];
  List<Widget> extraPortfolioMenuItems(ManagementRepositoryClientBloc client) =>
      [];
  List<Widget> extraGlobalMenuItems(ManagementRepositoryClientBloc client) =>
      [];

  String externalOrganisationUrl(String urlPrefix, String urlPartial, ManagementRepositoryClientBloc client) => "";

  Widget errorMessageDetailsWidget({required FHError fhError}) {
    return FHErrorMessageDetailsWidget(fhError: fhError);
  }

  Widget edgeUrlCopyWidget(ManagementRepositoryClientBloc mrClient) =>
      const SizedBox.shrink();

  EditUserBloc createEditUserBloc(mrBloc, String? personId,
      {required SelectPortfolioGroupBloc selectGroupBloc}) {
    return EditUserBloc(mrBloc, personId, selectGroupBloc: selectGroupBloc);
  }

  adminSdkBaseUrlWidget(ManagementRepositoryClientBloc mrClient) {
    return const SizedBox.shrink();
  }

  Widget newVersionCheckWidget() => VersionCheckWidget();
}

WidgetCreator widgetCreator = WidgetCreator();

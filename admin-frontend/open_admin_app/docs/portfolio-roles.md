# portfolio roles

## context

This requirement is to add portfolio roles into the Flutter GroupUpdateDialogWidget

# OpenAPI definitions and source code

- OpenAPI partial referring to this functionallity is here ../../backend/mr-api/groups.yaml - PortfolioGroupRoleType, and the roles are mutually exclusive. 
- full OpenAPI is assembled here (including partial) ../app_mr_layer/final.yaml

From the root of this project, the OpenAPI partial document that refers to this functionality is here ../../backend/mr-api/feature-filters.yaml and the fully formed document is here

# requirements

- we need to update the bloc code which updates and creates groups in `admin-frontend/open_admin_app/lib/widgets/group/group_bloc.dart` to cater for portfolioRoles
- we need to update the UI in `admin-frontend/open_admin_app/lib/widgets/group/group_update_widget.dart` to cater for mutually exclusive portfolio roles (drop down box)
- all text needs to be internationalized using the existing mechanisms in both english and chinese

# acceptance criteria
- all update calls must be done in the `GroupBloc`
- all UI elements in `GroupUpdateDialogWidget`
- it must use the existing UI components and patterns
- the UI must work in both light and dark modes

# what you do not need to ask for permission for
- you do not need to ask for permission to read any of the files in this folder
- you do not need to ask for permission to read any of the files mention in the OpenAPI definitions and source code section above


# what is not required
- it does not require any tests
- it does not require any documentation

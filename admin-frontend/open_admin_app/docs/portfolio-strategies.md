# Portfolio Strategies

## context

We are trying to add a UI for Portfolio Strategies that match the similar functionality that exists for Application Strategies in the UI.


- We are not interested in backend support, only the Flutter/Dart front end
- The pattern used for Application Strategies must be followed, and this is stored in the folder `admin-frontend/open_admin_app/lib/widgets/application-strategies` and in the files `admin-frontend/open_admin_app/lib/routes/create_application_strategy_route.dart` and `admin-frontend/open_admin_app/lib/routes/edit_application_strategy_route.dart`
- existing routing is in this folder `admin-frontend/open_admin_app/lib/config`, the application strategies pattern is there and can be followed for portfolio strategies
- internationalisation must be followed and english and chinese translations must be provided.
- The OpenAPI specifically for this code is in `backend/mr-api/portfolio-strategies.yaml`
- The full OpenAPI specification is in `admin-frontend/app_mr_layer/final.yaml`
- The Dart class for the API calls is `admin-frontend/app_mr_layer/lib/api/portfolio_rollout_strategy_service_api.dart`

# what you do not need to ask for permission for
- you do not need to ask for permission to read any of the files in this project
- you do not need to ask for permission to read any of the files mention in the OpenAPI definitions and source code section above
- the git history for this branch

# acceptance criteria
- it must use the existing BlocProvider pattern, keeping the API calls out of the UI
- it must use the existing UI components and patterns
- the UI must work in both light and dark modes

# flutter considerations
- when using addOverlay and adding a widget tree that needs a bloc in the context before the addOverlay gets called, you must
  explicitly create a new bloc (using BlocProvider.builder) and then wrap the widget created in the `addOverlay` as they do not share the same tree. Dialogs are
  a prime example of this in that they do not share the same context tree as their calling function. An example of this is in `feature_cell_holder.dart` when the `CreateFeatureDialogWidget` is created in the `addOverlay` function. You cannot use the same bloc as when the overlay closes it will close the bloc.

# what is not required
- it does not require any tests
- it does not require any documentation
 

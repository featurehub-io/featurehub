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
-  

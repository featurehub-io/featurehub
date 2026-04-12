# Feature Filters

# context
We are planning on implementing the functionality added by the FilterFeatureFilterServiceApi. The API is a CRUD API that is used at the Portfolio level
by Superusers, Portfolio Admins or people with Feature Create/Edit on any applications in the portfolio. All other users can see the available filters
as long as they have access to any application in the portfolio, but they cannot edit them.

A feature filter is created with a name and description, and these can be edited as their ID is immutable once created. 

# OpenAPI definitions and source code

- OpenAPI partial referring to this functionallity is here ../../backend/mr-api/feature-filters.yaml
- full OpenAPI is assembled here (including partial) ../app_mr_layer/final.yaml
- API for Feature Filters (this functionality) has tags `FeatureFilterService`
- full dart source for entire API is here ../app_mr_layer/lib. OpenAPI paths are split by `tags` into files matching those tag names in the `api` directory. OpenAPI component schema in the `model` directory.

From the root of this project, the OpenAPI partial document that refers to this functionality is here ../../backend/mr-api/feature-filters.yaml and the fully formed document is here

# requirements

- we need a new UI screen which is in the Portfolio section which allows users with the right permission to edit, update and delete filters. It should show which service accounts and features are using the filter. It should be paginated and allow for filtering and sorting. 
- features should have their edit dialog altered so that the user can select multiple feature filters to apply to the feature. If the user selects one or more filters, the dialog should call to the server using the `getMatchingFilters` endpoint (with `matchType` = `serviceaccount`) to get the list of service accounts that match the filters and display the names to the user so they know which service accounts will be allowed to see the feature.
- service accounts should have their edit dialog altered so that the user can select multiple feature filters to apply to the feature. If the user selects one or more filters, the dialog should call to the server using the `getMatchingFilters` endpoint (with `matchType` = `feature`) to get the list of features that match the filters and display the names to the user so they know which features will be allowed to be seen by the service account.
- the Feature dashboard should also allow people to filter the results by filters (along with the other critera)

# acceptance criteria
- it must used the existing BlocProvider pattern, keeping the API calls out of the UI
- it must use the existing UI components and patterns

# what you do not need to ask for permission for
- you do not need to ask for permission to read any of the files in this project
- you do not need to ask for permission to read any of the files mention in the OpenAPI definitions and source code section above

# flutter considerations
- when using addOverlay and adding a widget tree that needs a bloc in the context before the addOverlay gets called, you must
explicitly create a new bloc (using BlocProvider.builder) and then wrap the widget created in the `addOverlay` as they do not share the same tree. Dialogs are
a prime example of this in that they do not share the same context tree as their calling function. An example of this is in `feature_cell_holder.dart` when the `CreateFeatureDialogWidget` is created in the `addOverlay` function. You cannot use the same bloc as when the overlay closes it will close the bloc. 
- 

# what is not required
- it does not require any tests
- it does not require any documentation

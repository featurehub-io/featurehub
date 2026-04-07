Create a plan for the FeatureFilter API which has been added to mr-api.yaml and dacha-component.yaml.

The purpose of this change is to allow features (not feature values) which are created in applications to have associated filters (e.g. client,mobile) and have them match with filters in service accounts. This allows features making their way to clients via API keys to client SDKs have a limited subset but all be managed in a single application. Declaring them at the Portfolio level allows them to be standardised across all applications in a Portfolio and managed by Portfolio managers and Feature Managers.

It should include a REST Resource following the existing patterns that implements all
of the API endpoints in mr-api.yaml. It should particularly pay attention to the `includeDetails` of the `findFeatureFilters` to ensure that only the id and name when includeDetail is false, passing those details down to the database layer to ensure that only the minimum data is retrieved.

REST endpoints should be secured by ensuring that users who can change filters have portfolio or feature creation or editing permission in at least one application in the portfolio. Try and reuse existing security code for this, adding it in a reusable fashion if it doesn't exist. User's who can get the filters should have at least read access for any application in the portfolio. Modifying the filters on features follow the existing permission rules. Modifying the filters on service accounts follow the existing permission rules.  

The actual database work should be added to `mr-db-sql` as per the standard technique, and additional database models should be added to `mr-db-models` if that is appropriate. Standard feature change auditing and needs to make sure it is kept up to date and any messages that detect these changes and are sent will need to have their models updated with the minimum machine and human readable information about filtering (suggested id, name, who performed action).

It is expected the output will include recommended new and updated REST resources, Database models, Database endpoints, testing plans, and further api definition updates.  
 
If you encounter bugs along the way, if you can work without fixing them, add them to the bottom of this file. If you cannot, stop and ask for directions.

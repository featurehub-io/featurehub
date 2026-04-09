Create a plan for the FeatureFilter API which has been added to mr-api.yaml and dacha-component.yaml.

The purpose of this change is to allow customers who want to put all their features related to a single "application"
in one application, but where the features are targeted at specific client types (e.g. browser, mobiler, server),
only those clients actually get those features. Feature clients can end up with a lot of unnecessary state leaked into
them, particularly browsers having backend only features, which is time consuming and expensive on bandwith and
processing, as well as potentially leaking sensitive information simply by named features. 

This change is effected by allowing feature editors, portfolio admins or superusers be able to create named "feature filters" at the portfolio level. A feature (not a value) is able to be tagged with zero or more of these filters. A service
account is able to be tagged with zero or more of these filters.

If a service account requests features and has no filters, it will get all features. If it has filters it will get 
all features with matching filters ONLY. 

The UI needs to be modified so that it allows people to create filters, attach them to features and service accounts, and 
filter features in the dashboard by feature filters (e.g. show me only the mobile features).

The purpose of this change is to allow features (not feature values) which are created in applications to have associated filters (e.g. client,mobile) and have them match with filters in service accounts. This allows features making their way to clients via API keys to client SDKs have a limited subset but all be managed in a single application. Declaring them at the Portfolio level allows them to be standardised across all applications in a Portfolio and managed by Portfolio managers and Feature Managers.

It should include a REST Resource following the existing patterns that implements all
of the API endpoints in mr-api.yaml. It should particularly pay attention to the `includeDetails` of the `findFeatureFilters` to ensure that only the id and name when includeDetail is false, passing those details down to the database layer to ensure that only the minimum data is retrieved.

REST endpoints should be secured by ensuring that users who can change filters have portfolio or feature creation or editing permission in at least one application in the portfolio. Try and reuse existing security code for this, adding it in a reusable fashion if it doesn't exist. User's who can get the filters should have at least read access for any application in the portfolio. Modifying the filters on features follow the existing permission rules. Modifying the filters on service accounts follow the existing permission rules.  

The actual database work should be added to `mr-db-sql` as per the standard technique, and additional database models should be added to `mr-db-models` if that is appropriate. Standard feature change auditing and needs to make sure it is kept up to date and any messages that detect these changes and are sent will need to have their models updated with the minimum machine and human readable information about filtering (suggested id, name, who performed action).

It is expected the output will include recommended new and updated REST resources, Database models, Database endpoints, testing plans, and further api definition updates.  
 
If you encounter bugs along the way, if you can work without fixing them, add them to the bottom of this file. If you cannot, stop and ask for directions.

---

# Implementation Plan: FeatureFilter API

## Overview

The FeatureFilter system adds portfolio-level named filter tags (e.g. "mobile", "client") that can be:
- Assigned to **features** (in any application in the portfolio) to mark which filter categories they belong to
- Assigned to **service accounts** to declare which filter categories of features they want to receive

The Dacha cache model already has `filters` as a list of UUIDs on both `CacheFeature` and `CacheServiceAccount`. The MR API schema has the full CRUD REST surface for filters and the association fields on `Feature`, `CreateFeature`, and `ServiceAccount`.

---

## 1. Database Model — `mr-db-models`

### 1.1 New: `DbFeatureFilter.java`

Create `backend/mr-db-models/src/main/java/io/featurehub/db/model/DbFeatureFilter.java`.

- Extends `DbVersionedBase` (inherits `id`, `version`, `whenCreated`, `whenUpdated`)
- Table: `fh_feature_filter`
- Unique index on `(fk_portfolio_id, name)` — name is unique within a portfolio
- Fields:
  - `@ManyToOne @JoinColumn(name="fk_portfolio_id") DbPortfolio portfolio` — NOT NULL
  - `@ManyToOne @JoinColumn(name="fk_person_who_created") DbPerson whoCreated` — nullable
  - `@Column(length=60) String name` — NOT NULL
  - `@Column(length=300) String description` — nullable
  - `@ManyToMany(mappedBy="featureFilters") List<DbServiceAccount> serviceAccounts` — inverse side of the service account join, needed so `QDbFeatureFilter.serviceAccounts.id` is available for efficient ID-only queries in cache publishing
  - `@ManyToMany(mappedBy="filters") List<DbApplicationFeature> applicationFeatures` — inverse side of the feature join, needed so `QDbFeatureFilter.applicationFeatures.id` is available for efficient ID-only queries in cache publishing
- Include a Builder following the same pattern as `DbPortfolio.Builder`

### 1.2 Updated: `DbApplicationFeature.java`

Add a `@ManyToMany` relationship to `DbFeatureFilter`:

```java
@ManyToMany
@JoinTable(
  name = "fh_app_feature_filter",
  joinColumns = @JoinColumn(name = "fk_feature_id"),
  inverseJoinColumns = @JoinColumn(name = "fk_filter_id")
)
private List<DbFeatureFilter> filters = new ArrayList<>();
```

### 1.3 Updated: `DbServiceAccount.java`

Add a `@ManyToMany` relationship to `DbFeatureFilter`:

```java
@ManyToMany
@JoinTable(
  name = "fh_service_account_filter",
  joinColumns = @JoinColumn(name = "fk_service_account_id"),
  inverseJoinColumns = @JoinColumn(name = "fk_filter_id")
)
private List<DbFeatureFilter> featureFilters = new ArrayList<>();
```

### 1.4 Updated: `DbPortfolio.java`

Add the back-reference `@OneToMany` for the portfolio's filters (cascade all, orphan removal):

```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
@JoinColumn(name = "fk_portfolio_id")
private List<DbFeatureFilter> featureFilters;
```

---

## 2. Database API Interface — `mr-db-api`

Create `backend/mr-db-api/src/main/kotlin/io/featurehub/db/api/FeatureFilterApi.kt`:

```kotlin
interface FeatureFilterApi {
  class DuplicateNameException : Exception()
  class OptimisticLockingException : Exception()
  class FilterNotFoundException : Exception()

  @Throws(DuplicateNameException::class)
  fun create(portfolioId: UUID, creator: Person, filter: CreateFeatureFilter): FeatureFilter

  @Throws(OptimisticLockingException::class, FilterNotFoundException::class)
  fun update(portfolioId: UUID, updater: Person, filter: FeatureFilter): FeatureFilter

  @Throws(OptimisticLockingException::class, FilterNotFoundException::class)
  fun delete(portfolioId: UUID, deleter: Person, filter: FeatureFilter): FeatureFilter

  /**
   * @param includeDetails when false, returns only id and name (for dropdowns).
   *   When true, returns full FeatureFilter data including which features use each filter.
   */
  fun find(
    portfolioId: UUID,
    filter: String?,
    max: Int?,
    page: Int?,
    sortOrder: SortOrder?,
    includeDetails: Boolean
  ): SearchFeatureFilterResult
}
```

---

## 4. Database SQL Implementation — `mr-db-sql`

Create `backend/mr-db-sql/src/main/kotlin/io/featurehub/db/services/FeatureFilterSqlApi.kt` annotated `@Singleton`.

### `create`
- Look up `QDbPortfolio().id.eq(portfolioId).findOne()` — throw `NotFoundException` if missing
- Resolve `whoCreated` via `convertUtils.byPerson(creator.id!!.id)`
- Build `DbFeatureFilter` via builder, call `save()`
- Catch `DuplicateKeyException` → throw `DuplicateNameException`
- Return `toFeatureFilter(saved)`

### `update`
- Look up `QDbFeatureFilter().id.eq(filter.id).portfolio.id.eq(portfolioId).findOne()` — throw `FilterNotFoundException` if null
- Check version matches — throw `OptimisticLockingException` if not
- Update `name`, `description`, call `save()`
- Return `toFeatureFilter(updated)`

### `delete`
- Look up filter, check version
- Validate filter is not used by any feature or service account — if it is, either cascade-clear the associations first, or reject with a meaningful error (see Bugs section)
- Call `delete()`
- Return the last known state as `FeatureFilter`

### `find`

Two code paths based on `includeDetails`:

**`includeDetails == false`** (lightweight — for dropdowns):
```
QDbFeatureFilter()
  .select(QDbFeatureFilter.Alias.id, QDbFeatureFilter.Alias.name)
  .portfolio.id.eq(portfolioId)
  [.name.ilike("%$filter%") if filter non-null]
  [.name.asc() or .name.desc() based on sortOrder]
  .setMaxRows(max ?: 100).setFirstRow((page ?: 0) * (max ?: 100))
  .findList()
```
Map results to `SearchFeatureFilterItem` with only `id` and `name` populated (no `features` list, no `whoCreated`).

**`includeDetails == true`** (full detail — for admin pages):
Fetch all `DbFeatureFilter` matching the criteria with `fetch()` on:
- `whoCreated` (for `OptionalAnemicPerson`)
- `environmentFeatures.parentApplication` via join through `fh_app_feature_filter`

For the `features` list on each `SearchFeatureFilterItem`, execute a secondary query:
```
QDbApplicationFeature()
  .select(QDbApplicationFeature.Alias.id, QDbApplicationFeature.Alias.key)
  .parentApplication.select(QDbApplication.Alias.id, QDbApplication.Alias.name)
  .filters.id.in(filterIds)
  .findList()
```
Group results by `filter.id` and assemble `SearchFeatureFilterDetail` objects.

Return `SearchFeatureFilterResult(max = totalCount, filters = items)`.

### Helper: `toFeatureFilter(db: DbFeatureFilter): FeatureFilter`
Map `id`, `name`, `description`, `version`, `whoCreated → OptionalAnemicPerson`.

### Helper: `toFeatureFilter(db: DbFeatureFilter, includeDetails: Boolean): SearchFeatureFilterItem`
Include `features` only when `includeDetails == true`.

---

## 5. REST Resource — `mr`

### 5.1 New: `FeatureFilterResource.kt`

Create `backend/mr/src/main/kotlin/io/featurehub/mr/resources/FeatureFilterResource.kt`.

Implement `FeatureFilterServiceDelegate` (generated from the `FeatureFilterService` tag in mr-api.yaml).

```kotlin
@Singleton
class FeatureFilterResource @Inject constructor(
  private val authManager: AuthManagerService,
  private val featureFilterApi: FeatureFilterApi,
  private val portfolioUtils: PortfolioUtils   // for portfolio existence check
) : FeatureFilterServiceDelegate {
```

#### Security model

The plan specifies:
- **Write** (create/update/delete filters): user must be portfolio admin OR org admin OR have feature-creator/editor permission in at least one application in the portfolio.
- **Read** (find/list): user must have at least read access to any application in the portfolio, OR be portfolio/org admin.

Introduce a **`PortfolioFeaturePermissionUtils`** helper (following the `ApplicationUtils` pattern) in `backend/mr/src/main/kotlin/io/featurehub/mr/utils/PortfolioFeaturePermissionUtils.kt`:

```kotlin
@Singleton
class PortfolioFeaturePermissionUtils @Inject constructor(
  private val authManager: AuthManagerService,
  private val applicationApi: ApplicationApi
) {
  /** Throws ForbiddenException unless user is portfolio/org admin or feature creator/editor in any app. */
  fun requireFeatureWriteAccessInPortfolio(portfolioId: UUID, current: Person) {
    if (authManager.isOrgAdmin(current) || authManager.isPortfolioAdmin(portfolioId, current)) return
    val personId = current.id!!.id
    // check if person has feature-creator or feature-editor role in at least one app in the portfolio
    val apps = applicationApi.findApplications(portfolioId, null, null, Opts.empty(), current, false)
    if (apps.any { app ->
        applicationApi.personIsFeatureCreator(app.id!!, personId) ||
        applicationApi.personIsFeatureEditor(app.id!!, personId)
      }) return
    throw ForbiddenException()
  }

  /** Throws ForbiddenException unless user has at least read access to any app in portfolio. */
  fun requireFeatureReadAccessInPortfolio(portfolioId: UUID, current: Person) {
    if (authManager.isOrgAdmin(current) || authManager.isPortfolioAdmin(portfolioId, current)) return
    val personId = current.id!!.id
    val apps = applicationApi.findApplications(portfolioId, null, null, Opts.empty(), current, false)
    if (apps.any { app -> applicationApi.personIsFeatureReader(app.id!!, personId) }) return
    throw ForbiddenException()
  }
}
```

#### `createFeatureFilter(id, createFeatureFilter, securityContext)`
1. Resolve `person` via `authManager.from(securityContext)`
2. Call `portfolioUtils.requireFeatureWriteAccessInPortfolio(id, person)` (see above)
3. Call `featureFilterApi.create(id, person, createFeatureFilter)` — catch `DuplicateNameException` → 409
4. Return 201 with `FeatureFilter`

#### `updateFeatureFilter(id, featureFilter, securityContext)`
1. Auth check (write)
2. Call `featureFilterApi.update(id, person, featureFilter)` — catch `OptimisticLockingException` → 409, `FilterNotFoundException` → 404
3. Return 201 with updated `FeatureFilter`

#### `deleteFeatureFilter(id, featureFilter, securityContext)`
1. Auth check (write)
2. Call `featureFilterApi.delete(id, person, featureFilter)` — catch `OptimisticLockingException` → 409, `FilterNotFoundException` → 404
3. Return 201 with last known `FeatureFilter`

#### `findFeatureFilters(id, filter, max, page, sortOrder, includeDetails, securityContext)`
1. Resolve person
2. Call `portfolioUtils.requireFeatureReadAccessInPortfolio(id, person)`
3. Call `featureFilterApi.find(id, filter, max, page, sortOrder, includeDetails ?: false)`
4. Return 200 with `SearchFeatureFilterResult`

### 5.2 Wire up in `ManagementRepositoryFeature.kt`

Add to the `listOf(...)`:
```kotlin
FeatureFilterServiceDelegator::class.java,
```

Add to the `AbstractBinder.configure()`:
```kotlin
bind(FeatureFilterResource::class.java).to(FeatureFilterServiceDelegate::class.java).`in`(Singleton::class.java)
bind(PortfolioFeaturePermissionUtils::class.java).to(PortfolioFeaturePermissionUtils::class.java).`in`(Singleton::class.java)
```

### 5.3 Wire up in `ApiToSqlApiBinder.kt`

Add:
```kotlin
import io.featurehub.db.api.FeatureFilterApi
import io.featurehub.db.services.FeatureFilterSqlApi
...
bind(FeatureFilterSqlApi::class.java).to(FeatureFilterApi::class.java).`in`(Singleton::class.java)
```

---

## 6. Feature and Service Account Filter Associations

### 6.1 Updating Feature filters (on `ApplicationSqlApi`)

The `filter` field on `CreateFeature` and `Feature` (list of UUIDs) must be persisted as a `ManyToMany` join.

In `ApplicationSqlApi.createApplicationFeature`:
- After saving `DbApplicationFeature`, if `createFeature.filter` is non-null and non-empty, resolve each UUID to a `DbFeatureFilter` (scoped to the portfolio via `parentApplication.portfolio.id`) and set `dbFeature.filters = resolvedSet`, then save.

In `ApplicationSqlApi.updateApplicationFeature`:
- When updating an existing feature, replace `dbFeature.filters` with the newly resolved set from `feature.filter`.
- Call `cacheSource.publishFeatureChange(dbFeature, PublishAction.UPDATE)` (already done in the existing update path; the cache publishing will now include filters).

### 6.2 Updating Service Account filters (on `ServiceAccountSqlApi`)

The `filter` field on `ServiceAccount` (list of filter UUIDs) and `featureFilters` (list of full `FeatureFilter` objects, read-only) must both be handled.

- The `filter` field is the writable input — resolve UUIDs to `DbFeatureFilter` records and set `sa.featureFilters = resolvedSet` before saving.
- The `featureFilters` field returned from the API should be populated by `Conversions.toServiceAccount()` by reading `sa.featureFilters` and mapping each to a `FeatureFilter` model object.
- When an existing service account update is saved, call `cacheSource.updateServiceAccount(sa, PublishAction.UPDATE)`.

---

## 7. Dacha Cache Publishing Updates

### 7.1 `DbCacheSource.fillServiceAccount` (in `mr-eventing`)

After existing permissions mapping, add a targeted query that selects only the filter IDs from the join table — do not traverse `sa.featureFilters` as that would load full `DbFeatureFilter` entities:

```kotlin
.filters(
  QDbFeatureFilter()
    .select(QDbFeatureFilter.Alias.id)
    .serviceAccounts.id.eq(sa.id)
    .findStream().map { it.id }.collect(Collectors.toList())
)
```

This populates the `filters: List<UUID>` field on `CacheServiceAccount` (already in dacha-component.yaml) with a single efficient query that retrieves only IDs via the join table.

### 7.2 Feature publishing

The `CacheFeature` in `dacha-component.yaml` has `filters: nullable array of uuid`. This is populated in `DbCacheSource` wherever `CacheFeature` is constructed from `DbApplicationFeature`.

Locate the construction of `CacheFeature` in `DbCacheSource` (typically in `fillEnvironmentCacheItem` or its helpers) and add a targeted ID-only query — do not traverse `dbFeature.filters` as that loads full `DbFeatureFilter` entities:

```kotlin
.filters(
  QDbFeatureFilter()
    .select(QDbFeatureFilter.Alias.id)
    .applicationFeatures.id.eq(dbFeature.id)
    .findStream().map { it.id }.collect(Collectors.toList())
    .ifEmpty { null }
)
```

This requires `DbFeatureFilter` to also carry the inverse `@ManyToMany(mappedBy="filters")` back to `DbApplicationFeature` (i.e. `List<DbApplicationFeature> applicationFeatures`), so the `QDbFeatureFilter.applicationFeatures` path is available.

---

## 8. Auditing / Messaging

The `@ChangeLog` annotation on `DbFeatureFilter` covers standard Ebean change logging for audit trails.

For the feature-change audit trail (tracked via `TrackingEventSqlApi` and related CloudEvents), when a feature's `filters` set changes:
- The existing `publishFeatureChange(appFeature, PublishAction.UPDATE)` path covers Dacha cache invalidation.
- For human-readable auditing, the `TrackingEventListener` / `CloudEventCacheBroadcaster` should include filter changes. The suggested approach is to include the list of filter `{id, name}` pairs as part of the feature CloudEvent metadata — this avoids creating a separate event type.

No new CloudEvent types are needed if filter changes are bundled into the existing feature-update event. The minimum readable fields per filter are: `id` and `name` (as suggested in the spec).

---

## 9. Conversions (`ConvertUtils`)

In `backend/mr-db-services/src/main/kotlin/io/featurehub/db/services/Conversions.kt` and its implementation:

### `toApplicationFeature(af: DbApplicationFeature?, opts: Opts?): Feature?`

Add population of `filter`:
```kotlin
.filter(af.filters?.map { it.id } ?: null)
```

### `toServiceAccount(sa: DbServiceAccount?, opts: Opts?): ServiceAccount?`

Add population of `featureFilters` (full objects) and `filter` (UUID list):
```kotlin
.featureFilters(sa.featureFilters?.map { toFeatureFilter(it) } ?: null)
.filter(sa.featureFilters?.map { it.id } ?: null)
```

Add new helper `toFeatureFilter(db: DbFeatureFilter): FeatureFilter`.

---

## 10. Testing Plan

### 10.1 Unit Tests (`mr-db-sql`)

Create `FeatureFilterSqlApiSpec.groovy` in `backend/mr-db-sql/src/test/groovy/.../services/`:

- `createFeatureFilter_succeeds` — creates with name+description, returns correct DTO
- `createFeatureFilter_duplicateName_throws` — same name in same portfolio is rejected
- `createFeatureFilter_differentPortfolios_allowed` — same name in different portfolios is fine
- `updateFeatureFilter_succeeds` — updates name/description, version increments
- `updateFeatureFilter_wrongVersion_throwsOptimisticLocking`
- `updateFeatureFilter_notFound_throws`
- `deleteFeatureFilter_succeeds`
- `deleteFeatureFilter_wrongVersion_throwsOptimisticLocking`
- `findFeatureFilters_withoutDetails_returnsOnlyIdAndName` — assert `whoCreated` and `features` are null
- `findFeatureFilters_withDetails_returnsFullData` — assert `features` lists correct apps/keys
- `findFeatureFilters_filterParam_filtersOnName`
- `findFeatureFilters_pagination`
- `feature_filtersAssociated_publishedToDacha` — save feature with filters, assert `CacheFeature.filters` populated
- `serviceAccount_filtersAssociated_publishedToDacha` — save SA with filters, assert `CacheServiceAccount.filters` populated

### 10.2 REST Resource Tests (`mr`)

Create `FeatureFilterResourceSpec.groovy`:

- Auth tests: verify `403` when person has no portfolio access
- Auth tests: verify feature creator can call write endpoints
- Auth tests: verify feature reader can call `findFeatureFilters` but not write endpoints
- `createFeatureFilter` happy path → 201 with body
- `createFeatureFilter` duplicate → 409
- `updateFeatureFilter` optimistic lock failure → 409
- `deleteFeatureFilter` not found → 404
- `findFeatureFilters` with `includeDetails=false` returns only id+name
- `findFeatureFilters` with `includeDetails=true` returns full detail

### 10.3 E2E Cucumber Tests (`adks/e2e-sdk`)

Add a new feature file `features/feature-filter.feature`:

- Scenario: Create portfolio-level filter, assign to feature, assign filter to service account, verify SDK receives only filtered features
- Scenario: Service account with no filters receives all features
- Scenario: Service account with filter receives only features matching that filter

---

## 11. API Definition Notes / Bugs

**Bug (non-blocking):** The `requestBody` for `createFeatureFilter` (POST), `updateFeatureFilter` (PUT), and `deleteFeatureFilter` (DELETE) in `mr-api.yaml` uses `$ref: "#/components/schemas/..."` at the top level without wrapping in `content: application/json: schema:`. This is technically invalid OpenAPI 3.0 — the correct form is:
```yaml
requestBody:
  content:
    application/json:
      schema:
        $ref: "#/components/schemas/CreateFeatureFilter"
```
The generator may handle this or may produce incorrect stubs. Should be fixed before code generation.

**Bug (non-blocking):** The `deleteFeatureFilter` response code is `201` (Created) rather than `200` (OK). Delete should return `200`. Likewise `updateFeatureFilter` returning `201` is unusual; convention is `200` for updates.

**Note:** The `mr-api.yaml` `Feature` schema has the field named `filter` (singular), while the `dacha-component.yaml` `CacheFeature` also uses `filters` (plural). The `ServiceAccount` schema uses `filter` (singular, writable) and `featureFilters` (plural, read-only full objects). Implementations should map these carefully.

**Note:** When deleting a `FeatureFilter` that is still assigned to features or service accounts, the plan is to cascade-clear associations (via the `@ManyToMany` join tables) before deleting. If this causes unexpected Dacha republishing, it may be worth adding a "filters in use" check and returning a 409 with a message listing what uses the filter.

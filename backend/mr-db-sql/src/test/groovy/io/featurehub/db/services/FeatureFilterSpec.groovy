package io.featurehub.db.services

import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.FeatureFilterApi
import io.featurehub.db.api.Opts
import io.featurehub.mr.model.CreateFeature
import io.featurehub.mr.model.CreateFeatureFilter
import io.featurehub.mr.model.CreatePortfolio
import io.featurehub.mr.model.CreateServiceAccount
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureFilter
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.ServiceAccount
import io.featurehub.mr.model.SortOrder
import org.apache.commons.lang3.RandomStringUtils

class FeatureFilterSpec extends Base3Spec {
  FeatureFilterSqlApi featureFilterApi
  ServiceAccountSqlApi serviceAccountApi
  PersonSqlApi personSqlApi
  InternalApplicationApi internalApplicationApi

  def setup() {
    internalApplicationApi = new InternalApplicationSqlApi(convertUtils)
    featureFilterApi = new FeatureFilterSqlApi(convertUtils, internalApplicationApi)
    personSqlApi = new PersonSqlApi(db, convertUtils, archiveStrategy, groupSqlApi)
    serviceAccountApi = new ServiceAccountSqlApi(convertUtils, cacheSource, archiveStrategy, personSqlApi)
  }

  // -------------------------------------------------------------------------
  // helpers
  // -------------------------------------------------------------------------

  private FeatureFilter createFilter(String name, String desc = null) {
    featureFilterApi.create(portfolio.id, superPerson,
      new CreateFeatureFilter().name(name).description(desc))
  }

  private Feature createFeature(UUID appId = app1.id, List<UUID> filterIds = null) {
    def key = RandomStringUtils.randomAlphabetic(10)
    def cf = new CreateFeature().name(key).key(key).valueType(FeatureValueType.BOOLEAN)
    if (filterIds != null) cf.featureFilter(filterIds)
    applicationSqlApi.createApplicationFeature(appId, cf, superPerson, Opts.empty()).find { it.key == key }
  }

  private ServiceAccount createServiceAccount(List<UUID> filterIds = null) {
    def name = RandomStringUtils.randomAlphabetic(10)
    def csa = new CreateServiceAccount().name(name).description("desc")
    if (filterIds != null) csa.featureFilter(filterIds)
    serviceAccountApi.create(portfolio.id, superPerson, csa, Opts.empty())
  }

  // =========================================================================
  // FeatureFilterSqlApi — create
  // =========================================================================

  def "can create a feature filter with name and description"() {
    when:
      def ff = createFilter("my-filter", "some description")
    then:
      ff != null
      ff.id != null
      ff.name == "my-filter"
      ff.description == "some description"
      ff.version > 0
      ff.whoCreated != null
  }

  def "creating a filter with a duplicate name in the same portfolio throws DuplicateNameException"() {
    given:
      def name = RandomStringUtils.randomAlphabetic(10)
      createFilter(name)
    when:
      createFilter(name)
    then:
      thrown(FeatureFilterApi.DuplicateNameException)
  }

  def "the same filter name can be used in different portfolios"() {
    given:
      def name = RandomStringUtils.randomAlphabetic(10)
      def other = portfolioSqlApi.createPortfolio(
        new CreatePortfolio().name(RandomStringUtils.randomAlphabetic(10)).description("other"),
        Opts.empty(), superuser)
    when:
      def ff1 = createFilter(name)
      def ff2 = featureFilterApi.create(other.id, superPerson, new CreateFeatureFilter().name(name))
    then:
      ff1.id != ff2.id
      ff1.name == ff2.name
  }

  // =========================================================================
  // FeatureFilterSqlApi — update
  // =========================================================================

  def "can update a filter's name and description"() {
    given:
      def ff = createFilter("original-name", "original desc")
    when:
      def updated = featureFilterApi.update(portfolio.id, superPerson,
        new FeatureFilter().id(ff.id).name("new-name").description("new desc").version(ff.version))
    then:
      updated.name == "new-name"
      updated.description == "new desc"
      updated.version > ff.version
  }

  def "updating a filter with wrong version throws OptimisticLockingException"() {
    given:
      def ff = createFilter(RandomStringUtils.randomAlphabetic(10))
    when:
      featureFilterApi.update(portfolio.id, superPerson,
        new FeatureFilter().id(ff.id).name("x").version(ff.version + 99))
    then:
      thrown(FeatureFilterApi.OptimisticLockingException)
  }

  def "updating a non-existent filter throws FilterNotFoundException"() {
    when:
      featureFilterApi.update(portfolio.id, superPerson,
        new FeatureFilter().id(UUID.randomUUID()).name("x").version(1))
    then:
      thrown(FeatureFilterApi.FilterNotFoundException)
  }

  def "renaming a filter to an already-used name in the same portfolio throws DuplicateNameException"() {
    given:
      def ff1 = createFilter(RandomStringUtils.randomAlphabetic(10))
      def ff2 = createFilter(RandomStringUtils.randomAlphabetic(10))
    when:
      featureFilterApi.update(portfolio.id, superPerson,
        new FeatureFilter().id(ff2.id).name(ff1.name).version(ff2.version))
    then:
      thrown(FeatureFilterApi.DuplicateNameException)
  }

  // =========================================================================
  // FeatureFilterSqlApi — delete
  // =========================================================================

  def "can delete a filter"() {
    given:
      def ff = createFilter(RandomStringUtils.randomAlphabetic(10))
    when:
      def deleted = featureFilterApi.delete(portfolio.id, superPerson,
              ff.id, ff.version)
    then:
      deleted.id == ff.id
    and: "the filter is gone from the database"
      Finder.findFeatureFilterById(ff.id) == null
  }

  def "deleting a filter with wrong version throws OptimisticLockingException"() {
    given:
      def ff = createFilter(RandomStringUtils.randomAlphabetic(10))
    when:
      featureFilterApi.delete(portfolio.id, superPerson,
        ff.id, ff.version + 1)
    then:
      thrown(FeatureFilterApi.OptimisticLockingException)
  }

  def "deleting a non-existent filter throws FilterNotFoundException"() {
    when:
      featureFilterApi.delete(portfolio.id, superPerson,
        UUID.randomUUID(), 1)
    then:
      thrown(FeatureFilterApi.FilterNotFoundException)
  }

  // =========================================================================
  // FeatureFilterSqlApi — find (lightweight, includeDetails=false)
  // =========================================================================

  def "find without details returns id, name, and version only"() {
    given:
      def name = "find-light-" + RandomStringUtils.randomAlphabetic(6)
      createFilter(name, "some desc")
    when:
      def result = featureFilterApi.find(portfolio.id, name, null, null, null, false, superPerson.id.id)
    then:
      result.pagination.total >= 1
      result.filters.any { it.name == name && it.id != null && it.version > 0 }
  }

  def "find filters by name pattern (case-insensitive)"() {
    given:
      def prefix = "FILTERTEST-" + RandomStringUtils.randomAlphabetic(6)
      createFilter(prefix + "-aaa")
      createFilter(prefix + "-bbb")
      createFilter(prefix + "-ccc")
      createFilter("unrelated-" + RandomStringUtils.randomAlphabetic(8))
    when:
      def result = featureFilterApi.find(portfolio.id, prefix.toLowerCase(), null, null, null, false, superPerson.id.id)
    then:
      result.pagination.total == 3
      result.filters.size() == 3
      result.filters.every { it.name.toLowerCase().contains(prefix.toLowerCase()) }
  }

  def "find returns results sorted ascending by default"() {
    given:
      def prefix = "SORT-ASC-" + RandomStringUtils.randomAlphabetic(6) + "-"
      createFilter(prefix + "zzz")
      createFilter(prefix + "aaa")
      createFilter(prefix + "mmm")
    when:
      def result = featureFilterApi.find(portfolio.id, prefix, null, null, SortOrder.ASC, false, superPerson.id.id)
    then:
      result.filters.size() == 3
      result.filters[0].name.endsWith("aaa")
      result.filters[1].name.endsWith("mmm")
      result.filters[2].name.endsWith("zzz")
  }

  def "find returns results sorted descending when requested"() {
    given:
      def prefix = "SORT-DESC-" + RandomStringUtils.randomAlphabetic(6) + "-"
      createFilter(prefix + "zzz")
      createFilter(prefix + "aaa")
      createFilter(prefix + "mmm")
    when:
      def result = featureFilterApi.find(portfolio.id, prefix, null, null, SortOrder.DESC, false, superPerson.id.id)
    then:
      result.filters.size() == 3
      result.filters[0].name.endsWith("zzz")
      result.filters[1].name.endsWith("mmm")
      result.filters[2].name.endsWith("aaa")
  }

  def "find respects pagination"() {
    given:
      def prefix = "PAGE-" + RandomStringUtils.randomAlphabetic(6) + "-"
      createFilter(prefix + "aaa")
      createFilter(prefix + "bbb")
      createFilter(prefix + "ccc")
    when: "page 0 with page size 2"
      def page0 = featureFilterApi.find(portfolio.id, prefix, 2, 0, SortOrder.ASC, false, superPerson.id.id)
    and: "page 1 with page size 2"
      def page1 = featureFilterApi.find(portfolio.id, prefix, 2, 1, SortOrder.ASC, false, superPerson.id.id)
    then:
      page0.pagination.total == 3
      page0.filters.size() == 2
      page0.filters[0].name.endsWith("aaa")
      page0.filters[1].name.endsWith("bbb")
    and:
      page1.pagination.total == 3
      page1.filters.size() == 1
      page1.filters[0].name.endsWith("ccc")
  }

  // =========================================================================
  // FeatureFilterSqlApi — find with full details (includeDetails=true)
  // =========================================================================

  def "find with details includes description and whoCreated"() {
    given:
      def name = "DETAIL-" + RandomStringUtils.randomAlphabetic(8)
      createFilter(name, "detailed desc")
    when:
      def result = featureFilterApi.find(portfolio.id, name, null, null, null, true, superPerson.id.id)
    then:
      result.filters.size() == 1
      def item = result.filters[0]
      item.name == name
      item.description == "detailed desc"
      item.whoCreated != null
  }

  def "find with details shows which features use the filter"() {
    given: "a filter exists"
      def ff = createFilter("FEAT-ASSOC-" + RandomStringUtils.randomAlphabetic(6))
    and: "a feature is associated with it"
      createFeature(app1.id, [ff.id])
    when:
      def result = featureFilterApi.find(portfolio.id, ff.name, null, null, null, true, superPerson.id.id)
    then:
      result.filters.size() == 1
      def item = result.filters[0]
      item.applications != null
      item.applications.size() == 1
      item.applications[0].name == app1.name
  }

  def "find with details shows which service accounts use the filter"() {
    given: "a filter exists"
      def ff = createFilter("SA-ASSOC-" + RandomStringUtils.randomAlphabetic(6))
    and: "a service account is associated with it"
      def sa = createServiceAccount([ff.id])
    when:
      def result = featureFilterApi.find(portfolio.id, ff.name, null, null, null, true, superPerson.id.id)
    then:
      result.filters.size() == 1
      def item = result.filters[0]
      item.serviceAccounts != null
      item.serviceAccounts.size() == 1
      item.serviceAccounts[0].id == sa.id
      item.serviceAccounts[0].name == sa.name
  }

  def "find with details returns empty feature and service account lists when none are associated"() {
    given:
      def ff = createFilter("LONELY-" + RandomStringUtils.randomAlphabetic(8))
    when:
      def result = featureFilterApi.find(portfolio.id, ff.name, null, null, null, true, superPerson.id.id)
    then:
      result.filters.size() == 1
      def item = result.filters[0]
      (item.applications == null || item.applications.isEmpty())
      (item.serviceAccounts == null || item.serviceAccounts.isEmpty())
  }

  // =========================================================================
  // ApplicationSqlApi — feature filter associations on create
  // =========================================================================

  def "creating a feature with filter IDs assigns the filters"() {
    given: "a filter exists in the same portfolio"
      def ff = createFilter("APP-FEAT-" + RandomStringUtils.randomAlphabetic(6))
    when: "a feature is created referencing that filter"
      def feature = createFeature(app1.id, [ff.id])
    then:
      feature != null
    and: "the DB record has the filter attached"
      def dbFeature = Finder.findApplicationFeatureWithFilters(feature.id)
      dbFeature.filters.size() == 1
      dbFeature.filters[0].id == ff.id
  }

  def "creating a feature with filter IDs from a different portfolio does not assign them"() {
    given: "a filter in a different portfolio"
      def otherPortfolio = portfolioSqlApi.createPortfolio(
        new CreatePortfolio().name(RandomStringUtils.randomAlphabetic(10)).description("other"),
        Opts.empty(), superuser)
      def ff = featureFilterApi.create(otherPortfolio.id, superPerson,
        new CreateFeatureFilter().name("OTHER-" + RandomStringUtils.randomAlphabetic(6)))
    when: "feature created in app1 referencing the other portfolio's filter"
      def feature = createFeature(app1.id, [ff.id])
    then:
      feature != null
    and: "no filters should be attached"
      def dbFeature = Finder.findApplicationFeatureWithFilters(feature.id)
      dbFeature.filters.isEmpty()
  }

  // =========================================================================
  // ApplicationSqlApi — feature filter associations on update
  // =========================================================================

  def "updating a feature can add filters"() {
    given: "a feature with no filters"
      def feature = createFeature()
    and: "a filter exists"
      def ff = createFilter("UPD-ADD-" + RandomStringUtils.randomAlphabetic(6))
    when: "the feature is updated to include that filter"
      applicationSqlApi.updateApplicationFeature(app1.id, feature.key,
        new Feature().key(feature.key).id(feature.id).name(feature.name)
          .valueType(feature.valueType).version(feature.version)
          .featureFilter([ff.id]),
        Opts.empty())
    then: "the filter is now attached"
      def dbFeature = Finder.findApplicationFeatureWithFilters(feature.id)
      dbFeature.filters.size() == 1
      dbFeature.filters[0].id == ff.id
  }

  def "updating a feature replaces existing filters with the new list"() {
    given: "two filters exist"
      def ff1 = createFilter("REPLACE-1-" + RandomStringUtils.randomAlphabetic(6))
      def ff2 = createFilter("REPLACE-2-" + RandomStringUtils.randomAlphabetic(6))
    and: "a feature starts with ff1"
      def feature = createFeature(app1.id, [ff1.id])
    when: "we update the feature to use ff2 instead"
      def current = applicationSqlApi.getApplicationFeatureByKey(app1.id, feature.key, Opts.empty())
      applicationSqlApi.updateApplicationFeature(app1.id, feature.key,
        new Feature().key(current.key).id(current.id).name(current.name)
          .valueType(current.valueType).version(current.version)
          .featureFilter([ff2.id]),
        Opts.empty())
    then:
      def dbFeature = Finder.findApplicationFeatureWithFilters(feature.id)
      dbFeature.filters.size() == 1
      dbFeature.filters[0].id == ff2.id
  }

  def "updating a feature with an empty filter list removes all filters"() {
    given: "a feature with a filter"
      def ff = createFilter("CLEAR-" + RandomStringUtils.randomAlphabetic(6))
      def feature = createFeature(app1.id, [ff.id])
    when: "the feature is updated with an empty filter list"
      def current = applicationSqlApi.getApplicationFeatureByKey(app1.id, feature.key, Opts.empty())
      applicationSqlApi.updateApplicationFeature(app1.id, feature.key,
        new Feature().key(current.key).id(current.id).name(current.name)
          .valueType(current.valueType).version(current.version)
          .featureFilter([]),
        Opts.empty())
    then:
      def dbFeature = Finder.findApplicationFeatureWithFilters(feature.id)
      dbFeature.filters.isEmpty()
  }

  def "getApplicationFeatureByKey with ServiceAccountFilters opt populates featureFilter ids"() {
    given: "a filter and a feature associated with it"
      def ff = createFilter("FETCH-OPTS-" + RandomStringUtils.randomAlphabetic(6))
      def feature = createFeature(app1.id, [ff.id])
    when: "fetching by key with ServiceAccountFilters opt"
      def result = applicationSqlApi.getApplicationFeatureByKey(app1.id, feature.key,
        Opts.opts(FillOpts.ServiceAccountFilters))
    then:
      result != null
      result.featureFilter != null
      result.featureFilter.contains(ff.id)
  }


  // =========================================================================
  // ServiceAccountSqlApi — filter associations on create and update
  // =========================================================================

  def "creating a service account with filter IDs assigns the filters"() {
    given: "a filter exists"
      def ff = createFilter("SA-CREATE-" + RandomStringUtils.randomAlphabetic(6))
    when: "a service account is created referencing the filter"
      def sa = createServiceAccount([ff.id])
    then:
      sa != null
    and: "the DB record has the filter attached"
      def dbSa = Finder.findServiceAccountWithFilters(sa.id)
      dbSa.featureFilters.size() == 1
      dbSa.featureFilters[0].id == ff.id
  }

  def "creating a service account with a filter from a different portfolio does not assign it"() {
    given: "a filter in a different portfolio"
      def otherPortfolio = portfolioSqlApi.createPortfolio(
        new CreatePortfolio().name(RandomStringUtils.randomAlphabetic(10)).description("other"),
        Opts.empty(), superuser)
      def ff = featureFilterApi.create(otherPortfolio.id, superPerson,
        new CreateFeatureFilter().name("XPORT-" + RandomStringUtils.randomAlphabetic(6)))
    when: "service account is created with the other portfolio's filter id"
      def sa = createServiceAccount([ff.id])
    then:
      sa != null
    and: "no filters attached"
      def dbSa = Finder.findServiceAccountWithFilters(sa.id)
      dbSa.featureFilters.isEmpty()
  }

  def "updating a service account replaces its filter associations"() {
    given: "two filters"
      def ff1 = createFilter("SA-UPD-1-" + RandomStringUtils.randomAlphabetic(6))
      def ff2 = createFilter("SA-UPD-2-" + RandomStringUtils.randomAlphabetic(6))
    and: "a service account starts with ff1"
      def sa = createServiceAccount([ff1.id])
    when: "the service account is updated with ff2"
      serviceAccountApi.update(portfolio.id, superuser,
        new ServiceAccount().id(sa.id).name(sa.name).version(sa.version)
          .featureFilters([ff2.id]),
        null, Opts.empty())
    then:
      def dbSa = Finder.findServiceAccountWithFilters(sa.id)
      dbSa.featureFilters.size() == 1
      dbSa.featureFilters[0].id == ff2.id
  }

  def "updating a service account with an empty filter list removes all filters"() {
    given: "a service account with a filter"
      def ff = createFilter("SA-CLEAR-" + RandomStringUtils.randomAlphabetic(6))
      def sa = createServiceAccount([ff.id])
    when: "updated with an empty list"
      serviceAccountApi.update(portfolio.id, superuser,
        new ServiceAccount().id(sa.id).name(sa.name).version(sa.version)
          .featureFilters([]),
        null, Opts.empty())
    then:
      def dbSa = Finder.findServiceAccountWithFilters(sa.id)
      dbSa.featureFilters.isEmpty()
  }

  def "updating a service account with null featureFilters does not change existing filters"() {
    given: "a service account with a filter"
      def ff = createFilter("SA-NULL-" + RandomStringUtils.randomAlphabetic(6))
      def sa = createServiceAccount([ff.id])
    when: "updated without specifying featureFilters (null — no change)"
      serviceAccountApi.update(portfolio.id, superuser,
        new ServiceAccount().id(sa.id).name(sa.name).version(sa.version)
          .description("changed description"),
        null, Opts.empty())
    then: "filters are unchanged"
      def dbSa = Finder.findServiceAccountWithFilters(sa.id)
      dbSa.featureFilters.size() == 1
      dbSa.featureFilters[0].id == ff.id
  }
}

package io.featurehub.db.services

import groovy.transform.CompileStatic
import io.featurehub.db.api.Opts
import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.query.QDbOrganization
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.db.test.DbSpecification
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.Person
import org.apache.commons.lang3.RandomStringUtils

class Base2Spec extends DbSpecification {
  Conversions convertUtils
  Person superPerson
  DbPerson dbSuperPerson
  GroupSqlApi groupSqlApi
  UUID superuser
  DbArchiveStrategy archiveStrategy
  Organization org
  WebhookEncryptionService encryptionService
  CacheSource cacheSource
  InternalFeatureApi internalFeatureApi
  FeatureMessagingPublisher featureMessagingPublisher

  String ranName() {
    return RandomStringUtils.randomAlphabetic(10)
  }

  def setup() {
    System.setProperty("webhooks.encryption.password", "foof")
    encryptionService = Mock()
    cacheSource = Mock()
    convertUtils = new ConvertUtils(encryptionService)
    featureMessagingPublisher = Mock()
    internalFeatureApi = new InternalFeatureSqlApi(convertUtils, cacheSource, featureMessagingPublisher)
    archiveStrategy = new DbArchiveStrategy(cacheSource)
    groupSqlApi = new GroupSqlApi(db, convertUtils, archiveStrategy)

    dbSuperPerson = Finder.findByEmail("irina@featurehub.io")
    if (dbSuperPerson == null) {
      dbSuperPerson = new DbPerson.Builder().email("irina@featurehub.io").name("Irina").build();
      db.save(dbSuperPerson)
    }

    db.save(dbSuperPerson);
    superuser = dbSuperPerson.getId()

    def organizationSqlApi = new OrganizationSqlApi(convertUtils)

    // ensure the org is created and we have an admin user in an admin group
    Group adminGroup
    def newOrganisation = !convertUtils.hasOrganisation()
    if (newOrganisation) {
      org = organizationSqlApi.save(new Organization().name(ranName()))
    } else {
      org = organizationSqlApi.get()
    }

    superPerson = convertUtils.toPerson(dbSuperPerson, Opts.empty())

    if (newOrganisation) {
      adminGroup = groupSqlApi.createOrgAdminGroup(org.id, ranName(), superPerson)
    } else {
      adminGroup = groupSqlApi.findOrganizationAdminGroup(org.id, Opts.empty())
    }

    groupSqlApi.addPersonToGroup(adminGroup.id, superuser, Opts.empty())
  }

  @CompileStatic
  DbOrganization findOrganization() {
    return new QDbOrganization().id.eq(org.id).findOne()
  }
}

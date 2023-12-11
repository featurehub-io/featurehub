package io.featurehub.db.services

import io.ebean.DB
import io.ebean.Database
import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbPerson
import io.featurehub.encryption.SymmetricEncrypter
import io.featurehub.encryption.SymmetricEncrypterImpl
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.encryption.WebhookEncryptionServiceImpl
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.Person
import spock.lang.Shared
import spock.lang.Specification

class BaseSpec extends Specification {
  @Shared Database database
  @Shared ConvertUtils convertUtils
  @Shared Person superPerson
  @Shared DbPerson dbSuperPerson
  @Shared GroupSqlApi groupSqlApi
  @Shared UUID superuser
  @Shared DbArchiveStrategy archiveStrategy
  @Shared Organization org
  @Shared Group adminGroup
  @Shared WebhookEncryptionService webhookEncryptionService
  @Shared SymmetricEncrypter symmetricEncrypter

  def baseSetupSpec() {
    System.setProperty("ebean.ddl.generate", "true")
    System.setProperty("ebean.ddl.run", "true")
    database = DB.getDefault()
    symmetricEncrypter = new SymmetricEncrypterImpl("password")
    webhookEncryptionService = new WebhookEncryptionServiceImpl(symmetricEncrypter)
    convertUtils = new ConvertUtils(webhookEncryptionService)
    archiveStrategy = new DbArchiveStrategy(database, Mock(CacheSource))
    groupSqlApi = new GroupSqlApi(database, convertUtils, archiveStrategy)

    dbSuperPerson = Finder.findByEmail("irina@featurehub.io")
    if (dbSuperPerson == null) {
      dbSuperPerson = new DbPerson.Builder().email("irina@featurehub.io").name("Irina").build();
      database.save(dbSuperPerson);
    }
    database.save(dbSuperPerson);
    superuser = dbSuperPerson.getId()

    def organizationSqlApi = new OrganizationSqlApi(convertUtils)


    def orgExists = convertUtils.hasOrganisation()

    if (orgExists) {
      org = organizationSqlApi.get()
    } else {
      org = organizationSqlApi.save(new Organization().name("org1"))
    }
    // ensure the org is created and we have an admin user in an admin group

    superPerson = convertUtils.toPerson(dbSuperPerson, Opts.empty())

    if (orgExists) {
      adminGroup = groupSqlApi.findOrganizationAdminGroup(org.id, Opts.empty())
    } else {
      adminGroup = groupSqlApi.createOrgAdminGroup(org.id, 'admin group', superPerson)
    }

    groupSqlApi.addPersonToGroup(adminGroup.id, superuser, Opts.empty())
  }
}

package io.featurehub.db.services

import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbPerson
import io.featurehub.db.publish.CacheSource
import io.featurehub.db.test.DbSpecification
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.Person
import spock.lang.Shared

class Base2Spec extends DbSpecification {
  ConvertUtils convertUtils
  Person superPerson
  DbPerson dbSuperPerson
  GroupSqlApi groupSqlApi
  UUID superuser
  DbArchiveStrategy archiveStrategy
  Organization org

  def setup() {
    convertUtils = new ConvertUtils()
    archiveStrategy = new DbArchiveStrategy(db, Mock(CacheSource))
    groupSqlApi = new GroupSqlApi(db, convertUtils, archiveStrategy)

    dbSuperPerson = Finder.findByEmail("irina@featurehub.io")
    if (dbSuperPerson == null) {
      dbSuperPerson = new DbPerson.Builder().email("irina@featurehub.io").name("Irina").build();
      db.save(dbSuperPerson);
    }

    db.save(dbSuperPerson);
    superuser = dbSuperPerson.getId()

    def organizationSqlApi = new OrganizationSqlApi(db, convertUtils)

    // ensure the org is created and we have an admin user in an admin group
    org = organizationSqlApi.get()
    Group adminGroup
    def createAdminGroup = (org == null)
    if (org == null) {
      org = organizationSqlApi.save(new Organization().name("org1"))
    }

    superPerson = convertUtils.toPerson(dbSuperPerson, Opts.empty())

    if (createAdminGroup) {
      adminGroup = groupSqlApi.createOrgAdminGroup(org.id, 'admin group', superPerson)
    } else {
      adminGroup = groupSqlApi.findOrganizationAdminGroup(org.id, Opts.empty())
    }

    groupSqlApi.addPersonToGroup(adminGroup.id, superuser, Opts.empty())
  }
}

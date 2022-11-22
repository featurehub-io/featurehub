package io.featurehub.db.services

import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonApi
import io.featurehub.mr.model.PersonType

class AdminServiceAccount1Spec extends Base2Spec {
  PersonSqlApi personSqlApi

  def setup() {
    personSqlApi = new PersonSqlApi(db, convertUtils, archiveStrategy, groupSqlApi)
    db.commitTransaction()
  }

  // this needs to be committed, hence it is isolated
  def "i can create service accounts and not find them with a person search but can find them with a service account search"() {
    given: "i create two service accounts"

      def sa1 = personSqlApi.createServicePerson("Treebeard", superuser)
      def sa2 = personSqlApi.createServicePerson("Pippin", superuser)
    when: "i search for them as people"
      def people = personSqlApi.search(null, null, 0, 1, Set.of(PersonType.PERSON), null, Opts.empty())
    and: "i search for them as service accounts"
      def accounts = personSqlApi.search(null, null, 0, 200, Set.of(PersonType.SERVICEACCOUNT), null, Opts.empty())
    then: "i have 0 items on person search"
      people.max > 0
      people.people.size() == 1
    and: "i have 2 items on service account search"
      accounts.max >= 2 // depends on whether it runs before or after the other test
      accounts.people.size() >= 2
      accounts.people.collect({it.id.id}).containsAll([sa1.person.id.id, sa2.person.id.id])
  }
}

package io.featurehub.db.services

class AdminServiceAccount2Spec extends Base2Spec {
  PersonSqlApi personSqlApi
  AuthenticationSqlApi authenticationSqlApi

  def setup() {
    personSqlApi = new PersonSqlApi(db, convertUtils, archiveStrategy, groupSqlApi)
    authenticationSqlApi = new AuthenticationSqlApi(db, convertUtils)
  }

  def "i cannot perform authentication operations with a service account user"() {
    when: "i have a service account"
      def sa1 = personSqlApi.createServicePerson("Saruman", superuser)
      db.commitTransaction()
    then: "i attempt to login i cannot"
      authenticationSqlApi.login(sa1.person.email, '') == null
    and: "i cannot reset the password of the account"
      authenticationSqlApi.resetPassword(sa1.person.id.id, 'fred', superuser, true) == null
    and: "i cannot replace the password"
      authenticationSqlApi.replaceTemporaryPassword(sa1.person.id.id, 'new-password') == null
    and: "i can't change my password"
      authenticationSqlApi.changePassword(sa1.person.id.id, '', 'new-password') == null
    and: 'i can get the person by token'
      authenticationSqlApi.findSession(sa1.token).person.id.id == sa1.person.id.id
    and: "i delete the token but i can still use it because it won't delete"
      authenticationSqlApi.invalidateSession(sa1.token)
      authenticationSqlApi.findSession(sa1.token).person.id.id == sa1.person.id.id
    and: "i cannot reset an expired token"
      authenticationSqlApi.resetExpiredRegistrationToken(sa1.person.email) == null
  }
}

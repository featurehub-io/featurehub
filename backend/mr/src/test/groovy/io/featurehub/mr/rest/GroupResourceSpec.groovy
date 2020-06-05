package io.featurehub.mr.rest

import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonApi
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import spock.lang.Specification

import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.SecurityContext


class GroupResourceSpec extends Specification {
  GroupApi groupApi
  PersonApi personApi
  GroupResource gr
  AuthManagerService authManager

  def setup() {
    groupApi = Mock(GroupApi)
    personApi = Mock(PersonApi)
    authManager = Mock(AuthManagerService)
    gr = new GroupResource(personApi, groupApi, authManager)
  }

  def "cannot add a person to a non-existent group"() {
    given: "the group does not exist"
      groupApi.getGroup("1", (Opts)_) >> null
    when: "i try and add a person to the group"
      gr.addPersonToGroup("1", null, true, null)
    then:
      thrown(NotFoundException)
  }

  SecurityContext setupGroupAndPortfoioAdmin(String portfolioId, String idPersonInChargeOfPortfolioAdminGroup, String currentPersonId, boolean groupIsAdmin = false, String personIdInAdminGroup = "admin-user") {
    groupApi.getGroup("1", (Opts)_) >> new Group().portfolioId(portfolioId).admin(groupIsAdmin)
    def sc = Mock(SecurityContext)
    groupApi.findPortfolioAdminGroup(portfolioId, (Opts)_) >> new Group().admin(true).members([new Person().id(new PersonId().id(idPersonInChargeOfPortfolioAdminGroup))])
    groupApi.findOrganizationAdminGroup(null, (Opts)_) >> new Group().admin(true).members([new Person().id(new PersonId().id(personIdInAdminGroup))])
    authManager.from(sc) >> new Person().id(new PersonId().id(currentPersonId))
    return sc
  }

  def "a person not in a portfolio admin group cannot change portfolio group"() {
    given: "the group is set up and the portfolio admin is setup"
      def sc = setupGroupAndPortfoioAdmin("port", "2", "1")
    and: "the person to be added exists"
      personApi.get("1", (Opts)_) >> new Person()
    when:
      gr.addPersonToGroup("1", "1", null, sc)
    then: "not allowed to add a person to the group"
      thrown(NotAuthorizedException)
  }

  def "a portfolio admin can change a portfolio group"() {
    given: "the group is set up and the portfolio admin is setup"
      def sc = setupGroupAndPortfoioAdmin("port", "2", "2")
    and: "the person to be added exists"
      personApi.get("7", (Opts)_) >> new Person()
    when:
      gr.addPersonToGroup("1", "7", null, sc)
    then: "the person is added to the group"
      1 * groupApi.addPersonToGroup("1", "7", (Opts)_) >> new Group()
  }

  def "a superadmin can change a portfolio group"() {
    given: "the group is set up and the portfolio admin is setup"
      def sc = setupGroupAndPortfoioAdmin("port", "2", "admin-user")
    and: "the person to be added exists"
      personApi.get("7", (Opts)_) >> new Person()
    when:
      gr.addPersonToGroup("1", "7", null, sc)
    then: "the person is added to the group"
      1 * groupApi.addPersonToGroup("1", "7", (Opts)_) >> new Group()
  }

  def "cannot add a person to an known group"() {
    when:
      gr.addPersonToGroup("1", "7", null, null)
    then:
      thrown(NotFoundException)
  }

  def "cannot add an unknown person to a known group"() {
    given: "the group is set up and the portfolio admin is setup"
      def sc = setupGroupAndPortfoioAdmin("port", "2", "2")
    when:
      gr.addPersonToGroup("1", "7", null, null)
    then:
      thrown(NotFoundException)
  }

  // not actually sure about the rules around the org level group now. Should they be able to change portfolio groups?
  // are there other groups at org level? i don't think so

  def "no-one can delete admin groups"() {
    given: "i have setup the group as a portfolio admin group"
      def sc = setupGroupAndPortfoioAdmin("pId", "pid-owner", "pid-owner", true)
    when:
      gr.deleteGroup("1", null, null, sc)
    then:
      thrown(NotAuthorizedException)
  }

  def "portfolio admin can delete groups in their portfolio"() {
    given:
      def sc = setupGroupAndPortfoioAdmin("pId", "pid-owner", "pid-owner")
    when:
      gr.deleteGroup("1", null, null, sc)
    then:
      1 * groupApi.deleteGroup("1")
  }

  def "a super-admin can delete groups in any portfolio"() {
    given:
      def sc = setupGroupAndPortfoioAdmin("pId", "pid-owner", "admin-user")
    when:
      gr.deleteGroup("1", null, null, sc)
    then:
      1 * groupApi.deleteGroup("1")
  }

  def "a generic person cannot delete groups in a portfolio"() {
    given:
      def sc = setupGroupAndPortfoioAdmin("pId", "pid-owner", "piddle")
    when:
      gr.deleteGroup("1", null, null, sc)
    then:
      thrown(NotAuthorizedException)
  }

  def "cannot delete an unknown group"() {
    when: "i try and delete an unknown group"
      gr.deleteGroup("1", null, null, null)
    then:
      thrown(NotFoundException)
  }

  // again, super-admins deleting portfolio groups not tested, nor deleting org level groups

  /// ---- delete person from group


  def "cannot delete a person from a group if you are not a portfolio admin or admin user"() {
    given:
      def sc = setupGroupAndPortfoioAdmin("pId", "pid-owner", "piddle")
    and: "the person to be deleted exists"
      personApi.get("7", (Opts)_) >> new Person()
    when:
      gr.deletePersonFromGroup("1", "7", null, sc)
    then:
      thrown(NotAuthorizedException)
  }

  def "can delete a person from a group if you are the portfolio admin"() {
    given:
      def sc = setupGroupAndPortfoioAdmin("pId", "pid-owner", "pid-owner")
    and: "the person to be deleted exists"
      personApi.get("7", (Opts)_) >> new Person()
    when:
      gr.deletePersonFromGroup("1", "7", null, sc)
    then:
      1 * groupApi.deletePersonFromGroup("1", "7", (Opts)_)  >> new Group()
  }

  def "can delete a person from a group if you are the super-admin"() {
    given:
      def sc = setupGroupAndPortfoioAdmin("pId", "pid-owner", "admin-user")
    and: "the person to be deleted exists"
      personApi.get("7", (Opts)_) >> new Person()
    when:
      gr.deletePersonFromGroup("1", "7", null, sc)
    then:
      1 * groupApi.deletePersonFromGroup("1", "7", (Opts)_)  >> new Group()
  }

  def "cannot delete person from non-existent group"() {
    when:
      gr.deletePersonFromGroup("1", "7", null, null)
    then:
      thrown(NotFoundException)
  }

  def "cannot delete unknown person from known group"() {
    given:
      def sc = setupGroupAndPortfoioAdmin("pId", "pid-owner", "pid-owner")
    when: "known group, known current user, unknown person deleted"
      gr.deletePersonFromGroup("1", "7", null, sc)
    then:
      thrown(NotFoundException)
  }

  /// ------ get group

  def "non existent group cannot get"() {
    when:
      gr.getGroup("1", true, null, null)
    then:
      thrown(NotFoundException)
  }

  def "can get existing group"() {
    when:
      gr.getGroup("1", true, null, null)
    then:
      1 * groupApi.getGroup("1", (Opts)_) >> new Group()
  }

  /// ------- rename group

  def "a portfolio admin can rename a portfolio group"() {
    given:
      SecurityContext sc = setupGroupAndPortfoioAdmin("pId", "pid-owner", "pid-owner")
    when:
      Group g = gr.updateGroup("1", new Group().name("sausage"), true, null, null, null, null, sc)
    then:
      1 * groupApi.updateGroup("1", { Group g1 -> g1.name == "sausage" }, false, false, false, (Opts)_) >> new Group().name("sausage")
      g.getName() == "sausage"
  }

  def "an admin can rename a portfolio group"() {
    given:
      SecurityContext sc = setupGroupAndPortfoioAdmin("pId", "pid-owner", "admin-user")
    when:
      Group g = gr.updateGroup("1", new Group().name("sausage"), true, null, Boolean.TRUE
        , null, null, sc)
    then:
      1 * groupApi.updateGroup("1", { Group g1 -> g1.name == "sausage" }, true, false, false, (Opts)_) >> new Group().name("sausage")
      g.getName() == "sausage"
  }
}

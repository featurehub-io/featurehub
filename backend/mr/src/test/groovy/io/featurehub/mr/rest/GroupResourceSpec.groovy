package io.featurehub.mr.rest

import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonApi
import io.featurehub.mr.api.GroupServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.CreateGroup
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.model.SortOrder
import io.featurehub.mr.model.UpdateGroup
import io.featurehub.mr.resources.GroupResource
import io.featurehub.mr.utils.PortfolioUtils
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.SecurityContext
import spock.lang.Specification

class GroupResourceSpec extends Specification {
  GroupApi groupApi
  PersonApi personApi
  GroupResource gr
  AuthManagerService authManager
  SecurityContext sc
  UUID groupId
  UUID portfolioId // portfolio id
  UUID personId
  Person person
  UUID adminUser
  PortfolioUtils portfolioUtils

  def setup() {
    groupApi = Mock(GroupApi)
    personApi = Mock(PersonApi)
    authManager = Mock(AuthManagerService)
    portfolioUtils = Mock()

    gr = new GroupResource(personApi, groupApi, authManager, portfolioUtils)
    groupId = UUID.randomUUID()
    portfolioId = UUID.randomUUID()
    adminUser = UUID.randomUUID()
    personId = UUID.randomUUID()
    sc = Mock()
    person = new Person().id(new PersonId().id(personId))
    authManager.from(sc) >> person
    authManager.who(sc) >> personId
  }

  def "cannot add a person to a non-existent group"() {
    given: "the group does not exist"
      groupApi.getGroup(groupId, (Opts) _, _) >> null
    when: "i try and add a person to the group"
      gr.addPersonToGroup(groupId, UUID.randomUUID(), new GroupServiceDelegate.AddPersonToGroupHolder(includeMembers: true), sc)
    then:
      thrown(NotFoundException)
  }

  def "if you are a portfolio admin you can create a group"() {
    given: "i am a portfolio admin"
      authManager.isPortfolioAdmin(portfolioId, person, null) >> true
    and: "i have a group"
      def group = new CreateGroup()
    when: "i attempt to create said Group"
      gr.createGroup(portfolioId, group, new GroupServiceDelegate.CreateGroupHolder(includePeople: true), sc)
    then:
      1 * groupApi.createGroup(portfolioId, group, person) >> new Group().id(groupId)
  }

  def "findGroups works"() {
    given:
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isOrgAdmin(person) >> true
    when: "i find groups"
      gr.findGroups(groupId, new GroupServiceDelegate.FindGroupsHolder(includePeople: true, order: SortOrder.DESC, filter: "turn"), sc)
    then:
      1 * groupApi.findGroups(groupId, "turn", SortOrder.DESC, Opts.opts(FillOpts.People)) >> []
  }


  def "if you are not a portfolio admin, you cannot create a group"() {
    given: "i am not a portfolio admin"
      portfolioUtils.portfolioUserManager(sc, portfolioId) >> { throw new ForbiddenException() }
    and: "i have a group"
      def group = new CreateGroup()
    when: "i attempt to create a group"
      gr.createGroup(portfolioId, group, new GroupServiceDelegate.CreateGroupHolder(includePeople: true), sc)
    then:
      thrown(ForbiddenException)
  }

  SecurityContext setupGroupAndPortfoioAdmin(UUID portfolioId, UUID idPersonInChargeOfPortfolioAdminGroup, UUID currentPersonId, boolean groupIsAdmin = false, UUID personIdInAdminGroup = null) {
    if (personIdInAdminGroup == null) {
      personIdInAdminGroup = adminUser
    }
    UUID orgId = UUID.randomUUID()
    groupApi.getGroup(groupId, (Opts) _, _) >> new Group().id(groupId).portfolioId(portfolioId).admin(groupIsAdmin).organizationId(orgId)
    def sc = Mock(SecurityContext)
    groupApi.findPortfolioAdminGroup(portfolioId, (Opts)_) >> new Group().id(groupId).admin(true).members([new Person().id(new PersonId().id(idPersonInChargeOfPortfolioAdminGroup))])
    groupApi.findOrganizationAdminGroup(orgId, (Opts)_) >> new Group().id(groupId).admin(true).members([new Person().id(new PersonId().id(personIdInAdminGroup))])
    authManager.from(sc) >> new Person().id(new PersonId().id(currentPersonId))
    return sc
  }

  def "a person not in a portfolio admin group cannot change portfolio group"() {
    given: "the group is set up and the portfolio admin is setup"
      def sc = setupGroupAndPortfoioAdmin(portfolioId, UUID.randomUUID(), personId)
      portfolioUtils.portfolioUserManager(sc, portfolioId) >> { throw new ForbiddenException() }
    and: "the person to be added exists"
      personApi.get(personId, (Opts)_) >> new Person()
    when:
      gr.addPersonToGroup(groupId, personId, new GroupServiceDelegate.AddPersonToGroupHolder(), sc)
    then: "not allowed to add a person to the group"
      thrown(ForbiddenException)
  }

  def "a portfolio admin can change a portfolio group"() {
    given: "the group is set up and the portfolio admin is setup"
      def personId = UUID.randomUUID()
      def sc = setupGroupAndPortfoioAdmin(UUID.randomUUID(), personId, personId)
    and: "the person to be added exists"
      personApi.get(personId, (Opts)_) >> new Person()
    when:
      gr.addPersonToGroup(groupId, personId, new GroupServiceDelegate.AddPersonToGroupHolder(), sc)
    then: "the person is added to the group"
      1 * groupApi.addPersonsToGroup(groupId, [personId], (Opts)_) >> new Group()
  }

  def "a superadmin can change a portfolio group"() {
    given: "the group is set up and the portfolio admin is setup"
      def personInChargeOfPortfolioAdminGroup = UUID.randomUUID()
      def sc = setupGroupAndPortfoioAdmin(UUID.randomUUID(), personInChargeOfPortfolioAdminGroup, adminUser)
    and: "the person to be added exists"
      personApi.get(personInChargeOfPortfolioAdminGroup, (Opts)_) >> new Person()
    when:
      gr.addPersonToGroup(groupId, personInChargeOfPortfolioAdminGroup, new GroupServiceDelegate.AddPersonToGroupHolder(), sc)
    then: "the person is added to the group"
      1 * groupApi.addPersonsToGroup(groupId, [personInChargeOfPortfolioAdminGroup], (Opts)_) >> new Group()
  }

  def "cannot add a person to an known group"() {
    when:
      gr.addPersonToGroup(groupId, UUID.randomUUID(), new GroupServiceDelegate.AddPersonToGroupHolder(), sc)
    then:
      thrown(NotFoundException)
  }

  def "cannot add an unknown person to a known group"() {
    given: "the group is set up and the portfolio admin is setup"
      def pidOwner = UUID.randomUUID()
      def sc = setupGroupAndPortfoioAdmin(UUID.randomUUID(), UUID.randomUUID(), pidOwner)
    when:
      gr.addPersonToGroup(groupId, pidOwner, new GroupServiceDelegate.AddPersonToGroupHolder(), sc)
    then:
      thrown(NotFoundException)
  }

  // not actually sure about the rules around the org level group now. Should they be able to change portfolio groups?
  // are there other groups at org level? i don't think so

  def "no-one can delete admin groups"() {
    given: "i have setup the group as a portfolio admin group"
      def pidOwner = UUID.randomUUID()
      def sc = setupGroupAndPortfoioAdmin(UUID.randomUUID(), pidOwner, pidOwner, true)
    when:
      gr.deleteGroup(groupId, new GroupServiceDelegate.DeleteGroupHolder(), sc)
    then:
      thrown(ForbiddenException)
  }

  def "portfolio admin can delete groups in their portfolio"() {
    given:
      def pidOwner = UUID.randomUUID()
      def sc = setupGroupAndPortfoioAdmin(UUID.randomUUID(), pidOwner, pidOwner)
    when:
      gr.deleteGroup(groupId, new GroupServiceDelegate.DeleteGroupHolder(), sc)
    then:
      1 * groupApi.deleteGroup(groupId)
  }

  def "a super-admin can delete groups in any portfolio"() {
    given:
      def pidOwner = UUID.randomUUID()
      def admin = UUID.randomUUID()
      def sc = setupGroupAndPortfoioAdmin(UUID.randomUUID(), pidOwner, adminUser)
    when:
      gr.deleteGroup(groupId, new GroupServiceDelegate.DeleteGroupHolder(), sc)
    then:
      1 * groupApi.deleteGroup(groupId)
  }

  def "a generic person cannot delete groups in a portfolio"() {
    given:
      def pidOwner = UUID.randomUUID()
      def piddle = UUID.randomUUID()
      def sc = setupGroupAndPortfoioAdmin(portfolioId, pidOwner, piddle)
      portfolioUtils.portfolioUserManager(sc, portfolioId) >> { throw new ForbiddenException() }
    when:
      gr.deleteGroup(groupId, new GroupServiceDelegate.DeleteGroupHolder(), sc)
    then:
      thrown(ForbiddenException)
  }

  def "cannot delete an unknown group"() {
    when: "i try and delete an unknown group"
      gr.deleteGroup(groupId, new GroupServiceDelegate.DeleteGroupHolder(), sc)
    then:
      thrown(NotFoundException)
  }

  // again, super-admins deleting portfolio groups not tested, nor deleting org level groups

  /// ---- delete person from group


  def "cannot delete a person from a group if you are not a portfolio admin or admin user"() {
    given:
      def pidOwner = UUID.randomUUID()
      def piddle = UUID.randomUUID()
      def sc = setupGroupAndPortfoioAdmin(portfolioId, pidOwner, piddle)
      portfolioUtils.portfolioUserManager(sc, portfolioId) >> { throw new ForbiddenException() }
    and: "the person to be deleted exists"
      personApi.get(piddle, (Opts)_) >> new Person()
    when:
      gr.deletePersonFromGroup(groupId, piddle, new GroupServiceDelegate.DeletePersonFromGroupHolder(), sc)
    then:
      thrown(ForbiddenException)
  }

  def "can delete a person from a group if you are the portfolio admin"() {
    given:
      def pidOwner = UUID.randomUUID()
      def sc = setupGroupAndPortfoioAdmin(UUID.randomUUID(), pidOwner, pidOwner)
    and: "the person to be deleted exists"
      personApi.get(pidOwner, (Opts)_) >> new Person()
    when:
      gr.deletePersonFromGroup(groupId, pidOwner, new GroupServiceDelegate.DeletePersonFromGroupHolder(), sc)
    then:
      1 * groupApi.deletePersonFromGroup(groupId, pidOwner, (Opts)_)  >> new Group()
  }

  def "can delete a person from a group if you are the super-admin"() {
    given:
      def pidOwner = UUID.randomUUID()
      def sc = setupGroupAndPortfoioAdmin(UUID.randomUUID(), pidOwner, adminUser)
    and: "the person to be deleted exists"
      personApi.get(pidOwner, (Opts)_) >> new Person()
    when:
      gr.deletePersonFromGroup(groupId, pidOwner, new GroupServiceDelegate.DeletePersonFromGroupHolder(), sc)
    then:
      1 * groupApi.deletePersonFromGroup(groupId, pidOwner, (Opts)_)  >> new Group()
  }

  def "cannot delete person from non-existent group"() {
    when:
      gr.deletePersonFromGroup(groupId, UUID.randomUUID(), new GroupServiceDelegate.DeletePersonFromGroupHolder(), sc)
    then:
      thrown(NotFoundException)
  }

  def "cannot delete unknown person from known group"() {
    given:
      def pidOwner = UUID.randomUUID()
      def sc = setupGroupAndPortfoioAdmin(UUID.randomUUID(), pidOwner, pidOwner)
    when: "known group, known current user, unknown person deleted"
      gr.deletePersonFromGroup(groupId, pidOwner, new GroupServiceDelegate.DeletePersonFromGroupHolder(), sc)
    then:
      thrown(NotFoundException)
  }

  /// ------ get group

  def "non existent group cannot get"() {
    when:
      gr.getGroup(groupId, new GroupServiceDelegate.GetGroupHolder(), sc)
    then:
      thrown(NotFoundException)
  }

  def "can get existing group"() {
    when:
      gr.getGroup(groupId, new GroupServiceDelegate.GetGroupHolder(), sc)
    then:
      1 * groupApi.getGroup(groupId, (Opts) _, _) >> new Group()
  }

  /// ------- rename group

  def "a portfolio admin can rename a portfolio group"() {
    given:
      def pidOwner = UUID.randomUUID()
      SecurityContext sc = setupGroupAndPortfoioAdmin(UUID.randomUUID(), pidOwner, pidOwner)
    when:
      Group g = gr.updateGroupOnPortfolioV2(portfolioId, new UpdateGroup().id(groupId).name("sausage"), new GroupServiceDelegate.UpdateGroupOnPortfolioV2Holder(), sc)
    then:
      1 * groupApi.updateGroup(groupId, { UpdateGroup g1 -> g1.name == "sausage" }, null, false, false, (Opts)_) >> new Group().name("sausage")
      g.getName() == "sausage"
  }

  def "an admin can rename a portfolio group"() {
    given:
      def pidOwner = UUID.randomUUID()
      SecurityContext sc = setupGroupAndPortfoioAdmin(UUID.randomUUID(), pidOwner, adminUser)
    when:
      Group g = gr.updateGroupOnPortfolioV2(portfolioId, new UpdateGroup().id(groupId).name("sausage"), new GroupServiceDelegate.UpdateGroupOnPortfolioV2Holder(applicationId: pidOwner), sc)
    then:
      1 * groupApi.updateGroup(groupId, { UpdateGroup g1 -> g1.name == "sausage" }, pidOwner, false, false, (Opts)_) >> new Group().name("sausage")
      g.getName() == "sausage"
  }
}

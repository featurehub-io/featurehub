package io.featurehub.mr.utils

import io.featurehub.db.api.ApplicationApi
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.core.SecurityContext
import spock.lang.Specification

class ApplicationUtilsSpec extends Specification {
  AuthManagerService authManager
  ApplicationApi appApi
  ApplicationUtils  appUtils
  SecurityContext ctx
  Person person
  UUID appId

  def setup() {
    authManager = Mock()
    appApi = Mock()
    ctx = Mock()
    appId = UUID.randomUUID()
    person = new Person().id(new PersonId().id(UUID.randomUUID())).name('Prince Frederick 1st').email('pf@fred.com')
    authManager.from(ctx) >> person
    appUtils = new ApplicationUtils(authManager, appApi)
  }

  def "when a person has a creator role, they will pass the creator check"() {
    when: "we ask for permission check"
      def result = appUtils.featureCreatorCheck(ctx, appId)
    then:
      1 * appApi.personIsFeatureCreator(appId, person.id.id) >> true
      result.current == person
      result.app.id == appId
  }

  def "when a person has an org admin role, they will pass the creator check"() {
    when: "we ask for permission check"
      def result = appUtils.featureCreatorCheck(ctx, appId)
    then:
      1 * appApi.personIsFeatureCreator(appId, person.id.id) >> false
      1 * appApi.getApplication(appId, _) >> new Application().portfolioId(UUID.randomUUID())
      1 * authManager.isOrgAdmin(person.id.id) >> true
      result.current == person
      result.app != null
  }

  def "when a person has an portfolio admin role, they will pass the creator check"() {
    given: "we have an app"
      def app = new Application().portfolioId(UUID.randomUUID())
    when: "we ask for permission check"
      def result = appUtils.featureCreatorCheck(ctx, appId)
    then:
      1 * appApi.personIsFeatureCreator(appId, person.id.id) >> false
      1 * appApi.getApplication(appId, _) >> app
      1 * authManager.isOrgAdmin(person.id.id) >> true
//      1 * authManager.isPortfolioAdmin(app.portfolioId, person.id.id, null) >> true
      result.current == person
      result.app != null
  }

  def "when a person has no creator role and isn't an admin, they will fail the creator check"() {
    given: "we have an app"
      def app = new Application().portfolioId(UUID.randomUUID())
    when: "we ask for permission check"
      def result = appUtils.featureCreatorCheck(ctx, appId)
    then:
      1 * appApi.personIsFeatureCreator(appId, person.id.id) >> false
      1 * appApi.getApplication(appId, _) >> app
      1 * authManager.isOrgAdmin(person.id.id) >> false
      1 * authManager.isPortfolioAdmin(app.portfolioId, person.id.id, null) >> false
      thrown(ForbiddenException)
  }

  def "when a person has an editor role, it passes the editor check"() {
    when: "we ask for permission check"
      appUtils.featureEditorCheck(ctx, appId)
    then:
      1 * appApi.personIsFeatureEditor(appId, person.id.id) >> true
  }
}

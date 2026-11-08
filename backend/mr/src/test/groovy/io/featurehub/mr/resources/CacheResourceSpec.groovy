package io.featurehub.mr.resources

import io.featurehub.db.api.CacheRefresherApi
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.CacheRefreshRequest
import io.featurehub.mr.model.Person
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.core.SecurityContext
import spock.lang.Specification

class CacheResourceSpec extends Specification {
  AuthManagerService authManager
  CacheRefresherApi refresher
  CacheResource resource
  SecurityContext sc
  Person person

  def setup() {
    authManager = Mock()
    refresher = Mock()
    sc = Mock()
    person = new Person()
    authManager.from(sc) >> person
    resource = new CacheResource(authManager, refresher)
  }

  def "a non org admin cannot refresh the cache"() {
    given:
      authManager.isOrgAdmin(person) >> false
    when:
      resource.cacheRefresh(new CacheRefreshRequest().allTheThings(true), sc)
    then:
      thrown(ForbiddenException)
      0 * refresher._
  }

  def "allTheThings true refreshes the whole database"() {
    given:
      authManager.isOrgAdmin(person) >> true
    when:
      resource.cacheRefresh(new CacheRefreshRequest().allTheThings(true), sc)
    then:
      1 * refresher.refreshEntireDatabase()
  }

  def "allTheThings false does not refresh the whole database"() {
    given:
      authManager.isOrgAdmin(person) >> true
    when:
      resource.cacheRefresh(new CacheRefreshRequest().allTheThings(false), sc)
    then:
      0 * refresher.refreshEntireDatabase()
  }

  def "portfolio and application refreshes are passed through"() {
    given:
      authManager.isOrgAdmin(person) >> true
      def pId = UUID.randomUUID()
      def aId = UUID.randomUUID()
    when:
      resource.cacheRefresh(new CacheRefreshRequest().portfolioId([pId]).applicationId([aId]), sc)
    then:
      1 * refresher.refreshPortfolios([pId])
      1 * refresher.refreshApplications([aId])
      0 * refresher.refreshEntireDatabase()
  }
}

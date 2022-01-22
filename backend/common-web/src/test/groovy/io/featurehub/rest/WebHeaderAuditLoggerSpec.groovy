package io.featurehub.rest

import cd.connect.context.ConnectContext
import jakarta.ws.rs.container.ContainerRequestContext
import spock.lang.Specification

class WebHeaderAuditLoggerSpec extends Specification {
  def cleanup() {
    ConnectContext.clear()
    System.clearProperty(WebHeaderAuditLogger.@Companion.CONFIG_KEY)
  }

  def "when i configure headers, it will pick up what was asked for and ignore what wasn't"() {
    given: 'i have some headers'
      System.setProperty(WebHeaderAuditLogger.@Companion.CONFIG_KEY, "X-Forwarded-For,X-Forwarded-Proto,X-Forwarded-Port")
    and: 'i have an audit logger'
      def auditLogger = new WebHeaderAuditLogger()
    and: 'i have mocked my request'
      def request = Mock(ContainerRequestContext)
    when:
      auditLogger.filter(request)
    then:
      1 * request.getHeaderString('X-Forwarded-For') >> 'http://chipmonnks-inc.io'
      1 * request.getHeaderString('X-Forwarded-Proto') >> 'http'
      1 * request.getHeaderString('X-Forwarded-Port') >> null
      0 * _  // no other mock interactions
      ConnectContext.get(WebHeaderAuditLogger.@Companion.WEBHEADERS, Map<String, String>) ==
        ['X-Forwarded-For': 'http://chipmonnks-inc.io', 'X-Forwarded-Proto':'http']
  }
}

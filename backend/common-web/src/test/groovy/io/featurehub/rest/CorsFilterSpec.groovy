package io.featurehub.rest

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.spockframework.gentyref.TypeToken
import spock.lang.Specification

class CorsFilterSpec extends Specification {
  CorsFilter filter
  ContainerResponseContext response
  ContainerRequestContext request

  def setup() {
    response = Mock(ContainerResponseContext)
    request = Mock(ContainerRequestContext)

    filter = new CorsFilter()
  }

  def "when i request an OPTIONS request, the filter catches it and returns ok"() {
    when:
      filter.filter(request)
    then:
      1 * request.getHeaderString("Origin") >> "origin"
      1 * request.getMethod() >> "OPTIONS"
      1 * request.abortWith({ Response r -> r.status == 200 })
      0 * _
  }

  def "when it is not an options request, the filter ignores it"() {
    when:
      filter.filter(request)
    then:
      1 * request.getHeaderString("Origin") >> null
      0 * _
  }

  def "if it is not a preflight request, we do not get any response"() {
    when:
      filter.filter(request, response)
    then:
      1 * request.getHeaderString("Origin") >> null
      0 * _
  }

  def "If it is an origin request but not preflight, we should get access control headers back"() {
    given: "i have headers"
      def headers = Mock(type: new TypeToken<MultivaluedMap<String, Object>>(){}.type) as MultivaluedMap<String, Object>
    when:
      filter.filter(request, response)
    then:
      2 * request.getHeaderString("Origin") >> "Origin"
      1 * request.getMethod() >> null
      2 * response.headers >> headers
      1 * headers.add("Access-Control-Expose-Headers", "etag")
      1 * headers.add("Access-Control-Allow-Origin", "*")
      0 * _
  }

  // this is a fragile test and i don't like it
  def "If it is a preflight request, we should get a full set of headers back"() {
    given: "i have headers"
      def headers = Mock(type: new TypeToken<MultivaluedMap<String, Object>>(){}.type) as MultivaluedMap<String, Object>
    when:
      filter.filter(request, response)
    then:
      2 * request.getHeaderString("Origin") >> "Origin"
      1 * request.getMethod() >> "OPTIONS"
      5 * response.headers >> headers
      1 * headers.add("Access-Control-Expose-Headers", "etag")
      1 * headers.add("Access-Control-Allow-Origin", "*")
      1 * headers.add("Access-Control-Allow-Credentials", "true")
      1 * headers.add("Access-Control-Allow-Methods",
        "GET, POST, PUT, DELETE, OPTIONS, HEAD")
      1 * headers.addAll("Access-Control-Allow-Headers", { Object[] val -> val.length == 1 && val[0].toString().contains("etag") })
      0 * _
  }
}

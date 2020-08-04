package io.featurehub.client

import io.featurehub.client.interceptor.OpenTracingValueInterceptor
import io.opentracing.Span
import io.opentracing.Tracer
import spock.lang.Specification

class OpenTracingSpec extends Specification {
  def "without the system property, there is no feature override"() {
    given: "i have a mock Tracer"
      Tracer tracer = Mock(Tracer)
    and: "i remove the system property"
      System.clearProperty(OpenTracingValueInterceptor.FEATUREHUB_OPENTRACING_ENABLED)
    and: "i register this"
      def intercept = new OpenTracingValueInterceptor()
      intercept.tracer = tracer
    when: "i create a span and hold onto the baggage"
      def span = Mock(Span)
      tracer.activeSpan() >> span
      span.getBaggageItem(OpenTracingValueInterceptor.FEATUREHUB_OPENTRACING_BAGGAGE_PREFIX + "fish") >> "true"
    then: "i expect it to be null"
      intercept.getValue("fish") == null
  }

  def "with the system property, there is a feature override"() {
    given: "i have a mock Tracer"
      Tracer tracer = Mock(Tracer)
    and: "i remove the system property"
      System.setProperty(OpenTracingValueInterceptor.FEATUREHUB_OPENTRACING_ENABLED, "true")
    and: "i register this"
      def intercept = new OpenTracingValueInterceptor()
      intercept.tracer = tracer
    when: "i create a span and hold onto the baggage"
      def span = Mock(Span)
      tracer.activeSpan() >> span
      span.getBaggageItem(OpenTracingValueInterceptor.FEATUREHUB_OPENTRACING_BAGGAGE_PREFIX + "fish") >> "true"
    then: "i expect it to be null"
      intercept.getValue("fish").matched
      intercept.getValue("fish").value == "true"
  }
}

package io.featurehub.utils

import org.glassfish.jersey.internal.inject.Injectee
import org.glassfish.jersey.internal.inject.InjectionResolver
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Constructor


@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FeatureHubConfig(val source: String)

class ConfigInjectionResolver : InjectionResolver<FeatureHubConfig> {
  override fun resolve(injectee: Injectee): Any? {
    // should be able to handle almost anything
    if (String::class.java == injectee.requiredType) {
      val elem: AnnotatedElement = injectee.parent

      return if (elem is Constructor<*>) {
        val ctor: Constructor<*> = elem
        val config: FeatureHubConfig = ctor.parameterAnnotations[injectee.position][0] as FeatureHubConfig
        val prop = FallbackPropertyConfig.getConfig(config.source)

        return prop
      } else {
        throw RuntimeException("You should not be using this outside config injection")
      }
    }

    return null
  }

  override fun isConstructorParameterIndicator(): Boolean = true

  override fun isMethodParameterIndicator(): Boolean = false

  override fun getAnnotation(): Class<FeatureHubConfig> {
    return FeatureHubConfig::class.java
  }
}

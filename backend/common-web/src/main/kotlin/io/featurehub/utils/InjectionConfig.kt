package io.featurehub.utils

import org.glassfish.jersey.internal.inject.Injectee
import org.glassfish.jersey.internal.inject.InjectionResolver
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Constructor


/**
 * You should only use this field where the value is required to be provided.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FeatureHubConfig(val source: String, val required: Boolean = false)

class ConfigInjectionResolver : InjectionResolver<FeatureHubConfig> {
  override fun resolve(injectee: Injectee): Any? {
    val elem: AnnotatedElement = injectee.parent

    return if (elem is Constructor<*>) {
      val ctor: Constructor<*> = elem
      val config: FeatureHubConfig = ctor.parameterAnnotations[injectee.position][0] as FeatureHubConfig
      val prop = FallbackPropertyConfig.getConfig(config.source)

      if (prop == null) {
        if (config.required) {
          throw RuntimeException("Property ${config.source} does not have a value and must provide one")
        }
        return null
      } else if (String::class.java == injectee.requiredType) {
        return prop
      } else if (Boolean::class.java == injectee.requiredType) {
        return java.lang.Boolean.parseBoolean(prop)
      } else if (Integer::class.java == injectee.requiredType) {
        return Integer.parseInt(prop)
      } else if (Double::class.java == injectee.requiredType) {
        return java.lang.Double.parseDouble(prop)
      } else {
        throw RuntimeException("Don't know how to parse string to " + injectee.requiredType.typeName)
      }
    } else {
      throw RuntimeException("You should not be using this outside config injection")
    }
  }

  override fun isConstructorParameterIndicator(): Boolean = true

  override fun isMethodParameterIndicator(): Boolean = false

  override fun getAnnotation(): Class<FeatureHubConfig> {
    return FeatureHubConfig::class.java
  }
}

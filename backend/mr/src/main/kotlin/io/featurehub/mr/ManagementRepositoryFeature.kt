package io.featurehub.mr

import io.featurehub.app.db.utils.CommonDbFeature
import io.featurehub.db.utils.ApiToSqlApiBinder
import io.featurehub.db.utils.ComplexUpdateMigrations
import io.featurehub.messaging.MessagingFeature
import io.featurehub.mr.api.*
import io.featurehub.mr.auth.*
import io.featurehub.mr.dacha2.Dacha2Feature
import io.featurehub.mr.events.EventingFeature
import io.featurehub.mr.resources.*
import io.featurehub.mr.resources.oauth2.OAuth2MRAdapter
import io.featurehub.mr.utils.ApplicationUtils
import io.featurehub.mr.utils.ConfigurationUtils
import io.featurehub.mr.utils.PortfolioUtils
import io.featurehub.mr.webhook.ManagementRepositoryWebhookFeature
import io.featurehub.rest.CacheControlFilter
import io.featurehub.rest.CorsFilter
import io.featurehub.web.security.oauth.*
import io.featurehub.web.security.oauth.OAuth2Feature.Companion.oauth2ProvidersExist
import io.featurehub.web.security.saml.SamlEnvironmentalFeature
import io.featurehub.web.security.saml.SamlEnvironmentalFeature.Companion.samlProvidersExist
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class ManagementRepositoryFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    listOf( //ValidationFeature.class,
      ApplicationServiceDelegator::class.java,
      FeatureHistoryServiceDelegator::class.java,
      WebhookServiceDelegator::class.java,
      AuthServiceDelegator::class.java,
      EnvironmentFeatureServiceDelegator::class.java,
      EnvironmentServiceDelegator::class.java,
      Environment2ServiceDelegator::class.java,
      FeatureServiceDelegator::class.java,
      GroupServiceDelegator::class.java,
      PersonServiceDelegator::class.java,
      PortfolioServiceDelegator::class.java,
      ServiceAccountServiceDelegator::class.java,
      SetupServiceDelegator::class.java,
      UserStateServiceDelegator::class.java,
      RolloutStrategyServiceDelegator::class.java,
      CorsFilter::class.java,
      CacheControlFilter::class.java,  //      ConstraintExceptionHandler.class,
      AuthApplicationEventListener::class.java,
      Dacha2Feature::class.java,
      OAuth2Feature::class.java,
      SamlEnvironmentalFeature::class.java,
      AuthProvidersFeature::class.java
    ).forEach { componentClass: Class<out Any?>? -> context.register(componentClass) }

    if (ConfigurationUtils.dacha1Enabled) {
      context.register(CacheServiceDelegator::class.java)
    }

    // only mount the dacha2 endpoints on the public API if the keys exist to protect it.
    if (Dacha2Feature.dacha2ApiKeysExist()) {
      context.register(Dacha2Feature::class.java)
    }

    context.register(CommonDbFeature::class.java)
    context.register(ApiToSqlApiBinder())
    context.register(ComplexUpdateMigrations())
    context.register(EventingFeature::class.java)
    context.register(ManagementRepositoryWebhookFeature::class.java)

    context.register(object : AbstractBinder() {
      override fun configure() {
        if (oauth2ProvidersExist() || samlProvidersExist()) {
          bind(AuthProviders::class.java).to(AuthProviderCollection::class.java).`in`(Singleton::class.java)
        } else {
          bind(NoAuthProviders::class.java).to(AuthProviderCollection::class.java).`in`(Singleton::class.java)
        }

        bind(OAuth2MRAdapter::class.java).to(SSOCompletionListener::class.java).`in`(Singleton::class.java)
        bind(DatabaseAuthRepository::class.java).to(AuthenticationRepository::class.java).`in`(
          Singleton::class.java
        )
        bind(PortfolioUtils::class.java).to(PortfolioUtils::class.java).`in`(Singleton::class.java)
        bind(AuthManager::class.java).to(AuthManagerService::class.java).`in`(Singleton::class.java)
        bind(ApplicationResource::class.java).to(ApplicationServiceDelegate::class.java).`in`(
          Singleton::class.java
        )
        if (ConfigurationUtils.dacha1Enabled) {
          bind(CacheResource::class.java).to(CacheServiceDelegate::class.java).`in`(Singleton::class.java)
        }
        bind(AuthResource::class.java).to(AuthServiceDelegate::class.java).`in`(Singleton::class.java)
        bind(WebhookResource::class.java).to(WebhookServiceDelegate::class.java).`in`(Singleton::class.java)
        bind(EnvironmentFeatureResource::class.java).to(
          EnvironmentFeatureServiceDelegate::class.java
        ).`in`(Singleton::class.java)
        bind(UserStateResource::class.java).to(UserStateServiceDelegate::class.java).`in`(
          Singleton::class.java
        )
        bind(EnvironmentResource::class.java).to(EnvironmentServiceDelegate::class.java).`in`(
          Singleton::class.java
        )
        bind(Environment2Resource::class.java).to(Environment2ServiceDelegate::class.java).`in`(
          Singleton::class.java
        )
        bind(FeatureHistoryResource::class.java).to(FeatureHistoryServiceDelegate::class.java).`in`(Singleton::class.java)
        bind(FeatureResource::class.java).to(FeatureServiceDelegate::class.java).`in`(Singleton::class.java)
        bind(GroupResource::class.java).to(GroupServiceDelegate::class.java).`in`(Singleton::class.java)
        bind(PersonResource::class.java).to(PersonServiceDelegate::class.java).`in`(Singleton::class.java)
        bind(PortfolioResource::class.java).to(PortfolioServiceDelegate::class.java).`in`(
          Singleton::class.java
        )
        bind(RolloutStrategyResource::class.java).to(RolloutStrategyServiceDelegate::class.java).`in`(
          Singleton::class.java
        )
        bind(ServiceAccountResource::class.java).to(ServiceAccountServiceDelegate::class.java).`in`(
          Singleton::class.java
        )
        bind(SetupResource::class.java).to(SetupServiceDelegate::class.java).`in`(Singleton::class.java)
        bind(ApplicationUtils::class.java).to(ApplicationUtils::class.java).`in`(Singleton::class.java)
        bind(BlankProviderCollection::class.java).to(SSOProviderCollection::class.java).`in`(
          Singleton::class.java
        )
      }
    })
    return true
  }
}

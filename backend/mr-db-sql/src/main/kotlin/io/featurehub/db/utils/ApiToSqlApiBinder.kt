package io.featurehub.db.utils

import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.ApplicationRolloutStrategyApi
import io.featurehub.db.api.AuthenticationApi
import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.FeatureGroupApi
import io.featurehub.db.api.FeatureHistoryApi
import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.OrganizationApi
import io.featurehub.db.api.PersonApi
import io.featurehub.db.api.PortfolioApi
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.api.ServiceAccountApi
import io.featurehub.db.api.SessionApi
import io.featurehub.db.api.SetupApi
import io.featurehub.db.api.TrackingEventApi
import io.featurehub.db.api.UserStateApi
import io.featurehub.db.api.WebhookApi
import io.featurehub.db.listener.FeatureUpdateBySDKApi
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.db.publish.CacheSourceFeatureGroupSqlApi
import io.featurehub.db.services.ApplicationRolloutStrategySqlApi
import io.featurehub.db.services.ApplicationSqlApi
import io.featurehub.db.services.ArchiveStrategy
import io.featurehub.db.services.AuthenticationSqlApi
import io.featurehub.db.services.Conversions
import io.featurehub.db.services.ConvertUtils
import io.featurehub.db.services.DbArchiveStrategy
import io.featurehub.db.services.EnvironmentSqlApi
import io.featurehub.db.services.FeatureGroupSqlApi
import io.featurehub.db.services.FeatureHistorySqlApi
import io.featurehub.db.services.FeatureSqlApi
import io.featurehub.db.services.GroupSqlApi
import io.featurehub.db.services.InternalFeatureApi
import io.featurehub.db.services.InternalFeatureSqlApi
import io.featurehub.db.services.InternalGroupSqlApi
import io.featurehub.db.services.InternalPersonApi
import io.featurehub.db.services.InternalServiceAccountApi
import io.featurehub.db.services.OrganizationSqlApi
import io.featurehub.db.services.PersonSqlApi
import io.featurehub.db.services.PortfolioSqlApi
import io.featurehub.db.services.ServiceAccountSqlApi
import io.featurehub.db.services.SetupSqlApi
import io.featurehub.db.services.TrackingEventListener
import io.featurehub.db.services.TrackingEventSqlApi
import io.featurehub.db.services.UserStateSqlApi
import io.featurehub.db.services.WebhookSqlApi
import io.featurehub.db.services.strategies.RolloutStrategyValidationUtils
import io.featurehub.lifecycle.LifecycleListeners
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class ApiToSqlApiBinder : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(OrganizationSqlApi::class.java).to(OrganizationApi::class.java).`in`(Singleton::class.java)
        bind(AuthenticationSqlApi::class.java).to(AuthenticationApi::class.java).to(SessionApi::class.java).`in`(
          Singleton::class.java
        )
        bind(SetupSqlApi::class.java).to(SetupApi::class.java).`in`(Singleton::class.java)
        bind(PortfolioSqlApi::class.java).to(PortfolioApi::class.java).`in`(Singleton::class.java)
        bind(GroupSqlApi::class.java).to(GroupApi::class.java).to(InternalGroupSqlApi::class.java).`in`(
          Singleton::class.java
        )
        bind(ConvertUtils::class.java).to(Conversions::class.java).`in`(
          Singleton::class.java
        )
        bind(PersonSqlApi::class.java).to(
          InternalPersonApi::class.java
        ).to(PersonApi::class.java).`in`(Singleton::class.java)
        bind(ServiceAccountSqlApi::class.java).to(InternalServiceAccountApi::class.java).to(
          ServiceAccountApi::class.java
        ).`in`(Singleton::class.java)
        bind(ApplicationSqlApi::class.java).to(ApplicationApi::class.java).`in`(Singleton::class.java)
        bind(EnvironmentSqlApi::class.java).to(EnvironmentApi::class.java).`in`(Singleton::class.java)
        bind(FeatureSqlApi::class.java).to(FeatureApi::class.java).to(FeatureUpdateBySDKApi::class.java).`in`(
          Singleton::class.java
        )
        bind(InternalFeatureSqlApi::class.java).to(InternalFeatureApi::class.java).`in`(Singleton::class.java)
        bind(DbArchiveStrategy::class.java).to(ArchiveStrategy::class.java).`in`(Singleton::class.java)
        bind(UserStateSqlApi::class.java).to(UserStateApi::class.java).`in`(Singleton::class.java)
        bind(RolloutStrategyValidationUtils::class.java).to(RolloutStrategyValidator::class.java).`in`(
          Singleton::class.java
        )
        bind(ApplicationRolloutStrategySqlApi::class.java).to(
          ApplicationRolloutStrategyApi::class.java
        ).`in`(Singleton::class.java)
        bind(WebhookSqlApi::class.java).to(WebhookApi::class.java).`in`(Singleton::class.java)
        bind(FeatureHistorySqlApi::class.java).to(FeatureHistoryApi::class.java).`in`(Singleton::class.java)
        bind(FeatureGroupSqlApi::class.java).to(FeatureGroupApi::class.java).`in`(Singleton::class.java)
        bind(CacheSourceFeatureGroupSqlApi::class.java).to(CacheSourceFeatureGroupApi::class.java).`in`(
          Singleton::class.java
        )
        bind(TrackingEventSqlApi::class.java).to(TrackingEventApi::class.java).`in`(Singleton::class.java)
      }
    })

    LifecycleListeners.wrap(TrackingEventListener::class.java, context)

    return true
  }
}

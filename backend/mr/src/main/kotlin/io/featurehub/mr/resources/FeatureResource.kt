package io.featurehub.mr.resources

import io.featurehub.db.api.*
import io.featurehub.mr.api.FeatureServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.*
import io.featurehub.mr.utils.ApplicationUtils
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.LoggerFactory
import java.util.*

class FeatureResource @Inject constructor(
    private val authManager: AuthManagerService,
    private val applicationApi: ApplicationApi,
    private val applicationUtils: ApplicationUtils,
    private val featureApi: FeatureApi
) : FeatureServiceDelegate {

  override fun createFeaturesForApplication(
    id: UUID,
    feature: CreateFeature,
    holder: FeatureServiceDelegate.CreateFeaturesForApplicationHolder,
    securityContext: SecurityContext?
  ): List<Feature> {
    // here we are only calling it to ensure the security check happens
    val appFeaturePermCheck = applicationUtils.featureCreatorCheck(securityContext!!, id)
    return try {
      applicationApi.createApplicationFeature(
        id,
        feature,
        appFeaturePermCheck.current,
        Opts.empty().add(FillOpts.MetaData, holder.includeMetaData)
      )
    } catch (e: ApplicationApi.DuplicateFeatureException) {
      throw WebApplicationException(409)
    }
  }

  override fun deleteFeatureForApplication(
        id: UUID,
        key: String,
        holder: FeatureServiceDelegate.DeleteFeatureForApplicationHolder,
        securityContext: SecurityContext
    ): List<Feature> {
        // here we are only calling it to ensure the security check happens
        applicationUtils.featureEditorCheck(securityContext, id)
        return applicationApi.deleteApplicationFeature(id, key) ?: throw NotFoundException()
    }

    override fun findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
        id: UUID,
        holder: FeatureServiceDelegate.FindAllFeatureAndFeatureValuesForEnvironmentsByApplicationHolder,
        securityContext: SecurityContext
    ): ApplicationFeatureValues {
        val current = authManager.from(securityContext)
        return featureApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
            id, current, holder.filter,
            holder.max, holder.page, holder.featureTypes, holder.sortOrder, holder.environmentIds
        ) ?: throw NotFoundException()
    }

    override fun getAllFeatureValuesByApplicationForKey(
        id: UUID, key: String,
        securityContext: SecurityContext
    ): List<FeatureEnvironment> {
        return featureApi.getFeatureValuesForApplicationForKeyForPerson(id, key, authManager.from(securityContext))
            ?: throw NotFoundException()
    }

    override fun getFeatureByKey(
        id: UUID,
        key: String,
        holder: FeatureServiceDelegate.GetFeatureByKeyHolder,
        securityContext: SecurityContext
    ): Feature {
        // TODO: permission to read the features
        return applicationApi.getApplicationFeatureByKey(
            id,
            key,
            Opts.empty().add(FillOpts.MetaData, holder.includeMetaData)
        )
            ?: throw NotFoundException()
    }

    override fun getAllFeaturesForApplication(
        id: UUID, holder: FeatureServiceDelegate.GetAllFeaturesForApplicationHolder,
        securityContext: SecurityContext
    ): List<Feature> {
        applicationUtils.featureReadCheck(securityContext, id)
        return applicationApi.getApplicationFeatures(id, Opts.empty().add(FillOpts.MetaData, holder.includeMetaData))
    }

    override fun updateAllFeatureValuesByApplicationForKey(
        id: UUID,
        key: String,
        featureValue: List<FeatureValue>,
        holder: FeatureServiceDelegate.UpdateAllFeatureValuesByApplicationForKeyHolder,
        securityContext: SecurityContext
    ): List<FeatureEnvironment> {
        val person = authManager.from(securityContext)
        try {
            featureApi.updateAllFeatureValuesByApplicationForKey(id, key, featureValue, person)
        } catch (e: OptimisticLockingException) {
            log.warn("Optimistic locking failure", e)
            throw WebApplicationException(409)
        } catch (noAppropriateRole: FeatureApi.NoAppropriateRole) {
            log.warn(
                "User attempted to update feature they had no access to",
                noAppropriateRole
            )
            throw BadRequestException(noAppropriateRole)
        } catch (locked: FeatureApi.LockedException) {
            log.warn("User attempted to change a locked value", locked)
            throw BadRequestException(locked)
        } catch (bad: RolloutStrategyValidator.InvalidStrategyCombination) {
            throw WebApplicationException(
                Response.status(422).entity(bad.failure).build()
            ) // can't do anything with it
        }
        return featureApi.getFeatureValuesForApplicationForKeyForPerson(id, key, person)
            ?: return ArrayList()
    }


  override fun updateFeatureForApplicationOnFeature(
    id: UUID,
    feature: Feature,
    holder: FeatureServiceDelegate.UpdateFeatureForApplicationOnFeatureHolder,
    securityContext: SecurityContext?
  ): List<Feature> {
    applicationUtils.featureEditorCheck(securityContext!!, id)
    return try {
      val features = applicationApi.updateApplicationFeature(
        id,
        feature,
        Opts.empty().add(FillOpts.MetaData, holder.includeMetaData)
      )
        ?: throw NotFoundException("no such feature name")
      features
    } catch (e: ApplicationApi.DuplicateFeatureException) {
      throw WebApplicationException(Response.Status.CONFLICT)
    } catch (e: OptimisticLockingException) {
      throw WebApplicationException(422)
    }
  }

  @Deprecated("Deprecated in Java")
  override fun updateFeatureForApplication(
    id: UUID,
    key: String,
    feature: Feature,
        holder: FeatureServiceDelegate.UpdateFeatureForApplicationHolder,
        securityContext: SecurityContext?
    ): List<Feature> {
        applicationUtils.featureEditorCheck(securityContext!!, id)
        return try {
            val features = applicationApi.updateApplicationFeature(
                id,
                key,
                feature,
                Opts.empty().add(FillOpts.MetaData, holder.includeMetaData)
            )
                ?: throw NotFoundException("no such feature name")
            features
        } catch (e: ApplicationApi.DuplicateFeatureException) {
            throw WebApplicationException(Response.Status.CONFLICT)
        } catch (e: OptimisticLockingException) {
            throw WebApplicationException(422)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(FeatureResource::class.java)
    }
}

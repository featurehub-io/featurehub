package io.featurehub.mr.resources

import io.featurehub.db.api.*
import io.featurehub.mr.api.EnvironmentFeatureServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.EnvironmentFeaturesResult
import io.featurehub.mr.model.FeatureValue
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.LoggerFactory
import java.util.*

class EnvironmentFeatureResource @Inject constructor(
    private val environmentApi: EnvironmentApi,
    private val authManagerService: AuthManagerService,
    private val featureApi: FeatureApi
) : EnvironmentFeatureServiceDelegate {
    private fun requireRoleCheck(eid: UUID, ctx: SecurityContext): PersonFeaturePermission {
        val current = authManagerService.from(ctx)
        val roles = environmentApi.personRoles(current, eid)
        return PersonFeaturePermission.Builder().person(current).appRoles(roles!!.applicationRoles)
            .roles(roles.environmentRoles).build()
    }

    override fun createFeatureForEnvironment(
        eid: UUID, key: String, featureValue: FeatureValue,
        securityContext: SecurityContext
    ): FeatureValue {
        val featureForEnvironment: FeatureValue?
        featureForEnvironment = try {
            featureApi.createFeatureValueForEnvironment(eid, key, featureValue, requireRoleCheck(eid, securityContext))
        } catch (e: OptimisticLockingException) {
            throw WebApplicationException(409)
        } catch (noAppropriateRole: FeatureApi.NoAppropriateRole) {
            throw ForbiddenException(noAppropriateRole)
        } catch (bad: RolloutStrategyValidator.InvalidStrategyCombination) {
            throw WebApplicationException(Response.status(422).entity(bad.failure).build()) // can't do anything with it
        }
        if (featureForEnvironment == null) {
            throw NotFoundException()
        }
        return featureForEnvironment
    }

    override fun deleteFeatureForEnvironment(eid: UUID, key: String, securityContext: SecurityContext): Response {
        return Response.status(400).build()
    }

    override fun getFeatureForEnvironment(
        eid: UUID,
        key: String,
        securityContext: SecurityContext
    ): FeatureValue {
        if (requireRoleCheck(eid, securityContext).hasNoRoles()) {
            throw ForbiddenException()
        }
        return featureApi.getFeatureValueForEnvironment(eid, key)
            ?: throw NotFoundException()
    }

    override fun getFeaturesForEnvironment(
        eid: UUID, holder: EnvironmentFeatureServiceDelegate.GetFeaturesForEnvironmentHolder,
        securityContext: SecurityContext
    ): EnvironmentFeaturesResult {
        if (requireRoleCheck(eid, securityContext).hasNoRoles()) {
            throw ForbiddenException()
        }
        return featureApi.getAllFeatureValuesForEnvironment(
            eid,
            holder.includeFeatures != null && holder.includeFeatures
        ) ?: throw BadRequestException("Not a valid environment id")
    }

    override fun updateAllFeaturesForEnvironment(
        eid: UUID, featureValues: List<FeatureValue>,
        securityContext: SecurityContext
    ): List<FeatureValue> {
        val updated: List<FeatureValue>?
        try {
            updated = featureApi.updateAllFeatureValuesForEnvironment(
                eid, featureValues,
                requireRoleCheck(eid, securityContext)
            )
        } catch (e: OptimisticLockingException) {
            throw WebApplicationException(409)
        } catch (noAppropriateRole: FeatureApi.NoAppropriateRole) {
            throw ForbiddenException(noAppropriateRole)
        } catch (bad: RolloutStrategyValidator.InvalidStrategyCombination) {
            throw WebApplicationException(Response.status(422).entity(bad.failure).build()) // can't do anything with it
        }

        return updated
    }

    override fun updateFeatureForEnvironment(
        eid: UUID, key: String, featureValue: FeatureValue,
        securityContext: SecurityContext
    ): FeatureValue {
        return try {
            featureApi.updateFeatureValueForEnvironment(
                eid,
                key,
                featureValue,
                requireRoleCheck(eid, securityContext)
            )!!
        } catch (e: OptimisticLockingException) {
            log.error("optimistic locking", e)
            throw WebApplicationException(409)
        } catch (noAppropriateRole: FeatureApi.NoAppropriateRole) {
            throw ForbiddenException(noAppropriateRole)
        } catch (bad: RolloutStrategyValidator.InvalidStrategyCombination) {
            throw WebApplicationException(Response.status(422).entity(bad.failure).build()) // can't do anything with it
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(EnvironmentFeatureResource::class.java)
    }
}

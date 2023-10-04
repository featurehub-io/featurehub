package io.featurehub.mr.auth

import io.featurehub.lifecycle.WebBaggageSource
import jakarta.ws.rs.container.ContainerRequestContext


class BaggageSourceAuth: WebBaggageSource {
  override fun sourceBaggage(carrier: ContainerRequestContext): List<Pair<String, String>> {
    carrier.securityContext?.userPrincipal?.let {
      val holder = it as AuthHolder
      holder.person.id?.let {  id ->
        return listOf(Pair("x-fh-uid", id.id.toString()))
      }
    }

    return listOf()
  }
}

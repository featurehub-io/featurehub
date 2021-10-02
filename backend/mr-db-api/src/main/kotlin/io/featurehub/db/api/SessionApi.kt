package io.featurehub.db.api

interface SessionApi {
  fun findSession(token: String): DBLoginSession?

  fun createSession(session: DBLoginSession): DBLoginSession?

  fun invalidateSession(sessionToken: String)
}

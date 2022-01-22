package io.featurehub.db.api

interface SessionApi {
  fun findSession(token: String): DBLoginSession?

  /**
   * Creates the session that has been externally created. The token should be randomised and
   * the person provided.
   */
  fun createSession(session: DBLoginSession): DBLoginSession?

  fun invalidateSession(sessionToken: String)
}

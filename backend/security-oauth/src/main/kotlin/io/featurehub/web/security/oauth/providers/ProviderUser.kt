package io.featurehub.web.security.oauth.providers

class ProviderUser private constructor(builder: Builder) {
  var email: String? = null
  var name: String? = null

  class Builder {
    var email: String? = null
    var name: String? = null
    fun email(`val`: String?): Builder {
      email = `val`
      return this
    }

    fun name(`val`: String?): Builder {
      name = `val`
      return this
    }

    fun build(): ProviderUser {
      return ProviderUser(this)
    }
  }

  init {
    email = builder.email
    name = builder.name
  }
}

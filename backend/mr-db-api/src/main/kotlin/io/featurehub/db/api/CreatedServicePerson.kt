package io.featurehub.db.api

import io.featurehub.mr.model.Person

data class CreatedServicePerson(val person: Person, val token: String)

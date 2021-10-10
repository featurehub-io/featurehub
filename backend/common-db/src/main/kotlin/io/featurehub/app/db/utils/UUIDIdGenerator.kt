package io.featurehub.app.db.utils

import io.ebean.config.IdGenerator
import java.util.*

class UUIDIdGenerator : IdGenerator {
  override fun nextValue(): Any {
    return UUID.randomUUID().toString()
  }

  override fun getName(): String {
    return "uuidStr"
  }
}

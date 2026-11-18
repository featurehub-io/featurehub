package io.featurehub.utils

import java.lang.IllegalArgumentException
import java.util.*

class UuidUtils {
  companion object {
    fun shortUuiid(id: UUID): String {
      val buf = StringBuffer()

      for(c in id.toString().toCharArray()) {
        if (c != '-') {
          buf.append(c)
        }
      }

      return buf.toString()
    }

    fun uuidShort(id: String): UUID {
      if (id.length == 32) {
        return UUID.fromString(id.substring(0,8) + "-" +
          id.substring(8,12) + "-" + id.substring(12,16) + "-" + id.substring(16,20) + "-" + id.substring(20))
      } else if (id.length == 36) {
        return UUID.fromString(id)
      }

      throw IllegalArgumentException("invalid uuid ${id}")
    }
  }
}

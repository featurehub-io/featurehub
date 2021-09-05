package io.featurehub.jersey

import jakarta.ws.rs.ext.ParamConverter
import jakarta.ws.rs.ext.ParamConverterProvider
import jakarta.ws.rs.ext.Provider
import java.lang.reflect.Type
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Provider
class OffsetDateTimeQueryProvider : ParamConverterProvider {
  override fun <T : Any?> getConverter(
    rawType: Class<T>,
    genericType: Type,
    annotations: Array<out Annotation>?
  ): ParamConverter<T?>? {
    if (rawType.name.equals(OffsetDateTime::class.java.name)) {
      return object: ParamConverter<T?> {
        override fun toString(value: T?): String? {
          if (value == null) {
            return null
          }

          return (value as OffsetDateTime).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }

        override fun fromString(value: String?): T? {
          if (value == null) {
            return null;
          }

          return OffsetDateTime.parse(value) as T
        }

      }
    }

    return null;
  }
}

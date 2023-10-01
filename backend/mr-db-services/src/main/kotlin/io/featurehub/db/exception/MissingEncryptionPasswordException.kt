package io.featurehub.db.exception

import jakarta.ws.rs.ClientErrorException

data class MissingEncryptionPasswordException(val statusCode: Int = 422): ClientErrorException(statusCode)

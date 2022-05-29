package io.featurehub.web.security.saml

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

class SamlCertificateConverter(x509Cert: String, x509CertNew: String?, spPrivateKey: String, spPrivateKeyAlgorithm: String) {
  /**
   * Logger.
   */
  private val log: Logger =
    LoggerFactory.getLogger(SamlCertificateConverter::class.java)

  val x509Certificate: X509Certificate
  val x509CertificateNew: X509Certificate?
  val privateKey: PrivateKey

  init {
    val cert = convertBase64EncodedStringToCert(x509Cert)

    x509Certificate = if (cert is X509Certificate) cert else {
      log.error("Certificate `{}` is not a valid X509 certificate", x509Cert)
      throw RuntimeException("Certificate not valid")
    }

    if (x509CertNew != null) {
      val newCert = convertBase64EncodedStringToCert(x509CertNew)
      x509CertificateNew = if (newCert is X509Certificate) cert else {
        log.error("New Certificate `{}` is not a valid X509 certificate", x509Cert)
        throw RuntimeException("Certificate not valid")
      }
    } else {
      x509CertificateNew = null
    }

    val kf = KeyFactory.getInstance(spPrivateKeyAlgorithm)

    val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(spPrivateKey))
    privateKey = kf.generatePrivate(keySpecPKCS8)
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(SamlCertificateConverter::class.java)
    val certFactory = CertificateFactory.getInstance("X.509")

    fun convertBase64EncodedStringToCert(x509Cert: String): Certificate {
      return certFactory.generateCertificate(ByteArrayInputStream(Base64.getDecoder().decode(x509Cert)))
    }
  }
}

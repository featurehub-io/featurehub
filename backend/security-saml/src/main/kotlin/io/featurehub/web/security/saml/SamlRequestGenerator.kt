package io.featurehub.web.security.saml

import com.onelogin.saml2.authn.AuthnRequest
import com.onelogin.saml2.authn.AuthnRequestParams
import com.onelogin.saml2.settings.Saml2Settings
import com.onelogin.saml2.util.Util
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.SignatureException


class SamlRequestGenerator(private val config: SamlServiceProviderConfig) {
  private val log: Logger = LoggerFactory.getLogger(SamlRequestGenerator::class.java)

  /**
   * returns a url that should be sent as a 302 to the browser. We should do this with each config to ensure it can
   * actually be used, probably on startup?
   */
  fun createRequest(): String {
    // get the settings and geneate the request construct
    val settings = config.saml2Settings()
    val req = AuthnRequest(settings, AuthnRequestParams(true, false, true))

    val samlRequest = req.getEncodedAuthnRequest(true)
    // this is the returning url i.e. /sso
    val acsUrl = settings.spAssertionConsumerServiceUrl.toExternalForm()

    val params = if (settings.authnRequestsSigned) {
      val signature = buildSignature(settings, samlRequest, acsUrl, "SAMLRequest")
      "&SigAlg=${enc(settings.signatureAlgorithm)}&Signature=${enc(signature)}"
    } else ""

    val idpUrl = settings.idpSingleSignOnServiceUrl.toExternalForm()
    val sep = if (idpUrl.contains("?")) "&" else "?"
    val samlReq = "${idpUrl}${sep}SAMLRequest=${enc(samlRequest)}&RelayState=${enc(acsUrl)}${params}"
    log.trace("SAML request {}", samlReq)
    return samlReq
  }

  private fun enc(data: String): String {
    return URLEncoder.encode(data, StandardCharsets.UTF_8)
  }

  private fun buildSignature(settings: Saml2Settings, samlMessage: String, relayState: String, type: String): String {
    var signature = ""

    val key: PrivateKey = settings.sPkey
    var msg = type + "=" + Util.urlEncoder(samlMessage)

    if (relayState.isNotEmpty()) {
      msg += "&RelayState=" + Util.urlEncoder(relayState)
    }

    msg += "&SigAlg=" + Util.urlEncoder(settings.signatureAlgorithm)

    try {
      signature = Util.base64encoder(Util.sign(msg, key, settings.signatureAlgorithm))
    } catch (e: InvalidKeyException) {
      log.error("buildSignature error", e)
    } catch (e: NoSuchAlgorithmException) {
      log.error("buildSignature error", e)
    } catch (e: SignatureException) {
      log.error("buildSignature error", e)
    }
    if (signature.isEmpty()) {
      log.error("There was a problem when calculating the Signature of the {}", type)
      throw IllegalArgumentException("bad signature $type")
    }

    log.debug("buildResponseSignature success. --> {}", signature)

    return signature
  }
}

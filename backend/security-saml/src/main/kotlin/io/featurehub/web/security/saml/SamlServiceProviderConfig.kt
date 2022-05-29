package io.featurehub.web.security.saml

import com.onelogin.saml2.settings.Saml2Settings
import java.security.PrivateKey
import java.security.cert.X509Certificate

interface SamlServiceProviderConfig {
  val samlProviderName: String

  /**
   * IDP Entity ID - where their metadata is
   */
  val idpEntityId: String

  /**
   * This is the resolved metadata from the above URL
   */
  val idpMetadataText: String

  /**
   * the url of the REST endpoint that receives the SSO completion
   */
//  val spResponseUrl: String
//  val spEntityId: String
  val spBaseUrl: String

  val spX509Cert: X509Certificate

  val spX509CertNew: X509Certificate?

  val debug: Boolean

  /**
   * # Requires Format PKCS#8   BEGIN PRIVATE KEY
   * # If you have     PKCS#1   BEGIN RSA PRIVATE KEY  convert it by   openssl pkcs8 -topk8 -inform pem -nocrypt -in sp.rsa_key -outform pem -out sp.pem
   */
  val spPrivateKey: PrivateKey

  // the following are required to display this as a valid login option
  val icon: String?
  val buttonBackgroundColor: String?
  val buttonText: String?

  fun saml2Settings(): Saml2Settings {
    return FeatureHubSaml2Settings(this)
  }
}

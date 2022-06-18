package io.featurehub.web.security.saml

import com.onelogin.saml2.settings.IdPMetadataParser
import com.onelogin.saml2.settings.Saml2Settings
import com.onelogin.saml2.settings.SettingsBuilder
import com.onelogin.saml2.util.Constants
import com.onelogin.saml2.util.Util
import org.xml.sax.InputSource
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Translates our settings into ONELOGIN settings
 */
class FeatureHubSaml2Settings(config: SamlServiceProviderConfig) : Saml2Settings() {
  init {
    val idpSettings =
        IdPMetadataParser.parseXML(Util.parseXML(InputSource(config.idpMetadataText.byteInputStream(StandardCharsets.UTF_8))))

    val settingsBuilder = SettingsBuilder().fromValues(idpSettings)
    settingsBuilder.build(this)

    spEntityId = config.spBaseUrl + "/saml/${config.samlProviderName}/metadata"
    spAssertionConsumerServiceUrl = URL(config.spBaseUrl + "/saml/${config.samlProviderName}/sso")
    spNameIDFormat = Constants.NAMEID_EMAIL_ADDRESS

    setSpX509cert(config.spX509Cert)
    setSpX509certNew(config.spX509CertNew)
    setSpPrivateKey(config.spPrivateKey)

    rejectDeprecatedAlg = true
    authnRequestsSigned = true
    wantMessagesSigned = true
    wantAssertionsSigned = false

    setDebug(config.debug)
    uniqueIDPrefix = "FEATUREHUB"
  }
}

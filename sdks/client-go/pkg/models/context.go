package models

import (
	"fmt"
	"net/url"
)

// Context defines metadata for the client:
// This is sent to the FeatureHub server, and powers rollout strategy decisions.
// It can also be applied each time you use a feature (with the Get* methods).
type Context struct {
	Userkey  string                 // Unique key which will be hashed to calculate percentage rollouts
	Session  string                 // Session ID key
	Device   ContextDevice          // [browser, mobile, desktop]
	Platform ContextPlatform        // [linux, windows,	macos, android, ios]
	Country  ContextCountry         // Country / geographic region: https://www.britannica.com/topic/list-of-countries-1993160
	Version  string                 // Version of the client
	Custom   map[string]interface{} // Custom attributes
}

// String concatenates the context and URL encodes it:
func (c *Context) String() string {
	return url.QueryEscape(fmt.Sprintf("userkey=%s,session=%s,device=%s,platform=%s,country=%s,version=%s", c.Userkey, c.Session, c.Device, c.Platform, c.Country, c.Version))
}

// UniqueKey returns our preferred unique key:
func (c *Context) UniqueKey() (string, bool) {
	switch {
	case c == nil:
		return "", false
	case len(c.Session) > 0:
		return c.Session, true
	case len(c.Userkey) > 0:
		return c.Userkey, true
	default:
		return "", false
	}
}

// ContextDevice is the client's device type:
type ContextDevice string

const (
	ContextDeviceBrowser  ContextDevice = "browser"
	ContextDeviceDesktop  ContextDevice = "desktop"
	ContextDeviceEmbedded ContextDevice = "embedded"
	ContextDeviceMobile   ContextDevice = "mobile"
	ContextDeviceServer   ContextDevice = "server"
	ContextDeviceWatch    ContextDevice = "watch"
)

// ContextPlatform is the client's platform (OS):
type ContextPlatform string

const (
	ContextPlatformAndroid ContextPlatform = "android"
	ContextPlatformIos     ContextPlatform = "ios"
	ContextPlatformLinux   ContextPlatform = "linux"
	ContextPlatformMacos   ContextPlatform = "macos"
	ContextPlatformWindows ContextPlatform = "windows"
)

// ContextCountry is the client's country:
type ContextCountry string

const (
	ContextCountryAfghanistan                  ContextCountry = "afghanistan"
	ContextCountryAlbania                      ContextCountry = "albania"
	ContextCountryAlgeria                      ContextCountry = "algeria"
	ContextCountryAndorra                      ContextCountry = "andorra"
	ContextCountryAngola                       ContextCountry = "angola"
	ContextCountryAntiguaAndBarbuda            ContextCountry = "antigua_and_barbuda"
	ContextCountryArgentina                    ContextCountry = "argentina"
	ContextCountryArmenia                      ContextCountry = "armenia"
	ContextCountryAustralia                    ContextCountry = "australia"
	ContextCountryAustria                      ContextCountry = "austria"
	ContextCountryAzerbaijan                   ContextCountry = "azerbaijan"
	ContextCountryTheBahamas                   ContextCountry = "the_bahamas"
	ContextCountryBahrain                      ContextCountry = "bahrain"
	ContextCountryBangladesh                   ContextCountry = "bangladesh"
	ContextCountryBarbados                     ContextCountry = "barbados"
	ContextCountryBelarus                      ContextCountry = "belarus"
	ContextCountryBelgium                      ContextCountry = "belgium"
	ContextCountryBelize                       ContextCountry = "belize"
	ContextCountryBenin                        ContextCountry = "benin"
	ContextCountryBhutan                       ContextCountry = "bhutan"
	ContextCountryBolivia                      ContextCountry = "bolivia"
	ContextCountryBosniaAndHerzegovina         ContextCountry = "bosnia_and_herzegovina"
	ContextCountryBotswana                     ContextCountry = "botswana"
	ContextCountryBrazil                       ContextCountry = "brazil"
	ContextCountryBrunei                       ContextCountry = "brunei"
	ContextCountryBulgaria                     ContextCountry = "bulgaria"
	ContextCountryBurkinaFaso                  ContextCountry = "burkina_faso"
	ContextCountryBurundi                      ContextCountry = "burundi"
	ContextCountryCaboVerde                    ContextCountry = "cabo_verde"
	ContextCountryCambodia                     ContextCountry = "cambodia"
	ContextCountryCameroon                     ContextCountry = "cameroon"
	ContextCountryCanada                       ContextCountry = "canada"
	ContextCountryCentralAfricanRepublic       ContextCountry = "central_african_republic"
	ContextCountryChad                         ContextCountry = "chad"
	ContextCountryChile                        ContextCountry = "chile"
	ContextCountryChina                        ContextCountry = "china"
	ContextCountryColombia                     ContextCountry = "colombia"
	ContextCountryComoros                      ContextCountry = "comoros"
	ContextCountryCongoDemocraticRepublicOfThe ContextCountry = "congo_democratic_republic_of_the"
	ContextCountryCongoRepublicOfThe           ContextCountry = "congo_republic_of_the"
	ContextCountryCostaRica                    ContextCountry = "costa_rica"
	ContextCountryCoteDivoire                  ContextCountry = "cote_divoire"
	ContextCountryCroatia                      ContextCountry = "croatia"
	ContextCountryCuba                         ContextCountry = "cuba"
	ContextCountryCyprus                       ContextCountry = "cyprus"
	ContextCountryCzechRepublic                ContextCountry = "czech_republic"
	ContextCountryDenmark                      ContextCountry = "denmark"
	ContextCountryDjibouti                     ContextCountry = "djibouti"
	ContextCountryDominica                     ContextCountry = "dominica"
	ContextCountryDominicanRepublic            ContextCountry = "dominican_republic"
	ContextCountryEastTimor                    ContextCountry = "east_timor"
	ContextCountryEcuador                      ContextCountry = "ecuador"
	ContextCountryEgypt                        ContextCountry = "egypt"
	ContextCountryElSalvador                   ContextCountry = "el_salvador"
	ContextCountryEquatorialGuinea             ContextCountry = "equatorial_guinea"
	ContextCountryEritrea                      ContextCountry = "eritrea"
	ContextCountryEstonia                      ContextCountry = "estonia"
	ContextCountryEswatini                     ContextCountry = "eswatini"
	ContextCountryEthiopia                     ContextCountry = "ethiopia"
	ContextCountryFiji                         ContextCountry = "fiji"
	ContextCountryFinland                      ContextCountry = "finland"
	ContextCountryFrance                       ContextCountry = "france"
	ContextCountryGabon                        ContextCountry = "gabon"
	ContextCountryTheGambia                    ContextCountry = "the_gambia"
	ContextCountryGeorgia                      ContextCountry = "georgia"
	ContextCountryGermany                      ContextCountry = "germany"
	ContextCountryGhana                        ContextCountry = "ghana"
	ContextCountryGreece                       ContextCountry = "greece"
	ContextCountryGrenada                      ContextCountry = "grenada"
	ContextCountryGuatemala                    ContextCountry = "guatemala"
	ContextCountryGuinea                       ContextCountry = "guinea"
	ContextCountryGuineaBissau                 ContextCountry = "guinea_bissau"
	ContextCountryGuyana                       ContextCountry = "guyana"
	ContextCountryHaiti                        ContextCountry = "haiti"
	ContextCountryHonduras                     ContextCountry = "honduras"
	ContextCountryHungary                      ContextCountry = "hungary"
	ContextCountryIceland                      ContextCountry = "iceland"
	ContextCountryIndia                        ContextCountry = "india"
	ContextCountryIndonesia                    ContextCountry = "indonesia"
	ContextCountryIran                         ContextCountry = "iran"
	ContextCountryIraq                         ContextCountry = "iraq"
	ContextCountryIreland                      ContextCountry = "ireland"
	ContextCountryIsrael                       ContextCountry = "israel"
	ContextCountryItaly                        ContextCountry = "italy"
	ContextCountryJamaica                      ContextCountry = "jamaica"
	ContextCountryJapan                        ContextCountry = "japan"
	ContextCountryJordan                       ContextCountry = "jordan"
	ContextCountryKazakhstan                   ContextCountry = "kazakhstan"
	ContextCountryKenya                        ContextCountry = "kenya"
	ContextCountryKiribati                     ContextCountry = "kiribati"
	ContextCountryKoreaNorth                   ContextCountry = "korea_north"
	ContextCountryKoreaSouth                   ContextCountry = "korea_south"
	ContextCountryKosovo                       ContextCountry = "kosovo"
	ContextCountryKuwait                       ContextCountry = "kuwait"
	ContextCountryKyrgyzstan                   ContextCountry = "kyrgyzstan"
	ContextCountryLaos                         ContextCountry = "laos"
	ContextCountryLatvia                       ContextCountry = "latvia"
	ContextCountryLebanon                      ContextCountry = "lebanon"
	ContextCountryLesotho                      ContextCountry = "lesotho"
	ContextCountryLiberia                      ContextCountry = "liberia"
	ContextCountryLibya                        ContextCountry = "libya"
	ContextCountryLiechtenstein                ContextCountry = "liechtenstein"
	ContextCountryLithuania                    ContextCountry = "lithuania"
	ContextCountryLuxembourg                   ContextCountry = "luxembourg"
	ContextCountryMadagascar                   ContextCountry = "madagascar"
	ContextCountryMalawi                       ContextCountry = "malawi"
	ContextCountryMalaysia                     ContextCountry = "malaysia"
	ContextCountryMaldives                     ContextCountry = "maldives"
	ContextCountryMali                         ContextCountry = "mali"
	ContextCountryMalta                        ContextCountry = "malta"
	ContextCountryMarshallIslands              ContextCountry = "marshall_islands"
	ContextCountryMauritania                   ContextCountry = "mauritania"
	ContextCountryMauritius                    ContextCountry = "mauritius"
	ContextCountryMexico                       ContextCountry = "mexico"
	ContextCountryMicronesiaFederatedStatesOf  ContextCountry = "micronesia_federated_states_of"
	ContextCountryMoldova                      ContextCountry = "moldova"
	ContextCountryMonaco                       ContextCountry = "monaco"
	ContextCountryMongolia                     ContextCountry = "mongolia"
	ContextCountryMontenegro                   ContextCountry = "montenegro"
	ContextCountryMorocco                      ContextCountry = "morocco"
	ContextCountryMozambique                   ContextCountry = "mozambique"
	ContextCountryMyanmar                      ContextCountry = "myanmar"
	ContextCountryNamibia                      ContextCountry = "namibia"
	ContextCountryNauru                        ContextCountry = "nauru"
	ContextCountryNepal                        ContextCountry = "nepal"
	ContextCountryNetherlands                  ContextCountry = "netherlands"
	ContextCountryNewZealand                   ContextCountry = "new_zealand"
	ContextCountryNicaragua                    ContextCountry = "nicaragua"
	ContextCountryNiger                        ContextCountry = "niger"
	ContextCountryNigeria                      ContextCountry = "nigeria"
	ContextCountryNorthMacedonia               ContextCountry = "north_macedonia"
	ContextCountryNorway                       ContextCountry = "norway"
	ContextCountryOman                         ContextCountry = "oman"
	ContextCountryPakistan                     ContextCountry = "pakistan"
	ContextCountryPalau                        ContextCountry = "palau"
	ContextCountryPanama                       ContextCountry = "panama"
	ContextCountryPapuaNewGuinea               ContextCountry = "papua_new_guinea"
	ContextCountryParaguay                     ContextCountry = "paraguay"
	ContextCountryPeru                         ContextCountry = "peru"
	ContextCountryPhilippines                  ContextCountry = "philippines"
	ContextCountryPoland                       ContextCountry = "poland"
	ContextCountryPortugal                     ContextCountry = "portugal"
	ContextCountryQatar                        ContextCountry = "qatar"
	ContextCountryRomania                      ContextCountry = "romania"
	ContextCountryRussia                       ContextCountry = "russia"
	ContextCountryRwanda                       ContextCountry = "rwanda"
	ContextCountrySaintKittsAndNevis           ContextCountry = "saint_kitts_and_nevis"
	ContextCountrySaintLucia                   ContextCountry = "saint_lucia"
	ContextCountrySaintVincentAndTheGrenadines ContextCountry = "saint_vincent_and_the_grenadines"
	ContextCountrySamoa                        ContextCountry = "samoa"
	ContextCountrySanMarino                    ContextCountry = "san_marino"
	ContextCountrySaoTomeAndPrincipe           ContextCountry = "sao_tome_and_principe"
	ContextCountrySaudiArabia                  ContextCountry = "saudi_arabia"
	ContextCountrySenegal                      ContextCountry = "senegal"
	ContextCountrySerbia                       ContextCountry = "serbia"
	ContextCountrySeychelles                   ContextCountry = "seychelles"
	ContextCountrySierraLeone                  ContextCountry = "sierra_leone"
	ContextCountrySingapore                    ContextCountry = "singapore"
	ContextCountrySlovakia                     ContextCountry = "slovakia"
	ContextCountrySlovenia                     ContextCountry = "slovenia"
	ContextCountrySolomonIslands               ContextCountry = "solomon_islands"
	ContextCountrySomalia                      ContextCountry = "somalia"
	ContextCountrySouthAfrica                  ContextCountry = "south_africa"
	ContextCountrySpain                        ContextCountry = "spain"
	ContextCountrySriLanka                     ContextCountry = "sri_lanka"
	ContextCountrySudan                        ContextCountry = "sudan"
	ContextCountrySudanSouth                   ContextCountry = "sudan_south"
	ContextCountrySuriname                     ContextCountry = "suriname"
	ContextCountrySweden                       ContextCountry = "sweden"
	ContextCountrySwitzerland                  ContextCountry = "switzerland"
	ContextCountrySyria                        ContextCountry = "syria"
	ContextCountryTaiwan                       ContextCountry = "taiwan"
	ContextCountryTajikistan                   ContextCountry = "tajikistan"
	ContextCountryTanzania                     ContextCountry = "tanzania"
	ContextCountryThailand                     ContextCountry = "thailand"
	ContextCountryTogo                         ContextCountry = "togo"
	ContextCountryTonga                        ContextCountry = "tonga"
	ContextCountryTrinidadAndTobago            ContextCountry = "trinidad_and_tobago"
	ContextCountryTunisia                      ContextCountry = "tunisia"
	ContextCountryTurkey                       ContextCountry = "turkey"
	ContextCountryTurkmenistan                 ContextCountry = "turkmenistan"
	ContextCountryTuvalu                       ContextCountry = "tuvalu"
	ContextCountryUganda                       ContextCountry = "uganda"
	ContextCountryUkraine                      ContextCountry = "ukraine"
	ContextCountryUnitedArabEmirates           ContextCountry = "united_arab_emirates"
	ContextCountryUnitedKingdom                ContextCountry = "united_kingdom"
	ContextCountryUnitedStates                 ContextCountry = "united_states"
	ContextCountryUruguay                      ContextCountry = "uruguay"
	ContextCountryUzbekistan                   ContextCountry = "uzbekistan"
	ContextCountryVanuatu                      ContextCountry = "vanuatu"
	ContextCountryVaticanCity                  ContextCountry = "vatican_city"
	ContextCountryVenezuela                    ContextCountry = "venezuela"
	ContextCountryVietnam                      ContextCountry = "vietnam"
	ContextCountryYemen                        ContextCountry = "yemen"
	ContextCountryZambia                       ContextCountry = "zambia"
	ContextCountryZimbabwe                     ContextCountry = "zimbabwe"
)

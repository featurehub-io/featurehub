import { FeatureStateUpdate, StrategyAttributeDeviceName, RoleType, StrategyAttributeWellKnownNames, SSEResultState, FeatureState, StrategyAttributeCountryName, RolloutStrategyAttribute, RolloutStrategy, Environment, FeatureValueType, StrategyAttributePlatformName, RolloutStrategyAttributeConditional, RolloutStrategyFieldType } from './';


export class EnvironmentTypeTransformer {
public static toJson(__val: Environment): any {
    const __data: any = {};
    
  if (__val.id !== null && __val.id !== undefined) {
    __data['id'] = __val.id;
  }
  if (__val.features !== null && __val.features !== undefined) {
    __data['features'] = ObjectSerializer.serialize(__val.features, 'Array<FeatureState>');
  }

    return __data;
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): Environment {
    const __init: any = {
          id: __val['id'],
    features: ObjectSerializer.deserialize(__val['features'], 'Array<FeatureState>'),

    };
    
    return new Environment(__init);
  }
}

export class FeatureStateTypeTransformer {
public static toJson(__val: FeatureState): any {
    const __data: any = {};
    
  if (__val.id !== null && __val.id !== undefined) {
    __data['id'] = __val.id;
  }
  if (__val.key !== null && __val.key !== undefined) {
    __data['key'] = __val.key;
  }
  if (__val.l !== null && __val.l !== undefined) {
    __data['l'] = __val.l;
  }
  if (__val.version !== null && __val.version !== undefined) {
    __data['version'] = __val.version;
  }
  if (__val.type !== null && __val.type !== undefined) {
    __data['type'] = ObjectSerializer.serialize(__val.type, 'FeatureValueType');
  }
  if (__val.value !== null && __val.value !== undefined) {
    __data['value'] = __val.value;
  }
  if (__val.environmentId !== null && __val.environmentId !== undefined) {
    __data['environmentId'] = __val.environmentId;
  }
  if (__val.strategies !== null && __val.strategies !== undefined) {
    __data['strategies'] = ObjectSerializer.serialize(__val.strategies, 'Array<RolloutStrategy>');
  }

    return __data;
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): FeatureState {
    const __init: any = {
          id: __val['id'],
    key: __val['key'],
    l: __val['l'],
    version: __val['version'],
    type: ObjectSerializer.deserialize(__val['type'], 'FeatureValueType'),
    value: __val['value'],
    environmentId: __val['environmentId'],
    strategies: ObjectSerializer.deserialize(__val['strategies'], 'Array<RolloutStrategy>'),

    };
    
    return new FeatureState(__init);
  }
}

export class FeatureStateUpdateTypeTransformer {
public static toJson(__val: FeatureStateUpdate): any {
    const __data: any = {};
    
  if (__val.value !== null && __val.value !== undefined) {
    __data['value'] = __val.value;
  }
  if (__val.updateValue !== null && __val.updateValue !== undefined) {
    __data['updateValue'] = __val.updateValue;
  }
  if (__val.lock !== null && __val.lock !== undefined) {
    __data['lock'] = __val.lock;
  }

    return __data;
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): FeatureStateUpdate {
    const __init: any = {
          value: __val['value'],
    updateValue: __val['updateValue'],
    lock: __val['lock'],

    };
    
    return new FeatureStateUpdate(__init);
  }
}

export class FeatureValueTypeTypeTransformer {
public static toJson(__val: FeatureValueType): any {
    return __val?.toString();
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): FeatureValueType {
    if (__val === null || __val === undefined) return undefined;
    switch (__val.toString()) {
        case 'BOOLEAN':
          return FeatureValueType.Boolean;
        case 'STRING':
          return FeatureValueType.String;
        case 'NUMBER':
          return FeatureValueType.Number;
        case 'JSON':
          return FeatureValueType.Json;
    }

    return undefined;
  }
}

export class RoleTypeTypeTransformer {
public static toJson(__val: RoleType): any {
    return __val?.toString();
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): RoleType {
    if (__val === null || __val === undefined) return undefined;
    switch (__val.toString()) {
        case 'READ':
          return RoleType.Read;
        case 'LOCK':
          return RoleType.Lock;
        case 'UNLOCK':
          return RoleType.Unlock;
        case 'CHANGE_VALUE':
          return RoleType.ChangeValue;
    }

    return undefined;
  }
}

export class RolloutStrategyTypeTransformer {
public static toJson(__val: RolloutStrategy): any {
    const __data: any = {};
    
  if (__val.id !== null && __val.id !== undefined) {
    __data['id'] = __val.id;
  }
  if (__val.name !== null && __val.name !== undefined) {
    __data['name'] = __val.name;
  }
  if (__val.percentage !== null && __val.percentage !== undefined) {
    __data['percentage'] = __val.percentage;
  }
  if (__val.percentageAttributes !== null && __val.percentageAttributes !== undefined) {
    __data['percentageAttributes'] = __val.percentageAttributes;
  }
  if (__val.colouring !== null && __val.colouring !== undefined) {
    __data['colouring'] = __val.colouring;
  }
  if (__val.avatar !== null && __val.avatar !== undefined) {
    __data['avatar'] = __val.avatar;
  }
  if (__val.value !== null && __val.value !== undefined) {
    __data['value'] = __val.value;
  }
  if (__val.attributes !== null && __val.attributes !== undefined) {
    __data['attributes'] = ObjectSerializer.serialize(__val.attributes, 'Array<RolloutStrategyAttribute>');
  }

    return __data;
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): RolloutStrategy {
    const __init: any = {
          id: __val['id'],
    name: __val['name'],
    percentage: __val['percentage'],
    percentageAttributes: __val['percentageAttributes'],
    colouring: __val['colouring'],
    avatar: __val['avatar'],
    value: __val['value'],
    attributes: ObjectSerializer.deserialize(__val['attributes'], 'Array<RolloutStrategyAttribute>'),

    };
    
    return new RolloutStrategy(__init);
  }
}

export class RolloutStrategyAttributeTypeTransformer {
public static toJson(__val: RolloutStrategyAttribute): any {
    const __data: any = {};
    
  if (__val.id !== null && __val.id !== undefined) {
    __data['id'] = __val.id;
  }
  if (__val.conditional !== null && __val.conditional !== undefined) {
    __data['conditional'] = ObjectSerializer.serialize(__val.conditional, 'RolloutStrategyAttributeConditional');
  }
  if (__val.fieldName !== null && __val.fieldName !== undefined) {
    __data['fieldName'] = __val.fieldName;
  }
  if (__val.values !== null && __val.values !== undefined) {
    __data['values'] = __val.values;
  }
  if (__val.type !== null && __val.type !== undefined) {
    __data['type'] = ObjectSerializer.serialize(__val.type, 'RolloutStrategyFieldType');
  }

    return __data;
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): RolloutStrategyAttribute {
    const __init: any = {
          id: __val['id'],
    conditional: ObjectSerializer.deserialize(__val['conditional'], 'RolloutStrategyAttributeConditional'),
    fieldName: __val['fieldName'],
    values: __val['values'],
    type: ObjectSerializer.deserialize(__val['type'], 'RolloutStrategyFieldType'),

    };
    
    return new RolloutStrategyAttribute(__init);
  }
}

export class RolloutStrategyAttributeConditionalTypeTransformer {
public static toJson(__val: RolloutStrategyAttributeConditional): any {
    return __val?.toString();
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): RolloutStrategyAttributeConditional {
    if (__val === null || __val === undefined) return undefined;
    switch (__val.toString()) {
        case 'EQUALS':
          return RolloutStrategyAttributeConditional.Equals;
        case 'ENDS_WITH':
          return RolloutStrategyAttributeConditional.EndsWith;
        case 'STARTS_WITH':
          return RolloutStrategyAttributeConditional.StartsWith;
        case 'GREATER':
          return RolloutStrategyAttributeConditional.Greater;
        case 'GREATER_EQUALS':
          return RolloutStrategyAttributeConditional.GreaterEquals;
        case 'LESS':
          return RolloutStrategyAttributeConditional.Less;
        case 'LESS_EQUALS':
          return RolloutStrategyAttributeConditional.LessEquals;
        case 'NOT_EQUALS':
          return RolloutStrategyAttributeConditional.NotEquals;
        case 'INCLUDES':
          return RolloutStrategyAttributeConditional.Includes;
        case 'EXCLUDES':
          return RolloutStrategyAttributeConditional.Excludes;
        case 'REGEX':
          return RolloutStrategyAttributeConditional.Regex;
    }

    return undefined;
  }
}

export class RolloutStrategyFieldTypeTypeTransformer {
public static toJson(__val: RolloutStrategyFieldType): any {
    return __val?.toString();
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): RolloutStrategyFieldType {
    if (__val === null || __val === undefined) return undefined;
    switch (__val.toString()) {
        case 'STRING':
          return RolloutStrategyFieldType.String;
        case 'SEMANTIC_VERSION':
          return RolloutStrategyFieldType.SemanticVersion;
        case 'NUMBER':
          return RolloutStrategyFieldType.Number;
        case 'DATE':
          return RolloutStrategyFieldType.Date;
        case 'DATETIME':
          return RolloutStrategyFieldType.Datetime;
        case 'BOOLEAN':
          return RolloutStrategyFieldType.Boolean;
        case 'IP_ADDRESS':
          return RolloutStrategyFieldType.IpAddress;
    }

    return undefined;
  }
}

export class SSEResultStateTypeTransformer {
public static toJson(__val: SSEResultState): any {
    return __val?.toString();
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): SSEResultState {
    if (__val === null || __val === undefined) return undefined;
    switch (__val.toString()) {
        case 'ack':
          return SSEResultState.Ack;
        case 'bye':
          return SSEResultState.Bye;
        case 'failure':
          return SSEResultState.Failure;
        case 'features':
          return SSEResultState.Features;
        case 'feature':
          return SSEResultState.Feature;
        case 'delete_feature':
          return SSEResultState.DeleteFeature;
    }

    return undefined;
  }
}

export class StrategyAttributeCountryNameTypeTransformer {
public static toJson(__val: StrategyAttributeCountryName): any {
    return __val?.toString();
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): StrategyAttributeCountryName {
    if (__val === null || __val === undefined) return undefined;
    switch (__val.toString()) {
        case 'afghanistan':
          return StrategyAttributeCountryName.Afghanistan;
        case 'albania':
          return StrategyAttributeCountryName.Albania;
        case 'algeria':
          return StrategyAttributeCountryName.Algeria;
        case 'andorra':
          return StrategyAttributeCountryName.Andorra;
        case 'angola':
          return StrategyAttributeCountryName.Angola;
        case 'antigua_and_barbuda':
          return StrategyAttributeCountryName.AntiguaAndBarbuda;
        case 'argentina':
          return StrategyAttributeCountryName.Argentina;
        case 'armenia':
          return StrategyAttributeCountryName.Armenia;
        case 'australia':
          return StrategyAttributeCountryName.Australia;
        case 'austria':
          return StrategyAttributeCountryName.Austria;
        case 'azerbaijan':
          return StrategyAttributeCountryName.Azerbaijan;
        case 'the_bahamas':
          return StrategyAttributeCountryName.TheBahamas;
        case 'bahrain':
          return StrategyAttributeCountryName.Bahrain;
        case 'bangladesh':
          return StrategyAttributeCountryName.Bangladesh;
        case 'barbados':
          return StrategyAttributeCountryName.Barbados;
        case 'belarus':
          return StrategyAttributeCountryName.Belarus;
        case 'belgium':
          return StrategyAttributeCountryName.Belgium;
        case 'belize':
          return StrategyAttributeCountryName.Belize;
        case 'benin':
          return StrategyAttributeCountryName.Benin;
        case 'bhutan':
          return StrategyAttributeCountryName.Bhutan;
        case 'bolivia':
          return StrategyAttributeCountryName.Bolivia;
        case 'bosnia_and_herzegovina':
          return StrategyAttributeCountryName.BosniaAndHerzegovina;
        case 'botswana':
          return StrategyAttributeCountryName.Botswana;
        case 'brazil':
          return StrategyAttributeCountryName.Brazil;
        case 'brunei':
          return StrategyAttributeCountryName.Brunei;
        case 'bulgaria':
          return StrategyAttributeCountryName.Bulgaria;
        case 'burkina_faso':
          return StrategyAttributeCountryName.BurkinaFaso;
        case 'burundi':
          return StrategyAttributeCountryName.Burundi;
        case 'cabo_verde':
          return StrategyAttributeCountryName.CaboVerde;
        case 'cambodia':
          return StrategyAttributeCountryName.Cambodia;
        case 'cameroon':
          return StrategyAttributeCountryName.Cameroon;
        case 'canada':
          return StrategyAttributeCountryName.Canada;
        case 'central_african_republic':
          return StrategyAttributeCountryName.CentralAfricanRepublic;
        case 'chad':
          return StrategyAttributeCountryName.Chad;
        case 'chile':
          return StrategyAttributeCountryName.Chile;
        case 'china':
          return StrategyAttributeCountryName.China;
        case 'colombia':
          return StrategyAttributeCountryName.Colombia;
        case 'comoros':
          return StrategyAttributeCountryName.Comoros;
        case 'congo_democratic_republic_of_the':
          return StrategyAttributeCountryName.CongoDemocraticRepublicOfThe;
        case 'congo_republic_of_the':
          return StrategyAttributeCountryName.CongoRepublicOfThe;
        case 'costa_rica':
          return StrategyAttributeCountryName.CostaRica;
        case 'cote_divoire':
          return StrategyAttributeCountryName.CoteDivoire;
        case 'croatia':
          return StrategyAttributeCountryName.Croatia;
        case 'cuba':
          return StrategyAttributeCountryName.Cuba;
        case 'cyprus':
          return StrategyAttributeCountryName.Cyprus;
        case 'czech_republic':
          return StrategyAttributeCountryName.CzechRepublic;
        case 'denmark':
          return StrategyAttributeCountryName.Denmark;
        case 'djibouti':
          return StrategyAttributeCountryName.Djibouti;
        case 'dominica':
          return StrategyAttributeCountryName.Dominica;
        case 'dominican_republic':
          return StrategyAttributeCountryName.DominicanRepublic;
        case 'east_timor':
          return StrategyAttributeCountryName.EastTimor;
        case 'ecuador':
          return StrategyAttributeCountryName.Ecuador;
        case 'egypt':
          return StrategyAttributeCountryName.Egypt;
        case 'el_salvador':
          return StrategyAttributeCountryName.ElSalvador;
        case 'equatorial_guinea':
          return StrategyAttributeCountryName.EquatorialGuinea;
        case 'eritrea':
          return StrategyAttributeCountryName.Eritrea;
        case 'estonia':
          return StrategyAttributeCountryName.Estonia;
        case 'eswatini':
          return StrategyAttributeCountryName.Eswatini;
        case 'ethiopia':
          return StrategyAttributeCountryName.Ethiopia;
        case 'fiji':
          return StrategyAttributeCountryName.Fiji;
        case 'finland':
          return StrategyAttributeCountryName.Finland;
        case 'france':
          return StrategyAttributeCountryName.France;
        case 'gabon':
          return StrategyAttributeCountryName.Gabon;
        case 'the_gambia':
          return StrategyAttributeCountryName.TheGambia;
        case 'georgia':
          return StrategyAttributeCountryName.Georgia;
        case 'germany':
          return StrategyAttributeCountryName.Germany;
        case 'ghana':
          return StrategyAttributeCountryName.Ghana;
        case 'greece':
          return StrategyAttributeCountryName.Greece;
        case 'grenada':
          return StrategyAttributeCountryName.Grenada;
        case 'guatemala':
          return StrategyAttributeCountryName.Guatemala;
        case 'guinea':
          return StrategyAttributeCountryName.Guinea;
        case 'guinea_bissau':
          return StrategyAttributeCountryName.GuineaBissau;
        case 'guyana':
          return StrategyAttributeCountryName.Guyana;
        case 'haiti':
          return StrategyAttributeCountryName.Haiti;
        case 'honduras':
          return StrategyAttributeCountryName.Honduras;
        case 'hungary':
          return StrategyAttributeCountryName.Hungary;
        case 'iceland':
          return StrategyAttributeCountryName.Iceland;
        case 'india':
          return StrategyAttributeCountryName.India;
        case 'indonesia':
          return StrategyAttributeCountryName.Indonesia;
        case 'iran':
          return StrategyAttributeCountryName.Iran;
        case 'iraq':
          return StrategyAttributeCountryName.Iraq;
        case 'ireland':
          return StrategyAttributeCountryName.Ireland;
        case 'israel':
          return StrategyAttributeCountryName.Israel;
        case 'italy':
          return StrategyAttributeCountryName.Italy;
        case 'jamaica':
          return StrategyAttributeCountryName.Jamaica;
        case 'japan':
          return StrategyAttributeCountryName.Japan;
        case 'jordan':
          return StrategyAttributeCountryName.Jordan;
        case 'kazakhstan':
          return StrategyAttributeCountryName.Kazakhstan;
        case 'kenya':
          return StrategyAttributeCountryName.Kenya;
        case 'kiribati':
          return StrategyAttributeCountryName.Kiribati;
        case 'korea_north':
          return StrategyAttributeCountryName.KoreaNorth;
        case 'korea_south':
          return StrategyAttributeCountryName.KoreaSouth;
        case 'kosovo':
          return StrategyAttributeCountryName.Kosovo;
        case 'kuwait':
          return StrategyAttributeCountryName.Kuwait;
        case 'kyrgyzstan':
          return StrategyAttributeCountryName.Kyrgyzstan;
        case 'laos':
          return StrategyAttributeCountryName.Laos;
        case 'latvia':
          return StrategyAttributeCountryName.Latvia;
        case 'lebanon':
          return StrategyAttributeCountryName.Lebanon;
        case 'lesotho':
          return StrategyAttributeCountryName.Lesotho;
        case 'liberia':
          return StrategyAttributeCountryName.Liberia;
        case 'libya':
          return StrategyAttributeCountryName.Libya;
        case 'liechtenstein':
          return StrategyAttributeCountryName.Liechtenstein;
        case 'lithuania':
          return StrategyAttributeCountryName.Lithuania;
        case 'luxembourg':
          return StrategyAttributeCountryName.Luxembourg;
        case 'madagascar':
          return StrategyAttributeCountryName.Madagascar;
        case 'malawi':
          return StrategyAttributeCountryName.Malawi;
        case 'malaysia':
          return StrategyAttributeCountryName.Malaysia;
        case 'maldives':
          return StrategyAttributeCountryName.Maldives;
        case 'mali':
          return StrategyAttributeCountryName.Mali;
        case 'malta':
          return StrategyAttributeCountryName.Malta;
        case 'marshall_islands':
          return StrategyAttributeCountryName.MarshallIslands;
        case 'mauritania':
          return StrategyAttributeCountryName.Mauritania;
        case 'mauritius':
          return StrategyAttributeCountryName.Mauritius;
        case 'mexico':
          return StrategyAttributeCountryName.Mexico;
        case 'micronesia_federated_states_of':
          return StrategyAttributeCountryName.MicronesiaFederatedStatesOf;
        case 'moldova':
          return StrategyAttributeCountryName.Moldova;
        case 'monaco':
          return StrategyAttributeCountryName.Monaco;
        case 'mongolia':
          return StrategyAttributeCountryName.Mongolia;
        case 'montenegro':
          return StrategyAttributeCountryName.Montenegro;
        case 'morocco':
          return StrategyAttributeCountryName.Morocco;
        case 'mozambique':
          return StrategyAttributeCountryName.Mozambique;
        case 'myanmar':
          return StrategyAttributeCountryName.Myanmar;
        case 'namibia':
          return StrategyAttributeCountryName.Namibia;
        case 'nauru':
          return StrategyAttributeCountryName.Nauru;
        case 'nepal':
          return StrategyAttributeCountryName.Nepal;
        case 'netherlands':
          return StrategyAttributeCountryName.Netherlands;
        case 'new_zealand':
          return StrategyAttributeCountryName.NewZealand;
        case 'nicaragua':
          return StrategyAttributeCountryName.Nicaragua;
        case 'niger':
          return StrategyAttributeCountryName.Niger;
        case 'nigeria':
          return StrategyAttributeCountryName.Nigeria;
        case 'north_macedonia':
          return StrategyAttributeCountryName.NorthMacedonia;
        case 'norway':
          return StrategyAttributeCountryName.Norway;
        case 'oman':
          return StrategyAttributeCountryName.Oman;
        case 'pakistan':
          return StrategyAttributeCountryName.Pakistan;
        case 'palau':
          return StrategyAttributeCountryName.Palau;
        case 'panama':
          return StrategyAttributeCountryName.Panama;
        case 'papua_new_guinea':
          return StrategyAttributeCountryName.PapuaNewGuinea;
        case 'paraguay':
          return StrategyAttributeCountryName.Paraguay;
        case 'peru':
          return StrategyAttributeCountryName.Peru;
        case 'philippines':
          return StrategyAttributeCountryName.Philippines;
        case 'poland':
          return StrategyAttributeCountryName.Poland;
        case 'portugal':
          return StrategyAttributeCountryName.Portugal;
        case 'qatar':
          return StrategyAttributeCountryName.Qatar;
        case 'romania':
          return StrategyAttributeCountryName.Romania;
        case 'russia':
          return StrategyAttributeCountryName.Russia;
        case 'rwanda':
          return StrategyAttributeCountryName.Rwanda;
        case 'saint_kitts_and_nevis':
          return StrategyAttributeCountryName.SaintKittsAndNevis;
        case 'saint_lucia':
          return StrategyAttributeCountryName.SaintLucia;
        case 'saint_vincent_and_the_grenadines':
          return StrategyAttributeCountryName.SaintVincentAndTheGrenadines;
        case 'samoa':
          return StrategyAttributeCountryName.Samoa;
        case 'san_marino':
          return StrategyAttributeCountryName.SanMarino;
        case 'sao_tome_and_principe':
          return StrategyAttributeCountryName.SaoTomeAndPrincipe;
        case 'saudi_arabia':
          return StrategyAttributeCountryName.SaudiArabia;
        case 'senegal':
          return StrategyAttributeCountryName.Senegal;
        case 'serbia':
          return StrategyAttributeCountryName.Serbia;
        case 'seychelles':
          return StrategyAttributeCountryName.Seychelles;
        case 'sierra_leone':
          return StrategyAttributeCountryName.SierraLeone;
        case 'singapore':
          return StrategyAttributeCountryName.Singapore;
        case 'slovakia':
          return StrategyAttributeCountryName.Slovakia;
        case 'slovenia':
          return StrategyAttributeCountryName.Slovenia;
        case 'solomon_islands':
          return StrategyAttributeCountryName.SolomonIslands;
        case 'somalia':
          return StrategyAttributeCountryName.Somalia;
        case 'south_africa':
          return StrategyAttributeCountryName.SouthAfrica;
        case 'spain':
          return StrategyAttributeCountryName.Spain;
        case 'sri_lanka':
          return StrategyAttributeCountryName.SriLanka;
        case 'sudan':
          return StrategyAttributeCountryName.Sudan;
        case 'sudan_south':
          return StrategyAttributeCountryName.SudanSouth;
        case 'suriname':
          return StrategyAttributeCountryName.Suriname;
        case 'sweden':
          return StrategyAttributeCountryName.Sweden;
        case 'switzerland':
          return StrategyAttributeCountryName.Switzerland;
        case 'syria':
          return StrategyAttributeCountryName.Syria;
        case 'taiwan':
          return StrategyAttributeCountryName.Taiwan;
        case 'tajikistan':
          return StrategyAttributeCountryName.Tajikistan;
        case 'tanzania':
          return StrategyAttributeCountryName.Tanzania;
        case 'thailand':
          return StrategyAttributeCountryName.Thailand;
        case 'togo':
          return StrategyAttributeCountryName.Togo;
        case 'tonga':
          return StrategyAttributeCountryName.Tonga;
        case 'trinidad_and_tobago':
          return StrategyAttributeCountryName.TrinidadAndTobago;
        case 'tunisia':
          return StrategyAttributeCountryName.Tunisia;
        case 'turkey':
          return StrategyAttributeCountryName.Turkey;
        case 'turkmenistan':
          return StrategyAttributeCountryName.Turkmenistan;
        case 'tuvalu':
          return StrategyAttributeCountryName.Tuvalu;
        case 'uganda':
          return StrategyAttributeCountryName.Uganda;
        case 'ukraine':
          return StrategyAttributeCountryName.Ukraine;
        case 'united_arab_emirates':
          return StrategyAttributeCountryName.UnitedArabEmirates;
        case 'united_kingdom':
          return StrategyAttributeCountryName.UnitedKingdom;
        case 'united_states':
          return StrategyAttributeCountryName.UnitedStates;
        case 'uruguay':
          return StrategyAttributeCountryName.Uruguay;
        case 'uzbekistan':
          return StrategyAttributeCountryName.Uzbekistan;
        case 'vanuatu':
          return StrategyAttributeCountryName.Vanuatu;
        case 'vatican_city':
          return StrategyAttributeCountryName.VaticanCity;
        case 'venezuela':
          return StrategyAttributeCountryName.Venezuela;
        case 'vietnam':
          return StrategyAttributeCountryName.Vietnam;
        case 'yemen':
          return StrategyAttributeCountryName.Yemen;
        case 'zambia':
          return StrategyAttributeCountryName.Zambia;
        case 'zimbabwe':
          return StrategyAttributeCountryName.Zimbabwe;
    }

    return undefined;
  }
}

export class StrategyAttributeDeviceNameTypeTransformer {
public static toJson(__val: StrategyAttributeDeviceName): any {
    return __val?.toString();
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): StrategyAttributeDeviceName {
    if (__val === null || __val === undefined) return undefined;
    switch (__val.toString()) {
        case 'browser':
          return StrategyAttributeDeviceName.Browser;
        case 'mobile':
          return StrategyAttributeDeviceName.Mobile;
        case 'desktop':
          return StrategyAttributeDeviceName.Desktop;
        case 'server':
          return StrategyAttributeDeviceName.Server;
        case 'watch':
          return StrategyAttributeDeviceName.Watch;
        case 'embedded':
          return StrategyAttributeDeviceName.Embedded;
    }

    return undefined;
  }
}

export class StrategyAttributePlatformNameTypeTransformer {
public static toJson(__val: StrategyAttributePlatformName): any {
    return __val?.toString();
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): StrategyAttributePlatformName {
    if (__val === null || __val === undefined) return undefined;
    switch (__val.toString()) {
        case 'linux':
          return StrategyAttributePlatformName.Linux;
        case 'windows':
          return StrategyAttributePlatformName.Windows;
        case 'macos':
          return StrategyAttributePlatformName.Macos;
        case 'android':
          return StrategyAttributePlatformName.Android;
        case 'ios':
          return StrategyAttributePlatformName.Ios;
    }

    return undefined;
  }
}

export class StrategyAttributeWellKnownNamesTypeTransformer {
public static toJson(__val: StrategyAttributeWellKnownNames): any {
    return __val?.toString();
  }

  // expect this to be a decoded value
public   static fromJson(__val: any): StrategyAttributeWellKnownNames {
    if (__val === null || __val === undefined) return undefined;
    switch (__val.toString()) {
        case 'device':
          return StrategyAttributeWellKnownNames.Device;
        case 'country':
          return StrategyAttributeWellKnownNames.Country;
        case 'platform':
          return StrategyAttributeWellKnownNames.Platform;
        case 'userkey':
          return StrategyAttributeWellKnownNames.Userkey;
        case 'session':
          return StrategyAttributeWellKnownNames.Session;
        case 'version':
          return StrategyAttributeWellKnownNames.Version;
    }

    return undefined;
  }
}


const _regList = new RegExp('^Array\\<(.*)\\>$');
const _regSet = new RegExp('^Set\\<(.*)\\>$');
const _regRecord = new RegExp('^Record\\<string,(.*)\\>$');
const _regMap = new RegExp('^Map\\<string,(.*)\\>$');

const _baseEncoder = (type: string, value: any) => value;
const _dateEncoder = (type: string, value: any) => {
  const val = value as Date;
  return `${val.getFullYear()}-${val.getMonth()}-${val.getDay()}`;
};

export declare type EncoderFunc = (type: string, value: any) => any;

export const serializers: Record<string, EncoderFunc> = {
  'string': _baseEncoder,
  'String': _baseEncoder,
  'email': _baseEncoder,
  'uuid': _baseEncoder,
  'int': _baseEncoder,
  'num': _baseEncoder,
  'number': _baseEncoder,
  'double': _baseEncoder,
  'float': _baseEncoder,
  'boolean': _baseEncoder,
  'object': _baseEncoder,
  'any': _baseEncoder,
  'Array<string>': _baseEncoder,
  'Array<String>': _baseEncoder,
  'Array<email>': _baseEncoder,
  'Array<int>': _baseEncoder,
  'Array<num>': _baseEncoder,
  'Array<number>': _baseEncoder,
  'Array<double>': _baseEncoder,
  'Array<float>': _baseEncoder,
  'Array<boolean>': _baseEncoder,
  'Array<object>': _baseEncoder,
  'Array<any>': _baseEncoder,
  'Date': _dateEncoder,
  'DateTime': (t, value) => (value as Date).toISOString(),
  'Environment': (t, value) => EnvironmentTypeTransformer.toJson(value),
  'FeatureState': (t, value) => FeatureStateTypeTransformer.toJson(value),
  'FeatureStateUpdate': (t, value) => FeatureStateUpdateTypeTransformer.toJson(value),
  'FeatureValueType': (t, value) => FeatureValueTypeTypeTransformer.toJson(value),
  'RoleType': (t, value) => RoleTypeTypeTransformer.toJson(value),
  'RolloutStrategy': (t, value) => RolloutStrategyTypeTransformer.toJson(value),
  'RolloutStrategyAttribute': (t, value) => RolloutStrategyAttributeTypeTransformer.toJson(value),
  'RolloutStrategyAttributeConditional': (t, value) => RolloutStrategyAttributeConditionalTypeTransformer.toJson(value),
  'RolloutStrategyFieldType': (t, value) => RolloutStrategyFieldTypeTypeTransformer.toJson(value),
  'SSEResultState': (t, value) => SSEResultStateTypeTransformer.toJson(value),
  'StrategyAttributeCountryName': (t, value) => StrategyAttributeCountryNameTypeTransformer.toJson(value),
  'StrategyAttributeDeviceName': (t, value) => StrategyAttributeDeviceNameTypeTransformer.toJson(value),
  'StrategyAttributePlatformName': (t, value) => StrategyAttributePlatformNameTypeTransformer.toJson(value),
  'StrategyAttributeWellKnownNames': (t, value) => StrategyAttributeWellKnownNamesTypeTransformer.toJson(value),
};

const _stringDecoder = (type: string, value: any) => value.toString();
const _passthroughDecoder = (type: string, value: any) => value;
const _intDecoder = (type: string, value: any) => (value instanceof Number) ? value.toFixed() : parseInt(value.toString());
const _numDecoder = (type: string, value: any) => (value instanceof Number) ? value : parseFloat(value.toString());
const _dateDecoder = (type: string, value: any) => new Date(`${value}T00:00:00Z`);
const _dateTimeDecoder = (type: string, value: any) => new Date(value.toString());


export const deserializers: Record<string, EncoderFunc> = {
  'string': _stringDecoder,
  'String': _stringDecoder,
  'email': _stringDecoder,
  'uuid': _stringDecoder,
  'int': _intDecoder,
  'num': _numDecoder,
  'number': _numDecoder,
  'double': _numDecoder,
  'float': _numDecoder,
  'boolean': _passthroughDecoder,
  'object': _passthroughDecoder,
  'any': _passthroughDecoder,
  'Date': _dateDecoder,
  'DateTime': _dateTimeDecoder,
  'Environment': (t, value) => EnvironmentTypeTransformer.fromJson(value),
  'FeatureState': (t, value) => FeatureStateTypeTransformer.fromJson(value),
  'FeatureStateUpdate': (t, value) => FeatureStateUpdateTypeTransformer.fromJson(value),
  'FeatureValueType': (t, value) => FeatureValueTypeTypeTransformer.fromJson(value),
  'RoleType': (t, value) => RoleTypeTypeTransformer.fromJson(value),
  'RolloutStrategy': (t, value) => RolloutStrategyTypeTransformer.fromJson(value),
  'RolloutStrategyAttribute': (t, value) => RolloutStrategyAttributeTypeTransformer.fromJson(value),
  'RolloutStrategyAttributeConditional': (t, value) => RolloutStrategyAttributeConditionalTypeTransformer.fromJson(value),
  'RolloutStrategyFieldType': (t, value) => RolloutStrategyFieldTypeTypeTransformer.fromJson(value),
  'SSEResultState': (t, value) => SSEResultStateTypeTransformer.fromJson(value),
  'StrategyAttributeCountryName': (t, value) => StrategyAttributeCountryNameTypeTransformer.fromJson(value),
  'StrategyAttributeDeviceName': (t, value) => StrategyAttributeDeviceNameTypeTransformer.fromJson(value),
  'StrategyAttributePlatformName': (t, value) => StrategyAttributePlatformNameTypeTransformer.fromJson(value),
  'StrategyAttributeWellKnownNames': (t, value) => StrategyAttributeWellKnownNamesTypeTransformer.fromJson(value),
};


export class ObjectSerializer {
	public static deserializeOwn(value: any, innerType: string): any {
		const result: any = {};
		for (let __prop in value) {
			if (value.hasOwnProperty(__prop)) {
				result[__prop] = ObjectSerializer.deserialize(value[__prop], innerType);
			}
		}

    return result;
  }

	public static serializeOwn(value: any, innerType: string): any {
		const result: any = {};
		for (let __prop in value) {
			if (value.hasOwnProperty(__prop)) {
				result[__prop] = ObjectSerializer.serialize(value[__prop], innerType);
			}
		}

    return result;
	}

  public static serialize(value: any, targetType: string): any {
    if (value === null || value === undefined) {
      return undefined;
    }

    const encoder = serializers[targetType];
    if (encoder) {
      return encoder(targetType, value);
    }

    var match: any;
    if (((match = targetType.match(_regRecord)) !== null) && match.length === 2) {
      return ObjectSerializer.serializeOwn(value, match[1].trim());
    } else if ((value instanceof Array) &&
        ((match = targetType.match(_regList)) !== null) && match.length === 2) {
      return value.map((v) => ObjectSerializer.serialize(v, match[1]));
    } else if ((value instanceof Array) &&
        ((match = targetType.match(_regSet)) !== null) && match.length === 2) {
      return new Set(value.map((v) => ObjectSerializer.serialize(v, match[1])));
    } else if ((value instanceof Set) &&
    ((match = targetType.match(_regSet)) !== null) && match.length === 2) {
      return Array.from(value).map((v) => ObjectSerializer.serialize(v, match[1]));
    } else if (value instanceof Map && ((match = targetType.match(_regMap)) !== null) && match.length === 2) {
      return new Map(Array.from(value, ([k, v]) => [k, ObjectSerializer.serialize(v, match[1])]));
    }

    return undefined;
  }

  public static deserialize(value: any, targetType: string): any {
    if (value === null || value === undefined) return null; // 204
    if (targetType === null || targetType === undefined) return value.toString(); // best guess

    const decoder = deserializers[targetType];
    if (decoder) {
      return decoder(targetType, value);
    }

    var match: any;
    if (((match = targetType.match(_regRecord)) !== null) && match.length === 2) { // is an array we want an array
      return ObjectSerializer.deserializeOwn(value, match[1].trim());
    } else if ((value instanceof Array) &&
        ((match = targetType.match(_regList)) !== null) && match.length === 2) {
      return value.map((v) => ObjectSerializer.deserialize(v, match[1]));
    } else if ((value instanceof Array) && // is a array we want a set
        ((match = targetType.match(_regSet)) !== null) && match.length === 2) {
      return value.map((v) => ObjectSerializer.deserialize(v, match[1]));
    } else if ((value instanceof Set) && // is a set we want a set
        ((match = targetType.match(_regSet)) !== null) && match.length === 2) {
      return new Set(Array.from(value).map((v) => ObjectSerializer.deserialize(v, match[1])));
    } else if (value instanceof Map && ((match = targetType.match(_regMap)[1]) !== null) && match.length === 2) {
      return new Map(Array.from(value, ([k, v]) => [k, ObjectSerializer.deserialize(v, match[1])]));
    }

    return value;
  } // deserialize
} // end of serializer
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ObjectSerializer = exports.deserializers = exports.serializers = exports.StrategyAttributeWellKnownNamesTypeTransformer = exports.StrategyAttributePlatformNameTypeTransformer = exports.StrategyAttributeDeviceNameTypeTransformer = exports.StrategyAttributeCountryNameTypeTransformer = exports.SSEResultStateTypeTransformer = exports.RolloutStrategyFieldTypeTypeTransformer = exports.RolloutStrategyAttributeConditionalTypeTransformer = exports.RolloutStrategyAttributeTypeTransformer = exports.RolloutStrategyTypeTransformer = exports.RoleTypeTypeTransformer = exports.FeatureValueTypeTypeTransformer = exports.FeatureStateUpdateTypeTransformer = exports.FeatureStateTypeTransformer = exports.EnvironmentTypeTransformer = void 0;
const _1 = require("./");
class EnvironmentTypeTransformer {
    static toJson(__val) {
        const __data = {};
        if (__val.id !== null && __val.id !== undefined) {
            __data['id'] = __val.id;
        }
        if (__val.features !== null && __val.features !== undefined) {
            __data['features'] = ObjectSerializer.serialize(__val.features, 'Array<FeatureState>');
        }
        return __data;
    }
    // expect this to be a decoded value
    static fromJson(__val) {
        const __init = {
            id: __val['id'],
            features: ObjectSerializer.deserialize(__val['features'], 'Array<FeatureState>'),
        };
        return new _1.Environment(__init);
    }
}
exports.EnvironmentTypeTransformer = EnvironmentTypeTransformer;
class FeatureStateTypeTransformer {
    static toJson(__val) {
        const __data = {};
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
    static fromJson(__val) {
        const __init = {
            id: __val['id'],
            key: __val['key'],
            l: __val['l'],
            version: __val['version'],
            type: ObjectSerializer.deserialize(__val['type'], 'FeatureValueType'),
            value: __val['value'],
            environmentId: __val['environmentId'],
            strategies: ObjectSerializer.deserialize(__val['strategies'], 'Array<RolloutStrategy>'),
        };
        return new _1.FeatureState(__init);
    }
}
exports.FeatureStateTypeTransformer = FeatureStateTypeTransformer;
class FeatureStateUpdateTypeTransformer {
    static toJson(__val) {
        const __data = {};
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
    static fromJson(__val) {
        const __init = {
            value: __val['value'],
            updateValue: __val['updateValue'],
            lock: __val['lock'],
        };
        return new _1.FeatureStateUpdate(__init);
    }
}
exports.FeatureStateUpdateTypeTransformer = FeatureStateUpdateTypeTransformer;
class FeatureValueTypeTypeTransformer {
    static toJson(__val) {
        return __val === null || __val === void 0 ? void 0 : __val.toString();
    }
    // expect this to be a decoded value
    static fromJson(__val) {
        if (__val === null || __val === undefined)
            return undefined;
        switch (__val.toString()) {
            case 'BOOLEAN':
                return _1.FeatureValueType.Boolean;
            case 'STRING':
                return _1.FeatureValueType.String;
            case 'NUMBER':
                return _1.FeatureValueType.Number;
            case 'JSON':
                return _1.FeatureValueType.Json;
        }
        return undefined;
    }
}
exports.FeatureValueTypeTypeTransformer = FeatureValueTypeTypeTransformer;
class RoleTypeTypeTransformer {
    static toJson(__val) {
        return __val === null || __val === void 0 ? void 0 : __val.toString();
    }
    // expect this to be a decoded value
    static fromJson(__val) {
        if (__val === null || __val === undefined)
            return undefined;
        switch (__val.toString()) {
            case 'READ':
                return _1.RoleType.Read;
            case 'LOCK':
                return _1.RoleType.Lock;
            case 'UNLOCK':
                return _1.RoleType.Unlock;
            case 'CHANGE_VALUE':
                return _1.RoleType.ChangeValue;
        }
        return undefined;
    }
}
exports.RoleTypeTypeTransformer = RoleTypeTypeTransformer;
class RolloutStrategyTypeTransformer {
    static toJson(__val) {
        const __data = {};
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
    static fromJson(__val) {
        const __init = {
            id: __val['id'],
            name: __val['name'],
            percentage: __val['percentage'],
            percentageAttributes: __val['percentageAttributes'],
            colouring: __val['colouring'],
            avatar: __val['avatar'],
            value: __val['value'],
            attributes: ObjectSerializer.deserialize(__val['attributes'], 'Array<RolloutStrategyAttribute>'),
        };
        return new _1.RolloutStrategy(__init);
    }
}
exports.RolloutStrategyTypeTransformer = RolloutStrategyTypeTransformer;
class RolloutStrategyAttributeTypeTransformer {
    static toJson(__val) {
        const __data = {};
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
    static fromJson(__val) {
        const __init = {
            id: __val['id'],
            conditional: ObjectSerializer.deserialize(__val['conditional'], 'RolloutStrategyAttributeConditional'),
            fieldName: __val['fieldName'],
            values: __val['values'],
            type: ObjectSerializer.deserialize(__val['type'], 'RolloutStrategyFieldType'),
        };
        return new _1.RolloutStrategyAttribute(__init);
    }
}
exports.RolloutStrategyAttributeTypeTransformer = RolloutStrategyAttributeTypeTransformer;
class RolloutStrategyAttributeConditionalTypeTransformer {
    static toJson(__val) {
        return __val === null || __val === void 0 ? void 0 : __val.toString();
    }
    // expect this to be a decoded value
    static fromJson(__val) {
        if (__val === null || __val === undefined)
            return undefined;
        switch (__val.toString()) {
            case 'EQUALS':
                return _1.RolloutStrategyAttributeConditional.Equals;
            case 'ENDS_WITH':
                return _1.RolloutStrategyAttributeConditional.EndsWith;
            case 'STARTS_WITH':
                return _1.RolloutStrategyAttributeConditional.StartsWith;
            case 'GREATER':
                return _1.RolloutStrategyAttributeConditional.Greater;
            case 'GREATER_EQUALS':
                return _1.RolloutStrategyAttributeConditional.GreaterEquals;
            case 'LESS':
                return _1.RolloutStrategyAttributeConditional.Less;
            case 'LESS_EQUALS':
                return _1.RolloutStrategyAttributeConditional.LessEquals;
            case 'NOT_EQUALS':
                return _1.RolloutStrategyAttributeConditional.NotEquals;
            case 'INCLUDES':
                return _1.RolloutStrategyAttributeConditional.Includes;
            case 'EXCLUDES':
                return _1.RolloutStrategyAttributeConditional.Excludes;
            case 'REGEX':
                return _1.RolloutStrategyAttributeConditional.Regex;
        }
        return undefined;
    }
}
exports.RolloutStrategyAttributeConditionalTypeTransformer = RolloutStrategyAttributeConditionalTypeTransformer;
class RolloutStrategyFieldTypeTypeTransformer {
    static toJson(__val) {
        return __val === null || __val === void 0 ? void 0 : __val.toString();
    }
    // expect this to be a decoded value
    static fromJson(__val) {
        if (__val === null || __val === undefined)
            return undefined;
        switch (__val.toString()) {
            case 'STRING':
                return _1.RolloutStrategyFieldType.String;
            case 'SEMANTIC_VERSION':
                return _1.RolloutStrategyFieldType.SemanticVersion;
            case 'NUMBER':
                return _1.RolloutStrategyFieldType.Number;
            case 'DATE':
                return _1.RolloutStrategyFieldType.Date;
            case 'DATETIME':
                return _1.RolloutStrategyFieldType.Datetime;
            case 'BOOLEAN':
                return _1.RolloutStrategyFieldType.Boolean;
            case 'IP_ADDRESS':
                return _1.RolloutStrategyFieldType.IpAddress;
        }
        return undefined;
    }
}
exports.RolloutStrategyFieldTypeTypeTransformer = RolloutStrategyFieldTypeTypeTransformer;
class SSEResultStateTypeTransformer {
    static toJson(__val) {
        return __val === null || __val === void 0 ? void 0 : __val.toString();
    }
    // expect this to be a decoded value
    static fromJson(__val) {
        if (__val === null || __val === undefined)
            return undefined;
        switch (__val.toString()) {
            case 'ack':
                return _1.SSEResultState.Ack;
            case 'bye':
                return _1.SSEResultState.Bye;
            case 'failure':
                return _1.SSEResultState.Failure;
            case 'features':
                return _1.SSEResultState.Features;
            case 'feature':
                return _1.SSEResultState.Feature;
            case 'delete_feature':
                return _1.SSEResultState.DeleteFeature;
        }
        return undefined;
    }
}
exports.SSEResultStateTypeTransformer = SSEResultStateTypeTransformer;
class StrategyAttributeCountryNameTypeTransformer {
    static toJson(__val) {
        return __val === null || __val === void 0 ? void 0 : __val.toString();
    }
    // expect this to be a decoded value
    static fromJson(__val) {
        if (__val === null || __val === undefined)
            return undefined;
        switch (__val.toString()) {
            case 'afghanistan':
                return _1.StrategyAttributeCountryName.Afghanistan;
            case 'albania':
                return _1.StrategyAttributeCountryName.Albania;
            case 'algeria':
                return _1.StrategyAttributeCountryName.Algeria;
            case 'andorra':
                return _1.StrategyAttributeCountryName.Andorra;
            case 'angola':
                return _1.StrategyAttributeCountryName.Angola;
            case 'antigua_and_barbuda':
                return _1.StrategyAttributeCountryName.AntiguaAndBarbuda;
            case 'argentina':
                return _1.StrategyAttributeCountryName.Argentina;
            case 'armenia':
                return _1.StrategyAttributeCountryName.Armenia;
            case 'australia':
                return _1.StrategyAttributeCountryName.Australia;
            case 'austria':
                return _1.StrategyAttributeCountryName.Austria;
            case 'azerbaijan':
                return _1.StrategyAttributeCountryName.Azerbaijan;
            case 'the_bahamas':
                return _1.StrategyAttributeCountryName.TheBahamas;
            case 'bahrain':
                return _1.StrategyAttributeCountryName.Bahrain;
            case 'bangladesh':
                return _1.StrategyAttributeCountryName.Bangladesh;
            case 'barbados':
                return _1.StrategyAttributeCountryName.Barbados;
            case 'belarus':
                return _1.StrategyAttributeCountryName.Belarus;
            case 'belgium':
                return _1.StrategyAttributeCountryName.Belgium;
            case 'belize':
                return _1.StrategyAttributeCountryName.Belize;
            case 'benin':
                return _1.StrategyAttributeCountryName.Benin;
            case 'bhutan':
                return _1.StrategyAttributeCountryName.Bhutan;
            case 'bolivia':
                return _1.StrategyAttributeCountryName.Bolivia;
            case 'bosnia_and_herzegovina':
                return _1.StrategyAttributeCountryName.BosniaAndHerzegovina;
            case 'botswana':
                return _1.StrategyAttributeCountryName.Botswana;
            case 'brazil':
                return _1.StrategyAttributeCountryName.Brazil;
            case 'brunei':
                return _1.StrategyAttributeCountryName.Brunei;
            case 'bulgaria':
                return _1.StrategyAttributeCountryName.Bulgaria;
            case 'burkina_faso':
                return _1.StrategyAttributeCountryName.BurkinaFaso;
            case 'burundi':
                return _1.StrategyAttributeCountryName.Burundi;
            case 'cabo_verde':
                return _1.StrategyAttributeCountryName.CaboVerde;
            case 'cambodia':
                return _1.StrategyAttributeCountryName.Cambodia;
            case 'cameroon':
                return _1.StrategyAttributeCountryName.Cameroon;
            case 'canada':
                return _1.StrategyAttributeCountryName.Canada;
            case 'central_african_republic':
                return _1.StrategyAttributeCountryName.CentralAfricanRepublic;
            case 'chad':
                return _1.StrategyAttributeCountryName.Chad;
            case 'chile':
                return _1.StrategyAttributeCountryName.Chile;
            case 'china':
                return _1.StrategyAttributeCountryName.China;
            case 'colombia':
                return _1.StrategyAttributeCountryName.Colombia;
            case 'comoros':
                return _1.StrategyAttributeCountryName.Comoros;
            case 'congo_democratic_republic_of_the':
                return _1.StrategyAttributeCountryName.CongoDemocraticRepublicOfThe;
            case 'congo_republic_of_the':
                return _1.StrategyAttributeCountryName.CongoRepublicOfThe;
            case 'costa_rica':
                return _1.StrategyAttributeCountryName.CostaRica;
            case 'cote_divoire':
                return _1.StrategyAttributeCountryName.CoteDivoire;
            case 'croatia':
                return _1.StrategyAttributeCountryName.Croatia;
            case 'cuba':
                return _1.StrategyAttributeCountryName.Cuba;
            case 'cyprus':
                return _1.StrategyAttributeCountryName.Cyprus;
            case 'czech_republic':
                return _1.StrategyAttributeCountryName.CzechRepublic;
            case 'denmark':
                return _1.StrategyAttributeCountryName.Denmark;
            case 'djibouti':
                return _1.StrategyAttributeCountryName.Djibouti;
            case 'dominica':
                return _1.StrategyAttributeCountryName.Dominica;
            case 'dominican_republic':
                return _1.StrategyAttributeCountryName.DominicanRepublic;
            case 'east_timor':
                return _1.StrategyAttributeCountryName.EastTimor;
            case 'ecuador':
                return _1.StrategyAttributeCountryName.Ecuador;
            case 'egypt':
                return _1.StrategyAttributeCountryName.Egypt;
            case 'el_salvador':
                return _1.StrategyAttributeCountryName.ElSalvador;
            case 'equatorial_guinea':
                return _1.StrategyAttributeCountryName.EquatorialGuinea;
            case 'eritrea':
                return _1.StrategyAttributeCountryName.Eritrea;
            case 'estonia':
                return _1.StrategyAttributeCountryName.Estonia;
            case 'eswatini':
                return _1.StrategyAttributeCountryName.Eswatini;
            case 'ethiopia':
                return _1.StrategyAttributeCountryName.Ethiopia;
            case 'fiji':
                return _1.StrategyAttributeCountryName.Fiji;
            case 'finland':
                return _1.StrategyAttributeCountryName.Finland;
            case 'france':
                return _1.StrategyAttributeCountryName.France;
            case 'gabon':
                return _1.StrategyAttributeCountryName.Gabon;
            case 'the_gambia':
                return _1.StrategyAttributeCountryName.TheGambia;
            case 'georgia':
                return _1.StrategyAttributeCountryName.Georgia;
            case 'germany':
                return _1.StrategyAttributeCountryName.Germany;
            case 'ghana':
                return _1.StrategyAttributeCountryName.Ghana;
            case 'greece':
                return _1.StrategyAttributeCountryName.Greece;
            case 'grenada':
                return _1.StrategyAttributeCountryName.Grenada;
            case 'guatemala':
                return _1.StrategyAttributeCountryName.Guatemala;
            case 'guinea':
                return _1.StrategyAttributeCountryName.Guinea;
            case 'guinea_bissau':
                return _1.StrategyAttributeCountryName.GuineaBissau;
            case 'guyana':
                return _1.StrategyAttributeCountryName.Guyana;
            case 'haiti':
                return _1.StrategyAttributeCountryName.Haiti;
            case 'honduras':
                return _1.StrategyAttributeCountryName.Honduras;
            case 'hungary':
                return _1.StrategyAttributeCountryName.Hungary;
            case 'iceland':
                return _1.StrategyAttributeCountryName.Iceland;
            case 'india':
                return _1.StrategyAttributeCountryName.India;
            case 'indonesia':
                return _1.StrategyAttributeCountryName.Indonesia;
            case 'iran':
                return _1.StrategyAttributeCountryName.Iran;
            case 'iraq':
                return _1.StrategyAttributeCountryName.Iraq;
            case 'ireland':
                return _1.StrategyAttributeCountryName.Ireland;
            case 'israel':
                return _1.StrategyAttributeCountryName.Israel;
            case 'italy':
                return _1.StrategyAttributeCountryName.Italy;
            case 'jamaica':
                return _1.StrategyAttributeCountryName.Jamaica;
            case 'japan':
                return _1.StrategyAttributeCountryName.Japan;
            case 'jordan':
                return _1.StrategyAttributeCountryName.Jordan;
            case 'kazakhstan':
                return _1.StrategyAttributeCountryName.Kazakhstan;
            case 'kenya':
                return _1.StrategyAttributeCountryName.Kenya;
            case 'kiribati':
                return _1.StrategyAttributeCountryName.Kiribati;
            case 'korea_north':
                return _1.StrategyAttributeCountryName.KoreaNorth;
            case 'korea_south':
                return _1.StrategyAttributeCountryName.KoreaSouth;
            case 'kosovo':
                return _1.StrategyAttributeCountryName.Kosovo;
            case 'kuwait':
                return _1.StrategyAttributeCountryName.Kuwait;
            case 'kyrgyzstan':
                return _1.StrategyAttributeCountryName.Kyrgyzstan;
            case 'laos':
                return _1.StrategyAttributeCountryName.Laos;
            case 'latvia':
                return _1.StrategyAttributeCountryName.Latvia;
            case 'lebanon':
                return _1.StrategyAttributeCountryName.Lebanon;
            case 'lesotho':
                return _1.StrategyAttributeCountryName.Lesotho;
            case 'liberia':
                return _1.StrategyAttributeCountryName.Liberia;
            case 'libya':
                return _1.StrategyAttributeCountryName.Libya;
            case 'liechtenstein':
                return _1.StrategyAttributeCountryName.Liechtenstein;
            case 'lithuania':
                return _1.StrategyAttributeCountryName.Lithuania;
            case 'luxembourg':
                return _1.StrategyAttributeCountryName.Luxembourg;
            case 'madagascar':
                return _1.StrategyAttributeCountryName.Madagascar;
            case 'malawi':
                return _1.StrategyAttributeCountryName.Malawi;
            case 'malaysia':
                return _1.StrategyAttributeCountryName.Malaysia;
            case 'maldives':
                return _1.StrategyAttributeCountryName.Maldives;
            case 'mali':
                return _1.StrategyAttributeCountryName.Mali;
            case 'malta':
                return _1.StrategyAttributeCountryName.Malta;
            case 'marshall_islands':
                return _1.StrategyAttributeCountryName.MarshallIslands;
            case 'mauritania':
                return _1.StrategyAttributeCountryName.Mauritania;
            case 'mauritius':
                return _1.StrategyAttributeCountryName.Mauritius;
            case 'mexico':
                return _1.StrategyAttributeCountryName.Mexico;
            case 'micronesia_federated_states_of':
                return _1.StrategyAttributeCountryName.MicronesiaFederatedStatesOf;
            case 'moldova':
                return _1.StrategyAttributeCountryName.Moldova;
            case 'monaco':
                return _1.StrategyAttributeCountryName.Monaco;
            case 'mongolia':
                return _1.StrategyAttributeCountryName.Mongolia;
            case 'montenegro':
                return _1.StrategyAttributeCountryName.Montenegro;
            case 'morocco':
                return _1.StrategyAttributeCountryName.Morocco;
            case 'mozambique':
                return _1.StrategyAttributeCountryName.Mozambique;
            case 'myanmar':
                return _1.StrategyAttributeCountryName.Myanmar;
            case 'namibia':
                return _1.StrategyAttributeCountryName.Namibia;
            case 'nauru':
                return _1.StrategyAttributeCountryName.Nauru;
            case 'nepal':
                return _1.StrategyAttributeCountryName.Nepal;
            case 'netherlands':
                return _1.StrategyAttributeCountryName.Netherlands;
            case 'new_zealand':
                return _1.StrategyAttributeCountryName.NewZealand;
            case 'nicaragua':
                return _1.StrategyAttributeCountryName.Nicaragua;
            case 'niger':
                return _1.StrategyAttributeCountryName.Niger;
            case 'nigeria':
                return _1.StrategyAttributeCountryName.Nigeria;
            case 'north_macedonia':
                return _1.StrategyAttributeCountryName.NorthMacedonia;
            case 'norway':
                return _1.StrategyAttributeCountryName.Norway;
            case 'oman':
                return _1.StrategyAttributeCountryName.Oman;
            case 'pakistan':
                return _1.StrategyAttributeCountryName.Pakistan;
            case 'palau':
                return _1.StrategyAttributeCountryName.Palau;
            case 'panama':
                return _1.StrategyAttributeCountryName.Panama;
            case 'papua_new_guinea':
                return _1.StrategyAttributeCountryName.PapuaNewGuinea;
            case 'paraguay':
                return _1.StrategyAttributeCountryName.Paraguay;
            case 'peru':
                return _1.StrategyAttributeCountryName.Peru;
            case 'philippines':
                return _1.StrategyAttributeCountryName.Philippines;
            case 'poland':
                return _1.StrategyAttributeCountryName.Poland;
            case 'portugal':
                return _1.StrategyAttributeCountryName.Portugal;
            case 'qatar':
                return _1.StrategyAttributeCountryName.Qatar;
            case 'romania':
                return _1.StrategyAttributeCountryName.Romania;
            case 'russia':
                return _1.StrategyAttributeCountryName.Russia;
            case 'rwanda':
                return _1.StrategyAttributeCountryName.Rwanda;
            case 'saint_kitts_and_nevis':
                return _1.StrategyAttributeCountryName.SaintKittsAndNevis;
            case 'saint_lucia':
                return _1.StrategyAttributeCountryName.SaintLucia;
            case 'saint_vincent_and_the_grenadines':
                return _1.StrategyAttributeCountryName.SaintVincentAndTheGrenadines;
            case 'samoa':
                return _1.StrategyAttributeCountryName.Samoa;
            case 'san_marino':
                return _1.StrategyAttributeCountryName.SanMarino;
            case 'sao_tome_and_principe':
                return _1.StrategyAttributeCountryName.SaoTomeAndPrincipe;
            case 'saudi_arabia':
                return _1.StrategyAttributeCountryName.SaudiArabia;
            case 'senegal':
                return _1.StrategyAttributeCountryName.Senegal;
            case 'serbia':
                return _1.StrategyAttributeCountryName.Serbia;
            case 'seychelles':
                return _1.StrategyAttributeCountryName.Seychelles;
            case 'sierra_leone':
                return _1.StrategyAttributeCountryName.SierraLeone;
            case 'singapore':
                return _1.StrategyAttributeCountryName.Singapore;
            case 'slovakia':
                return _1.StrategyAttributeCountryName.Slovakia;
            case 'slovenia':
                return _1.StrategyAttributeCountryName.Slovenia;
            case 'solomon_islands':
                return _1.StrategyAttributeCountryName.SolomonIslands;
            case 'somalia':
                return _1.StrategyAttributeCountryName.Somalia;
            case 'south_africa':
                return _1.StrategyAttributeCountryName.SouthAfrica;
            case 'spain':
                return _1.StrategyAttributeCountryName.Spain;
            case 'sri_lanka':
                return _1.StrategyAttributeCountryName.SriLanka;
            case 'sudan':
                return _1.StrategyAttributeCountryName.Sudan;
            case 'sudan_south':
                return _1.StrategyAttributeCountryName.SudanSouth;
            case 'suriname':
                return _1.StrategyAttributeCountryName.Suriname;
            case 'sweden':
                return _1.StrategyAttributeCountryName.Sweden;
            case 'switzerland':
                return _1.StrategyAttributeCountryName.Switzerland;
            case 'syria':
                return _1.StrategyAttributeCountryName.Syria;
            case 'taiwan':
                return _1.StrategyAttributeCountryName.Taiwan;
            case 'tajikistan':
                return _1.StrategyAttributeCountryName.Tajikistan;
            case 'tanzania':
                return _1.StrategyAttributeCountryName.Tanzania;
            case 'thailand':
                return _1.StrategyAttributeCountryName.Thailand;
            case 'togo':
                return _1.StrategyAttributeCountryName.Togo;
            case 'tonga':
                return _1.StrategyAttributeCountryName.Tonga;
            case 'trinidad_and_tobago':
                return _1.StrategyAttributeCountryName.TrinidadAndTobago;
            case 'tunisia':
                return _1.StrategyAttributeCountryName.Tunisia;
            case 'turkey':
                return _1.StrategyAttributeCountryName.Turkey;
            case 'turkmenistan':
                return _1.StrategyAttributeCountryName.Turkmenistan;
            case 'tuvalu':
                return _1.StrategyAttributeCountryName.Tuvalu;
            case 'uganda':
                return _1.StrategyAttributeCountryName.Uganda;
            case 'ukraine':
                return _1.StrategyAttributeCountryName.Ukraine;
            case 'united_arab_emirates':
                return _1.StrategyAttributeCountryName.UnitedArabEmirates;
            case 'united_kingdom':
                return _1.StrategyAttributeCountryName.UnitedKingdom;
            case 'united_states':
                return _1.StrategyAttributeCountryName.UnitedStates;
            case 'uruguay':
                return _1.StrategyAttributeCountryName.Uruguay;
            case 'uzbekistan':
                return _1.StrategyAttributeCountryName.Uzbekistan;
            case 'vanuatu':
                return _1.StrategyAttributeCountryName.Vanuatu;
            case 'vatican_city':
                return _1.StrategyAttributeCountryName.VaticanCity;
            case 'venezuela':
                return _1.StrategyAttributeCountryName.Venezuela;
            case 'vietnam':
                return _1.StrategyAttributeCountryName.Vietnam;
            case 'yemen':
                return _1.StrategyAttributeCountryName.Yemen;
            case 'zambia':
                return _1.StrategyAttributeCountryName.Zambia;
            case 'zimbabwe':
                return _1.StrategyAttributeCountryName.Zimbabwe;
        }
        return undefined;
    }
}
exports.StrategyAttributeCountryNameTypeTransformer = StrategyAttributeCountryNameTypeTransformer;
class StrategyAttributeDeviceNameTypeTransformer {
    static toJson(__val) {
        return __val === null || __val === void 0 ? void 0 : __val.toString();
    }
    // expect this to be a decoded value
    static fromJson(__val) {
        if (__val === null || __val === undefined)
            return undefined;
        switch (__val.toString()) {
            case 'browser':
                return _1.StrategyAttributeDeviceName.Browser;
            case 'mobile':
                return _1.StrategyAttributeDeviceName.Mobile;
            case 'desktop':
                return _1.StrategyAttributeDeviceName.Desktop;
            case 'server':
                return _1.StrategyAttributeDeviceName.Server;
            case 'watch':
                return _1.StrategyAttributeDeviceName.Watch;
            case 'embedded':
                return _1.StrategyAttributeDeviceName.Embedded;
        }
        return undefined;
    }
}
exports.StrategyAttributeDeviceNameTypeTransformer = StrategyAttributeDeviceNameTypeTransformer;
class StrategyAttributePlatformNameTypeTransformer {
    static toJson(__val) {
        return __val === null || __val === void 0 ? void 0 : __val.toString();
    }
    // expect this to be a decoded value
    static fromJson(__val) {
        if (__val === null || __val === undefined)
            return undefined;
        switch (__val.toString()) {
            case 'linux':
                return _1.StrategyAttributePlatformName.Linux;
            case 'windows':
                return _1.StrategyAttributePlatformName.Windows;
            case 'macos':
                return _1.StrategyAttributePlatformName.Macos;
            case 'android':
                return _1.StrategyAttributePlatformName.Android;
            case 'ios':
                return _1.StrategyAttributePlatformName.Ios;
        }
        return undefined;
    }
}
exports.StrategyAttributePlatformNameTypeTransformer = StrategyAttributePlatformNameTypeTransformer;
class StrategyAttributeWellKnownNamesTypeTransformer {
    static toJson(__val) {
        return __val === null || __val === void 0 ? void 0 : __val.toString();
    }
    // expect this to be a decoded value
    static fromJson(__val) {
        if (__val === null || __val === undefined)
            return undefined;
        switch (__val.toString()) {
            case 'device':
                return _1.StrategyAttributeWellKnownNames.Device;
            case 'country':
                return _1.StrategyAttributeWellKnownNames.Country;
            case 'platform':
                return _1.StrategyAttributeWellKnownNames.Platform;
            case 'userkey':
                return _1.StrategyAttributeWellKnownNames.Userkey;
            case 'session':
                return _1.StrategyAttributeWellKnownNames.Session;
            case 'version':
                return _1.StrategyAttributeWellKnownNames.Version;
        }
        return undefined;
    }
}
exports.StrategyAttributeWellKnownNamesTypeTransformer = StrategyAttributeWellKnownNamesTypeTransformer;
const _regList = new RegExp('^Array\\<(.*)\\>$');
const _regSet = new RegExp('^Set\\<(.*)\\>$');
const _regRecord = new RegExp('^Record\\<string,(.*)\\>$');
const _regMap = new RegExp('^Map\\<string,(.*)\\>$');
const _baseEncoder = (type, value) => value;
const _dateEncoder = (type, value) => {
    const val = value;
    return `${val.getFullYear()}-${val.getMonth()}-${val.getDay()}`;
};
exports.serializers = {
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
    'DateTime': (t, value) => value.toISOString(),
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
const _stringDecoder = (type, value) => value.toString();
const _passthroughDecoder = (type, value) => value;
const _intDecoder = (type, value) => (value instanceof Number) ? value.toFixed() : parseInt(value.toString());
const _numDecoder = (type, value) => (value instanceof Number) ? value : parseFloat(value.toString());
const _dateDecoder = (type, value) => new Date(`${value}T00:00:00Z`);
const _dateTimeDecoder = (type, value) => new Date(value.toString());
exports.deserializers = {
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
class ObjectSerializer {
    static deserializeOwn(value, innerType) {
        const result = {};
        for (let __prop in value) {
            if (value.hasOwnProperty(__prop)) {
                result[__prop] = ObjectSerializer.deserialize(value[__prop], innerType);
            }
        }
        return result;
    }
    static serializeOwn(value, innerType) {
        const result = {};
        for (let __prop in value) {
            if (value.hasOwnProperty(__prop)) {
                result[__prop] = ObjectSerializer.serialize(value[__prop], innerType);
            }
        }
        return result;
    }
    static serialize(value, targetType) {
        if (value === null || value === undefined) {
            return undefined;
        }
        const encoder = exports.serializers[targetType];
        if (encoder) {
            return encoder(targetType, value);
        }
        var match;
        if (((match = targetType.match(_regRecord)) !== null) && match.length === 2) {
            return ObjectSerializer.serializeOwn(value, match[1].trim());
        }
        else if ((value instanceof Array) &&
            ((match = targetType.match(_regList)) !== null) && match.length === 2) {
            return value.map((v) => ObjectSerializer.serialize(v, match[1]));
        }
        else if ((value instanceof Array) &&
            ((match = targetType.match(_regSet)) !== null) && match.length === 2) {
            return new Set(value.map((v) => ObjectSerializer.serialize(v, match[1])));
        }
        else if ((value instanceof Set) &&
            ((match = targetType.match(_regSet)) !== null) && match.length === 2) {
            return Array.from(value).map((v) => ObjectSerializer.serialize(v, match[1]));
        }
        else if (value instanceof Map && ((match = targetType.match(_regMap)) !== null) && match.length === 2) {
            return new Map(Array.from(value, ([k, v]) => [k, ObjectSerializer.serialize(v, match[1])]));
        }
        return undefined;
    }
    static deserialize(value, targetType) {
        if (value === null || value === undefined)
            return null; // 204
        if (targetType === null || targetType === undefined)
            return value.toString(); // best guess
        const decoder = exports.deserializers[targetType];
        if (decoder) {
            return decoder(targetType, value);
        }
        var match;
        if (((match = targetType.match(_regRecord)) !== null) && match.length === 2) { // is an array we want an array
            return ObjectSerializer.deserializeOwn(value, match[1].trim());
        }
        else if ((value instanceof Array) &&
            ((match = targetType.match(_regList)) !== null) && match.length === 2) {
            return value.map((v) => ObjectSerializer.deserialize(v, match[1]));
        }
        else if ((value instanceof Array) && // is a array we want a set
            ((match = targetType.match(_regSet)) !== null) && match.length === 2) {
            return value.map((v) => ObjectSerializer.deserialize(v, match[1]));
        }
        else if ((value instanceof Set) && // is a set we want a set
            ((match = targetType.match(_regSet)) !== null) && match.length === 2) {
            return new Set(Array.from(value).map((v) => ObjectSerializer.deserialize(v, match[1])));
        }
        else if (value instanceof Map && ((match = targetType.match(_regMap)[1]) !== null) && match.length === 2) {
            return new Map(Array.from(value, ([k, v]) => [k, ObjectSerializer.deserialize(v, match[1])]));
        }
        return value;
    } // deserialize
} // end of serializer
exports.ObjectSerializer = ObjectSerializer;
//# sourceMappingURL=model_serializer.js.map
import { FeatureStateUpdate, StrategyAttributeDeviceName, RoleType, StrategyAttributeWellKnownNames, SSEResultState, FeatureState, StrategyAttributeCountryName, RolloutStrategyAttribute, RolloutStrategy, Environment, FeatureValueType, StrategyAttributePlatformName, RolloutStrategyAttributeConditional, RolloutStrategyFieldType } from './';
export declare class EnvironmentTypeTransformer {
    static toJson(__val: Environment): any;
    static fromJson(__val: any): Environment;
}
export declare class FeatureStateTypeTransformer {
    static toJson(__val: FeatureState): any;
    static fromJson(__val: any): FeatureState;
}
export declare class FeatureStateUpdateTypeTransformer {
    static toJson(__val: FeatureStateUpdate): any;
    static fromJson(__val: any): FeatureStateUpdate;
}
export declare class FeatureValueTypeTypeTransformer {
    static toJson(__val: FeatureValueType): any;
    static fromJson(__val: any): FeatureValueType;
}
export declare class RoleTypeTypeTransformer {
    static toJson(__val: RoleType): any;
    static fromJson(__val: any): RoleType;
}
export declare class RolloutStrategyTypeTransformer {
    static toJson(__val: RolloutStrategy): any;
    static fromJson(__val: any): RolloutStrategy;
}
export declare class RolloutStrategyAttributeTypeTransformer {
    static toJson(__val: RolloutStrategyAttribute): any;
    static fromJson(__val: any): RolloutStrategyAttribute;
}
export declare class RolloutStrategyAttributeConditionalTypeTransformer {
    static toJson(__val: RolloutStrategyAttributeConditional): any;
    static fromJson(__val: any): RolloutStrategyAttributeConditional;
}
export declare class RolloutStrategyFieldTypeTypeTransformer {
    static toJson(__val: RolloutStrategyFieldType): any;
    static fromJson(__val: any): RolloutStrategyFieldType;
}
export declare class SSEResultStateTypeTransformer {
    static toJson(__val: SSEResultState): any;
    static fromJson(__val: any): SSEResultState;
}
export declare class StrategyAttributeCountryNameTypeTransformer {
    static toJson(__val: StrategyAttributeCountryName): any;
    static fromJson(__val: any): StrategyAttributeCountryName;
}
export declare class StrategyAttributeDeviceNameTypeTransformer {
    static toJson(__val: StrategyAttributeDeviceName): any;
    static fromJson(__val: any): StrategyAttributeDeviceName;
}
export declare class StrategyAttributePlatformNameTypeTransformer {
    static toJson(__val: StrategyAttributePlatformName): any;
    static fromJson(__val: any): StrategyAttributePlatformName;
}
export declare class StrategyAttributeWellKnownNamesTypeTransformer {
    static toJson(__val: StrategyAttributeWellKnownNames): any;
    static fromJson(__val: any): StrategyAttributeWellKnownNames;
}
export declare type EncoderFunc = (type: string, value: any) => any;
export declare const serializers: Record<string, EncoderFunc>;
export declare const deserializers: Record<string, EncoderFunc>;
export declare class ObjectSerializer {
    static deserializeOwn(value: any, innerType: string): any;
    static serializeOwn(value: any, innerType: string): any;
    static serialize(value: any, targetType: string): any;
    static deserialize(value: any, targetType: string): any;
}

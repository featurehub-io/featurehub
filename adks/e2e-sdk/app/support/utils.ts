import {FeatureValueType, RoleType} from "../apis/mr-service";
import {expect} from "chai";
import {logger} from "./logging";
import {When} from "@cucumber/cucumber";
import {SdkWorld} from "./world";


export function decodeAndValidateRoles(roles: string): Array<RoleType> {
  const roleTypes = roles.split(",").map(s => s.trim()).filter(s => s.length > 0);
  // for (let [key, value] of Object.entries(EnumName)) {
  //     console.log(`${key}: ${value}`);
  // }
  if (roleTypes.length === 1 && roleTypes[0].toLowerCase() === "all") {
    return [RoleType.Lock, RoleType.Unlock, RoleType.ChangeValue, RoleType.Read, RoleType.ExtendedData];
  }

  const foundRoles: Array<RoleType> = [];
  for (let [key,value] of Object.entries(RoleType)) {
    const compareWith = value.toString().toLowerCase();
    logger.info(`Comparing ${roleTypes} to ${compareWith}`);
    if (roleTypes.includes(compareWith)) {
      foundRoles.push(value);
    }
  }

  expect(foundRoles.length, `Found unknown role type in ${roleTypes} vs ${foundRoles}`).to.eq(roleTypes.length);
  return foundRoles;
}

export function convertValue(value: string, featureType: FeatureValueType) {
  switch (featureType) {
    case FeatureValueType.Boolean:
      return ('true' === value);
    case FeatureValueType.Number:
      return value == null ? null : parseFloat(value);
    case FeatureValueType.String:
    case FeatureValueType.Json:
      return value;
  }
}


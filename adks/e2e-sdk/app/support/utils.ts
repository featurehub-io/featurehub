import {RoleType} from "../apis/mr-service";
import {expect} from "chai";
import {logger} from "./logging";


export function decodeAndValidateRoles(roles: string): Array<RoleType> {
  const roleTypes = roles.split(",").map(s => s.trim()).filter(s => s.length > 0);
  // for (let [key, value] of Object.entries(EnumName)) {
  //     console.log(`${key}: ${value}`);
  // }
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

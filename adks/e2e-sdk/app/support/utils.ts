import {FeatureValueType, RoleType, RolloutStrategy, RolloutStrategyAttribute} from "../apis/mr-service";
import {expect} from "chai";
import {logger} from "./logging";
import DataTable from "@cucumber/cucumber/lib/models/data_table";
import {attributeType, conditional} from "../steps/feature-groups";


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

export function convertValue(value: string, featureType: FeatureValueType) : string|undefined|number|boolean{
  switch (featureType) {
    case FeatureValueType.Boolean:
      return ('true' === value) || ("on" === value);
    case FeatureValueType.Number:
      return value === undefined ? undefined : parseFloat(value);
    case FeatureValueType.String:
    case FeatureValueType.Json:
      return value;
  }
}


/**
 * it picks up the name, percentage and percentage attributes the first time it encounters a strategy name,
 * and then the attributes from every duplicate name are added in order.
 *
 * @param table
 * @param valueType - the type of the value if provided. only true RolloutStrategies have one (directly attached to a feature value)
 */
export function extractRolloutStrategyFromDataTable(table: DataTable, valueType = FeatureValueType.String): Array<RolloutStrategy> {
  const strategies: Array<RolloutStrategy> = [];

  for (const row of table.hashes()) {
    if (!strategies.find(s => s.name === row['name'])) {
      strategies.push(new RolloutStrategy({
        name: row["name"],
        percentage: (row['percentage']?.trim() || '_') === '_' ? null : parseInt(row['percentage'].trim()),
        percentageAttributes: (row['percentageAttributes']?.trim() || '_') === '_' ? null : row['percentageAttributes'].trim().split(",").map(s => s.trim()),
        attributes: [],
        value: convertValue(row['value'] || undefined, valueType) // only rollout strategies use this, portfolio/app/feature group's don't
      }));
    }
  }
  // now go thru again and add the attributes
  for (const row of table.hashes()) {
    const strat = strategies.find(s => s.name === row['name']);
    if (row['fieldName']?.trim().length > 0 && row['fieldName'].trim() != '_') {
      strat.attributes.push(new RolloutStrategyAttribute({
        fieldName: row['fieldName'].trim(),
        conditional: conditional(row['conditional'].trim()),
        values: row['values'].trim().split(','),
        type: attributeType(row['type'].trim())
      }));
    }
  }

  return strategies;
}

function nullOutIds(strategy: RolloutStrategy) {
  delete (strategy as any)['id'];
  delete (strategy as any)['disabled'];
  if (strategy.percentage === undefined || strategy.percentage === null) {
    delete (strategy as any)['percentage'];
  }
  if (strategy.percentageAttributes === undefined || strategy.percentageAttributes === null)  {
    delete (strategy as any)['percentageAttributes'];
  }
  strategy.id = undefined;
  strategy.attributes?.forEach(a => delete (a as any)['id']);
  return strategy;
}

export function strategyComparison(rs1: Array<any>, rs2: Array<any>): boolean {
  const strategies1 = rs1.map(r => nullOutIds(JSON.parse(JSON.stringify(r))));
  const strategies2 = rs1.map(r => nullOutIds(JSON.parse(JSON.stringify(r))));

  return JSON.stringify(strategies1) === JSON.stringify(strategies2);
}

export function compareStrategies(strategy1: any, strategy2: any) {
  const s1 = JSON.parse(JSON.stringify(strategy1));
  const s2 = JSON.parse(JSON.stringify(strategy2));
  nullOutIds(s1);
  nullOutIds(s2)

  expect(s1, `expected ${JSON.stringify(s1)} to deep equal ${JSON.stringify(s2)}`).to.deep.eq(s2);
}

import {Given, Then, When} from "@cucumber/cucumber";
import {cloudEvents, getWebserverExternalAddress, resetCloudEvents} from "../support/make_me_a_webserver";
import waitForExpect from "wait-for-expect";
import {expect} from "chai";
import {SdkWorld} from "../support/world";
import {
  FeatureValueType,
  RolloutStrategy,
  RolloutStrategyAttribute,
  RolloutStrategyAttributeConditional,
  RolloutStrategyFieldType,
  UpdateEnvironment
} from "../apis/mr-service";
import {sleep} from "../support/random";
import {featurehubCloudEventBodyParser} from "../support/ce_utils";
import {logger} from "../support/logging";
import {CloudEvent} from "cloudevents";
import {FeatureMessagingUpdate, FeatureMessagingUpdateTypeTransformer, StrategyUpdateType} from "../apis/messaging";
import DataTable from "@cucumber/cucumber/lib/models/data_table";
import {compareStrategies, convertValue, extractArray, extractFloat, trimField} from "../support/utils";

When('I clear cloud events', function() {
  resetCloudEvents();
});

function messageDecoder(ce: CloudEvent<any>) {
  const msg = featurehubCloudEventBodyParser<any>(ce);
  if (msg === undefined) {
    return [false, undefined];
  }
}


function extractRolloutStrategyParentFromRow(prefix: string, row: Record<string,string>, featureValueType: FeatureValueType ): RolloutStrategy {
  const assignedValue = trimField(row[`${prefix}-value`]);

  return new RolloutStrategy({
    name: row[`${prefix}-name`],
    percentage: extractFloat(row[`${prefix}-percentage`]),
    percentageAttributes: extractArray(row[`${prefix}-percentage-attributes`]),
    value: convertValue(assignedValue, featureValueType),
    attributes: []
  });
}

function extractRolloutStrategyAttributeFromRow(prefix: string, row: Record<string,string>, rs: RolloutStrategy) {
  // all are required for an attributes
  const fieldName = trimField(row[`${prefix}-fieldName`]);
  const conditional = trimField(row[`${prefix}-conditional`]);
  const values = extractArray(row[`${prefix}-values`]);
  const fieldType = trimField(row[`${prefix}-type`]);
  if (fieldName||conditional||values?.length > 0||fieldType) {
    expect(fieldName).to.not.be.undefined;
    expect(conditional).to.not.be.undefined;
    expect(fieldType).to.not.be.undefined;
    expect(values).to.not.be.undefined;
    expect(values.length).to.be.gt(0);

    const ft = fieldType.toUpperCase() as RolloutStrategyFieldType;
    rs.attributes.push({
      fieldName: fieldName,
      type: ft,
      conditional: conditional.toUpperCase() as RolloutStrategyAttributeConditional,
      values: values
    } as RolloutStrategyAttribute);
  }
}

interface RolloutStrategyUpdate {
  updateType: string;
  name: string;
  previous: RolloutStrategy;
  next: RolloutStrategy;
}

function extractRolloutStrategies(prefix: string, dataTable: DataTable, msg: FeatureMessagingUpdate): Array<RolloutStrategyUpdate> {
  const updated: Array<RolloutStrategyUpdate> = [];

  for(const row of dataTable.hashes()) {
    const name = row[`${prefix}-name`];
    if (name) {
      const updateType = row['previous-or-next'].trim();
      let found = updated.find(p => p.name === name);
      let strategy: RolloutStrategy;
      if (!found || found.updateType !== updateType) {
        strategy = extractRolloutStrategyParentFromRow(prefix, row, msg.featureValueType);
        if (!found) {
          found = { updateType: updateType, name: name  } as RolloutStrategyUpdate;
          updated.push(found);
        }

        if (updateType === 'a' || updateType === 'n') { found.next = strategy}
        if (updateType === 'd' || updateType === 'p') { found.previous = strategy}
      } else {
        if (updateType === 'a' || updateType === 'n') { strategy = found.next }
        if (updateType === 'd' || updateType === 'p') { strategy = found.previous }
      }

      extractRolloutStrategyAttributeFromRow(prefix, row, strategy);
    }
  }

  return updated;
}

function convertUpdateType(updateType: string): StrategyUpdateType|undefined {
  if (updateType === 'a') return StrategyUpdateType.Added;
  if (updateType === 'd') return StrategyUpdateType.Deleted;
  if (updateType === 'p') return StrategyUpdateType.Changed;
  if (updateType === 'n') return StrategyUpdateType.Changed;
}

function evalPortfolioStrategy(dataTable: DataTable, msg: FeatureMessagingUpdate) {
  const strategies = extractRolloutStrategies('portfolio-strategy', dataTable, msg);

  if (strategies.length) {
    expect(strategies.length, `The strategies discovered ${strategies.length} did not match the number sent in  the cloud event ${msg.portfolioStrategiesUpdated?.length}`).to.eq(msg.portfolioStrategiesUpdated?.length || -1);

    msg.portfolioStrategiesUpdated.forEach((val, index) => {
      const found = strategies[index];
      expect(val.updateType).to.eq(convertUpdateType(found.updateType));
      if (val.updateType === StrategyUpdateType.Changed || val.updateType === StrategyUpdateType.Added) {
        expect(val.newStrategy).to.not.be.undefined;
        expect(found.next).to.not.be.undefined;
        compareStrategies(found.next, val.newStrategy);
      }

      if (val.updateType === StrategyUpdateType.Changed || val.updateType === StrategyUpdateType.Deleted) {
        expect(val.oldStrategy).to.not.be.undefined;
        expect(found.previous).to.not.be.undefined;
        compareStrategies(val.oldStrategy, found.previous);
      }
    });
  }

}

function comparFeatureeMessageWithDataTable(msg: FeatureMessagingUpdate, dataTable: DataTable) {
  const rows = dataTable.rows;
  let row: Record<string,string> = {};
  const evalFunc = function(name: string, previous: (val: string) => void, updated: (val: string) => void) {
    if (row[name]?.trim().length > 0) {
      const cback = row[name].trim();
      const isPrevious = row['previous-or-next'] === 'p';
      logger.info(`cback is ${cback}, name is ${name}`);
      if (isPrevious) {
        previous(cback);
      } else {
        updated(cback)
      }
    } else {
      logger.info(`could not find ${name} in ${JSON.stringify(row)}`);
    }

    evalPortfolioStrategy(dataTable, msg);
  }

  for(const myRow of dataTable.hashes()) {
    row = myRow;

    evalFunc('locked', p => {
      expect(msg.lockUpdated.previous).to.eq((p === 'true'));
    }, upd => {
      expect(msg.lockUpdated.updated).to.eq((upd === 'true'));
    });

    evalFunc('value', v => {
      expect(convertValue(msg.featureValueUpdated.previous, msg.featureValueType)).to.eq(v === 'true');
    }, v => {
      expect(convertValue(msg.featureValueUpdated.updated, msg.featureValueType)).to.eq(v === 'true');
    });
  }
}

Then('I receive a cloud event of type {string}', async function (ceType: string, dataTable: DataTable) {
  const world = this as SdkWorld;
  let msg: FeatureMessagingUpdate;
  await waitForExpect(async () => {
    const events = cloudEvents.filter(ce => ce.type === ceType);
    expect(events.length).to.be.gt(0);
    const found = events.find((ce) => {
      const msg1 = featurehubCloudEventBodyParser<any>(ce);
      if (msg1 !== undefined) {
        if (ce.subject === 'io.featurehub.events.messaging') {
          const result = FeatureMessagingUpdateTypeTransformer.fromJson(msg1);
          logger.info(`envId ${result.environmentId} vs ${world.environment.id} app ${result.applicationId} vs ${world.application.id} port ${world.portfolio.id} ${result.portfolioId}`);
          const correct = (result.environmentId === world.environment.id && result.applicationId === world.application.id && world.portfolio.id === result.portfolioId);
          if (correct) {
            console.log(`message is ${JSON.stringify(result, null, 2)}`);
            msg = result;
          }
          return correct;
        } else {
          return false;
        }
      }
    });

    expect(found, `Events ${events} did not match environment ${world.environment.id}, appId ${world.application.id}, portfolio ${world.portfolio.id}`).to.not.be.undefined;
  }, 10000, 500);
  if (msg) {
    expect(msg.whoUpdated).to.eq(world.currentUser.me.name);
    expect(msg.whoUpdatedId).to.eq(world.currentUser.personId);
    expect(msg.whenUpdated).to.not.be.undefined;
    expect(msg.featureKey, `key from message ${msg.featureKey} ${JSON.stringify(msg)} is not the same as the key we know ${world.feature.key}`).to.eq(world.feature.key);
    expect(msg.featureName, `key from message ${msg.featureName} ${JSON.stringify(msg)} is not the same as the key we know ${world.feature.name}`).to.eq(world.feature.name);
    expect(msg.featureValueType).to.eq(world.feature.valueType);
    expect(msg.featureId).to.eq(world.feature.id);
    expect(msg.featureValueId).to.eq((await world.getFeatureValue()).id);
    expect(msg.environmentName).to.eq(world.environment.name);
    expect(msg.portfolioName).to.eq(world.portfolio.name);
    expect(msg.appName).to.eq(world.application.name);


    comparFeatureeMessageWithDataTable(msg, dataTable);
  }
});

Given(/^I update the environment for Slack$/, async function() {
  const world= this as SdkWorld;

  if (world.environment === undefined) {
    const app = await world.applicationApi.getApplication(world.application.id, true);
    world.application = app.data;
    world.environment = app.data.environments[0];
  }

  // console.log("ensure previous environment has propagated");
  // await sleep(3);
  const env = world.environment;

  const webhookAddress = getWebserverExternalAddress();
  await world.environment2Api.updateEnvironmentV2(env.id, new UpdateEnvironment({
    version: env.version,
    webhookEnvironmentInfo: {
      'integration.slack.enabled': 'true',
      'integration.slack.encrypt': 'integration.slack.token',
      'integration.slack.token': 'abcd1234%^&',
      'integration.slack.channel_name': '@Uabcdefg',
    }
  }));

  world.environment = (await world.applicationApi.getApplication(world.application.id, true)).data.environments[0];

  console.log('sleeping to ensure it is propagated');
  await sleep(3000);
});

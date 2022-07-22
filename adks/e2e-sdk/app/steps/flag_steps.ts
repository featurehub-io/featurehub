import { Given, Then, When } from '@cucumber/cucumber';
import { Feature, FeatureValueType } from 'featurehub-javascript-admin-sdk';
import { makeid } from '../support/random';
import { expect } from 'chai';
import waitForExpect from 'wait-for-expect';
import { FeatureStateHolder, Readyness } from 'featurehub-javascript-node-sdk';

Given(/^There is a new feature flag$/, async function () {
  const name = makeid(5).toUpperCase();
  const fCreate = await this.featureApi.createFeaturesForApplication(this.application.id, new Feature({
    name: name,
    key: name,
    valueType: FeatureValueType.Boolean
  }));
  expect(fCreate.status).to.eq(200);
  const feat = fCreate.data.find(f => f.key == name);
  expect(feat).to.not.be.undefined;
  this.feature = feat;
});

Given(/^There is a feature flag with the key (.*)$/, async function (key: string) {
  const fCreate = await this.featureApi.createFeaturesForApplication(this.application.id, new Feature({
    name: key,
    key: key,
    valueType: FeatureValueType.Boolean
  }));
  expect(fCreate.status).to.eq(200);
  const feature = fCreate.data.find(f => f.key == key);
  expect(feature).to.not.be.undefined;

  this.feature = feature;
});

Then(/^the feature flag is (locked|unlocked) and (off|on)$/, async function (lockedStatus, value) {
  await waitForExpect(() => {
    expect(this.repository).to.not.be.undefined;
    expect(this.repository.readyness).to.eq(Readyness.Ready);
    const f = this.featureState(this.feature.key) as FeatureStateHolder;
    // console.log('key is val', this.feature.key, f.getBoolean(), value, f.isLocked(), lockedStatus);
    // logger.info('the feature %s is value %s and locked status %s', this.feature.key, f.valueBoolean, f.locked);
    expect(f.getBoolean()).to.eq(value === 'on');
    expect(f.isLocked()).to.eq(lockedStatus === 'locked');
  }, 4000, 500);
});

When(/^I (unlock|lock) the feature$/, async function (lockUnlock) {
  const fValue = await this.getFeature();
  fValue.locked = (lockUnlock === 'lock');
  await this.updateFeature(fValue);
});

Then(/^I set the feature flag to (on|off|locked|unlocked)$/, async function (flagChange) {
  const fValue = await this.getFeature();
  if (flagChange === 'on' || flagChange === 'off') {
    fValue.valueBoolean = (flagChange === 'on');
  } else {
    fValue.locked = (flagChange === 'locked');
  }

  fValue.whenUpdated = undefined;
  fValue.whoUpdated = undefined;
  await this.updateFeature(fValue);
});

Then(/^I set the feature flag to (on|off|locked|unlocked) and (on|off|locked|unlocked)$/, async function (flagChange1, flagChange2) {
  const fValue = await this.getFeature();
  if (flagChange1 === 'on' || flagChange1 === 'off') {
    fValue.valueBoolean = (flagChange1 === 'on');
  } else {
    fValue.locked = (flagChange1 === 'locked');
  }
  if (flagChange2 === 'on' || flagChange2 === 'off') {
    fValue.valueBoolean = (flagChange2 === 'on');
  } else {
    fValue.locked = (flagChange2 === 'locked');
  }
  await this.updateFeature(fValue);
});


When(/^I setup (\d+) random feature (flags|strings|numbers|json)$/, async function (counter: number, type: string) {
  let valueType = FeatureValueType.Boolean;

  if (type === 'strings') {
    valueType = FeatureValueType.String;
  }
  if (type === 'numbers') {
    valueType = FeatureValueType.Number;
  }
  if (type === 'json') {
    valueType = FeatureValueType.Json;
  }

  for (let pos = 0; pos < counter; pos ++) {
    const key = makeid(10);

    const fCreate = await this.featureApi.createFeaturesForApplication(this.application.id, new Feature({
      name: key,
      key: key,
      valueType: valueType
    }));

    expect(fCreate.status).to.eq(200);
  }

});

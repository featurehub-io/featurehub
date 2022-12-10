import { Given, Then, When } from '@cucumber/cucumber';
import { makeid } from '../support/random';
import { Feature, FeatureValueType } from 'featurehub-javascript-admin-sdk';
import { expect } from 'chai';
import waitForExpect from 'wait-for-expect';
import { FeatureStateHolder, Readyness } from 'featurehub-javascript-node-sdk';
import { SdkWorld } from '../support/world';

async function newFeature(world: SdkWorld, featureType: string, name: string) {
  let vt = FeatureValueType.String;

  if (featureType === 'number') {
    vt = FeatureValueType.Number;
  } else if (featureType === 'json') {
    vt = FeatureValueType.Json;
  }
  const fCreate = await world.featureApi.createFeaturesForApplication(world.application.id, new Feature({
    name: name,
    key: name,
    valueType: vt
  }));

  expect(fCreate.status).to.eq(200);
  const feature = fCreate.data.find(f => f.key == name);
  expect(feature).to.not.be.undefined;

  world.feature = feature;
}

Given(/^There is a new (string|json|number) feature$/, async function (featureType: string) {
  const name = makeid(5).toUpperCase();
  await newFeature(this as SdkWorld, featureType, name);
});

Given(/^There is a (string|json|number) feature named (.*)$/, async function (featureType: string, name: string) {
  await newFeature(this as SdkWorld, featureType, name);
});

Then(/^the (string|json|number) feature is (locked|unlocked) and (.*)$/, async function (featureType: string, lockedStatus: string, value: string) {
  await waitForExpect(() => {
    expect(this.repository.readyness).to.eq(Readyness.Ready);
    const f = this.featureState(this.feature.key) as FeatureStateHolder;
    // console.log('key is val', this.feature.key, f.getBoolean(), value, f.isLocked(), lockedStatus);
    // logger.info('the feature %s is value %s and locked status %s', this.feature.key, f.valueBoolean, f.locked);
    if (value === 'null') {
      if (featureType === 'number') {
        expect(f.getNumber()).to.be.undefined;
      } else if (featureType === 'string') {
        expect(f.getString()).to.be.undefined;
      } else if (featureType === 'json') {
        expect(f.getRawJson()).to.be.undefined;
      } else {
        expect(false).to.be.true; // this is a big fail
      }
    } else {
      if (featureType === 'number') {
        expect(f.getNumber()).to.eq(parseFloat(value));
      } else if (featureType === 'string') {
        expect(f.getString()).to.eq(value);
      } else if (featureType === 'json') {
        expect(f.getRawJson()).to.eq(value);
      } else {
        expect(false).to.be.true; // this is a big fail
      }
    }

    expect(f.isLocked()).to.eq(lockedStatus === 'locked');
  }, 2000, 200);
});

Then(/^I set the (string|json|number) feature value to (.*)$/, async function (featureType: string, value: string) {
  const fValue = await this.getFeature();

  expect(fValue).to.not.be.undefined;

  if (featureType === 'number') {
    fValue.valueNumber = parseFloat(value);
  } else if (featureType === 'string') {
    fValue.valueString = value;
  } else if (featureType === 'json') {
    fValue.valueJson = value;
  } else {
    expect(false).to.be.true; // this is a big fail
  }

  if (fValue.locked === undefined) {
    fValue.locked = false;
  }

  fValue.whenUpdated = undefined;
  fValue.whoUpdated = undefined;
  await this.updateFeature(fValue);
});

When(/^I (retire|unretire) the feature flag$/, async function (status) {
  const fValue = await this.getFeature();
  fValue.retired = status === 'retire';
  fValue.locked = false;
  await this.updateFeature(fValue);
});

import { Given, Then } from '@cucumber/cucumber';
import { makeid } from '../support/random';
import { Feature, FeatureValueType } from 'featurehub-javascript-admin-sdk';
import { expect } from 'chai';
import waitForExpect from 'wait-for-expect';
import { FeatureStateHolder, Readyness } from 'featurehub-javascript-node-sdk';

Given(/^There is a new (string|json|number) feature$/, async function (featureType: string) {
  const name = makeid(5).toUpperCase();
  let vt = FeatureValueType.String;

  if (featureType === 'number') {
    vt = FeatureValueType.Number;
  } else if (featureType === 'json') {
    vt = FeatureValueType.Json;
  }
  const fCreate = await this.featureApi.createFeaturesForApplication(this.application.id, new Feature({
    name: name,
    key: name,
    valueType: vt
  }));

  expect(fCreate.status).to.eq(200);
  expect(fCreate.data.length).to.eq(1);

  this.feature = fCreate.data[0];
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
  if (featureType === 'number') {
    fValue.valueNumber = parseFloat(value);
  } else if (featureType === 'string') {
    fValue.valueString = value;
  } else if (featureType === 'json') {
    fValue.valueJson = value;
  } else {
    expect(false).to.be.true; // this is a big fail
  }

  fValue.whenUpdated = undefined;
  fValue.whoUpdated = undefined;
  await this.updateFeature(fValue);
});

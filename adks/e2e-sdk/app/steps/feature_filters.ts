import {Given, Then, When} from "@cucumber/cucumber";
import {SdkWorld} from "../support/world";
import {expect} from "chai";
import {
  CreateFeature,
  CreateFeatureFilter,
  Feature,
  FeatureValueType,
  SearchFeatureFilterItem,
  SortOrder
} from "../apis/mr-service";
import {AxiosError} from "axios";

Given("I create a new feature filter called {string}", async function(featureFilter: string) {
  const world = this as SdkWorld;

  const result = await world.currentUser.featureFilterApi.createFeatureFilter(world.portfolio.id, {
    name: featureFilter.toString().trim(),
    description: featureFilter
  } as CreateFeatureFilter);

  expect(result.status).to.eq(201);
});


export async function filterFeatures(filterNames: string, world: SdkWorld, filter?: string): Promise<Array<SearchFeatureFilterItem>> {
  const result = await world.currentUser.featureFilterApi.findFeatureFilters(world.portfolio.id, filter);
  const expectedNames = filterNames.trim().split(",");
  expect(result.data.filters).to.not.be.undefined;
  expect(result.data.filters.length, `${expectedNames} not ${JSON.stringify(result.data.filters)}`).to.eq(expectedNames.length);
  for(const f of result.data.filters) {
    expect(expectedNames).to.include(f.name);
  }
  return result.data.filters;
}

When("I ask for feature filters I get {string}", async function(filterNames: string) {
  await filterFeatures(filterNames, this as SdkWorld);
});

When("I ask for feature filters {string} I get {string}", async function(filter: string, filterNames: string) {
  await filterFeatures(filterNames, this as SdkWorld, filter.trim());
});

export async function findFilters(filterNames: string, world: SdkWorld): Promise<Array<SearchFeatureFilterItem>> {
  const expectedNames = filterNames.trim().split(",").map(f => f.trim()).filter(f => f.length > 0);
  return (await world.currentUser.featureFilterApi.findFeatureFilters(world.portfolio.id))
    .data.filters.filter(f => expectedNames.includes(f.name));
}

When("I create a feature flag {string} with the filters {string}", async function(flagName: string, filterNames: string) {
  const world = this as SdkWorld;

  const filters = await findFilters(filterNames, world);

  const fCreate = await world.featureApi.createFeaturesForApplication(world.application.id, new CreateFeature({
    name: flagName,
    key: flagName,
    valueType: FeatureValueType.Boolean,
    featureFilter: filters.map(f => f.id)
  }));

  const feat = fCreate.data.find((f: Feature) => f.key == flagName);

  this.feature = feat;
});

When("I update the feature flag {string} with the filters {string}", async function(featureKey: string, filterNames: string) {
  const world = this as SdkWorld;

  const filters = await findFilters(filterNames, world);
  const feature = (await world.featureApi.getFeatureByKey(world.application.id, featureKey)).data;
  feature.featureFilter = filters.map(f => f.id);
  const result = await world.featureApi.updateFeatureForApplication(world.application.id, featureKey, feature, false, false);
  expect(result.status).to.eq(200);
  const updatedFeature = (await world.featureApi.getFeatureByKey(world.application.id, featureKey, false, true)).data;
  expect(updatedFeature.featureFilter).to.have.members(filters.map(f => f.id));
});

export async function findSingleFilter(filter: string, world: SdkWorld) {
  const filters = (await world.currentUser.featureFilterApi.findFeatureFilters(world.portfolio.id, filter.trim(), 100, 0, SortOrder.Desc, true))
    .data.filters;
  expect(filters.length).to.eq(1);
  return filters[0];
}

Then("the feature filters {string} have features attached {string}", async function(filter: string, flagKeys: string) {
  const world = this as SdkWorld;

  const flags = flagKeys.split(",").map(k => k.toString().trim());
  const filt = await findSingleFilter(filter, world);
  const found = filt.applications.map(f => (f.features || []).map(ff => ff.key)).flat().sort();
  expect(flags.sort(),
    `flags ${flags} is not ${found} ${JSON.stringify(filt)}`).to.deep.eq(found);
});

Then("I can see the feature filter {string} contains the service accounts {string}", async function(filterName: string, serviceAccountNames: string) {
  const world = this as SdkWorld;

  const filt = await findSingleFilter(filterName, world);
  const saNames = serviceAccountNames.split(",").map(s => s.trim()).filter(s => s.length > 0);
  expect(filt.serviceAccounts, `${JSON.stringify(filt)} does not contain service accounts`).to.not.be.undefined;
  const found = filt.serviceAccounts.filter(sa => saNames.includes(sa.name));
  expect(found.length, `Filter service accounts ${JSON.stringify(filt.serviceAccounts)} does not match ${saNames}`).to.eq(saNames.length);
});

Then('the feature flag {string} contains the filters {string} when I get it by key', async function(featureKey: string, filterNames: string) {
  const world = this as SdkWorld;
  const filters = (await findFilters(filterNames, world)).map(f => f.id);

  const feature = (await world.featureApi.getFeatureByKey(world.application.id, featureKey, false, true)).data;

  const matchingFilters = feature.featureFilter.filter(id => filters.includes(id));
  expect(matchingFilters.length, `Feature ${JSON.stringify(feature)} does not contain filters ${filters}`).to.eq(filters.length);
});

Then('I delete the feature filter {string}', async function(filterName: string) {
  const world = this as SdkWorld;
  const filters = await findSingleFilter(filterName, world);
  const viaGet = (await world.currentUser.featureFilterApi.getFeatureFilter(world.portfolio.id, filters.id)).data;
  expect(filters.version).to.eq(viaGet.version);
  await world.currentUser.featureFilterApi.deleteFeatureFilter(world.portfolio.id, viaGet.id, viaGet.version);
  try {
    const result = await world.currentUser.featureFilterApi.getFeatureFilter(world.portfolio.id, filters.id);
    expect(true, `this should never get hit if it returns an error, which it should`).to.be.false;
  } catch (e) {
    const err = e as unknown as AxiosError;
    expect(err.status).to.eq(404);
  }
});



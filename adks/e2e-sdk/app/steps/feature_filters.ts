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

Given("I create a new feature filter called {string}", async function(featureFilter: string) {
  const world = this as SdkWorld;

  const result = await world.superuser.featureFilterApi.createFeatureFilter(world.portfolio.id, {
    name: featureFilter.toString().trim(),
    description: featureFilter
  } as CreateFeatureFilter);

  expect(result.status).to.eq(201);
});


async function filterFeatures(filterNames: string, world: SdkWorld, filter?: string): Promise<Array<SearchFeatureFilterItem>> {
  const result = await world.superuser.featureFilterApi.findFeatureFilters(world.portfolio.id, filter);
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

async function findFilters(filterNames: string, world: SdkWorld): Promise<Array<SearchFeatureFilterItem>> {
  const expectedNames = filterNames.trim().split(",");
  return (await world.superuser.featureFilterApi.findFeatureFilters(world.portfolio.id))
    .data.filters.filter(f => expectedNames.includes(f.name));
}

When("I create a feature flag {string} with the filters {string}", async function(flagName: string, filterNames: string) {
  const world = this as SdkWorld;

  const filters = await findFilters(filterNames, world);

  const fCreate = await this.featureApi.createFeaturesForApplication(this.application.id, new CreateFeature({
    name: flagName,
    key: flagName,
    valueType: FeatureValueType.Boolean,
    filter: filters.map(f => f.id)
  }));

  const feat = fCreate.data.find((f: Feature) => f.key == flagName);

  this.feature = feat;
});

Then("the feature filters {string} have features attached {string}", async function(filter: string, flagKeys: string) {
  const world = this as SdkWorld;

  const filters = (await world.superuser.featureFilterApi.findFeatureFilters(world.portfolio.id, filter.trim(), 100, 0, SortOrder.Desc, true))
    .data.filters;

  const flags = flagKeys.split(",").map(k => k.toString().trim());
  expect(filters.length).to.eq(1);
  const filt = filters[0];
  const found = filt.features.map(f => f.key).sort();
  expect(flags.sort(),
    `flags ${flags} is not ${found} ${JSON.stringify(filt)}`).to.deep.eq(found);
});

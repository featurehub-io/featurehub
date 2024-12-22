import {Given, Then} from "@cucumber/cucumber";
import {SdkWorld} from "../support/world";
import {expect} from "chai";
import {logger} from "../support/logging";

Given(/^I ask for api keys for the application for (superuser|new user)$/, async function(user: string) {
  const world = this as SdkWorld;

  const apiUser = (user === "superuser") ? world.superuser : world.user;

  const response = await apiUser.serviceAccountApi.searchServiceAccountsInPortfolio(
    world.portfolio.id, true, undefined, world.application.id, true );

  expect(response.status).to.eq(200);
  apiUser.serviceAccounts = response.data;
});

Then(/^the current environment api keys are visible for (superuser|user)$/, function(user: string) {
  const world = this as SdkWorld;

  const apiUser = (user === "superuser") ? world.superuser : world.user;

  const found = apiUser.serviceAccounts.find(s => {
    return s.permissions.find(se => se.environmentId === world.environment.id && se.sdkUrlClientEval && se.sdkUrlServerEval);
  });

  expect(found, `Could not find env ${world.environment.id} in ${JSON.stringify(apiUser.serviceAccounts)}`).to.not.be.undefined;
});


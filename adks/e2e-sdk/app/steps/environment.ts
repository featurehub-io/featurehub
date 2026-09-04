import {Given, When} from '@cucumber/cucumber';
import { SdkWorld } from '../support/world';
import { expect } from 'chai';
import {CreateEnvironment, Environment} from '../apis/mr-service';

Given("I create an environment {string}", async function(name: string) {
  const world = this as SdkWorld;

  expect(world.application).to.not.be.undefined;

  const env = new CreateEnvironment({ name: name, description: name });

  const response = await world.environmentApi.createEnvironment(world.application.id, env);

  expect(response.status).to.eq(200);
  world.environment = response.data;
});

When('I get the default environment', async function() {
  const world = this as SdkWorld;

  expect(world.application).to.not.be.undefined;

  if (world.application.environments.length === 0) {
    const result = await world.currentUser.applicationApi.getApplication(world.application.id, true);
    expect(result.status).to.eq(200);
    world.application = result.data;
  }

  world.environment = world.application.environments[0];
});

import { Given } from '@cucumber/cucumber';
import { SdkWorld } from '../support/world';
import { expect } from 'chai';
import { Environment } from '../apis/mr-service';

Given("I create an environment {string}", async function(name: string) {
  const world = this as SdkWorld;

  expect(world.application).to.not.be.undefined;

  const env = new Environment({ name: name, description: name });

  const response = await world.environmentApi.createEnvironment(world.application.id, env);

  expect(response.status).to.eq(200);
  world.environment = response.data;
});

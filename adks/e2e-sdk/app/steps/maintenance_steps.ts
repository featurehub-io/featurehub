import { Given, Then, When } from '@cucumber/cucumber';
import { SetupServiceApi, UpdatedSystemConfig, UpdatedSystemConfigs } from '../apis/mr-service';
import { SdkWorld } from '../support/world';
import { expect } from 'chai';

const MAINTENANCE_ACTIVE_KEY = 'maintenance.active';
const MAINTENANCE_MESSAGE_KEY = 'maintenance.message';

async function setMaintenanceConfig(world: SdkWorld, active: boolean, message?: string) {
  const sData = await world.systemConfigApi.getSystemConfig(['maintenance.']);
  expect(sData.status).to.eq(200);
  const existingConfigs = sData.data.configs;

  const findVersion = (key: string) =>
    existingConfigs.find((c: any) => c.key === key)?.version ?? -1;

  const configs: UpdatedSystemConfig[] = [
    new UpdatedSystemConfig({
      key: MAINTENANCE_ACTIVE_KEY,
      version: findVersion(MAINTENANCE_ACTIVE_KEY),
      value: active,
    }),
  ];

  // always update message so previous values don't leak between scenarios
  configs.push(new UpdatedSystemConfig({
    key: MAINTENANCE_MESSAGE_KEY,
    version: findVersion(MAINTENANCE_MESSAGE_KEY),
    value: message ?? null,
  }));

  const result = await world.systemConfigApi.createOrUpdateSystemConfigs(new UpdatedSystemConfigs({ configs }));
  expect(result.status).to.eq(200);
}

Given('I enable the maintenance window with message {string}', async function (message: string) {
  await setMaintenanceConfig(this as SdkWorld, true, message);
});

Given('I enable the maintenance window without a message', async function () {
  await setMaintenanceConfig(this as SdkWorld, true);
});

When('I disable the maintenance window', async function () {
  await setMaintenanceConfig(this as SdkWorld, false);
});

When('I call the initialize endpoint', async function () {
  const world = this as SdkWorld;
  const setupApi = new SetupServiceApi(world.adminApiConfig);
  const result = await setupApi.isInstalled();
  expect(result.status).to.eq(200);
  (this as any).lastInitializeResponse = result.data;
});

Then('the initialize response has maintenanceInfo active with message {string}', function (message: string) {
  const raw = (this as any).lastInitializeResponse;
  const maintenanceInfo = (raw as any).maintenanceInfo;
  expect(maintenanceInfo, 'maintenanceInfo should be present in response').to.not.be.undefined;
  expect(maintenanceInfo.active, 'maintenanceInfo.active should be true').to.eq(true);
  expect(maintenanceInfo.message, 'maintenanceInfo.message should match').to.eq(message);
});

Then('the initialize response has maintenanceInfo active with no message', function () {
  const raw = (this as any).lastInitializeResponse;
  const maintenanceInfo = (raw as any).maintenanceInfo;
  expect(maintenanceInfo, 'maintenanceInfo should be present in response').to.not.be.undefined;
  expect(maintenanceInfo.active, 'maintenanceInfo.active should be true').to.eq(true);
  expect(maintenanceInfo.message ?? null, 'maintenanceInfo.message should be absent').to.be.null;
});

Then('the initialize response has no maintenanceInfo', function () {
  const raw = (this as any).lastInitializeResponse;
  const maintenanceInfo = (raw as any).maintenanceInfo;
  expect(maintenanceInfo ?? null, 'maintenanceInfo should be absent when maintenance is inactive').to.be.null;
});

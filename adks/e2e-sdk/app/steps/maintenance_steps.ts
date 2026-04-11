import { Given, Then, When } from '@cucumber/cucumber';
import { SetupServiceApi, UpdatedSystemConfig, UpdatedSystemConfigs } from '../apis/mr-service';
import { SdkWorld } from '../support/world';
import { expect } from 'chai';

const MAINTENANCE_ACTIVE_KEY = 'maintenance.active';
const MAINTENANCE_MESSAGE_KEY = 'maintenance.message';

// ── helpers ──────────────────────────────────────────────────────────────────

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

/**
 * Calls GET /mr-api/maintenance-banner.
 * Returns the parsed MaintenanceInfo body on 200, or null on 204.
 * Any other status causes the test to fail.
 */
async function fetchMaintenanceBanner(world: SdkWorld): Promise<any | null> {
  const response = await fetch(`${world.adminUrl}/mr-api/maintenance-banner`);
  if (response.status === 204) return null;
  expect(response.status, `Unexpected status from maintenance-banner: ${response.status}`).to.eq(200);
  return response.json();
}

// ── Given / When / Then ───────────────────────────────────────────────────────

Given('I enable the maintenance window with message {string}', async function (message: string) {
  await setMaintenanceConfig(this as SdkWorld, true, message);
});

Given('I enable the maintenance window without a message', async function () {
  await setMaintenanceConfig(this as SdkWorld, true);
});

When('I disable the maintenance window', async function () {
  await setMaintenanceConfig(this as SdkWorld, false);
});

// ── /mr-api/initialize (initial page load) ────────────────────────────────────

When('I call the initialize endpoint', async function () {
  const world = this as SdkWorld;
  const setupApi = new SetupServiceApi(world.adminApiConfig);
  const result = await setupApi.isInstalled();
  expect(result.status).to.eq(200);
  (this as any).lastInitializeResponse = result.data;
});

Then('the initialize response has maintenanceInfo active with message {string}', function (message: string) {
  const maintenanceInfo = (this as any).lastInitializeResponse?.maintenanceInfo;
  expect(maintenanceInfo, 'maintenanceInfo should be present in initialize response').to.not.be.undefined;
  expect(maintenanceInfo.active, 'maintenanceInfo.active').to.eq(true);
  expect(maintenanceInfo.message, 'maintenanceInfo.message').to.eq(message);
});

Then('the initialize response has maintenanceInfo active with no message', function () {
  const maintenanceInfo = (this as any).lastInitializeResponse?.maintenanceInfo;
  expect(maintenanceInfo, 'maintenanceInfo should be present in initialize response').to.not.be.undefined;
  expect(maintenanceInfo.active, 'maintenanceInfo.active').to.eq(true);
  expect(maintenanceInfo.message ?? null, 'maintenanceInfo.message should be absent').to.be.null;
});

Then('the initialize response has no maintenanceInfo', function () {
  const maintenanceInfo = (this as any).lastInitializeResponse?.maintenanceInfo;
  expect(maintenanceInfo ?? null, 'maintenanceInfo should be absent when maintenance is inactive').to.be.null;
});

// ── /mr-api/maintenance-banner (in-app navigation / already logged in) ────────

/**
 * "Navigate to another page" simulates the frontend calling the maintenance-banner
 * endpoint that fires on every route change for already-logged-in users.
 */
When('I navigate to another page', async function () {
  const world = this as SdkWorld;
  (this as any).lastMaintenanceBanner = await fetchMaintenanceBanner(world);
});

Given('the maintenance banner endpoint reports no active maintenance', async function () {
  const world = this as SdkWorld;
  const banner = await fetchMaintenanceBanner(world);
  expect(banner, 'Expected no active maintenance banner').to.be.null;
});

Given('the maintenance banner endpoint reports active maintenance with message {string}', async function (message: string) {
  const world = this as SdkWorld;
  const banner = await fetchMaintenanceBanner(world);
  expect(banner, 'Expected an active maintenance banner').to.not.be.null;
  expect(banner.active, 'banner.active').to.eq(true);
  expect(banner.message, 'banner.message').to.eq(message);
});

Then('the maintenance banner endpoint reports no active maintenance', async function () {
  const world = this as SdkWorld;
  // Re-poll so this Then-step is self-contained (doesn't rely on When storing state)
  const banner = await fetchMaintenanceBanner(world);
  expect(banner, 'Expected no active maintenance banner').to.be.null;
});

Then('the maintenance banner endpoint reports active maintenance with message {string}', async function (message: string) {
  const world = this as SdkWorld;
  const banner = (this as any).lastMaintenanceBanner ?? await fetchMaintenanceBanner(world);
  expect(banner, 'Expected an active maintenance banner').to.not.be.null;
  expect(banner.active, 'banner.active').to.eq(true);
  expect(banner.message, 'banner.message').to.eq(message);
});

Then('the maintenance banner endpoint reports active maintenance with no message', async function () {
  const world = this as SdkWorld;
  const banner = (this as any).lastMaintenanceBanner ?? await fetchMaintenanceBanner(world);
  expect(banner, 'Expected an active maintenance banner').to.not.be.null;
  expect(banner.active, 'banner.active').to.eq(true);
  expect(banner.message ?? null, 'banner.message should be absent').to.be.null;
});

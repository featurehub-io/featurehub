import {Given, When} from "@cucumber/cucumber";
import {SdkWorld} from "../support/world";
import {decodeAndValidateRoles} from "../support/utils";
import {expect} from "chai";
import {RoleType, ServiceAccount, ServiceAccountPermission, ServiceAccountServiceApi} from "../apis/mr-service";
import {logger} from "../support/logging";


When('I assign the service account named permissions {string} to the current environment', async function(roleTypes: string) {
  const world = this as SdkWorld;

  const roles = decodeAndValidateRoles(roleTypes);
  const response = await world.serviceAccountApi.searchServiceAccountsInPortfolio(world.portfolio.id, true);
  expect(response.status).to.eq(200);

  const accounts = response.data.map(sa => {
    const perms = sa.permissions.find(sap => sap.environmentId === world.environment.id);

    if (perms) {
      perms.permissions = roles;
    } else {
      sa.permissions.push(new ServiceAccountPermission({
        environmentId: world.environment.id,
        permissions: roles
      }));
    }

    return sa;
  });

  for(let account of accounts) {
    const saResponse = await world.serviceAccountApi.updateServiceAccountOnPortfolio(world.portfolio.id, account);
    expect(saResponse.status).to.eq(200);
  }
});

export async function serviceAccountPermission(envId: string, roleTypes: string, world: SdkWorld) {
  const roles = roleTypes === 'full' ? [RoleType.Read, RoleType.Unlock, RoleType.Lock, RoleType.ChangeValue] : [RoleType.Read];
  await serviceAccountPermissionRoles(envId, roles, world);
}

export async function serviceAccountPermissionRoles(envId: string, roles: Array<RoleType>, world: SdkWorld) {
  const permissions: ServiceAccountPermission[] = [
    new ServiceAccountPermission({
      environmentId: envId,
      permissions: roles
    })
  ];

  const serviceAccountApi: ServiceAccountServiceApi = world.serviceAccountApi;
  const serviceAccountCreate = await serviceAccountApi.createServiceAccountInPortfolio(world.portfolio.id, new ServiceAccount({
    name: world.portfolio.name, description: world.portfolio.name, permissions: permissions
  }), true);
  expect(serviceAccountCreate.status).to.eq(200);
  expect(serviceAccountCreate.data.permissions.length).to.eq(permissions.length);
  world.serviceAccount = serviceAccountCreate.data;
  logger.info(`storing service account ${world.serviceAccount}`);

  // this adds a new permission based on the environment we are actually in
  if (world.environment.id !== world.application.environments[0].id) {
    const updatedAccount = serviceAccountCreate.data;
    const newPerm = new ServiceAccountPermission({
      environmentId: world.environment.id,
      permissions: roles
    });

    updatedAccount.permissions.push(newPerm);
    permissions.push(newPerm);

    const saUpdate = await serviceAccountApi.updateServiceAccount(updatedAccount.id, updatedAccount, true);

    expect(saUpdate.status).to.eq(200);
    expect(saUpdate.data.permissions.length).to.eq(permissions.length);
    world.serviceAccount = saUpdate.data;
    logger.info(`storing service account ${world.serviceAccount}`);

    const accounts = await serviceAccountApi.searchServiceAccountsInPortfolio(world.portfolio.id, true,
      saUpdate.data.name, world.application.id, true);

    const sa = accounts.data.find(sa => sa.id == saUpdate.data.id);

    serviceAccountCreate.data.permissions = sa.permissions;
  }

  let perm: ServiceAccountPermission;

  for (const p of serviceAccountCreate.data.permissions) {
    if (p.environmentId === world.environment.id) {
      perm = p;
      break;
    }
  }

  expect(perm).to.not.be.undefined;

  world.serviceAccountPermission = perm;
  expect(perm.permissions.length).to.eq(roles.length);
  expect(perm.sdkUrlClientEval).to.not.be.undefined;
  expect(perm.sdkUrlServerEval).to.not.be.undefined;
  expect(perm.environmentId).to.not.be.undefined;
}

Given(/^I create a service account and (full|read) permissions for environment (.*)$/, async function (roleTypes: string, environment: string) {
  const world = this as SdkWorld;
  const env = world.application.environments.find(e => e.name === environment);
  expect(env, `Unable to find environment ${environment} in application`).to.not.be.undefined;

  await serviceAccountPermission(env.id, roleTypes, world);
});

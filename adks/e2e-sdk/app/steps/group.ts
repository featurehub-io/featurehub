import {Then, When} from "@cucumber/cucumber";
import {SdkWorld} from "../support/world";
import {
  CreateGroup,
  EnvironmentGroupRole,
  Group,
  Person,
  PersonId,
  ServiceAccountPermission,
  UpdateEnvironment
} from "../apis/mr-service";
import {makeid} from "../support/random";
import {expect} from "chai";
import {decodeAndValidateRoles} from "../support/utils";


When('I create a new normal group', async function() {
  const world = this as SdkWorld;
  const response = await world.superuser.groupApi.createGroup(world.portfolio.id, new CreateGroup({
    name: makeid(10),
  }));

  expect(response.status).to.eq(200);
  world.group = response.data;
});

Then('I assign the new user to the new group', async function() {
  const world = this as SdkWorld;
  expect(world.user).to.not.be.undefined;
  const userResponse = await world.user.personApi.getPerson("self");
  const userId = (userResponse.data as Person).id.id;
  const response = await world.superuser.groupApi.addPersonToGroup(world.group.id,
    userId, true);
  expect(response.status).to.eq(200);
  expect(response.data.members.find( p => p.id.id === userId)).to.not.be.undefined;
});

When('I assign roles {string} to the group for the current environment', async function (roleTypes: string) {
  const world = this as SdkWorld;

  const roles = decodeAndValidateRoles(roleTypes);
  const curGroupResponse = await world.superuser.groupApi.getGroup(world.group.id);
  expect(curGroupResponse.status).to.eq(200);
  const group = curGroupResponse.data;
  group.environmentRoles.push(new EnvironmentGroupRole({
    environmentId: world.environment.id,
    groupId: group.id,
    roles: roles
  }));
  const groupResponse = await world.superuser.groupApi.updateGroupOnPortfolio(world.portfolio.id, group,
 false, false, true, false, true);

  expect(groupResponse.status).to.eq(200);
  expect(groupResponse.data.environmentRoles.find(er => er.environmentId === world.environment.id));
  world.group = groupResponse.data;
});

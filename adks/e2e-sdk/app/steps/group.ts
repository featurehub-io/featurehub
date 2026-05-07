import {Then, When} from "@cucumber/cucumber";
import {SdkWorld} from "../support/world";
import {ApplicationGroupRole, ApplicationRoleType, CreateGroup, EnvironmentGroupRole, Person} from "../apis/mr-service";
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

When('I get the portfolio admin group', async function() {
  const world = this as SdkWorld;

  const groups = await world.superuser.groupApi.findGroups(world.portfolio.id);
  const adminGroup = groups.data.find(g => g.admin);
  expect(adminGroup, `${JSON.stringify(groups.data)} - could not find admin group!`).to.not.be.undefined;
  const fullGroup = await world.superuser.groupApi.getGroup(adminGroup.id, false, true);
  world.group = fullGroup.data;
});

When('I create a new group with application roles {string}', async function(roles: string) {
  const world = this as SdkWorld;

  const response = await world.superuser.groupApi.createGroup(world.portfolio.id, new CreateGroup({
    name: makeid(10),
    applicationRoles: [new ApplicationGroupRole({
      applicationId: world.application.id,
      groupId: world.application.id, // this is not actually used
      roles: roles.split(",").map(r => r.trim() as ApplicationRoleType)
    })]
  }));

  expect(response.status).to.eq(200);
  world.group = response.data;
});

When('I assign the superuser to the group', async function() {
  const world = this as SdkWorld;
  const userResponse = await world.superuser.personApi.getPerson("self");
  const userId = (userResponse.data as Person).id.id;
  const response = await world.superuser.groupApi.addPersonToGroup(world.group.id,
    userId, true);
  expect(response.status).to.eq(200);
  expect(response.data.members.find( p => p.id.id === userId)).to.not.be.undefined;
});

Then('I can delete the supergroup from the group', async function() {
  const world = this as SdkWorld;
  const userResponse = await world.superuser.personApi.getPerson("self");
  const userId = (userResponse.data as Person).id.id;
  const response = await world.superuser.groupApi.deletePersonFromGroup(world.group.id, userId, true);
  expect(response.status).to.eq(200);
  expect(response.data.members.find( p => p.id.id === userId)).to.be.undefined;
});

Then(/^the (superuser|user) is marked in the group as a (superuser|user)$/, async function(userSource: string, userType: string) {
  const world = this as SdkWorld;
  const user = (userSource === 'superuser') ? world.superuser : world.user;
  expect(user.me).to.not.be.undefined;
  expect(world.group).to.not.be.undefined;
  const suserValue = userType === 'superuser';
  expect(world.group.sMembers.find(p => p.superuser === suserValue && p.person.id === user.personId))
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

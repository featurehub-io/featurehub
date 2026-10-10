import {Then, When} from "@cucumber/cucumber";
import {SdkWorld} from "../support/world";
import {
  ApplicationGroupRole, ApplicationRoleType, CreateGroup, EnvironmentGroupRole, Group, Person, PortfolioGroupRoleType,
  UpdateGroup
} from "../apis/mr-service";
import {makeid} from "../support/random";
import {expect} from "chai";
import {decodeAndValidateRoles} from "../support/utils";
import {logger} from "../support/logging";
import axios, {AxiosError} from "axios";


When('I create a new normal group', async function() {
  const world = this as SdkWorld;
  world.group = await createGroup(world);
});

When('I create a new group {string}', async function(name: string) {
  const world = this as SdkWorld;
  const g = await createGroup(world);
  world.namedGroups[name] = g;
  world.group = g;
});

async function createGroup(world: SdkWorld): Promise<Group> {
  const response = await world.superuser.groupApi.createGroup(world.portfolio.id, new CreateGroup({
    name: makeid(10),
  }));

  expect(response.status).to.eq(200);
  return response.data;
}

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

Then('I cannot delete the superuser from the group', async function() {
  const world = this as SdkWorld;
  const userResponse = await world.superuser.personApi.getPerson("self");
  const userId = (userResponse.data as Person).id.id;
  try {
    await world.superuser.groupApi.deletePersonFromGroup(world.group.id, userId, false);
    expect(true, `call succeeded to delete supergroup from portfolio group but should not have`).to.be.false;
  } catch (e) {
    expect(e.response.status).to.eq(404);
  }
});

Then(/^the (superuser|user) (is|is not) in the group as a (superuser|user)$/, async function(userSource: string, isRule: string, userType: string) {
  const world = this as SdkWorld;
  const user = (userSource === 'superuser') ? world.superuser : world.user;
  expect(user.me).to.not.be.undefined;
  expect(world.group).to.not.be.undefined;

  const foundSuperuser = world.group.superMembers.find(p => p === user.personId);
  const found = world.group.simpleMembers.find(p => p.id === user.personId);

  if (userType === 'superuser') {
    if (isRule === 'is') {
      expect(foundSuperuser, `${userSource} not superuser in group ${world.group.name}`).to.not.be.undefined;
    } else { // is not
      expect(foundSuperuser, `${userSource} not superuser in in group ${world.group.name}`).to.be.undefined;
    }
  } else {
    if (isRule === 'is') {
      expect(found, `${userSource} in group ${world.group.name} and is not`).to.not.be.undefined;
    } else { // is not
      expect(found, `${userSource} in group ${world.group.name} and should not be`).to.be.undefined;
    }
  }
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

Then(/^I (add|cannot add) the user "([^"]*)" to the group "([^"]*)"$/, async function (canOrNot: string, user: string, group: string) {
  const world = this as SdkWorld;
  const currentUser = world.namedUsers[user];
  expect(currentUser).to.not.be.undefined;
  const currentGroup = world.namedGroups[group];
  expect(currentGroup).to.not.be.undefined;

  const userResponse = await currentUser.personApi.getPerson("self");
  const userId = (userResponse.data as Person).id.id;
  try {
    const response = await world.superuser.groupApi.addPersonToGroup(currentGroup.id,
      userId, true);
    expect(canOrNot).to.eq("add");
    expect(response.status).to.eq(200);
    expect(response.data.members.find( p => p.id.id === userId)).to.not.be.undefined;
  } catch (e) {
    expect(canOrNot).to.not.eq("add");
    expect(e.response.status).to.eq(400);
  }
});

Then(/^I (can|cannot) remove the user "([^"]*)" from the group "([^"]*)"$/, async function (can: string, user: string, group: string) {
  const world = this as SdkWorld;
  const currentUser = world.namedUsers[user];
  expect(currentUser).to.not.be.undefined;
  const currentGroup = world.namedGroups[group];
  expect(currentGroup).to.not.be.undefined;

  const userResponse = await currentUser.personApi.getPerson("self");
  const userId = (userResponse.data as Person).id.id;
  try {
    const response = await world.superuser.groupApi.deletePersonFromGroup(currentGroup.id,
      userId, true);
    expect(can).to.eq("can");
    expect(response.status).to.eq(200);
    expect(response.data.members.find( p => p.id.id === userId)).to.be.undefined;
  } catch(e) {
    expect(can).to.not.eq("can");
    expect(e.response.status).to.eq(400);
  }
});




When(/I (can|cannot) assign portfolio strategy "([^"]*)" roles to the group/, async function (can: string, roles: string) {
  const world = this as SdkWorld;

  expect(world.group).is.not.undefined;

  const group = new UpdateGroup();

  group.portfolioRoles =  new Set<PortfolioGroupRoleType>(world.group.portfolioRoles instanceof Array ? world.group.portfolioRoles : []);
  logger.info(`Portfolio roles is ${typeof group.portfolioRoles} ${group.portfolioRoles instanceof Set}`);
  group.id = world.group.id;
  group.version = world.group.version;

  roles.trim().split(",").map(role => role.trim().toUpperCase() ).forEach(r => {
    if (r === 'EDIT') {
      group.portfolioRoles.add(PortfolioGroupRoleType.PortfolioStrategyEdit);
    }
    if (r === 'DELETE') {
      group.portfolioRoles.add(PortfolioGroupRoleType.PortfolioStrategyEditAndDelete);
    }
    if (r === 'MANAGE') {
      group.portfolioRoles.add(PortfolioGroupRoleType.GroupMemberManager);
    }
  });

  try {
    const updatedData = await world.superuser.groupApi.updateGroupOnPortfolioV2(world.portfolio.id, group);
    expect(can).to.eq("can");
    expect(updatedData.status).to.eq(200);
    world.group = updatedData.data;
  } catch (e) {
    expect(can).to.not.eq("can");
    expect(e.response.status).to.eq(400);
  }
});

async function findPortfolioAdminGroup(world: SdkWorld): Promise<Group> {
  const groupsResponse = await world.currentUser.groupApi.findGroups(world.portfolio.id, false);
  expect(groupsResponse.status).to.eq(200);
  const portfolioGroup = groupsResponse.data.find(g => g.admin);
  expect(portfolioGroup).to.not.be.undefined;
  return portfolioGroup;
}

When(/I (can|cannot) add the user "([^"]*)" to the portfolio admin group/, async function(can: string, user: string) {
  const world = this as SdkWorld;

  const addingUser = world.namedUsers[user];
  expect(addingUser).to.not.be.undefined;

  const portfolioGroup = await findPortfolioAdminGroup(world);

  try {
    const addUserResult = await world.currentUser.groupApi.addPersonToGroup(portfolioGroup.id, addingUser.personId, true);
    expect(can).to.eq("can");
    expect(addUserResult.status).to.eq(200);
    expect(addUserResult.data.members.find( p => p.id.id === addingUser.personId)).to.not.be.undefined;
  } catch (e) {
    expect(can).to.not.eq("can");
    expect(e.response.status).to.eq(400);
  }
});

When(/I (can|cannot) assign roles "([^"]*)" to the group for the current environment/, async function (can: string, roleTypes: string) {
  const world = this as SdkWorld;

  const roles = decodeAndValidateRoles(roleTypes);
  const curGroupResponse = await world.superuser.groupApi.getGroup(world.group.id);
  expect(curGroupResponse.status).to.eq(200);

  const group = curGroupResponse.data;

  const updateGroup = new UpdateGroup({
    version: group.version,
    id: group.id,
    environmentRoles: [...group.environmentRoles, new EnvironmentGroupRole({
      environmentId: world.environment.id,
      groupId: group.id,
      roles: roles
    })]
  });

  try {
    const groupResponse = await world.superuser.groupApi.updateGroupOnPortfolioV2(world.portfolio.id, updateGroup,
      false, false, true, false, true);
    expect(can).to.eq("can");
    expect(groupResponse.status).to.eq(200);
    expect(groupResponse.data.environmentRoles.find(er => er.environmentId === world.environment.id));
    world.group = groupResponse.data;
  } catch (e) {
    expect(can).to.not.eq("can");
    expect(e.response.status).to.eq(400);
  }
});

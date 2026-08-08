import {Given, When} from "@cucumber/cucumber";
import {SdkWorld} from "../support/world";
import {makeid, sleep} from "../support/random";
import {
  CreatePersonDetails,
  ObjectSerializer,
  PersonRegistrationDetails,
  PersonType,
  UserCredentials
} from "../apis/mr-service";
import {expect} from "chai";
import {logger} from "../support/logging";
import {createTestUser, testingSaas} from "../support/saas_test";

// Must satisfy the server's password complexity policy (see backend PasswordPolicy - by default
// 8+ chars with upper, lower and a digit). This used to be the user's email address, which only
// satisfied the policy when makeid() happened to produce an uppercase letter and a digit.
const testUserPassword = 'Passw0rdE2e';


When("I create a new user", async function() {
  const world = this as SdkWorld;

  const name = makeid(10);
  const email = name + '@mailinator.com';

  const created = await world.personApi.createPerson(new CreatePersonDetails({
    email: email, personType: PersonType.Person, groupIds: []}));
  expect(created.status).to.be.lessThan(205);

  if (testingSaas()) {
    const registeredPerson = await createTestUser(name, email, world);
    world.setUser(registeredPerson.token);
    return;
  }

  const person = created.data;

  const regPerson = new PersonRegistrationDetails({
    email: email,
    password: testUserPassword,
    confirmPassword: testUserPassword,
    name: name,
    registrationToken: person.token
  });
  logger.info(`new person is ${JSON.stringify(regPerson)}`);
  const val = ObjectSerializer.serialize(regPerson, 'PersonRegistrationDetails');
  logger.info(`serialized is ${JSON.stringify(val)}`);
  const result = await world.superuser.authorisationApi.registerPerson(regPerson);

  expect(result.status).to.be.lessThan(205);

  const loginResponse = await world.superuser.anonAuthorizationAPi.login(new UserCredentials({
    email: email, password: testUserPassword
  }));

  expect(loginResponse.status).to.eq(200);
  world.setUser(loginResponse.data.accessToken);
});

When('I am the created user', function () {
  const world = this as SdkWorld;
  world.currentUser = world.user;
});

When('I am the superuser', function () {
  const world = this as SdkWorld;
  world.currentUser = world.superuser;
});

Given("I am a superuser", function() {
  const world = this as SdkWorld;
  world.currentUser = world.superuser;
});


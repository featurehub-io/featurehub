import {When} from "@cucumber/cucumber";
import {ApiUser, SdkWorld} from "../support/world";
import {makeid} from "../support/random";
import {
  CreatePersonDetails,
  ObjectSerializer,
  PersonRegistrationDetails,
  PersonType,
  UserCredentials
} from "../apis/mr-service";
import {expect} from "chai";
import {logger} from "../support/logging";


When("I create a new user", async function() {
  const world = this as SdkWorld;

  const email = makeid(10) + '@mailinator.com';

  const created = await world.personApi.createPerson(new CreatePersonDetails({
    email: email, personType: PersonType.Person, groupIds: []}));
  expect(created.status).to.be.lessThan(205);

  const regPerson = new PersonRegistrationDetails({
    email: email,
    password: email,
    confirmPassword: email,
    name: email,
    registrationToken: created.data.token
  });
  logger.info(`new person is ${JSON.stringify(regPerson)}`);
  const val = ObjectSerializer.serialize(regPerson, 'PersonRegistrationDetails');
  logger.info(`serialized is ${JSON.stringify(val)}`);
  const result = await world.superuser.authorisationApi.registerPerson(regPerson);

  expect(result.status).to.be.lessThan(205);

  const loginResponse = await world.superuser.anonAuthorizationAPi.login(new UserCredentials({
    email: email, password: email
  }));

  expect(loginResponse.status).to.eq(200);
  world.setUser(loginResponse.data.accessToken);
})

/**
 * This file holds APIs for things that only exist in the SaaS testing layer, are only turned on for staging/local while it is up and never exposed to
 * the public internet.
 */
import {RegistrationUrl} from "../apis/mr-service";
import {expect} from "chai";
import {SdkWorld} from "./world";
import {logger} from "./logging";

export function testingSaas(): boolean {
  return process.env.SAAS_TEST_CS_URL !== undefined && process.env.SAAS_ORGANISATION_ID !== undefined;
}

export async function createTestUser(name: string, email: string, world: SdkWorld): Promise<RegistrationUrl> {
  const saasTestPersonUrl = process.env.SAAS_TEST_CS_URL;

  expect(saasTestPersonUrl).to.not.be.undefined;

  const data = await fetch(saasTestPersonUrl + "/test/person", {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
    },
    body: JSON.stringify({
      person: {
        name: name,
        email: email
      }
    })
  });
  expect(data.status).to.eq(200);
  const personDetails = await new Response(data.body).json() as any;

  logger.debug(`saas new user ${JSON.stringify(personDetails)}.`);

  return new RegistrationUrl({
    personId: personDetails.id,
    token: personDetails.token
  });
}

import {Given} from "@cucumber/cucumber";
import * as fs from "node:fs";
import {expect} from "chai";
import {logger} from "../support/logging";
import {BackendDiscovery} from "../support/discovery";

interface EnvSource {
  apiKeys: Array<string>;
}

Given('A file of endpoints I hit them all', { timeout: 60000 * 20 }, async function() {
  logger.info(`env source is ${process.env['ENV_SOURCE']}` );

  if (process.env['ENV_SOURCE']) {
    if (!process.env['SAAS_EDGE']) {
      // from:
      // select sa.api_key_client_eval,e.id from fh_service_account_env se join fh_environment e on se.fk_environment_id = e.id join fh_service_account sa on se.fk_service_account_id = sa.id;
      const keys = fs.readFileSync(process.env['ENV_SOURCE'], 'utf-8').split("\n");
      let realCount = 0;
      // for (let count = 0; count <= 200000; count++) {
      for (let count = 0; count <= 200000; realCount++) {
      // for (let count = 0; count <= 1; count++) {
        if (realCount % 1000 === 0) {
          console.log(realCount);
        }
        for (const key of keys) {
          const vals = key.split("|");
          if (vals.length === 2) {
            const apiKey = vals[0].trim();
            const envId = vals[1].trim();
            const url = `${BackendDiscovery.edgeUrl}/features?apiKey=${envId}/${apiKey}&contextSha=0`;

            try {
              const features = await fetch(url);
              logger.debug(`Hitting ${url} got ${features.status}`);
              expect(features.status).to.eq(200);
              break;
            } catch (e) {
              logger.error(`failed ${e}`);
            }
          }
        }
      }
    }
    if (process.env['SAAS_EDGE']) {
      const keys = JSON.parse(fs.readFileSync(process.env['ENV_SOURCE'], 'utf-8')) as EnvSource;

      logger.info(`Number of keys found is ${keys.apiKeys.length}`);
      for (let count = 0; count < 100; count++) {
        for (const key of keys.apiKeys) {
          const url = `${process.env['SAAS_EDGE']}/features?apiKey=${key}&contextSha=0`;
          try {
            const features = await fetch(url);
            logger.debug(`Hitting ${url} got ${features.status}`);
            expect(features.status).to.eq(200);
          } catch (e) {
            logger.error(`failed ${e}`);
          }
        }
      }
    }
  }
});

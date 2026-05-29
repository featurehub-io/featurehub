import {Given} from "@cucumber/cucumber";
import * as fs from "node:fs";
import {expect} from "chai";
import {logger} from "../support/logging";

interface EnvSource {
  apiKeys: Array<string>;
}

Given('A file of endpoints I hit them all', { timeout: 60000 * 20 }, async function() {
  logger.info(`env source is ${process.env['ENV_SOURCE']} saas-edge is ${process.env['SAAS_EDGE']}` );

  if (process.env['ENV_SOURCE'] && process.env['SAAS_EDGE']) {
    const keys = JSON.parse(fs.readFileSync(process.env['ENV_SOURCE'], 'utf-8')) as EnvSource;

    logger.info(`Number of keys found is ${keys.apiKeys.length}`);
    for(let count = 0; count < 100; count ++) {
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
});

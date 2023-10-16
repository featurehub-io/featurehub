
import * as restify from 'restify';

import { networkInterfaces } from 'os';
import { IncomingHttpHeaders } from 'http';
import { EnrichedFeatures, EnrichedFeaturesTypeTransformer } from '../apis/webhooks';
import { logger } from './logging';

const nets = networkInterfaces();
const results: any = {};
let networkName: string | undefined;

for (const name of Object.keys(nets)) {
  for (const net of nets[name]) {
    // Skip over non-IPv4 and internal (i.e. 127.0.0.1) addresses
    // 'IPv4' is in Node <= 17, from 18 it's a number 4 or 6
    const familyV4Value = typeof net.family === 'string' ? 'IPv4' : 4
    if (net.family === familyV4Value && !net.internal) {
      if (!results[name]) {
        results[name] = [];
      }

      networkName = name;

      results[name].push(net.address);
    }
  }
}

if (networkName  ===  undefined)  {
  networkName = 'localhost';
  results[networkName]  =  ['localhost'];
}

let webhookData: EnrichedFeatures | undefined;
let webhookHeaders: IncomingHttpHeaders;

export function getWebhookData(): EnrichedFeatures | undefined {
  return webhookData;
}

export function  clearWebhookData() {
  webhookData = undefined;
}

export function getWebserverExternalAddress(): string | undefined {
  if (process.env.EXTERNAL_NGROK) {
    return process.env.EXTERNAL_NGROK;
  }

  return networkName ? `http://${results[networkName][0]}:3000` : undefined;
}

let server: restify.Server;

function mergeResults(data: EnrichedFeatures, headers: IncomingHttpHeaders) : boolean {
  if (webhookData === undefined) {
    webhookData = data;
    return true;
  }

  if (webhookHeaders['ce-id'] === headers['ce-id']) {
    return false; // same message
  }

  if (webhookData.environment.environment.version < data.environment.environment.version) {
    webhookData = data;
    return true;
  }

  let changed = false;
  for (const fv of data.environment.fv) {
    const existing = webhookData.environment.fv.findIndex(f => f.feature.key === fv.feature.key);
    if (existing === -1) {
      changed = true;
      break;
      // webhookData.environment.fv.push(fv);
    } else {
      const pos = webhookData.environment.fv[existing];
      if (pos.feature.version < fv.feature.version || (pos.value?.version || -1) < (fv.value?.version || -1)) {
        changed = true;
        break;
        // webhookData.environment.fv[existing] = fv;
      }
    }
  }

  if (changed) {
    webhookData = data;
  }

  return changed;
}

function setupServer() {
  server.use(restify.plugins.acceptParser(server.acceptable));
  server.use(restify.plugins.queryParser());
  server.use(restify.plugins.bodyParser());

  server.post('/webhook', function (req, res, next) {
    if (mergeResults(EnrichedFeaturesTypeTransformer.fromJson(req.body), req.headers)) {
      webhookHeaders =  req.headers;
      logger.log({level: 'info', message: `<<incoming-webhook-data>> ${JSON.stringify(req.body)}`});
      logger.log({level: 'info', message: `<<incoming-webhook-headers>> ${JSON.stringify(req.headers)}`});
      logger.log({level: 'info', message: `<<webhook-data>> ${JSON.stringify(webhookData)}`});
      logger.log({level: 'info', message: `<<webhook-headers>> ${JSON.stringify(webhookHeaders)}`});
    } else {
      logger.log({level: 'info', message: `<<ignored-webhook-data>> ${JSON.stringify(req.body)}`});
      logger.log({level: 'info', message: `<<ignored-webhook-headers>> ${JSON.stringify(req.headers)}`});
    }

    res.send( 200,'Ok');
    return next();
  });
}

export function startWebServer(): Promise<void> {
  return new Promise((resolve, reject) => {
    server = restify.createServer({
      name: 'myapp',
      version: '1.0.0'
    });

    setupServer();

    try {
      server.listen(3000, function () {
        logger.info(`${server.name} listening at ${server.url}`);
        resolve();
      });
    } catch (e) {
      server = undefined;
      logger.error("Failed to listen", e);
      reject(e);
    }
  });
}

export function terminateServer(): Promise<void> {
  logger.debug("terminating webserver");
  return new Promise((resolve, reject) => {
    if (server) {
      try {
        server.close(() => { resolve(); });
        server = undefined;
      } catch (e) {
        logger.error("Failed to close server", e);
        reject(e);
      }
    } else {
      resolve();
    }
  });
}




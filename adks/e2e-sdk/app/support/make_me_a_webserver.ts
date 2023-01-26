
import * as restify from 'restify';

import { networkInterfaces } from 'os';
import { IncomingHttpHeaders } from 'http';
import { EnrichedFeatures } from '../../../../../javascript-adk/webhook-sdk/app';
import { logger } from './logging';

const nets = networkInterfaces();
const results = {};
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

const server = restify.createServer({
  name: 'myapp',
  version: '1.0.0'
});

server.use(restify.plugins.acceptParser(server.acceptable));
server.use(restify.plugins.queryParser());
server.use(restify.plugins.bodyParser());

let webhookData: EnrichedFeatures | undefined;
let webhookHeaders: IncomingHttpHeaders;

export function getWebhookData(): EnrichedFeatures | undefined {
  return webhookData;
}

export function getWebserverExternalAddress(): string | undefined {
  return networkName ? `http://${results[networkName][0]}:3000` : undefined;
}

export function startWebServer(): string | undefined {
  server.listen(3000, function () {
    console.log('%s listening at %s', server.name, server.url);
  });

  return networkName;
}

export function terminateServer() {
  server.close();
}

server.post('/webhook', function (req, res, next) {
  webhookData = req.body as EnrichedFeatures;
  webhookHeaders =  req.headers;
  logger.log({level: 'info', message: `received webhook ${JSON.stringify(webhookData)}`});

  res.send( 200,'Ok');
  return next();
});



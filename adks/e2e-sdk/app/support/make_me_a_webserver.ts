import * as restify from 'restify';

import {networkInterfaces} from 'os';
import {IncomingHttpHeaders} from 'http';
import {logger} from './logging';
import {CloudEvent, HTTP} from "cloudevents";

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

export function getWebserverExternalAddress(): string | undefined {
  if (process.env.EXTERNAL_NGROK) {
    return process.env.EXTERNAL_NGROK;
  }

  return networkName ? `http://${results[networkName][0]}:3000` : undefined;
}

let server: restify.Server;

export const cloudEvents: Array<CloudEvent<any>> = [];

export function resetCloudEvents() {
  cloudEvents.length = 0;
  logger.info("------------\ncloud events reset\n--------")
}

function mergeCloudEvent<T>(body: T, headers: IncomingHttpHeaders) : CloudEvent<T>[] {
  const ce = HTTP.toEvent<T>({headers: headers, body: body});

  let events = Array.isArray(ce) ? (ce as CloudEvent<T>[]) : [ce as CloudEvent<T>];

  cloudEvents.push(...events);

  logger.info(`Cloud Events provided ${events}`);

  return events;
}

function setupServer() {
  server.use(restify.plugins.acceptParser(server.acceptable));
  server.use(restify.plugins.queryParser());
  server.use (function(req, res, next) {
    logger.info(`received request on path ${req.path()}`);
    if (req.contentType() === 'application/json') {
      var data='';
      req.setEncoding('utf8');
      req.on('data', function(chunk) {
        data += chunk;
      });

      req.on('end', function() {
        req.body = JSON.parse(data);
        logger.info(`'------------------------------\\nbody was ${data}\n---------------------------'`);
        next();
      });
    } else if (req.contentType() === 'application/json+gzip') {
      let data = Buffer.from([]);

      req.on('data', function(chunk) {
        data = Buffer.concat([data, Buffer.from(chunk)]);
        data += chunk;
      });

      req.on('end', function() {
        req.body = data;
        next();
      });
    }
  });

  // server.use(restify.plugins.bodyParser());

  server.post('/featurehub/slack', function (req, res, next) {
    mergeCloudEvent(req.body, req.headers);
    res.send(200, 'ok');
    return next();
  });

  server.post('/webhook', function (req, res, next) {
    mergeCloudEvent(req.body, req.headers);

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




import express from "express";

import {networkInterfaces} from 'os';
import {IncomingHttpHeaders} from 'http';
import {logger} from './logging';
import {CloudEvent, CloudEventV1, HTTP} from "cloudevents";
import http from "http";

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

const port = 3001;

export function getWebserverExternalAddress(): string | undefined {
  if (process.env.EXTERNAL_NGROK) {
    return process.env.EXTERNAL_NGROK;
  }

  return networkName ? `http://${results[networkName][0]}:${port}` : undefined;
}

// let server : express.Express;

export const cloudEvents: Array<CloudEvent<any>> = [];

export function resetCloudEvents() {
  cloudEvents.length = 0;
  logger.info("------------\ncloud events reset\n--------")
  console.log("------------\ncloud events reset\n--------")
}


function mergeCloudEvent<T>(body: T, headers: IncomingHttpHeaders) : CloudEvent<T>[] {
  console.log('headers are ', headers);
  console.log(`body is of type ${typeof body}`);
  const ce = HTTP.toEvent<T>({headers: headers, body: body});

  let events = Array.isArray(ce) ? (ce as CloudEvent<T>[]) : [ce as CloudEvent<T>];

  cloudEvents.push(...events);

  logger.info(`Cloud Events provided ${events}`);

  return events;
}

function setupServer() {
  let server = express()
  server.use(express.json());
  server.use(express.urlencoded({ extended: true }));

  server.use (function(req, res, next) {
    logger.info(`received request on path ${req.path} of content-type ${req.header('content-type')}`);
    if (req.header('content-type') === 'application/json') {
      var data='';
      req.setEncoding('utf8');
      req.on('data', function(chunk) {
        data += chunk;
      });

      req.on('end', function() {
        logger.debug(`'------------------------------\\nbody was ${data}\n---------------------------'`);
        req.body = JSON.parse(data);
        next();
      });
    } else if (req.header('content-type') === 'application/json+gzip') {
      let data = Buffer.from([]);

      console.log(`data created is of type ${typeof data}`);

      req.on('data', function(chunk) {
        data = Buffer.concat([data, Buffer.from(chunk)]);
      });

      req.on('end', function() {
        try {

          // const inflated = Zlib.inflateSync(data).toString();
          // logger.info(`'------------------------------\\nbody was ${inflated}\n---------------------------'`);
          console.log(`data is of type ${typeof data}`);
          req.body = data;
        } catch (e) {
          logger.error('failed to parse', e);
          res.status(500).send('failed to parse');
        }
        next();
      });
    } else {
      next();
    }
  });

  // server.use(restify.plugins.bodyParser());

  server.post('/featurehub/slack', function (req, res, next) {
    console.log('received');
    try {
      mergeCloudEvent(req.body, req.headers);
    } catch (e) {
      logger.error("failed", e);
    }
    console.log('responding ok');
    res.status(200).send('ok');
    return next();
  });

  server.post('/webhook', function (req, res, next) {
    try {
      mergeCloudEvent(req.body, req.headers);
    } catch (e) {
      logger.error("failed", e);
    }

    res.status(200).send( 'Ok');
    return next();
  });

  return server;
}

export let terminateServer: () => Promise<void>;

export function startWebServer(): Promise<void> {
  return new Promise((resolve, reject) => {
    let server = setupServer();
    let app : http.Server | undefined;

    try {
      app = server.listen(port, function () {
        logger.debug(`${server.name}`);
        resolve();
      });
    } catch (e) {
      server = undefined;
      logger.error("Failed to listen", e);
      reject(e);
    }

    terminateServer = () => {
      return new Promise((resolve, reject) => {
        if (server) {
          try {
            app?.close(() => {
              resolve();
            });
            server = undefined;
            app = undefined;
          } catch (e) {
            logger.error("Failed to close server", e);
            reject(e);
          }
        } else {
          resolve();
        }
      });
    }
  });
}




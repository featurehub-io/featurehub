import { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import * as winston from 'winston';
import { MESSAGE } from 'triple-beam';
import stringify from 'safe-stable-stringify';
import { fhLog } from 'featurehub-javascript-client-sdk';

const httpAwareJsonFormatter = winston.format((info, opts) => {
  const json = {};

  if (info.message) {
    json['@message'] = info.message;
    delete info.message;
  }

  if (info.timestamp) {
    json['@timestamp'] = info.timestamp;
    delete info.timestamp;
  }

  let http;
  if (info.http) {
    http = info.http;
    delete info.http;
  }

  if (info.level) {
    json['@level'] = info.level;
    delete info.level;
  }

  json['@timestamp'] = new Date().toISOString()

  // any extra fields
  json['@fields'] = info;

  let msg = stringify(json);

  if (http) {
    msg = msg.substring(0, msg.length - 1) + ', http: ' + http + '}';
  }

  info[MESSAGE] = msg;

  return info;
});

export const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'verbose',
  format: winston.format.combine(
    winston.format.splat(),
    httpAwareJsonFormatter()
  ),
  defaultMeta: {service: 'e2e-sdk-testing'},
  transports: [
    //
    // - Write all logs with level `error` and below to `error.log`
    // - Write all logs with level `verbose` and below to `combined.log`
    //
    new winston.transports.File({filename: 'logs/error.log', level: 'error'}),
    new winston.transports.File({filename: 'logs/combined.log', level: 'verbose'}),
  ],
});

fhLog.error = (...args: any[]) => {
  logger.log({level: 'error', message: args.join(" "), sdkTrace: true});
};

fhLog.log = (...args: any[]) => {
  logger.log({level: 'info', message: args.join(" "), sdkTrace: true});
};

fhLog.trace = (...args: any[]) => {
  logger.log({level: 'error', message: args.map(o => (typeof o === 'string') ? o : JSON.stringify(o).replace('"', "'")).join(" "), sdkTrace: true});
};

fhLog.trace('this is a message');

if (process.env.DEBUG) {
  logger.add(new winston.transports.Console({
    format: winston.format.combine(
      winston.format.splat(),
      winston.format.metadata()
    ),
  }));
}

/*
 * Now we need to ensure we can track all HTTP request/responses if we need to. This
 * code always logs all requests/responses in verbose mode, and error returns are logged
 * in the error mode.
 */

export const responseToRecord = function (response: AxiosResponse) {
  const reqConfig = response.config;
  if (reqConfig.url.endsWith('/mr-api/login')) return undefined;
  return {
    type: 'response',
    status: response.status,
    statusText: response.statusText,
    headers: response.headers,
    data: response.data,
    request: {
      headers: reqConfig.headers,
      method: reqConfig.method,
      data: reqConfig.data,
      url: reqConfig.url,
    }
  };
};

export function axiosLoggingAttachment(axiosInstances: Array<AxiosInstance>) {
  axiosInstances.forEach((axios) => {
    axios.interceptors.response.use((resp: AxiosResponse) => {
      const responseToLog = responseToRecord(resp);
      if (responseToLog !== undefined) {
        logger.log({level: 'verbose', message: 'response:', http: JSON.stringify(responseToLog, undefined, 2)});
      }
      return resp;
    }, (error) => {
      if (error.response) {
        logger.log({
          level: 'error',
          message: 'response rejected:',
          http: JSON.stringify(responseToRecord(error.response), undefined, 2)
        });
      }
      return Promise.reject(error);
    });

    axios.interceptors.request.use((reqConfig: InternalAxiosRequestConfig) => {
      const req = {
        type: 'request',
        headers: reqConfig.headers,
        method: reqConfig.method,
        data: reqConfig.data,
        url: reqConfig.url,
      };
      logger.log({level: 'verbose', message: 'request', http: JSON.stringify(req, undefined, 2)});
      return reqConfig;
    }, (error) => Promise.reject(error));
  });
}



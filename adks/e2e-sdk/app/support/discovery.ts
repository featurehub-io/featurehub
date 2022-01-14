import { Configuration, InfoServiceApi } from 'featurehub-javascript-admin-sdk';
import { logger } from './logging';

export abstract class BackendDiscovery {
  private static _mrPort = 8903;
  private static _featuresPort = 8604;
  private static _isRESTEdge = false;
  private static _discovered = false;

  public static get mrPort() {
    return BackendDiscovery._mrPort;
  }

  public static get featuresPort() {
    return BackendDiscovery._featuresPort;
  }

  public static get supportsSSE() {
    return !this._isRESTEdge;
  }

  static async edgePortCheck(port: number): Promise<boolean> {
    try {
      logger.info('looking for edge on port %d', port);
      const versionInfo = await (new InfoServiceApi(new Configuration({ basePath: `http://localhost:${port}` }))).getInfoVersion();
      logger.info('edge version is', versionInfo.data);
      if (versionInfo.data.name === 'edge-full') {
        this._featuresPort = port;
        this._isRESTEdge = false;
        logger.info('found edge full on port %d', port);
        return true;
      } else if (versionInfo.data.name === 'edge-rest') {
        this._featuresPort = port;
        this._isRESTEdge = true;
        logger.info('found edge rest on port %d', port);
        return true;
      }
      // eslint-disable-next-line no-empty
    } catch (e) {
      // console.log('failed ', e);
    }

    return false;
  }

  static async mrPortCheck(port: number): Promise<boolean> {
    try {
      logger.info('checking for mr on port %d', port);
      const versionInfo = await (new InfoServiceApi(new Configuration({ basePath: `http://localhost:${port}` }))).getInfoVersion();
      if (versionInfo.data.name == 'party-server') {
        this._discovered = true;
        this._featuresPort = port;
        this._mrPort = port;
        this._isRESTEdge = false;
        return true;
      } else if (versionInfo.data.name === 'party-server-ish') {
        this._discovered = true;
        this._featuresPort = port;
        this._mrPort = port;
        this._isRESTEdge = true;
      } else if (versionInfo.data.name === 'management-repository') {
        this._mrPort = port;
        // now lets try and find the features repo
        if (!await this.edgePortCheck(8064)) {
          if (!await this.edgePortCheck(8553)) {
            if (!await this.edgePortCheck(8702)) {
              throw new Error(`Cannot find edge! But we found MR on port ${port}`);
            }
          }
        }

        return true;
      } else {
        logger.info(`No MR variant on ${port}`);
      }
      // eslint-disable-next-line no-empty
    } catch (e) {
      // console.log('failed', e);
    }

    return this._discovered;
  }

  static async discover(): Promise<void> {
    if (!BackendDiscovery._discovered) {
      if (!await this.mrPortCheck(8903)) { // local run port
        if (!await this.mrPortCheck(8085)) { // normal docker port
          if (!await this.mrPortCheck(80)) { // kubernetes
            throw new Error('Cannot determine where MR is');
          }
        }
      }
    }
  }
}

export async function discover() {
  await BackendDiscovery.discover();
}

export function mrHost() {
  return `http://localhost:${BackendDiscovery.mrPort}`;
}

export function edgeHost() {
  return `http://localhost:${BackendDiscovery.featuresPort}`;
}

export function supportsSSE() {
  return BackendDiscovery.supportsSSE;
}

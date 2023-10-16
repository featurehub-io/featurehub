import { Configuration, InfoServiceApi } from '../apis/mr-service';
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
        logger.info('we are using party-server-ish');
        return true;
      } else if (versionInfo.data.name === 'management-repository') {
        this._mrPort = port;
        if (port === 80) { // its k8s, all god
          this._discovered = true;
          this._featuresPort = port;
          this._isRESTEdge = true;
        } else {
          // now lets try and find the features repo
          if (!await this.edgePortCheck(8064)) {
            if (!await this.edgePortCheck(8553)) {
              if (!await this.edgePortCheck(8702)) {
                throw new Error(`Cannot find edge! But we found MR on port ${port}`);
              }
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

  static async discoverRestEdge(baseUrl: string) {
    try {
      const versionInfo = await (new InfoServiceApi(new Configuration({ basePath: baseUrl }))).getInfoVersion();
      this._isRESTEdge = (versionInfo.data.name == 'party-server-ish');
    } catch (e) {
      console.log('unable to determine what type of server is running', e);
    }
  }

  static async discover(): Promise<void> {
    if (process.env.REMOTE_BACKEND || process.env.FEATUREHUB_BASE_URL) {
      await this.discoverRestEdge(process.env.REMOTE_BACKEND || process.env.FEATUREHUB_BASE_URL || '');
      return;
    }
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
  return  process.env.FEATUREHUB_BASE_URL || process.env.REMOTE_BACKEND || `http://localhost:${BackendDiscovery.mrPort}`;
}

export function edgeHost() {
  if (process.env.FEATUREHUB_EDGE_URL) {
    return process.env.FEATUREHUB_EDGE_URL;
  }

  if (process.env.REMOTE_BACKEND) {
    const backend = process.env.REMOTE_BACKEND;

    if (backend.includes('/pistachio/')) {
      return backend.substring(0, backend.lastIndexOf('/'));
    }

    return backend;
  }
  return `http://localhost:${BackendDiscovery.featuresPort}`;
}

export function supportsSSE() {
  return (process.env.REMOTE_BACKEND || process.env.FEATUREHUB_BASE_URL) ? true : BackendDiscovery.supportsSSE;
}

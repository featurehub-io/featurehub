import { expect } from 'chai';
import { EdgeFeatureHubConfig, EdgeService } from '../app';
import { Substitute, } from '@fluffy-spoon/substitute';

describe('We can initialize the config', () => {

  it('should construct urls properly', () => {
    const fc = new EdgeFeatureHubConfig('http://localhost:8080', '123*345');
    expect(fc.url()).to.eq('http://localhost:8080/features/123*345');
  });

  it('should allow me to specify a config and initialise the config', () => {
    const edge = Substitute.for<EdgeService>();
    EdgeFeatureHubConfig.defaultEdgeServiceSupplier = () =>
      edge;

    const fc = new EdgeFeatureHubConfig('http://localhost:8080', '123*345');
    // tslint:disable-next-line:no-unused-expression
    expect(fc.clientEvaluated()).to.be.true;
    fc.init();

    edge.received(1).poll();
  });

  it('asking a new config for edge and repository should repeatedly give the same one', () => {
    const edge = Substitute.for<EdgeService>();
    const edgeProvider = (repo1, config) => edge;
    EdgeFeatureHubConfig.defaultEdgeServiceSupplier = edgeProvider;

    const fc = new EdgeFeatureHubConfig('http://localhost:8080', '123*345');
    expect(fc.edgeServiceProvider()).to.eq(edgeProvider);
    expect(fc.edgeServiceProvider()).to.eq(edgeProvider);
    const repo = fc.repository();
    // tslint:disable-next-line:no-unused-expression
    expect(repo).to.not.be.null;
    expect(fc.repository()).to.eq(repo);
  });

  it('should allow for the creation of a new context which on building should poll the edge repo', async () => {
    const edge = Substitute.for<EdgeService>();
    EdgeFeatureHubConfig.defaultEdgeServiceSupplier = (repo, config) =>
      edge;

    const fc = new EdgeFeatureHubConfig('http://localhost:8080', '123345');
    // tslint:disable-next-line:no-unused-expression
    expect(fc.clientEvaluated()).to.be.false;
    await fc.newContext().build();
    edge.received(1).contextChange('');
  });
});

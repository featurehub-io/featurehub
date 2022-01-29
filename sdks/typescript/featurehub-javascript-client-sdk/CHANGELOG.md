## Changelog

### featurehub-javascript-client-sdk
#### 1.0.11
- Provided additional getters to get feature values and properties [GitHub PR](https://github.com/featurehub-io/featurehub/pull/656/)
#### 1.0.10
- Fix a bug related to Catch & Release mode [GitHub issue](https://github.com/featurehub-io/featurehub/issues/648)
#### 1.0.9
- Enabled e-tag support
#### 1.0.8
- Enabled Tree Shaking [GitHub issue](https://github.com/featurehub-io/featurehub/issues/509)
- Decrease sdk size by replacing ip6addr.ts with netmask package.
#### 1.0.7
- Support static flag evaluation [GitHub issue](https://github.com/featurehub-io/featurehub/issues/497)
- Decrease sdk size by replacing semver with semver-compare [GitHub issue](https://github.com/featurehub-io/featurehub/issues/498)
#### 1.0.6
- Fix to the SSE client to prevent excess of connections to the server.
#### 1.0.5
- Fix an issue with the polling client
#### 1.0.4
- Documentation updates
#### 1.0.3
- Bugfix: Edge server urls passed to the config that include '/feature' should be processed correctly
#### 1.0.2
- Documentation updates
#### 1.0.1
- Fix regression bug with strategies not being passed correctly and thus not serving the expected feature values
#### 1.0.0
- Move from featurehub-eventsource-sdk + featurehub-repository, split out nodejs into its own repository to allow
  Angular & Vue to use this library.

### featurehub-javascript-node-sdk
#### 1.0.11
- Provided additional getters to get feature values and properties [GitHub PR](https://github.com/featurehub-io/featurehub/pull/656/)
#### 1.0.10
- Fix a bug related to Catch & Release mode [GitHub issue](https://github.com/featurehub-io/featurehub/issues/648)
#### 1.0.9
- Enabled e-tag support
#### 1.0.8
- Decrease sdk size by replacing ip6addr.ts with netmask package.
#### 1.0.7
- Support static flag evaluation [GitHub issue](https://github.com/featurehub-io/featurehub/issues/497)
- Decrease sdk size by replacing semver with semver-compare [GitHub issue](https://github.com/featurehub-io/featurehub/issues/498)
#### 1.0.6
- Fix to the SSE client to prevent excess of connections to the server.
#### 1.0.5
- Fix an issue with the polling client
#### 1.0.4
- Documentation updates
#### 1.0.3
- Bugfix: Edge server urls passed to the config that include '/feature' should be processed correctly
#### 1.0.2
- Documentation updates
#### 1.0.1
- Symlink readme file from featurehub-javascript-client-sdk

#### 1.0.0
- Move from featurehub-eventsource-sdk + featurehub-repository, split out nodejs into its own repository to allow
  Angular & Vue to use this library. 

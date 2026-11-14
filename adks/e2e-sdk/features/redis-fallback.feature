# Only run by the e2e variant that has Redis configured (sep-google-redis). It sets up the data, then the build
# (fh-github-watcher) stops MR, restarts dacha2/edge and checks the feature is still served from Redis, then turns
# Redis off and checks it is not. It is deliberately not tagged @streamingvariants / @allvariants.
@redis-fallback-setup
Feature: Data for the dacha2 Redis fallback test

  Scenario: I set up a feature to be served from Redis while MR is down
    Given I create a new portfolio
    And I create an application
    And I create a service account called "Redis Fallback" with named permissions "all" with current environment
    And I connect to the feature server
    When There is a feature flag with the key REDIS_FALLBACK_FLAG
    And I set the feature flag to unlocked and on
    Then I write out a redis-fallback config file

package io.featurehub.dacha.api;

/**
 *
 */
public enum CacheAction {
    WAITING_FOR_COMPLETE_SOURCE,
    WAITING_FOR_COMPLETE_CACHE,
    ATTEMPTING_TO_BECOME_MASTER,
    WAITING_FOR_NEW_MASTER,
    AM_MASTER,
    AT_REST
}

package io.featurehub.client.edge;

public enum EdgeConnectionState {
  // {SSE + GET]
  // the api key was not known by the server, and this is a terminal failure. We cannot recover from this
  // so we need to set the repository into FAILURE mode
  API_KEY_NOT_FOUND,

  // [SSE + GET]
  // we timed out trying to connect to the server. We should backoff briefly and try and connect again. May
  // require increasing backoff
  SERVER_CONNECT_TIMEOUT, // timeout connecting to url, retryable

  // [SSE Only] this is the normal ping/pong of the server connection disconnecting us, we should delay a random amount
  // of time an reconnect.
  SERVER_SAID_BYE, // we got kicked off after a normal timeout using eventsource

  // [SSE + GET] we never received a response after we did actually connect, we should backoff
  SERVER_WAS_DISCONNECTED, // we got a disconnect before we received a "bye"

  SUCCESS
}

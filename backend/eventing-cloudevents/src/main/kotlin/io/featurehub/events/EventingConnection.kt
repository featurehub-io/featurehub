package io.featurehub.events

interface EventingConnection {
  enum class ConnectionStatus { DISCONNECTED, CONNECTED }

  fun registerForConnectionEvents(handler: (event: ConnectionStatus) -> Unit)

  fun status(): ConnectionStatus
}

/**
 * The default implementation (used by PubSub) ignores disconnection errors.
 */
class DefaultEventingConnection : EventingConnection {
  override fun registerForConnectionEvents(handler: (event: EventingConnection.ConnectionStatus) -> Unit) {
    handler(EventingConnection.ConnectionStatus.CONNECTED)
  }

  override fun status(): EventingConnection.ConnectionStatus {
    return EventingConnection.ConnectionStatus.CONNECTED
  }
}

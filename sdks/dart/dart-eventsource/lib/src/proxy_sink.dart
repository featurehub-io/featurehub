library featurehub_sse_client.src.proxy_sink;

/// Just a simple [Sink] implementation that proxies the [add] and [close]
/// methods.
class ProxySink<T> implements Sink<T> {
  Function onAdd;
  Function onClose;
  ProxySink({this.onAdd, this.onClose});
  @override
  void add(t) => onAdd(t);
  @override
  void close() => onClose();
}

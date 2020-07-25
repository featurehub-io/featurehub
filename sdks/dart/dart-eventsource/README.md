# SSE-Client 

A client only library for using EventSource or Server-Sent Events (SSE). 

This library implements the interface as described [here](https://html.spec.whatwg.org/multipage/comms.html#server-sent-events).

## Origin

This packages is a clone of the `eventsource` library but because we have not been able to get critical changes
merged in, we have had to release this package instead. It drops the incomplete server functionality and we have
renamed the library to `featurehub_sse_client`.

Further, with Dart now supporting target platform detection (when detecting dart.io vs dart.html) it makes more sense
to use the browser specific SSE client (`dart:html / EventSource`) as it is native to the browser.

## Client usage

For more advanced usage, see the `example/` directory. 
Creating a new EventSource client is as easy as a single call.
The http package is used under the hood, so wherever this package works, this lbirary will also work.
Browser usage is slightly different.

```dart
EventSource eventSource = await EventSource.connect("http://example.com/events");
// in browsers, you need to pass a http.BrowserClient:
EventSource eventSource = await EventSource.connect("http://example.com/events", 
    client: new http.BrowserClient());
```

If you wish to have it connect only when the first listener attaches (and not otherwise), then pass the
optional parameter `openOnlyOnFirstListener` (as true). If you wish to close it after the last listener detaches, then
pass the optional parmeter `closeOnLastListener`.

```dart
EventSource eventSource = await EventSource.connect("http://example.com/events", 
  openOnlyOnFirstListener: true, closeOnLastListener: true);
```

## Licensing

This project is available under the MIT license, as can be found in the LICENSE file.

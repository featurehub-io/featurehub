export 'src/repository.dart';
export 'src/sse_client.dart'
    if (dart.library.io) 'src/sse_client_dartio.dart'
    if (dart.library.html) 'src/sse_client_darthtml.dart';

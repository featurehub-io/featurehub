import 'package:dio/dio.dart';

///
/// This class gets created and put in the stream in MRBloc if we get a response back from the server
/// indicating there is a pending maintenance event or an active maintenance event
///
class MaintenanceInfo {
  final String? message;
  final DateTime? start;
  final DateTime? end;

  bool isValid() {
    return message != null && start != null && end != null;
  }

  bool isActive() {
    final now = DateTime.now();
    return start != null && end != null && start!.isBefore(now) && end!.isAfter(now);
  }

  const MaintenanceInfo({this.message, this.start, this.end});

  /// The x-maintenance-* headers the server adds to every response while a maintenance window is announced.
  /// Returns null if they are missing or incomplete, which means there is no maintenance to tell the user about.
  static MaintenanceInfo? fromHeaders(Headers headers) {
    final endStr = headers.value('x-maintenance-end');
    if (endStr == null) return null;

    final info = MaintenanceInfo(
      message: headers.value('x-maintenance-message'),
      start: DateTime.tryParse(headers.value('x-maintenance-start') ?? ''),
      end: DateTime.tryParse(endStr),
    );

    return info.isValid() ? info : null;
  }

  // equality lets the bloc only push to its streams when the maintenance state actually changes
  @override
  bool operator ==(Object other) =>
      other is MaintenanceInfo &&
      other.message == message &&
      other.start == start &&
      other.end == end;

  @override
  int get hashCode => Object.hash(message, start, end);
}

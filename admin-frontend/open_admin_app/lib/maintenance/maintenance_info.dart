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
}

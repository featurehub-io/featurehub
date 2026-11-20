import 'package:intl/intl.dart';

/// Dates come back from the MR API as UTC [DateTime]s (the generated client
/// uses `DateTime.parse` on ISO-8601 strings with a `Z`/offset), and intl's
/// [DateFormat] does not convert time zones - so always go through here to
/// show a timestamp in the user's local time zone.
final DateFormat _localDateTimeFormat = DateFormat('yyyy-MM-dd HH:mm:ss');

/// Formats [dateTime] in the browser's local time zone as `yyyy-MM-dd HH:mm:ss`.
String formatLocalDateTime(DateTime dateTime) =>
    _localDateTimeFormat.format(dateTime.toLocal());

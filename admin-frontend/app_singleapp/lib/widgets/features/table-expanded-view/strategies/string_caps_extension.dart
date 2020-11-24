extension CapExtension on String {
  String get inCaps => '${this[0].toUpperCase()}${substring(1)}';
  String get capitalizeFirstofEach {
    if (this == null || isEmpty) return '';
    return split(' ').map((str) {
      return str.inCaps;
    }).join(' ');
  }
}


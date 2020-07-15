import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/widgets.dart';
import 'package:logging/logging.dart';
import 'package:universal_html/html.dart' as html;

final _log = Logger("customCursor");

class CustomCursor extends MouseRegion {
  static final appContainer =
      html.window.document.getElementById('app-container');
  // cursor types from http://www.javascripter.net/faq/stylesc.htm
  static final String pointer = 'pointer';
  static final String auto = 'auto';
  static final String move = 'move';
  static final String noDrop = 'no-drop';
  static final String colResize = 'col-resize';
  static final String allScroll = 'all-scroll';
  static final String notAllowed = 'not-allowed';
  static final String rowResize = 'row-resize';
  static final String crosshair = 'crosshair';
  static final String progress = 'progress';
  static final String eResize = 'e-resize';
  static final String neResize = 'ne-resize';
  static final String text = 'text';
  static final String nResize = 'n-resize';
  static final String nwResize = 'nw-resize';
  static final String help = 'help';
  static final String verticalText = 'vertical-text';
  static final String sResize = 's-resize';
  static final String seResize = 'se-resize';
  static final String inherit = 'inherit';
  static final String wait = 'wait';
  static final String wResize = 'w-resize';
  static final String swResize = 'sw-resize';
  CustomCursor({Widget child, String cursorStyle = 'pointer'})
      : super(
            onHover: (PointerHoverEvent evt) {
              if (appContainer == null) {
                _log.severe(
                    'You need to make the body of the index.html say <body id="app-container"> so the hover works');
              }
              if (kIsWeb && appContainer != null) {
                appContainer.style.cursor = cursorStyle;
              }
            },
            onExit: (PointerExitEvent evt) {
              if (kIsWeb && appContainer != null) {
                appContainer.style.cursor = 'default';
              }
            },
            child: child);
}

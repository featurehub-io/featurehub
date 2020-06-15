import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

typedef ChipsInputSuggestions<T> = FutureOr<List<T>> Function(String query);
typedef ChipSelected<T> = void Function(T data, bool selected);
typedef ChipsBuilder<T> = Widget Function(
    BuildContext context, ChipsInputState<T> state, T data);

class ChipsInput<T> extends StatefulWidget {
  ChipsInput({
    Key key,
    this.initialValue = const [],
    this.decoration = const InputDecoration(),
    this.enabled = true,
    @required this.chipBuilder,
    @required this.suggestionBuilder,
    @required this.findSuggestions,
    @required this.onChanged,
    this.onChipTapped,
    this.maxChips,
    this.textStyle,
  })  : assert(maxChips == null || initialValue.length <= maxChips),
        super(key: key);

  final InputDecoration decoration;
  final TextStyle textStyle;
  final bool enabled;
  final ChipsInputSuggestions findSuggestions;
  final ValueChanged<List<T>> onChanged;
  final ValueChanged<T> onChipTapped;
  final ChipsBuilder<T> chipBuilder;
  final ChipsBuilder<T> suggestionBuilder;
  final List<T> initialValue;
  final int maxChips;

  @override
  ChipsInputState<T> createState() => ChipsInputState<T>();
}

class ChipsInputState<T> extends State<ChipsInput<T>>
    implements TextInputClient {
  static const kObjectReplacementChar = 0xFFFC;
  Set<T> _chips = <T>{};
  List<T> _suggestions;
  StreamController<List<T>> _suggestionsStreamController;
  int _searchId = 0;
  FocusNode _focusNode;
  TextEditingValue _value = TextEditingValue();
  TextInputConnection _connection;
  _SuggestionsBoxController _suggestionsBoxController;
  final LayerLink _layerLink = LayerLink();
  Size size;

  String get text => String.fromCharCodes(
        _value.text.codeUnits.where((ch) => ch != kObjectReplacementChar),
      );

  bool get _hasInputConnection => _connection != null && _connection.attached;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    _chips.clear();
    _chips.addAll(widget.initialValue);
    _updateTextInputState();

    _suggestionsBoxController = _SuggestionsBoxController(context);
    _suggestionsStreamController ??= StreamController<List<T>>.broadcast();

    _initFocusNode();
  }

  void _initFocusNode() {
    setState(() {
      if (widget.enabled && mounted) {
        if (widget.maxChips == null || _chips.length < widget.maxChips) {
          _focusNode = FocusNode();
          (() async {
            await _initOverlayEntry();
            _focusNode.addListener(_onFocusChanged);
            // in case we already missed the focus event
            if (_focusNode.hasFocus) {
              _suggestionsBoxController.open();
            }
          })();
        } else {
          _focusNode = AlwaysDisabledFocusNode();
        }
      } else {
        _focusNode = AlwaysDisabledFocusNode();
      }
    });
  }

  void _onFocusChanged() {
    if (_suggestionsBoxController != null && mounted) {
      if (_focusNode.hasFocus) {
        _openInputConnection();
        // if()
        _suggestionsBoxController.open();
      } else {
        _closeInputConnectionIfNeeded();
        _suggestionsBoxController.close();
      }
      setState(() {
        /*rebuild so that _TextCursor is hidden.*/
      });
    }
  }

  Future<void> _initOverlayEntry() async {
    if (_suggestionsStreamController == null || !mounted) {
      return;
    }

    RenderBox renderBox = context.findRenderObject();
    // TODO: See if after_layout mixin (https://pub.dartlang.org/packages/after_layout) works instead of keep checking if rendered

    while (renderBox == null) {
      await Future.delayed(Duration(milliseconds: 10));

      renderBox = context.findRenderObject();
    }

    while (!renderBox.hasSize) {
      await Future.delayed(Duration(milliseconds: 10));
    }

    size = renderBox.size;

    _suggestionsBoxController._overlayEntry = OverlayEntry(
      builder: (context) {
        return StreamBuilder(
          stream: _suggestionsStreamController.stream,
          builder:
              (BuildContext context, AsyncSnapshot<List<dynamic>> snapshot) {
            if (snapshot.data != null && snapshot.data.isNotEmpty) {
              return Positioned(
                width: size.width,
                child: CompositedTransformFollower(
                  link: _layerLink,
                  showWhenUnlinked: false,
                  offset: Offset(0.0, size.height / 2),
                  child: Material(
                    elevation: 4.0,
                    child: ListView.builder(
                      shrinkWrap: true,
                      padding: EdgeInsets.zero,
                      itemCount: snapshot.data?.length ?? 0,
                      itemBuilder: (BuildContext context, int index) {
                        return widget.suggestionBuilder(
                            context, this, _suggestions[index]);
                      },
                    ),
                  ),
                ),
              );
            } else {
              return Container();
            }
          },
        );
      },
    );
  }

  @override
  void dispose() {
    //TODO upgrade to latest when fixed
    //_focusNode?.dispose();  https://github.com/danvick/flutter_chips_input/issues/26
    _closeInputConnectionIfNeeded();
    _suggestionsBoxController.dispose();
    _suggestionsBoxController = null;
    super.dispose();
  }

  void requestKeyboard() {
    if (_focusNode.hasFocus) {
      _openInputConnection();
    } else {
      FocusScope.of(context).requestFocus(_focusNode);
    }
  }

  void selectSuggestion(T data) {
    setState(() {
      _chips.add(data);
      if (widget.maxChips != null) _initFocusNode();
      _updateTextInputState();
      _suggestions = null;
      _suggestionsStreamController.add(_suggestions);
    });
    widget.onChanged(_chips.toList(growable: false));
  }

  void deleteChip(T data) {
    if (widget.enabled) {
      setState(() {
        _chips.remove(data);
        _updateTextInputState();
      });
      if (widget.maxChips != null) _initFocusNode();
      widget.onChanged(_chips.toList(growable: false));
    }
  }

  void _openInputConnection() {
    if (!_hasInputConnection) {
      _connection = TextInput.attach(this, TextInputConfiguration());
      _connection.setEditingState(_value);
    }
    _connection.show();
  }

  void _closeInputConnectionIfNeeded() {
    if (_hasInputConnection) {
      _connection.close();
      _connection = null;
    }
  }

  @override
  Widget build(BuildContext context) {
    var chipsChildren = _chips
        .map<Widget>((data) => widget.chipBuilder(context, this, data))
        .toList();

    final theme = Theme.of(context);

    chipsChildren.add(
      Container(
        height: 32.0,
        child: Row(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Text(
              text,
              style: widget.textStyle ??
                  theme.textTheme.subtitle1.copyWith(height: 1.5),
            ),
            _TextCaret(
              resumed: _focusNode.hasFocus,
            ),
          ],
        ),
      ),
    );

    return CompositedTransformTarget(
      link: _layerLink,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: requestKeyboard,
        child: InputDecorator(
          decoration: widget.decoration,
          isFocused: _focusNode.hasFocus,
          isEmpty: _value.text.isEmpty && _chips.isEmpty,
          child: Wrap(
            children: chipsChildren,
            spacing: 4.0,
            runSpacing: 4.0,
          ),
        ),
      ),
    );
  }

  @override
  void updateEditingValue(TextEditingValue value) {
    final oldCount = _countReplacements(_value);
    final newCount = _countReplacements(value);
    setState(() {
      if (newCount < oldCount) {
        _chips = Set.from(_chips.take(newCount));
        widget.onChanged(_chips.toList(growable: false));
      }
      _value = value;
    });
    _onSearchChanged(text);
  }

  int _countReplacements(TextEditingValue value) {
    return value.text.codeUnits
        .where((ch) => ch == kObjectReplacementChar)
        .length;
  }

  @override
  void performAction(TextInputAction action) {
    _focusNode.unfocus();
  }

  void _updateTextInputState() {
    final text =
        String.fromCharCodes(_chips.map((_) => kObjectReplacementChar));
    _value = TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: text.length),
      composing: TextRange(start: 0, end: text.length),
    );

    // if _connection is null, assign it something
    _connection ??= TextInput.attach(this, TextInputConfiguration());
    _connection.setEditingState(_value);
  }

  void _onSearchChanged(String value) async {
    final localId = ++_searchId;
    final results = await widget.findSuggestions(value);
    if (_searchId == localId && mounted) {
      setState(() => _suggestions = results
          .where((profile) => !_chips.contains(profile))
          .toList(growable: false));
    }
    _suggestionsStreamController.add(_suggestions);
  }

  @override
  void updateFloatingCursor(RawFloatingCursorPoint point) {}

  @override
  void connectionClosed() {}

  @override
  void showAutocorrectionPromptRect(int start, int end) {}

  @override
  // TODO: implement currentTextEditingValue
  TextEditingValue get currentTextEditingValue => throw UnimplementedError();

  @override
  // TODO: implement currentAutofillScope
  AutofillScope get currentAutofillScope => throw UnimplementedError();
}

class AlwaysDisabledFocusNode extends FocusNode {
  @override
  bool get hasFocus => false;
}

class _TextCaret extends StatefulWidget {
  const _TextCaret({
    Key key,
    this.duration = const Duration(milliseconds: 500),
    this.resumed = false,
  }) : super(key: key);

  final Duration duration;
  final bool resumed;

  @override
  _TextCursorState createState() => _TextCursorState();
}

class _TextCursorState extends State<_TextCaret>
    with SingleTickerProviderStateMixin {
  bool _displayed = false;
  Timer _timer;

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(widget.duration, _onTimer);
  }

  void _onTimer(Timer timer) {
    setState(() => _displayed = !_displayed);
  }

  @override
  void dispose() {
    _timer.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return FractionallySizedBox(
      heightFactor: 0.7,
      child: Opacity(
        opacity: _displayed && widget.resumed ? 1.0 : 0.0,
        child: Container(
          width: 2.0,
          color: theme.primaryColor,
        ),
      ),
    );
  }
}

class _SuggestionsBoxController {
  final BuildContext context;

  OverlayEntry _overlayEntry;

  bool _isOpened = false;

  _SuggestionsBoxController(this.context);

  void open() {
    if (_isOpened) return;
    assert(_overlayEntry != null);
    Overlay.of(context).insert(_overlayEntry);
    _isOpened = true;
  }

  void close() {
    if (!_isOpened) return;
    assert(_overlayEntry != null);
    _overlayEntry.remove();
    _isOpened = false;
  }

  void dispose() {
    if (_overlayEntry != null) {
      _overlayEntry.remove();
    }
    _isOpened = false;
  }

  void toggle() {
    if (_isOpened) {
      close();
    } else {
      open();
    }
  }
}

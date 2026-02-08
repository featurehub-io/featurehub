import 'package:flutter/material.dart';

class FHMultiSelect<T> extends StatefulWidget {
  final List<T> availableValues;
  final List<T> selectedValues;
  final ValueChanged<List<T>> onChanged;
  final Widget? icon;
  final Widget? hint;

  const FHMultiSelect({
    Key? key,
    required this.availableValues,
    required this.selectedValues,
    required this.onChanged,
    this.icon,
    this.hint,
  }) : super(key: key);

  @override
  _FHMultiSelectState<T> createState() => _FHMultiSelectState<T>();
}

class _FHMultiSelectState<T> extends State<FHMultiSelect<T>> {
  late List<T> _selectedValues;
  final LayerLink _layerLink = LayerLink();
  OverlayEntry? _overlayEntry;

  @override
  void initState() {
    super.initState();
    _selectedValues = List<T>.from(widget.selectedValues);
  }

  @override
  void didUpdateWidget(covariant FHMultiSelect<T> oldWidget) {
    super.didUpdateWidget(oldWidget);

    // Update selected values if parent provides new data
    if (widget.selectedValues != oldWidget.selectedValues) {
      _selectedValues = List<T>.from(widget.selectedValues);
    }

    // Remove any selected values that no longer exist in availableValues
    if (widget.availableValues != oldWidget.availableValues) {
      _selectedValues = _selectedValues
          .where((v) => widget.availableValues.contains(v))
          .toList();
    }
  }

  void _toggleDropdown() {
    if (_overlayEntry == null) {
      _overlayEntry = _createOverlayEntry();
      Overlay.of(context).insert(_overlayEntry!);
    } else {
      _overlayEntry?.remove();
      _overlayEntry = null;
    }
    setState(() {});
  }

  OverlayEntry _createOverlayEntry() {
    final renderBox = context.findRenderObject() as RenderBox;
    final size = renderBox.size;
    final offset = renderBox.localToGlobal(Offset.zero);

    return OverlayEntry(
      builder: (context) => Stack(
        children: [
          // barrier to detect taps outside
          Positioned.fill(
            child: GestureDetector(
              onTap: _toggleDropdown,
              behavior: HitTestBehavior.translucent,
              child: Container(color: Colors.transparent),
            ),
          ),
          Positioned(
            left: offset.dx,
            top: offset.dy + size.height + 5,
            width: size.width,
            child: CompositedTransformFollower(
              link: _layerLink,
              showWhenUnlinked: false,
              offset: Offset(0, size.height + 5),
              child: Material(
                elevation: 4.0,
                borderRadius: BorderRadius.circular(8),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(
                    maxHeight: 250,
                  ),
                  child: StatefulBuilder(
                    builder: (context, setStateOverlay) {
                      return ListView(
                        padding: EdgeInsets.zero,
                        shrinkWrap: true,
                        children: widget.availableValues.map((item) {
                          final selected = _selectedValues.contains(item);
                          return Material(
                            color: Colors.transparent,
                            child: InkWell(
                              onTap: () {
                                setState(() {
                                  if (selected) {
                                    _selectedValues.remove(item);
                                  } else {
                                    _selectedValues.add(item);
                                  }
                                  widget.onChanged(_selectedValues);
                                });
                                setStateOverlay(() {});
                              },
                              hoverColor: Theme.of(context)
                                  .colorScheme
                                  .primaryContainer
                                  .withAlpha(153),
                              highlightColor: Theme.of(context)
                                  .colorScheme
                                  .primaryContainer
                                  .withAlpha(253),
                              child: Padding(
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 8, vertical: 4),
                                child: Row(
                                  children: [
                                    Checkbox(
                                      value: selected,
                                      onChanged: (checked) {
                                        setState(() {
                                          if (checked == true) {
                                            _selectedValues.add(item);
                                          } else {
                                            _selectedValues.remove(item);
                                          }
                                          widget.onChanged(_selectedValues);
                                        });
                                        setStateOverlay(() {});
                                      },
                                    ),
                                    Expanded(
                                      child: Text(
                                        item.toString(),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          );
                        }).toList(),
                      );
                    },
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _overlayEntry?.remove();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return CompositedTransformTarget(
      link: _layerLink,
      child: MouseRegion(
        cursor: SystemMouseCursors.click,
        child: GestureDetector(
          onTap: _toggleDropdown,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            height: 40,
            decoration: BoxDecoration(
              border: Border.all(color: Theme.of(context).dividerColor),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Row(
              children: [
                if (widget.icon != null) ...[
                  widget.icon!,
                  const SizedBox(width: 8),
                ],
                Expanded(
                  child: _selectedValues.isEmpty
                      ? (widget.hint ?? const SizedBox())
                      : Text(
                          _selectedValues.join(', '),
                          overflow: TextOverflow.ellipsis,
                        ),
                ),
                Icon(
                  _overlayEntry == null
                      ? Icons.arrow_drop_down
                      : Icons.arrow_drop_up,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

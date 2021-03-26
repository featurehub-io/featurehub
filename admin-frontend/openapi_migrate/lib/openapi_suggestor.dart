import 'package:analyzer/analyzer.dart';
import 'package:codemod/codemod.dart';

List<String> openapiClasses = ['UserCredentials'];

class OpenPatchSuggestor extends GeneralizingAstVisitor<void>
    with AstVisitingSuggestor {
  @override
  void visitCascadeExpression(CascadeExpression node) {
    final target = node.target;
    String className;

    if (target is InstanceCreationExpression) {
      if (openapiClasses.contains(target.constructorName.type.name.name)) {
        className = target.constructorName.type.name.name;
      }
    } else if (target is MethodInvocation) {
      if (openapiClasses.contains(target.methodName.name)) {
        className = target.methodName.name;
      }

      if (className != null) {
        String replacement = '${className}(';

        var failed = false;
        node.cascadeSections.forEach((cs) {
          if (cs is AssignmentExpression && cs.leftHandSide is PropertyAccess) {
            final lhs = cs.leftHandSide as PropertyAccess;
            replacement += lhs.propertyName.name +
                ': ' +
                cs.rightHandSide.toSource() +
                ', ';
          } else {
            failed = true;
          }
        });

        if (!failed) {
          replacement += ')';

          yieldPatch(replacement, node.offset, node.end);
        }
      }
    }
  }
}

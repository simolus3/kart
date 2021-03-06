import 'dart:io';

import 'package:kart_support/read_input.dart';
import 'package:kernel/kernel.dart';
import 'package:kernel/text/ast_to_text.dart';

/// Reads a [Component] from stdin and serializes it to text. If a parameter is
/// passed, its parsed as an int and serves as a maximum file size (in bytes).
Future<void> main(List<String> args) async {
  final bytes = await readWithArgs(args);

  final component = loadComponentFromBytes(bytes);
  component.transformChildren(_RemoveSourceTransformer());

  final buffer = StringBuffer();
  Printer(buffer).writeComponentFile(component);
  stdout.write(buffer);
}

class _RemoveSourceTransformer extends Transformer {
  @override
  TreeNode visitClass(Class node) {
    node.fileUri = null;
    return super.visitClass(node);
  }

  @override
  TreeNode visitField(Field node) {
    node.fileUri = null;
    return super.visitField(node);
  }

  @override
  TreeNode visitProcedure(Procedure node) {
    node.fileUri = null;
    return super.visitProcedure(node);
  }

  @override
  TreeNode defaultTreeNode(TreeNode node) {
    node.fileOffset = TreeNode.noOffset;
    return super.defaultTreeNode(node);
  }
}

import 'package:kart_support/read_input.dart';
import 'package:kernel/kernel.dart';

/// At the moment, the Kernel reader from this project doesn't support all
/// Kernel nodes.
///
/// This file will lower an outline Kernel file generated by Dart's frontend
/// into something that our reader can understand.
Future<void> main(List<String> args) async {
  final bytes = await readWithArgs(args);

  final component = loadComponentFromBytes(bytes);
  component.transformChildren(_RemoveBodiesTransformer());

  await writeComponentToBinary(component, 'stdout');
}

class _RemoveBodiesTransformer extends Transformer {
  @override
  TreeNode visitFunctionNode(FunctionNode node) {
    node.body = null;
    return super.visitFunctionNode(node);
  }

  @override
  TreeNode visitConstructor(Constructor node) {
    if (node.initializers.isNotEmpty) {
      node.initializers.clear();
    }
    return super.visitConstructor(node);
  }

  @override
  TreeNode defaultTreeNode(TreeNode node) {
    node.fileOffset = TreeNode.noOffset;

    if (node is Annotatable && node.annotations.isNotEmpty) {
      node.annotations.clear();
    }

    return super.defaultTreeNode(node);
  }
}
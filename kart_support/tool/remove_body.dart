import 'package:kart_support/read_input.dart';
import 'package:kernel/kernel.dart';

/// Reads a Kernel file, removes all the body from function nodes and outputs
/// that transformed Kernel file.
Future<void> main(List<String> args) async {
  final bytes = await readWithArgs(args);

  final component = loadComponentFromBytes(bytes);
  component.transformChildren(_RemoveBodiesTransformer());

  await writeComponentToBinary(component, 'stdout');
}

class _RemoveBodiesTransformer extends Transformer {
  @override
  TreeNode visitComponent(Component node) {
    node.uriToSource.clear();
    return node;
  }

  @override
  TreeNode visitClass(Class node) {
    node.fileUri = null;
    return super.visitClass(node);
  }

  @override
  TreeNode visitField(Field node) {
    node.fileUri = null;
    node.initializer = null;
    return super.visitField(node);
  }

  @override
  TreeNode visitProcedure(Procedure node) {
    node.fileUri = null;
    return super.visitProcedure(node);
  }

  @override
  TreeNode visitFunctionNode(FunctionNode node) {
    node.body = null;
    return super.visitFunctionNode(node);
  }

  @override
  TreeNode defaultTreeNode(TreeNode node) {
    node.fileOffset = TreeNode.noOffset;
    return super.defaultTreeNode(node);
  }
}

import 'dart:io';
import 'dart:typed_data';

import 'package:kernel/kernel.dart';
import 'package:kernel/text/ast_to_text.dart';

/// Reads a [Component] from stdin and serializes it to text. If a parameter is
/// passed, its parsed as an int and serves as a maximum file size (in bytes).
Future<void> main(List<String> args) async {
  final bytes = args.isEmpty
      ? await _readFromStdin()
      : await _readFromStdin(int.parse(args.single));

  final component = loadComponentFromBytes(bytes);
  component.transformChildren(_RemoveSourceTransformer());

  final buffer = StringBuffer();
  Printer(buffer).writeComponentFile(component);
  stdout.write(buffer);
}

Future<Uint8List> _readFromStdin([int maxSize]) async {
  if (maxSize == null) {
    final builder = BytesBuilder(copy: false);

    await stdin.forEach(builder.add);
    return builder.takeBytes();
  } else {
    final bytes = Uint8List(maxSize);
    var currentOffset = 0;

    await for (final chunk in stdin) {
      bytes.setAll(currentOffset, chunk);
      currentOffset += chunk.length;

      if (currentOffset >= maxSize) break;
    }

    return bytes;
  }
}

class _RemoveSourceTransformer extends Transformer {
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

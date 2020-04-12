import 'dart:io';
import 'dart:typed_data';

Future<Uint8List> readWithArgs(List<String> args) async {
  if (args.isEmpty) {
    return await readFromStdin();
  } else {
    final arg = args[0];

    try {
      return await readFromStdin(int.parse(arg));
    } on FormatException {
      return await File(arg).readAsBytes();
    }
  }
}

Future<Uint8List> readFromStdin([int maxSize]) async {
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

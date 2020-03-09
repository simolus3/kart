import 'dart:io';

import 'package:kernel/kernel.dart';

Future<void> main() async {
  final file = File('output.dill');
  final fileContent = await file.readAsBytes();

  final compiledKotlin = loadComponentFromBytes(fileContent);

  await writeComponentToBinary(compiledKotlin, 'output_linked.dill');
}

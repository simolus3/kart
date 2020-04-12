import 'dart:io';

import 'package:kart_support/dart_sdk.dart';
import 'package:kernel/kernel.dart';
import 'package:path/path.dart' as p;

Future<void> main(List<String> args) async {
  if (args.isEmpty) {
    print('Usage: link_stdlib.dart <file> [<platform>]');
    print('Supported platforms: ${DartPlatform.byName.keys.join(', ')}');
    return;
  }

  final filePath = args.first;
  var platform = DartPlatform.vm;

  if (args.length >= 2) {
    platform = DartPlatform.byName[args[1].toLowerCase()];

    if (platform == null) {
      print('Unknown platform. '
          'Supported: ${DartPlatform.byName.keys.join(', ')}');
      return;
    }
  }

  final file = File(filePath);
  final fileContent = await file.readAsBytes();

  final stdlibComponent =
      loadComponentFromBinary(locatePlatformStdlib(platform));

  final compiledKotlin = loadComponentFromBytes(fileContent, stdlibComponent);

  final output =
      p.relative('${p.basenameWithoutExtension(filePath)}_linked.dill');
  await writeComponentToBinary(compiledKotlin, output);
  print('Wrote linked Kernel file as $output');
}

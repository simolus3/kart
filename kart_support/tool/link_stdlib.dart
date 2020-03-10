import 'dart:io';

import 'package:kernel/kernel.dart';
import 'package:path/path.dart' as p;

class DartPlatform {
  final String dillName;

  const DartPlatform(this.dillName);

  static const dart2js = DartPlatform('dart2js_platform.dill');
  static const dart2jsServer = DartPlatform('dart2js_server_platform.dill');
  static const vm = DartPlatform('vm_platform_strong.dill');
  static const vmProduct = DartPlatform('vm_platform_strong_product.dill');

  static const byName = {
    'js': dart2js,
    'js-server': dart2jsServer,
    'vm': vm,
    'vm-product': vmProduct,
  };

  @override
  String toString() {
    final name = byName.keys
        .firstWhere((key) => byName[key] == this, orElse: () => 'unknown name');

    return 'DartPlatform: $name';
  }
}

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
      loadComponentFromBinary(_locatePlatformStdlib(platform));

  final compiledKotlin = loadComponentFromBytes(fileContent, stdlibComponent);

  final output =
      p.relative('${p.basenameWithoutExtension(filePath)}_linked.dill');
  await writeComponentToBinary(compiledKotlin, output);
  print('Wrote linked Kernel file as $output');
}

String _locatePlatformStdlib(DartPlatform platform) {
  final dartExec = Platform.resolvedExecutable;
  final dartSdk = p.dirname(p.dirname(dartExec));
  return p.join(dartSdk, 'lib', '_internal', platform.dillName);
}

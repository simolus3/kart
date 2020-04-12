import 'dart:io';
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

String locatePlatformStdlib(DartPlatform platform) {
  final dartExec = Platform.resolvedExecutable;
  final dartSdk = p.dirname(p.dirname(dartExec));
  return p.join(dartSdk, 'lib', '_internal', platform.dillName);
}

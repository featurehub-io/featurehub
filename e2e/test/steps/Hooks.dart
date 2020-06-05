

import 'package:app_singleapp/superuser_common.dart';
import 'package:ogurets/ogurets.dart';

class Hooks {
  final SuperuserCommon common;

  Hooks(this.common);

  @Before(tag: "superuser")
  void superuserInit() async {
    await common.initialize();
    await common.makeSuperuserCurrentUser();
  }
}

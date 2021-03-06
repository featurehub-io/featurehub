import 'package:e2e_tests/superuser_common.dart';
import 'package:ogurets/ogurets.dart';

class Hooks {
  final SuperuserCommon common;
  // final EventsCommon eventsCommon;

  // Hooks(this.common, this.eventsCommon);
  Hooks(this.common);

  @Before(tag: "superuser")
  void superuserInit() async {
    await common.initialize();
    // await common.makeSuperuserCurrentUser();
  }

  @After(tag: 'EdgeListener')
  void closeEventListener() async {
    // eventsCommon.close();
  }
}

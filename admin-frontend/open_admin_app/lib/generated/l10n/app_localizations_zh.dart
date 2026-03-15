// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Chinese (`zh`).
class AppLocalizationsZh extends AppLocalizations {
  AppLocalizationsZh([String locale = 'zh']) : super(locale);

  @override
  String get signInTitle => '登录 FeatureHub';

  @override
  String get signInWithCredentials => '或使用用户名和密码登录';

  @override
  String get emailLabel => '电子邮件地址';

  @override
  String get emailRequired => '请输入您的电子邮件';

  @override
  String get passwordLabel => '密码';

  @override
  String get passwordRequired => '请输入您的密码';

  @override
  String get incorrectCredentials => '电子邮件地址或密码不正确';

  @override
  String get signInButton => '登录';

  @override
  String get darkMode => '深色模式';

  @override
  String get lightMode => '浅色模式';

  @override
  String get signOut => '退出登录';

  @override
  String get applicationSettings => '应用程序设置';

  @override
  String get portfolioSettings => '项目组合设置';

  @override
  String get organizationSettings => '组织设置';

  @override
  String get portfolios => '项目组合';

  @override
  String get users => '用户';

  @override
  String get adminServiceAccounts => '管理员服务账号';

  @override
  String get systemConfig => '系统配置';

  @override
  String get groups => '用户组';

  @override
  String get serviceAccounts => '服务账号';

  @override
  String get environments => '环境';

  @override
  String get groupPermissions => '用户组权限';

  @override
  String get serviceAccountPermissions => '服务账号权限';

  @override
  String get integrations => '集成';

  @override
  String get applications => '应用程序';

  @override
  String get features => '功能标志';

  @override
  String get featureGroups => '功能标志组';

  @override
  String get applicationStrategies => '应用程序策略';

  @override
  String get apiKeys => 'API 密钥';

  @override
  String deleteConfirmTitle(String thing) {
    return '您确定要删除 $thing 吗？';
  }

  @override
  String get cannotBeUndone => '此操作无法撤销！';

  @override
  String get cancel => '取消';

  @override
  String get delete => '删除';

  @override
  String get reset => '重置';

  @override
  String get ok => '确定';

  @override
  String get edit => '编辑';

  @override
  String get viewDocumentation => '查看文档';

  @override
  String get createNewApplication => '创建新应用程序';

  @override
  String get applicationsDocumentation => '应用程序文档';

  @override
  String get republishPortfolioCache => '重新发布项目组合缓存';

  @override
  String get republishPortfolioCacheWarningTitle => '警告：高强度系统操作';

  @override
  String get republishPortfolioCacheWarningContent => '您确定要重新发布整个项目组合的缓存吗？';

  @override
  String get featureFlags => '功能标志';

  @override
  String get showMore => '显示更多';

  @override
  String get republishCacheForApp => '重新发布此应用程序的缓存';

  @override
  String get manageUsers => '管理用户';

  @override
  String get manageUsersDocumentation => '用户管理文档';

  @override
  String get createNewUser => '创建新用户';

  @override
  String get searchUsers => '搜索用户';

  @override
  String get columnName => '姓名';

  @override
  String get columnStatus => '状态';

  @override
  String get columnEmail => '电子邮件';

  @override
  String get columnLastSignIn => '最后登录时间 (UTC)';

  @override
  String get columnActions => '操作';

  @override
  String get notYetRegistered => '尚未注册';

  @override
  String get statusActive => '活跃';

  @override
  String get statusDeactivated => '已停用';

  @override
  String get activateUserTooltip => '激活用户';

  @override
  String activateUserTitle(String name) {
    return '激活用户「$name」';
  }

  @override
  String activateUserConfirm(String email) {
    return '您确定要激活电子邮件地址为 $email 的用户吗？';
  }

  @override
  String get activate => '激活';

  @override
  String userActivated(String name) {
    return '用户「$name」已激活！';
  }

  @override
  String userDeactivated(String name) {
    return '用户「$name」已停用！';
  }

  @override
  String get userInformation => '用户信息';

  @override
  String get registrationUrl => '注册链接';

  @override
  String get copyUrlToClipboard => '复制链接到剪贴板';

  @override
  String get registrationExpired => '注册已过期';

  @override
  String get renewRegistrationUrl => '续期注册链接并复制到剪贴板';

  @override
  String get registrationUrlRenewed => '注册链接已续期并复制到剪贴板';

  @override
  String get cantDeleteYourself => '您无法删除自己！';

  @override
  String get cantDeleteYourselfContent => '如需从组织中删除自己，请联系网站管理员。';

  @override
  String get deleteUserContent => '该用户将从所有用户组中移除，并在此组织中停用。';
}

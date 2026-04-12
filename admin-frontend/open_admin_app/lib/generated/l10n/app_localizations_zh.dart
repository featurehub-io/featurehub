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

  @override
  String get managePortfolios => '管理项目组合';

  @override
  String get managePortfoliosDocumentation => '项目组合管理文档';

  @override
  String get featureFilters => '功能过滤器';

  @override
  String get manageFeatureFilters => '管理功能过滤器';

  @override
  String get featureFiltersDocumentation => '功能过滤器文档';

  @override
  String get createNewFeatureFilter => '创建新功能过滤器';

  @override
  String get editFeatureFilter => '编辑功能过滤器';

  @override
  String get noFeatureFiltersFound => '项目组合中未找到过滤器。';

  @override
  String get filterNameLabel => '过滤器名称';

  @override
  String get filterDescriptionLabel => '过滤器描述';

  @override
  String get filterTableDescriptionLabel => '描述';

  @override
  String get filterNameRequired => '请输入过滤器名称';

  @override
  String get filterNameTooShort => '过滤器名称至少需要 4 个字符';

  @override
  String get filterDescriptionRequired => '请输入过滤器描述';

  @override
  String get filterDescriptionTooShort => '过滤器描述至少需要 4 个字符';

  @override
  String filterCreated(String name) {
    return '功能过滤器「$name」已创建！';
  }

  @override
  String filterUpdated(String name) {
    return '功能过滤器「$name」已更新！';
  }

  @override
  String filterDeleted(String name) {
    return '功能过滤器「$name」已删除！';
  }

  @override
  String appPlusFeatureCountSingular(String appName, int count) {
    return '$appName ($count feature)';
  }

  @override
  String appPlusFeatureCountPlural(String appName, int count) {
    return '$appName ($count features)';
  }

  @override
  String get filterDeleteContent => '删除此过滤器会将其从所有功能和服务账号中移除。\n\n此操作无法撤销！';

  @override
  String get appsUsingFilter => '应用程序/功能';

  @override
  String get saUsingFilter => '服务账号';

  @override
  String get selectFiltersToApply => '选择要应用的过滤器';

  @override
  String get matchingServiceAccounts => '使用过滤器的服务账号';

  @override
  String get matchingFeatures => '使用过滤器的应用程序';

  @override
  String get filterByFeatureFilters => '按功能过滤器筛选';

  @override
  String get createNewPortfolio => '创建新项目组合';

  @override
  String get searchPortfolios => '搜索项目组合';

  @override
  String get republishSystemCache => '重新发布系统缓存';

  @override
  String get republishEntireCacheWarningContent => '您确定要重新发布整个系统缓存吗？';

  @override
  String get manageGroupMembers => '管理用户组成员';

  @override
  String get userGroupsDocumentation => '用户组文档';

  @override
  String get fetchingGroups => '正在获取用户组...';

  @override
  String get createNewGroup => '创建新用户组';

  @override
  String get addMembers => '添加成员';

  @override
  String get columnMemberType => '类型（用户或管理员服务账号）';

  @override
  String get memberTypeUser => '用户';

  @override
  String get memberTypeServiceAccount => '服务账号';

  @override
  String get removeFromGroup => '从用户组中移除';

  @override
  String memberRemovedFromGroup(String name, String groupName) {
    return '「$name」已从用户组「$groupName」中移除';
  }

  @override
  String get noGroupsFound => '该项目组合中未找到用户组';

  @override
  String get portfolioGroups => '项目组合用户组';

  @override
  String get selectGroup => '选择用户组';

  @override
  String addMembersToGroupTitle(String groupName) {
    return '向用户组「$groupName」添加成员';
  }

  @override
  String get enterMembersToAdd => '输入要添加到用户组的成员...';

  @override
  String get addToGroup => '添加到用户组';

  @override
  String groupUpdated(String name) {
    return '用户组「$name」已更新！';
  }

  @override
  String get manageServiceAccounts => '管理服务账号';

  @override
  String get serviceAccountsDocumentation => '服务账号文档';

  @override
  String get createNewServiceAccount => '创建新服务账号';

  @override
  String get saHasPermissions => '该服务账号对此应用程序的一个或多个环境拥有权限';

  @override
  String get saHasNoPermissions => '该服务账号对此应用程序的任何环境均无权限';

  @override
  String get changeAccess => '修改访问权限';

  @override
  String get addAccess => '添加访问权限';

  @override
  String get saDeleteContent => '所有使用此服务账号的应用程序将无法再访问功能标志！\n\n此操作无法撤销！';

  @override
  String saDeleted(String name) {
    return '服务账号「$name」已删除！';
  }

  @override
  String saDeleteError(String name) {
    return '无法删除服务账号 $name';
  }

  @override
  String get editServiceAccount => '编辑服务账号';

  @override
  String get saNameLabel => '服务账号名称';

  @override
  String get saNameRequired => '请输入服务账号名称';

  @override
  String get saNameTooShort => '服务账号名称至少需要 4 个字符';

  @override
  String get saDescriptionLabel => '服务账号描述';

  @override
  String get saDescriptionRequired => '请输入服务账号描述';

  @override
  String get saDescriptionTooShort => '服务账号描述至少需要 4 个字符';

  @override
  String get update => '更新';

  @override
  String get create => '创建';

  @override
  String saUpdated(String name) {
    return '服务账号「$name」已更新！';
  }

  @override
  String saCreated(String name) {
    return '服务账号「$name」已创建！';
  }

  @override
  String saAlreadyExists(String name) {
    return '服务账号「$name」已存在';
  }

  @override
  String get resetClientApiKeys => '重置客户端评估 API 密钥';

  @override
  String get resetServerApiKeys => '重置服务端评估 API 密钥';

  @override
  String get featuresConsole => '功能标志控制台';

  @override
  String get featuresDocumentation => '功能标志文档';

  @override
  String get createNewFeature => '创建新功能标志';

  @override
  String get noApplicationsInPortfolio => '此项目组合中没有应用程序';

  @override
  String get noApplicationsAccessMessage =>
      '此项目组合中没有应用程序，或您没有任何应用程序的访问权限。\n请联系您的管理员。';

  @override
  String get apiKeysDocumentation => 'API 密钥文档';

  @override
  String get goToServiceAccountsSettings => '前往服务账号设置';

  @override
  String get noServiceAccountsAvailable => '暂无可用服务账号';

  @override
  String get permissions => '权限';

  @override
  String get clientServerApiKeys => '客户端和服务端 API 密钥';

  @override
  String get clientEvalApiKey => '客户端评估 API 密钥';

  @override
  String get serverEvalApiKey => '服务端评估 API 密钥';

  @override
  String get noPermissionsDefined => '未定义权限';

  @override
  String get apiKeyUnavailable => 'API 密钥不可用，因为您对此环境的当前权限级别不足';

  @override
  String get manageAdminSdkServiceAccounts => '管理管理员 SDK 服务账号';

  @override
  String get adminServiceAccountsDocumentation => '管理员服务账号文档';

  @override
  String get createAdminServiceAccount => '创建管理员服务账号';

  @override
  String get createUserInstructions => '请先提供新用户的电子邮件地址以创建用户';

  @override
  String get invalidEmailAddress => '请输入有效的电子邮件地址';

  @override
  String get addUserToGroupsHint => '将用户添加到项目组合用户组，或留空稍后再添加';

  @override
  String get userCreated => '用户已创建！';

  @override
  String get sendRegistrationUrlInstructions =>
      '您需要将此链接通过电子邮件发送给新用户，以便他们完成注册并设置密码。';

  @override
  String get userCanSignIn => '该用户现在可以登录并访问系统。';

  @override
  String get close => '关闭';

  @override
  String get createAnotherUser => '再创建一个用户';

  @override
  String userEmailAlreadyExists(String email) {
    return '电子邮件为「$email」的用户已存在';
  }

  @override
  String get appSettingsTitle => '应用程序设置';

  @override
  String get tabEnvironments => '环境';

  @override
  String get tabGroupPermissions => '用户组权限';

  @override
  String get tabServiceAccountPermissions => '服务账号权限';

  @override
  String get tabIntegrations => '集成';

  @override
  String get oauth2NotAuthorized => '您无权访问 FeatureHub';

  @override
  String get oauth2ContactAdmin => '请联系您的管理员，请他们将您的电子邮件添加到组织用户列表中';

  @override
  String get registerUrlUnexpectedError => '发生意外错误\n请联系您的 FeatureHub 管理员。';

  @override
  String get registerUrlExpiredOrInvalid =>
      '此注册链接已过期或无效。\n\n请检查链接是否正确，或联系您的 FeatureHub 管理员。';

  @override
  String get validatingInvitationUrl => '正在验证您的邀请链接';

  @override
  String get welcomeToFeatureHub => '欢迎使用 FeatureHub';

  @override
  String get registerCompleteDetails => '请完成以下信息以完成注册';

  @override
  String get nameLabel => '姓名';

  @override
  String get nameRequired => '请输入您的姓名';

  @override
  String get passwordMustBe7Chars => '密码长度至少需要 7 个字符！';

  @override
  String get passwordsDoNotMatch => '两次输入的密码不匹配';

  @override
  String get confirmPasswordLabel => '确认密码';

  @override
  String get confirmPasswordRequired => '请确认您的密码';

  @override
  String get registerButton => '注册';

  @override
  String get passwordStrengthWeak => '弱';

  @override
  String get passwordStrengthBelowAverage => '较弱';

  @override
  String get passwordStrengthGood => '良好';

  @override
  String get passwordStrengthStrong => '强';

  @override
  String get notFoundMessage => '抱歉，找不到该页面！';

  @override
  String get pageNotFoundMessage => '好像找不到相关信息可以显示！';

  @override
  String get featureGroupsDocumentation => '功能标志组文档';

  @override
  String get createFeatureGroup => '创建功能标志组';

  @override
  String get applicationStrategiesDocumentation => '应用程序策略文档';

  @override
  String get editUser => '编辑用户';

  @override
  String get resetPassword => '重置密码';

  @override
  String get editEmailAddress => '请输入电子邮件地址';

  @override
  String get editNames => '请输入姓名';

  @override
  String get removeOrAddUserToGroup => '从用户组中移除用户或添加到新用户组';

  @override
  String get saveAndClose => '保存并关闭';

  @override
  String userUpdated(String name) {
    return '用户「$name」已更新';
  }

  @override
  String get resetPasswordInstructions => '重置以下密码后，请务必将新密码通过电子邮件发送给该用户。';

  @override
  String get newPasswordLabel => '新密码';

  @override
  String get newPasswordRequired => '请输入新密码';

  @override
  String get confirmNewPasswordLabel => '确认新密码';

  @override
  String get confirmNewPasswordRequired => '请确认新密码';

  @override
  String get save => '保存';

  @override
  String get groupPrefix => '组：';

  @override
  String get applicationPrefix => '应用：';

  @override
  String get environmentPrefix => '环境：';

  @override
  String get applyAllChanges => '应用所有更改';

  @override
  String featureGroupSettingsUpdated(String name) {
    return '组「$name」的设置已更新';
  }

  @override
  String get noPermissions => '无权限';

  @override
  String get editSplitTargetingRules => '编辑分流定向规则';

  @override
  String get viewSplitTargetingRules => '查看分流定向规则';

  @override
  String get removeStrategy => '删除策略';

  @override
  String get addRolloutStrategy => '添加发布策略';

  @override
  String get featuresList => '功能标志列表';

  @override
  String get addFeature => '添加功能标志';

  @override
  String get featureValueLocked => '功能标志值已锁定。请在功能标志控制台中解锁以启用编辑';

  @override
  String get adminSaNameRequired => '请为管理员服务账号提供名称';

  @override
  String get adminSaGroupsHint => '分配到某些项目组合用户组，或留空稍后再添加';

  @override
  String adminSaCreated(String name) {
    return '管理员服务账号「$name」已创建！';
  }

  @override
  String get createAnotherServiceAccount => '再创建一个服务账号';

  @override
  String adminSaAlreadyExists(String name) {
    return '名称为「$name」的服务账号已存在';
  }

  @override
  String get editAdminSdkServiceAccount => '编辑管理员 SDK 服务账号';

  @override
  String get editName => '请输入名称';

  @override
  String get removeOrAddAdminSaToGroup => '从用户组中移除管理员服务账号或添加到新用户组';

  @override
  String adminSaUpdated(String name) {
    return '管理员服务账号「$name」已更新';
  }

  @override
  String createApplicationStrategyTitle(String name) {
    return '为「$name」创建应用程序策略';
  }

  @override
  String editApplicationStrategyTitle(String name) {
    return '编辑「$name」的应用程序策略';
  }

  @override
  String get editApplication => '编辑应用程序';

  @override
  String get appNameLabel => '应用程序名称';

  @override
  String get appNameRequired => '请输入应用程序名称';

  @override
  String get appNameTooShort => '应用程序名称至少需要 4 个字符';

  @override
  String get appDescriptionLabel => '应用程序描述';

  @override
  String get appDescriptionRequired => '请输入应用程序描述';

  @override
  String get appDescriptionTooShort => '应用程序描述至少需要 4 个字符';

  @override
  String appUpdated(String name) {
    return '应用程序「$name」已更新！';
  }

  @override
  String appCreated(String name) {
    return '应用程序「$name」已创建！';
  }

  @override
  String appAlreadyExists(String name) {
    return '应用程序「$name」已存在';
  }

  @override
  String get createNewFeatureGroup => '创建新功能标志组';

  @override
  String get editFeatureGroup => '编辑功能标志组';

  @override
  String get featureGroupNameLabel => '功能标志组名称';

  @override
  String get featureGroupNameRequired => '请输入功能标志组名称';

  @override
  String get featureGroupNameTooShort => '功能标志组名称至少需要 4 个字符';

  @override
  String get featureGroupDescriptionLabel => '功能标志组描述';

  @override
  String get featureGroupDescriptionRequired => '请输入功能标志组描述';

  @override
  String get featureGroupDescriptionTooShort => '描述至少需要 4 个字符';

  @override
  String featureGroupUpdated(String name) {
    return '功能标志组「$name」已更新！';
  }

  @override
  String featureGroupCreated(String name) {
    return '功能标志组「$name」已创建！';
  }

  @override
  String featureGroupAlreadyExists(String name) {
    return '功能标志组「$name」已存在';
  }

  @override
  String get editGroup => '编辑用户组';

  @override
  String get groupNameLabel => '用户组名称';

  @override
  String get groupNameRequired => '请输入用户组名称';

  @override
  String get groupNameTooShort => '用户组名称至少需要 4 个字符';

  @override
  String groupCreated(String name) {
    return '用户组「$name」已创建！';
  }

  @override
  String groupAlreadyExists(String name) {
    return '用户组「$name」已存在';
  }

  @override
  String get groupDeleteContent => '该用户组的所有权限将被删除 \n\n此操作无法撤销！';

  @override
  String groupDeleted(String name) {
    return '用户组「$name」已删除！';
  }

  @override
  String couldNotDeleteGroup(String name) {
    return '无法删除用户组「$name」';
  }

  @override
  String get viewFeature => '查看功能标志';

  @override
  String get editFeature => '编辑功能标志';

  @override
  String get featureNameLabel => '功能标志名称';

  @override
  String get featureNameRequired => '请输入功能标志名称';

  @override
  String get featureNameTooShort => '功能标志名称至少需要 4 个字符';

  @override
  String get featureKeyLabel => '功能标志键';

  @override
  String get featureKeyHint => '用于在代码中与 FeatureHub SDK 配合使用';

  @override
  String get featureKeyRequired => '请输入功能标志键';

  @override
  String get featureKeyNoWhitespace => '不能包含空格';

  @override
  String get featureDescriptionLabel => '描述（可选）';

  @override
  String get featureDescriptionHint => '关于功能标志的一些信息';

  @override
  String get featureLinkLabel => '参考链接（可选）';

  @override
  String get featureLinkHint => '外部追踪系统的可选链接，例如 Jira';

  @override
  String get selectFeatureType => '选择功能标志类型';

  @override
  String featureUpdated(String name) {
    return '功能标志「$name」已更新！';
  }

  @override
  String featureCreated(String name) {
    return '功能标志「$name」已创建！';
  }

  @override
  String featureKeyAlreadyExists(String key) {
    return '键为「$key」的功能标志已存在';
  }

  @override
  String get featureTypeString => '字符串';

  @override
  String get featureTypeNumber => '数字';

  @override
  String get featureTypeBoolean => '标准开关（布尔值）';

  @override
  String get featureTypeJson => '远程配置（JSON）';

  @override
  String get adminSaResetTokenWarning => '您确定要重置此服务账号的访问令牌吗？\n这将使当前令牌失效！';

  @override
  String get adminSdkTokenReset => '管理员 SDK 访问令牌已重置';

  @override
  String get adminSdkTokenResetSnackbar => '管理员 SDK 访问令牌已重置！';

  @override
  String get unableToResetToken => '无法重置访问令牌';

  @override
  String get resetClientApiKeysWarning =>
      '您确定要重置此服务账号的所有客户端评估 API 密钥吗？\n这将影响该服务账号可访问的所有环境和应用程序中的密钥！';

  @override
  String get resetServerApiKeysWarning =>
      '您确定要重置此服务账号的所有服务端评估 API 密钥吗？\n这将影响该服务账号可访问的所有环境和应用程序中的密钥！';

  @override
  String get clientApiKeysReset => '「客户端」评估 API 密钥已重置！';

  @override
  String get serverApiKeysReset => '「服务端」评估 API 密钥已重置！';

  @override
  String get unableToResetApiKey => '无法重置 API 密钥';

  @override
  String viewMetadataFor(String name) {
    return '查看「$name」的元数据';
  }

  @override
  String editMetadataFor(String name) {
    return '编辑「$name」的元数据';
  }

  @override
  String get setValue => '设置值';

  @override
  String featureMetadataUpdated(String name) {
    return '功能标志「$name」的元数据已更新！';
  }

  @override
  String get setFeatureValue => '设置功能标志值';

  @override
  String get addRolloutStrategyTargetingRules => '添加发布策略定向规则';

  @override
  String get splitStrategyName => '分流策略名称';

  @override
  String get splitStrategyNameExample => '例如：20% 灰度发布';

  @override
  String get strategyNameRequired => '请输入策略名称';

  @override
  String get percentageValue => '百分比值';

  @override
  String get percentageValueHelperText => '您可以输入最多 4 位小数的值，例如 0.0005 %';

  @override
  String get percentageValueRequired => '请输入百分比值';

  @override
  String get addPercentageRolloutRule => '添加百分比发布规则';

  @override
  String get addPercentage => '+ 百分比';

  @override
  String get percentageTotalOver100Error => '所有发布值的百分比总和不能超过 100%。请输入不同的值。';

  @override
  String get add => '添加';

  @override
  String get filterByFeatureType => '按功能标志类型筛选';

  @override
  String get searchFeatures => '搜索功能标志';

  @override
  String get createNewStrategy => '创建新策略';

  @override
  String get searchStrategy => '搜索策略';

  @override
  String get columnStrategyName => '名称';

  @override
  String get columnDateCreated => '创建时间 (UTC)';

  @override
  String get columnDateUpdated => '更新时间 (UTC)';

  @override
  String get columnCreatedBy => '创建人';

  @override
  String get columnUsedIn => '使用情况';

  @override
  String get cannotCreateStrategyNoApps => '此项目组合中没有应用程序，无法创建应用程序策略';

  @override
  String get appStrategyDeleteContent =>
      '该应用程序策略将被删除并从所有功能标志中取消分配。\n\n此操作无法撤销！';

  @override
  String appStrategyDeleted(String name) {
    return '应用程序策略「$name」已删除！';
  }

  @override
  String strategyUsage(int envCount, int featureCount) {
    return '环境：$envCount，功能标志值：$featureCount';
  }

  @override
  String get addRule => '添加规则';

  @override
  String get addCustomRule => '添加自定义规则';

  @override
  String get addCustomButton => '+ 自定义';

  @override
  String get selectCondition => '选择条件';

  @override
  String get selectValueType => '选择值类型';

  @override
  String get selectValue => '选择值';

  @override
  String get selectCountry => '选择国家/地区';

  @override
  String get selectDevice => '选择设备';

  @override
  String get selectPlatform => '选择平台';

  @override
  String get customKey => '自定义键';

  @override
  String get customKeyExample => '例如：\"warehouse-id\"';

  @override
  String get ruleNameRequired => '请输入规则名称';

  @override
  String get deleteRule => '删除规则';

  @override
  String get userKeys => '用户键';

  @override
  String get userKeyExample => '例如：bob@xyz.com';

  @override
  String get versions => '版本号';

  @override
  String get versionExample => '例如：1.3.4, 7.8.1-SNAPSHOT';

  @override
  String get customValues => '自定义值';

  @override
  String get customValuesExample => '例如：WarehouseA, WarehouseB';

  @override
  String get numbers => '数字';

  @override
  String get numberExample => '例如：6, 7.87543';

  @override
  String get dates => '日期 - YYYY-MM-DD';

  @override
  String get dateExample => '例如：2017-04-16';

  @override
  String get dateTimes => '日期/时间 - UTC/ISO8601 格式';

  @override
  String get dateTimeExample => '例如：2007-03-01T13:00:00Z';

  @override
  String get ipAddresses => 'IP 地址（支持 CIDR）';

  @override
  String get ipAddressExample => '例如：168.192.54.3 或 192.168.86.1/8';

  @override
  String get addValue => '添加值';

  @override
  String get envOrderUpdated => '环境顺序已更新！';

  @override
  String get productionEnvironment => '生产环境';

  @override
  String deleteProductionEnvWarning(String name) {
    return '环境「$name」是您的生产环境，确定要删除吗？';
  }

  @override
  String envDeleted(String name) {
    return '环境「$name」已删除！';
  }

  @override
  String envDeleteError(String name) {
    return '无法删除环境 $name';
  }

  @override
  String get createNewEnvironment => '创建新环境';

  @override
  String get editEnvironment => '编辑环境';

  @override
  String get environmentName => '环境名称';

  @override
  String get envNameRequired => '请输入环境名称';

  @override
  String get envNameTooShort => '环境名称至少需要 2 个字符';

  @override
  String get markAsProductionEnvironment => '标记为生产环境';

  @override
  String envUpdated(String name) {
    return '环境 $name 已更新！';
  }

  @override
  String envCreated(String name) {
    return '环境 $name 已创建！';
  }

  @override
  String envAlreadyExists(String name) {
    return '名为 $name 的环境已存在';
  }

  @override
  String get environmentsInfoMessage =>
      '可通过拖动下方卡片对环境排序，顺序代表从上到下的部署推广流程（直至生产）。此顺序将在「功能标志」仪表板中体现，帮助团队按正确顺序查看各环境的功能状态。';

  @override
  String get environmentsDocumentation => '环境文档';

  @override
  String get group => '用户组';

  @override
  String get goToManageGroupMembers => '前往管理组成员';

  @override
  String get groupPermissionsDocumentation => '用户组权限文档';

  @override
  String get selectGroupToEditPermissions => '请选择要编辑权限的用户组。';

  @override
  String get needToCreateEnvironmentsFirst => '请先为此应用创建「环境」。';

  @override
  String get setFeatureLevelPermissions => '设置功能级别权限';

  @override
  String get setAppStrategyPermissions => '设置应用策略权限';

  @override
  String get setFeatureValuePermissions => '设置每个环境的功能值级别权限';

  @override
  String get permRead => '读取';

  @override
  String get permLock => '锁定';

  @override
  String get permUnlock => '解锁';

  @override
  String get permChangeValue => '修改值 / 停用';

  @override
  String get permReadExtendedData => '读取扩展功能数据';

  @override
  String noServiceAccountsInPortfolio(String name) {
    return '「$name」组合中没有服务账号。';
  }

  @override
  String get goToServiceAccountSettings => '前往服务账号设置';

  @override
  String get serviceAccount => '服务账号';

  @override
  String get serviceAccountInfoMessage =>
      '我们强烈建议将生产环境的服务账号权限设置为仅「读取」。「锁定/解锁」和「修改值」权限通常用于测试目的，例如通过 SDK 在运行测试时更改功能值状态。';

  @override
  String get selectServiceAccount => '选择服务账号';

  @override
  String get setServiceAccountPermissions => '为每个环境设置服务账号的功能访问权限';

  @override
  String serviceAccountUpdated(String name) {
    return '服务账号「$name」已更新！';
  }

  @override
  String get environmentLabel => '环境';

  @override
  String get noEnvironments => '暂无环境';

  @override
  String get integrationTypeLabel => '集成类型';

  @override
  String get selectWebhookType => '选择 Webhook 类型';

  @override
  String get selectEnvironment => '选择环境';

  @override
  String get slackChannelSettings => 'Slack 频道设置（按环境）';

  @override
  String get slackIntegrationDocumentation => 'Slack 集成文档';

  @override
  String get enabled => '已启用';

  @override
  String get slackChannelId => 'Slack 频道 ID（留空则使用默认值）';

  @override
  String get slackChannelIdExample => '例如：C0150T7AF25';

  @override
  String get slackSettingsUpdated => 'Slack 设置已更新';

  @override
  String get messageDeliveryStatus => '消息投递状态';

  @override
  String get refresh => '刷新';

  @override
  String get noActivity => '暂无活动记录。';

  @override
  String unacknowledgedRequest(String time) {
    return '未确认的请求，发送时间：$time';
  }

  @override
  String deliveryStatusReceived(String status, String time) {
    return '状态：$status，接收时间：$time';
  }

  @override
  String deliveryStatusError(String status, String time) {
    return '$status，接收时间：$time';
  }

  @override
  String get responseHeaders => '响应头：';

  @override
  String get content => '内容';

  @override
  String get moreRecords => '更多记录';

  @override
  String get retry => '重试';

  @override
  String get deliveredSuccessfully => '投递成功';

  @override
  String get undeliverableInfo => '无法投递，缺少部分信息';

  @override
  String get unableToCreateData => '无法创建向远程系统发送所需的数据';

  @override
  String get systemConfigMissing => '缺少完成操作所需的系统配置';

  @override
  String get remoteSystemError => '与远程系统通信时发生错误（例如系统宕机）';

  @override
  String get unexpectedResult => '远程系统返回意外结果';

  @override
  String get networkError => '网络错误，主机未知';

  @override
  String get webhookHistory => 'Webhook 历史记录';

  @override
  String get webhookConfiguration => 'Webhook 配置';

  @override
  String get webhooksDocumentation => 'Webhooks 文档';

  @override
  String get colType => '类型';

  @override
  String get colMethod => '方法';

  @override
  String get colHttpCode => 'HTTP 状态码';

  @override
  String get colWhenSent => '发送时间';

  @override
  String get colActions => '操作';

  @override
  String get webhookWhenSent => '发送时间';

  @override
  String get webhookCloudEventType => 'Webhook 云事件类型';

  @override
  String get webhookUrl => 'URL';

  @override
  String get webhookDetailMethod => '方法';

  @override
  String get webhookHttpStatus => 'HTTP 状态';

  @override
  String get cloudEventType => '云事件类型';

  @override
  String get incomingHeaders => '传入请求头';

  @override
  String get outgoingHeaders => '传出请求头';

  @override
  String get webhookContent => 'Webhook 内容';

  @override
  String get copyContent => '复制内容';

  @override
  String get searchServiceAccounts => '搜索服务账号';

  @override
  String get colName => '名称';

  @override
  String get colGroups => '用户组';

  @override
  String get resetAdminSdkToken => '重置管理员 SDK 访问令牌';

  @override
  String get adminSADeleteContent => '该服务账号将从所有用户组中移除并从组织中删除。\n\n此操作无法撤销！';

  @override
  String adminSADeleted(String name) {
    return '服务账号「$name」已删除！';
  }

  @override
  String get adminSADetailsTitle => '管理员服务账号详情';

  @override
  String get accessToken => '访问令牌';

  @override
  String get copyAccessToken => '复制访问令牌到剪贴板';

  @override
  String get accessTokenSecurityNote => '出于安全考虑，关闭此窗口后将无法再查看访问令牌。';

  @override
  String get systemConfigurationsTitle => '系统配置';

  @override
  String get siteConfigurationTitle => '站点配置';

  @override
  String get siteConfigurationSubtitle => '配置您的 FeatureHub 系统';

  @override
  String get slackConfigurationTitle => 'Slack 配置';

  @override
  String get slackConfigurationSubtitle => '允许 FeatureHub 发送 Slack 消息';

  @override
  String get encryptionRequiredForSlack =>
      '您需要在 FeatureHub 系统属性文件中配置加密密钥/密码才能启用 Slack 集成';

  @override
  String get encryptionDocumentation => '加密文档';

  @override
  String get siteUrlLabel => '您组织的 FeatureHub 应用地址';

  @override
  String get siteUrlEmptyError => '不能指定空的 URL';

  @override
  String get siteUrlInvalidError => '必须为您的站点指定有效的 URL';

  @override
  String get allowSearchRobots => '允许搜索引擎爬取';

  @override
  String get redirectBadHostsHeader => '重定向无效 Host 头的流量';

  @override
  String get enableSlack => '启用 Slack';

  @override
  String get connectFeatureHubToSlack => '将 FeatureHub 连接到 Slack';

  @override
  String get installFeatureHubBot => '将 FeatureHub Bot 应用安装到您的 Slack 工作区';

  @override
  String get connectToSlack => '连接到 Slack';

  @override
  String get slackBotTokenLabel => 'Slack Bot 用户 OAuth 令牌';

  @override
  String get slackBotTokenRequired => '请输入 Slack Bot 用户 OAuth 令牌';

  @override
  String get defaultSlackChannelIdLabel => '默认 Slack 频道 ID';

  @override
  String get slackChannelIdRequired => '请输入 Slack 频道 ID';

  @override
  String get externalSlackDeliveryMessage =>
      '如果您的 Slack 消息由外部应用程序负责投递，请在此处指定相关信息。';

  @override
  String externalSlackDeliveryUrlLabel(String prefixes) {
    return '外部 Slack 消息投递服务（可选，有效前缀：$prefixes）';
  }

  @override
  String get invalidUrlPrefix => '必须选择有效的 URL 前缀';

  @override
  String get clickToEdit => '点击编辑';

  @override
  String get showAction => '显示';

  @override
  String get clearAction => '清除';

  @override
  String get decryptAction => '解密';

  @override
  String get encryptAction => '加密';

  @override
  String addRowButton(String name) {
    return '添加 $name';
  }

  @override
  String get headerColumnLabel => '标头';

  @override
  String get valueColumnLabel => '值';

  @override
  String get selectEnvironmentsToDisplay => '选择要显示的环境';

  @override
  String appThingLabel(String name) {
    return '应用程序「$name」';
  }

  @override
  String appDeleted(String name) {
    return '应用程序「$name」已删除！';
  }

  @override
  String appDeleteError(String name) {
    return '无法删除应用程序 $name';
  }

  @override
  String get undelivered => '未投递';

  @override
  String get webhookDetailsTitle => 'Webhook 详情';

  @override
  String get viewWebhookDetails => '查看 Webhook 详情';

  @override
  String get enableJsonValidation => '启用 JSON 验证';

  @override
  String get formatJson => '格式化 JSON';

  @override
  String get jsonValue => 'JSON 值';

  @override
  String get errorNotFound => '找不到请求的资源';

  @override
  String get errorForbidden => '您没有权限访问此资源';

  @override
  String get errorInternalServer => '发生内部服务器错误';

  @override
  String get errorLoadingData => '加载数据时发生错误';

  @override
  String get yourCurrentPortfolio => '您当前的项目组合';

  @override
  String get selectPortfolio => '选择项目组合';

  @override
  String get selectApplication => '选择应用程序';

  @override
  String get featureOn => '开';

  @override
  String get featureOff => '关';

  @override
  String get notSet => '未设置';

  @override
  String get retired => '已停用';

  @override
  String featureThingLabel(String name) {
    return '功能标志「$name」';
  }

  @override
  String get featureDeleteContent =>
      '您需要确保所有相关代码已清理，且能够在没有此功能标志的情况下正常运行！\n\n此操作无法撤销！';

  @override
  String featureDeleted(String name) {
    return '功能标志「$name」已删除！';
  }

  @override
  String get noPermissionsForOperation => '您没有权限执行此操作';

  @override
  String featureDeleteError(String name) {
    return '无法删除功能标志 $name';
  }

  @override
  String get featureGroupDeleteContent =>
      '此操作将删除该功能标志组及其关联的策略。\n\n各功能标志不会被删除，仍将保留在系统中。\n\n此操作无法撤销！';

  @override
  String featureGroupDeleted(String name) {
    return '功能标志组「$name」已删除！';
  }

  @override
  String featureGroupDeleteError(String name) {
    return '无法删除功能标志组 $name';
  }

  @override
  String get noEnvironmentsAvailable => '暂无可用环境';

  @override
  String get selectFeatureToAdd => '选择要添加的功能标志';

  @override
  String get setupWelcomeTitle => '让我们开始吧！';

  @override
  String get setupWelcomeMessage =>
      '做得好，FeatureHub 已成功运行。您将成为 FeatureHub 账户的第一位「组织超级管理员」。';

  @override
  String get setupOrRegisterBelow => '或通过填写以下信息进行注册';

  @override
  String get next => '下一步';

  @override
  String get setupAllSet => '一切就绪！';

  @override
  String get setupNextStepsMessage =>
      '下一步是创建您的第一个应用程序并添加一些功能标志。默认情况下，系统会创建第一个名为「Production」的环境。您可以点击应用栏右侧的「火箭」图标，通过「快速设置」助手查看您的进度。';

  @override
  String get stepperTitle => '应用程序设置进度';

  @override
  String get stepCreateApplication => '创建应用程序';

  @override
  String get stepSelectApplicationHint => '选择一个应用程序，或点击下方链接创建新应用程序';

  @override
  String get goToApplications => '前往应用程序';

  @override
  String get stepCreateTeamGroup => '创建团队用户组';

  @override
  String get stepCreateTeamGroupHint =>
      '用户组适用于整个项目组合。建议为每个应用程序创建专属用户组，例如「MyApp 开发者」';

  @override
  String get goToGroups => '前往用户组';

  @override
  String get stepCreateServiceAccount => '创建服务账号';

  @override
  String get stepCreateServiceAccountHint =>
      '服务账号适用于整个项目组合。建议为每个应用程序至少创建两个专属服务账号，例如「SA-MyApp-Prod」和「SA-MyApp-Non-Prod」';

  @override
  String get goToServiceAccounts => '前往服务账号';

  @override
  String get stepCreateEnvironment => '创建环境';

  @override
  String get stepCreateEnvironmentHint => '为所选应用程序创建一个环境，例如「test」、「dev」、「prod」';

  @override
  String get goToEnvironments => '前往环境';

  @override
  String get stepGiveAccessToGroups => '授予用户组访问权限';

  @override
  String get stepGiveAccessToGroupsHint => '为用户组分配应用程序环境级别的权限';

  @override
  String get goToGroupPermissions => '前往用户组权限';

  @override
  String get stepGiveAccessToServiceAccount => '授予服务账号访问权限';

  @override
  String get stepGiveAccessToServiceAccountHint => '为服务账号分配应用程序环境级别的权限';

  @override
  String get goToSAPermissions => '前往服务账号权限';

  @override
  String get stepCreateFeature => '创建功能标志';

  @override
  String get stepCreateFeatureHint => '为应用程序创建一个功能标志';

  @override
  String get goToFeatures => '前往功能标志';

  @override
  String get featureHubAdministrators => 'FeatureHub 管理员';

  @override
  String get portfolioLabel => '项目组合';

  @override
  String get andOperator => '且';

  @override
  String systemConfigUpdated(String section) {
    return '$section 已成功更新';
  }

  @override
  String systemConfigNoUpdates(String section) {
    return '未发现 $section 的更新';
  }

  @override
  String get unableToSaveUpdates => '无法保存更新';

  @override
  String get showValue => '显示值';

  @override
  String get strategyNameTooLong => '策略名称过长';

  @override
  String get strategyEmptyMatchCriteria => '您尚未提供任何匹配规则，请添加规则';

  @override
  String get strategyNegativePercentage => '百分比不能为负数';

  @override
  String get strategyPercentageOver100 => '所有策略的百分比总和超过 100%，请降低百分比规则';

  @override
  String get strategyArrayAttributeNoValues => '请为此规则提供至少一个值';

  @override
  String get strategyAttrInvalidWellKnownEnum => '请为此规则选择一个值';

  @override
  String get strategyAttrMissingValue => '请为此规则提供一个值';

  @override
  String get strategyAttrMissingConditional => '请为此规则选择匹配条件';

  @override
  String get strategyAttrMissingFieldName => '请输入规则名称';

  @override
  String get strategyAttrMissingFieldType => '请为此规则选择值类型';

  @override
  String get strategyAttrValNotSemanticVersion => '请提供有效的语义化版本号';

  @override
  String get strategyAttrValNotNumber => '请提供有效的数字';

  @override
  String get strategyAttrValNotDate => '请提供 YYYY-MM-DD 格式的有效日期';

  @override
  String get strategyAttrValNotDateTime => '请提供 YYYY-MM-DDTHH:MM:SS 格式的有效日期时间';

  @override
  String get strategyAttrValNotCidr => '请提供有效的 IP 或 CIDR 地址';

  @override
  String get strategyAttrUnknownFailure => '出现未知策略验证错误';

  @override
  String get strategyDefault => '默认';

  @override
  String get strategyServe => '提供';

  @override
  String get editStrategySettings => '编辑策略设置';

  @override
  String get selectStrategyToAdd => '选择要添加的策略';

  @override
  String get wellKnownCountry => '国家/地区';

  @override
  String get wellKnownDevice => '设备';

  @override
  String get wellKnownPlatform => '平台';

  @override
  String get wellKnownVersion => '版本';

  @override
  String get wellKnownUserKey => '用户键';

  @override
  String get tooltipAppliedRules => '已应用规则';

  @override
  String tooltipPercentage(String value) {
    return '百分比：$value%';
  }

  @override
  String get tooltipUserKey => '用户键';

  @override
  String get tooltipCountry => '国家/地区';

  @override
  String get tooltipPlatform => '平台';

  @override
  String get tooltipDevice => '设备';

  @override
  String get tooltipVersion => '版本';

  @override
  String get tooltipCustom => '自定义';

  @override
  String get unsavedChanges => '您有未保存的更改，是否保存？';

  @override
  String featureValueUpdated(String feature, String environment) {
    return '功能标志 $feature 在环境 $environment 中已更新！';
  }

  @override
  String get defaultValue => '默认值';

  @override
  String get strategyVariations => '策略变体';

  @override
  String get strategyVariationsInfo =>
      '添加策略变体以提供非默认值。您可以通过拖放下方的卡片更改策略评估顺序。策略从上到下依次评估，命中匹配策略后停止。「用户组策略」最后评估。如果没有策略匹配，则提供「默认」功能值。';

  @override
  String get noStrategiesSet => '未设置策略';

  @override
  String get groupStrategyVariations => '用户组策略变体';

  @override
  String get groupStrategyVariationsInfo =>
      '当您希望在同一环境中为多个功能设置相同策略时，建议使用功能分组。功能组策略可在「功能分组」页面中创建和编辑。';

  @override
  String get noGroupStrategiesSet => '未设置用户组策略';

  @override
  String get applicationStrategyVariations => '应用程序策略变体';

  @override
  String get applicationStrategyVariationsInfo =>
      '应用程序策略在应用程序层面创建，可分配给任意环境中的多个功能。应用程序策略可在「应用程序策略」页面中创建和编辑。';

  @override
  String get noApplicationStrategiesSet => '未设置应用程序策略';

  @override
  String get showAvailableAppStrategies => '显示可用的应用程序策略';

  @override
  String get addStrategy => '添加策略';

  @override
  String get retiredStatus => '退役状态';

  @override
  String get retiredStatusInfo =>
      '当功能标志在您的应用程序中不再需要并准备删除时，您可以先在指定环境中将此功能「退役」，以测试应用程序的行为。这意味着 SDK 将看不到该功能，模拟「已删除」状态。如果改变主意，您可以取消勾选来「取消退役」，此操作是可逆的。在所有环境中退役功能值并确认应用程序行为符合预期后，即可删除整个功能。';

  @override
  String get hideHistory => '隐藏历史记录';

  @override
  String get showHistory => '显示历史记录';

  @override
  String get showingLast20 => '显示最近 20 条';

  @override
  String get historyColumnTimestamp => '时间戳（UTC）';

  @override
  String get historyColumnName => '姓名';

  @override
  String get historyColumnEmail => '邮箱';

  @override
  String get historyColumnType => '类型';

  @override
  String get historyColumnDefaultValue => '默认值';

  @override
  String get historyColumnLocked => '已锁定';

  @override
  String get historyColumnRetired => '已退役';

  @override
  String get historyColumnRolloutStrategies => '发布策略';

  @override
  String get historyTypeUser => '用户';

  @override
  String get historyTypeServiceAccount => '服务账号';

  @override
  String get strategyRules => '策略规则';

  @override
  String get percentageRollout => '百分比发布';

  @override
  String get moreDetails => '更多';

  @override
  String get groupStrategyTooltip => '用户组策略';

  @override
  String get applicationStrategyTooltip => '应用程序策略';

  @override
  String get lockedStatus => '锁定状态';

  @override
  String get lockedStatusInfo =>
      '锁定机制在将未完成代码部署到生产环境时，为功能更改提供了额外的保障。锁定状态会阻止对默认值、策略、策略值和「退役」状态的任何更改。通常，开发人员通过锁定功能来表示该功能尚未准备好向测试人员、产品负责人、客户及其他利益相关者开放。';

  @override
  String get clickToUnlock => '点击解锁';

  @override
  String get clickToLock => '点击锁定';

  @override
  String get featureIsLocked => '功能已锁定，无法更改';

  @override
  String get featureIsUnlocked => '功能已解锁，可以更改';

  @override
  String get featuresColumnHeader => '功能标志';

  @override
  String get noFeaturesToDisplay => '没有可显示的功能标志';

  @override
  String get noEnvironmentsForApp => '此应用程序尚未定义任何环境，或您没有访问任何环境的权限';

  @override
  String get goToEnvironmentsSettings => '前往环境设置';

  @override
  String get noFeaturesForApp => '此应用程序尚未定义任何功能标志';

  @override
  String get setAsOrgSuperAdmin => '将此用户设为组织超级管理员';

  @override
  String get copyFeatureKeyToClipboard => '复制功能标志键到剪贴板';

  @override
  String get editDetails => '编辑详情';

  @override
  String get viewDetails => '查看详情';

  @override
  String get editMetadata => '编辑元数据';

  @override
  String get viewMetadata => '查看元数据';

  @override
  String get manageGroup => '管理分组';

  @override
  String get strategy => '策略';
}

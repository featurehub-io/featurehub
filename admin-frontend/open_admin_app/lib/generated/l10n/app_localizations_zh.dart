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
}

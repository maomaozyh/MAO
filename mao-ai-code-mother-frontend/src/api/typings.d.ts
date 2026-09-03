declare namespace API {
  type AppAddRequest = {
    initPrompt?: string
    appName?: string
    codeGenType?: string
    category?: string
    isPublic?: number
  }

  type AppAdminUpdateRequest = {
    id?: number
    appName?: string
    cover?: string
    priority?: number
    isPublic?: number
  }

  type AppDeployRequest = {
    appId?: number
  }

  type AppQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    priority?: number
    userId?: number
    category?: string
    skillId?: number
    status?: number
    isPublic?: number
  }

  type AppUpdateRequest = {
    id?: number
    appName?: string
    isPublic?: number
  }

  type AppVO = {
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    lastOpenTime?: string
    priority?: number
    userId?: number
    createTime?: string
    updateTime?: string
    isPublic?: number
    user?: UserVO
  }

  type BaseResponseAppVO = {
    code?: number
    data?: AppVO
    message?: string
  }

  type BaseResponseListAppVO = {
    code?: number
    data?: AppVO[]
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageAppVO = {
    code?: number
    data?: PageAppVO
    message?: string
  }

  type BaseResponsePageChatHistory = {
    code?: number
    data?: PageChatHistory
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type ChatHistory = {
    id?: number
    message?: string
    messageType?: string
    appId?: number
    userId?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type ChatHistoryQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    message?: string
    messageType?: string
    appId?: number
    userId?: number
    lastCreateTime?: string
  }

  type chatToGenCodeParams = {
    appId: number
    message: string
  }

  type DeleteRequest = {
    id?: number
  }

  type downloadAppCodeParams = {
    appId: number
  }

  type getAppVOByIdByAdminParams = {
    id: number
  }

  type getAppVOByIdParams = {
    id: number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type listAppChatHistoryParams = {
    appId: number
    pageSize?: number
    lastCreateTime?: string
  }

  type LoginUserVO = {
    id?: number
    userAccount?: string
    userPhone?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    membershipTier?: string
    secondsBalance?: number
    giftSecondsBalance?: number
    membershipExpireTime?: string
    hasPaidMembership?: boolean
    hasPaidPoints?: boolean
    createTime?: string
    updateTime?: string
  }

  type SecondsBalanceVO = {
    secondsBalance?: number
    giftSecondsBalance?: number
    totalSeconds?: number
  }

  type SecondsCheckinVO = {
    reward?: number
    checkedToday?: boolean
    secondsBalance?: number
    giftSecondsBalance?: number
    totalSeconds?: number
  }

  type SecondsRecordVO = {
    id?: string
    userId?: number
    amount?: number
    balanceAfter?: number
    giftAfter?: number
    bizType?: string
    bizTypeText?: string
    bizDesc?: string
    appId?: string
    status?: number
    createTime?: string
  }

  type PageSecondsRecordVO = {
    records?: SecondsRecordVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type PageAppVO = {
    records?: AppVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageChatHistory = {
    records?: ChatHistory[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type ServerSentEventString = true

  type serveStaticResourceParams = {
    deployKey: string
  }

  type User = {
    id?: number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
    /** 手机号（登录二次验证用） */
    phone?: string
    /** 短信验证码 */
    code?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
    phone?: string
    code?: string
  }

  type UserEmailCodeRequest = {
    email?: string
    /** 图形验证码标识 */
    captchaKey?: string
    /** 图形验证码（用户输入） */
    captcha?: string
  }

  type CaptchaVO = {
    /** 验证码标识 */
    captchaKey?: string
    /** 验证码图片（base64 PNG data URL） */
    captchaImg?: string
  }

  type UserResetPasswordByPhoneRequest = {
    phone?: string
    code?: string
    newPassword?: string
    checkPassword?: string
  }

  type UserResetPasswordByEmailRequest = {
    email?: string
    code?: string
    newPassword?: string
    checkPassword?: string
  }

  type UserUpdatePasswordRequest = {
    oldPassword?: string
    newPassword?: string
    checkPassword?: string
  }

  type UserBindEmailRequest = {
    email?: string
    code?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }

  type SkillAddRequest = {
    skillName?: string
    skillDesc?: string
    featureDesc?: string
    usageDesc?: string
    icon?: string
    category?: string
    price?: string
    originalPrice?: string
    priceUnit?: string
    tags?: string
  }

  type SkillUpdateRequest = {
    id?: number
    skillName?: string
    skillDesc?: string
    featureDesc?: string
    usageDesc?: string
    icon?: string
    category?: string
    price?: string
    originalPrice?: string
    priceUnit?: string
    tags?: string
    status?: number
  }

  type SkillQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    skillName?: string
    category?: string
    tags?: string
    status?: number
    userId?: number
  }

  type SkillVO = {
    id?: number
    skillName?: string
    skillDesc?: string
    featureDesc?: string
    usageDesc?: string
    icon?: string
    category?: string
    price?: string
    originalPrice?: string
    priceUnit?: string
    tags?: string
    usageCount?: number
    status?: number
    userId?: number
    createTime?: string
    updateTime?: string
    user?: UserVO
  }

  type BaseResponseSkillVO = {
    code?: number
    data?: SkillVO
    message?: string
  }

  type PageSkillVO = {
    records?: SkillVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type BaseResponsePageSkillVO = {
    code?: number
    data?: PageSkillVO
    message?: string
  }

  type SkillCenterConfig = {
    banners?: { title?: string; desc?: string; emoji?: string }[]
    categories?: { key?: string; label?: string }[]
    quota?: { label?: string; used?: number; total?: number }[]
  }

  type BaseResponseSkillCenterConfig = {
    code?: number
    data?: SkillCenterConfig
    message?: string
  }

  type MaterialQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    name?: string
    type?: string
    userId?: number
    folderId?: number
  }

  type MaterialVO = {
    id?: number
    name?: string
    type?: string
    url?: string
    size?: number
    userId?: number
    folderId?: number
    createTime?: string
    updateTime?: string
    user?: UserVO
  }

  type MaterialFolderVO = {
    id?: number
    name?: string
    userId?: number
    createTime?: string
    updateTime?: string
  }

  type MaterialFolderAddRequest = {
    name?: string
  }

  type BaseResponseListMaterialFolderVO = {
    code?: number
    data?: MaterialFolderVO[]
    message?: string
  }

  type PageMaterialVO = {
    records?: MaterialVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type BaseResponseMaterialVO = {
    code?: number
    data?: MaterialVO
    message?: string
  }

  type BaseResponsePageMaterialVO = {
    code?: number
    data?: PageMaterialVO
    message?: string
  }

  // ============ 社区帖子相关 ============
  type CommunityPostAddRequest = {
    title?: string
    content?: string
    category?: string
    tags?: string[]
    coverImage?: string
  }

  type CommunityPostQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    title?: string
    content?: string
    category?: string
    tags?: string
    status?: number
    userId?: number
    allStatus?: boolean
  }

  type CommunityPostUpdateRequest = {
    id?: number
    title?: string
    content?: string
    category?: string
    tags?: string[]
    coverImage?: string
    status?: number
  }

  type CommunityPostVO = {
    id?: number
    title?: string
    content?: string
    category?: string
    tags?: string[]
    coverImage?: string
    viewCount?: number
    likeCount?: number
    commentCount?: number
    status?: number
    userId?: number
    createTime?: string
    updateTime?: string
    user?: UserVO
    isLiked?: boolean
  }

  type BaseResponseCommunityPostVO = {
    code?: number
    data?: CommunityPostVO
    message?: string
  }

  type PageCommunityPostVO = {
    records?: CommunityPostVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type BaseResponsePageCommunityPostVO = {
    code?: number
    data?: PageCommunityPostVO
    message?: string
  }

  type getCommunityPostVOByIdParams = {
    id: number
  }

  type toggleCommunityPostLikeParams = {
    postId: number
  }

  // ============ 社区评论相关 ============
  type CommunityCommentAddRequest = {
    postId?: number
    content?: string
    parentId?: number
  }

  type CommunityCommentVO = {
    id?: number
    content?: string
    postId?: number
    parentId?: number
    userId?: number
    createTime?: string
    user?: UserVO
  }

  type PageCommunityCommentVO = {
    records?: CommunityCommentVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type BaseResponsePageCommunityCommentVO = {
    code?: number
    data?: PageCommunityCommentVO
    message?: string
  }

  type listCommunityCommentByPageParams = {
    postId: number
    pageNum?: number
    pageSize?: number
  }

  // ============ 管理员仪表盘相关 ============
  type DashboardStatsVO = {
    totalUsers?: number
    totalApps?: number
    totalPosts?: number
    totalChats?: number
    todayNewUsers?: number
    todayNewPosts?: number
    weekNewUsers?: number
    weekNewPosts?: number
    userGrowthTrend?: number[]
    postGrowthTrend?: number[]
    dateLabels?: string[]
  }

  type BaseResponseDashboardStatsVO = {
    code?: number
    data?: DashboardStatsVO
    message?: string
  }

  type SelfCheckResultVO = {
    hasIssue?: boolean
    issues?: string[]
    fixedCode?: string
  }

  type BaseResponseSelfCheckResultVO = {
    code?: number
    data?: SelfCheckResultVO
    message?: string
  }

  type OperationLog = {
    id?: number
    userId?: number
    userName?: string
    module?: string
    operation?: string
    targetId?: string
    detail?: string
    ip?: string
    status?: number
    errorMsg?: string
    createTime?: string
  }

  type OperationLogQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    userId?: number
    module?: string
    operation?: string
    keyword?: string
    startTime?: string
    endTime?: string
  }

  type PageOperationLog = {
    records?: OperationLog[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type BaseResponsePageOperationLog = {
    code?: number
    data?: PageOperationLog
    message?: string
  }

  type SysConfig = {
    id?: number
    configKey?: string
    configValue?: string
    configName?: string
    configType?: string
    description?: string
    createTime?: string
    updateTime?: string
  }

  type SysConfigUpdateRequest = {
    configKey?: string
    configValue?: string
  }

  type BaseResponseListSysConfig = {
    code?: number
    data?: SysConfig[]
    message?: string
  }
}

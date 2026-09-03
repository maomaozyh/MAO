-- =====================================================
-- RBAC 权限管理系统表结构
-- =====================================================

-- 1. 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id           BIGINT        NOT NULL COMMENT '角色ID',
    roleCode     VARCHAR(64)   NOT NULL COMMENT '角色编码（唯一）',
    roleName     VARCHAR(64)   NOT NULL COMMENT '角色名称',
    description  VARCHAR(256)  DEFAULT NULL COMMENT '角色描述',
    status       TINYINT       DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    sortOrder    INT           DEFAULT 0 COMMENT '排序',
    createTime   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT       DEFAULT 0 COMMENT '是否删除：0-否 1-是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (roleCode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 2. 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id              BIGINT        NOT NULL COMMENT '权限ID',
    permissionCode  VARCHAR(128)  NOT NULL COMMENT '权限编码（唯一，如 user:add）',
    permissionName  VARCHAR(128)  NOT NULL COMMENT '权限名称',
    description     VARCHAR(256)  DEFAULT NULL COMMENT '权限描述',
    type            VARCHAR(16)   DEFAULT 'button' COMMENT '类型：menu-菜单 button-按钮',
    parentId        BIGINT        DEFAULT 0 COMMENT '父级ID',
    sortOrder       INT           DEFAULT 0 COMMENT '排序',
    status          TINYINT       DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    createTime      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete        TINYINT       DEFAULT 0 COMMENT '是否删除：0-否 1-是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permissionCode),
    KEY idx_parent_id (parentId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 3. 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id           BIGINT        NOT NULL COMMENT '菜单ID',
    menuName     VARCHAR(64)   NOT NULL COMMENT '菜单名称',
    menuPath     VARCHAR(256)  DEFAULT NULL COMMENT '路由路径',
    menuComponent VARCHAR(256) DEFAULT NULL COMMENT '组件路径',
    parentId     BIGINT        DEFAULT 0 COMMENT '父级ID',
    icon         VARCHAR(64)   DEFAULT NULL COMMENT '菜单图标',
    sortOrder    INT           DEFAULT 0 COMMENT '排序',
    status       TINYINT       DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    visible      TINYINT       DEFAULT 1 COMMENT '是否显示：0-隐藏 1-显示',
    redirect     VARCHAR(256)  DEFAULT NULL COMMENT '重定向地址',
    permissionCode VARCHAR(128) DEFAULT NULL COMMENT '关联权限编码',
    createTime   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT       DEFAULT 0 COMMENT '是否删除：0-否 1-是',
    PRIMARY KEY (id),
    KEY idx_parent_id (parentId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

-- 4. 角色-权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            BIGINT       NOT NULL COMMENT '主键ID',
    roleId        BIGINT       NOT NULL COMMENT '角色ID',
    permissionId  BIGINT       NOT NULL COMMENT '权限ID',
    createTime    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (roleId, permissionId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 5. 角色-菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id          BIGINT       NOT NULL COMMENT '主键ID',
    roleId      BIGINT       NOT NULL COMMENT '角色ID',
    menuId      BIGINT       NOT NULL COMMENT '菜单ID',
    createTime  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (roleId, menuId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 6. 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT       NOT NULL COMMENT '主键ID',
    userId      BIGINT       NOT NULL COMMENT '用户ID',
    roleId      BIGINT       NOT NULL COMMENT '角色ID',
    createTime  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (userId, roleId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 初始化角色
INSERT INTO sys_role (id, roleCode, roleName, description, sortOrder) VALUES
(1, 'admin', '超级管理员', '拥有系统所有权限', 1),
(2, 'user', '普通用户', '普通注册用户，拥有基础功能权限', 2),
(3, 'operator', '运营人员', '负责内容运营和用户管理', 3);

-- 初始化权限
INSERT INTO sys_permission (id, permissionCode, permissionName, type, parentId, sortOrder) VALUES
-- 用户管理权限
(100, 'system:user', '用户管理', 'menu', 0, 1),
(101, 'system:user:list', '用户列表', 'button', 100, 1),
(102, 'system:user:add', '新增用户', 'button', 100, 2),
(103, 'system:user:edit', '编辑用户', 'button', 100, 3),
(104, 'system:user:delete', '删除用户', 'button', 100, 4),
(105, 'system:user:role', '分配角色', 'button', 100, 5),
-- 角色管理权限
(200, 'system:role', '角色管理', 'menu', 0, 2),
(201, 'system:role:list', '角色列表', 'button', 200, 1),
(202, 'system:role:add', '新增角色', 'button', 200, 2),
(203, 'system:role:edit', '编辑角色', 'button', 200, 3),
(204, 'system:role:delete', '删除角色', 'button', 200, 4),
(205, 'system:role:permission', '分配权限', 'button', 200, 5),
-- 菜单管理权限
(300, 'system:menu', '菜单管理', 'menu', 0, 3),
(301, 'system:menu:list', '菜单列表', 'button', 300, 1),
(302, 'system:menu:add', '新增菜单', 'button', 300, 2),
(303, 'system:menu:edit', '编辑菜单', 'button', 300, 3),
(304, 'system:menu:delete', '删除菜单', 'button', 300, 4),
-- 权限管理权限
(400, 'system:permission', '权限管理', 'menu', 0, 4),
(401, 'system:permission:list', '权限列表', 'button', 400, 1),
(402, 'system:permission:add', '新增权限', 'button', 400, 2),
(403, 'system:permission:edit', '编辑权限', 'button', 400, 3),
(404, 'system:permission:delete', '删除权限', 'button', 400, 4),
-- 应用管理权限
(500, 'app:manage', '应用管理', 'menu', 0, 5),
(501, 'app:manage:list', '应用列表', 'button', 500, 1),
(502, 'app:manage:edit', '编辑应用', 'button', 500, 2),
(503, 'app:manage:delete', '删除应用', 'button', 500, 3),
-- 内容管理权限
(600, 'content:manage', '内容管理', 'menu', 0, 6),
(601, 'content:post:list', '帖子列表', 'button', 600, 1),
(602, 'content:post:delete', '删除帖子', 'button', 600, 2);

-- 初始化菜单（后台管理菜单）
INSERT INTO sys_menu (id, menuName, menuPath, menuComponent, parentId, icon, sortOrder, redirect, permissionCode) VALUES
(1, '系统概览', '/admin/dashboard', '/admin/DashboardPage', 0, 'chart', 1, NULL, NULL),
(2, '系统管理', '/admin/system', NULL, 0, 'setting', 10, '/admin/system/user', NULL),
(3, '用户管理', '/admin/system/user', '/admin/UserManagePage', 2, 'user', 1, NULL, 'system:user:list'),
(4, '角色管理', '/admin/system/role', '/admin/RoleManagePage', 2, 'team', 2, NULL, 'system:role:list'),
(5, '菜单管理', '/admin/system/menu', '/admin/MenuManagePage', 2, 'menu', 3, NULL, 'system:menu:list'),
(6, '权限管理', '/admin/system/permission', '/admin/PermissionManagePage', 2, 'safety', 4, NULL, 'system:permission:list'),
(7, '应用管理', '/admin/apps', '/admin/AppManagePage', 0, 'appstore', 20, NULL, 'app:manage:list'),
(8, '内容管理', '/admin/posts', '/admin/PostManagePage', 0, 'file-text', 30, NULL, 'content:post:list'),
(9, '对话管理', '/admin/chats', '/admin/ChatManagePage', 0, 'message', 40, NULL, NULL),
(10, '技能管理', '/admin/skills', '/admin/SkillManagePage', 0, 'bulb', 50, NULL, NULL),
(11, '订单管理', '/admin/orders', '/admin/OrderManagePage', 0, 'shopping', 60, NULL, NULL),
(12, '积分管理', '/admin/points', '/admin/PointsManagePage', 0, 'star', 70, NULL, NULL),
(13, '操作日志', '/admin/logs', '/admin/LogsPage', 0, 'history', 80, NULL, NULL),
(14, '系统设置', '/admin/settings', '/admin/SettingsPage', 0, 'control', 90, NULL, NULL);

-- 超级管理员拥有所有权限（通过角色-权限关联）
-- 先给 admin 角色分配所有权限
INSERT INTO sys_role_permission (id, roleId, permissionId)
SELECT (10000 + ROW_NUMBER() OVER (ORDER BY id)), 1, id FROM sys_permission;

-- 超级管理员拥有所有菜单
INSERT INTO sys_role_menu (id, roleId, menuId)
SELECT (10000 + ROW_NUMBER() OVER (ORDER BY id)), 1, id FROM sys_menu;

-- 给 admin 用户分配超级管理员角色
INSERT INTO sys_user_role (id, userId, roleId) VALUES
(1, 1, 1);

-- 给普通用户角色分配基础权限
INSERT INTO sys_role_permission (id, roleId, permissionId) VALUES
(99999, 2, 101);

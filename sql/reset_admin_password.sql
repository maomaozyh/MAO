-- ============================================================
-- 重置默认管理员 admin 的弱口令(12345678) 为 BCrypt 强哈希
-- ============================================================
-- 新管理员登录口令： y@7X3jInGdPKgmA&
-- ⚠️ 此文件含明文口令，仅限本地执行，请勿提交到公开仓库；
--    执行后请尽快登录并在「个人中心/设置」自行修改为仅你知晓的口令。
--
-- 适用：user 表中 userAccount='admin' 仍为旧 MD5(口令+"yupi") 的记录。
-- 说明：直接写入 BCrypt($2b$10$) 哈希，登录时走 BCrypt 校验分支，
--      与现有 getEncryptPassword / verifyPasswordAndMigrate 逻辑完全兼容。
-- 注意：若 admin 账号此前已登录且 Redis 缓存了登录态，改密后需重新登录
--      （改密码不读 login:user 缓存，无需手动清缓存）。
-- ============================================================

UPDATE `user`
SET userPassword = '$2b$10$MjnxqkZ5IEz5w751G52ol.mDxocvYZ84eOfnkd7uC6WNliOssQ3LW'
WHERE userAccount = 'admin';

-- 复核：应返回 1 行（若返回 0，说明 admin 账号不存在或已被改名）
-- SELECT userAccount, LEFT(userPassword, 7) AS pwd_prefix FROM `user` WHERE userAccount = 'admin';

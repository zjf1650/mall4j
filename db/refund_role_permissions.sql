-- 为超级管理员角色分配退款管理权限
-- 假设超级管理员角色ID为1

-- 分配退款管理主菜单权限
INSERT INTO `tz_sys_role_menu`(`role_id`, `menu_id`) VALUES (1, 317);

-- 分配退款管理按钮权限
INSERT INTO `tz_sys_role_menu`(`role_id`, `menu_id`) VALUES (1, 318);
INSERT INTO `tz_sys_role_menu`(`role_id`, `menu_id`) VALUES (1, 319);
INSERT INTO `tz_sys_role_menu`(`role_id`, `menu_id`) VALUES (1, 320);

-- 查询确认权限是否添加成功
SELECT rm.role_id, rm.menu_id, m.name, m.perms
FROM tz_sys_role_menu rm
LEFT JOIN tz_sys_menu m ON rm.menu_id = m.menu_id
WHERE rm.menu_id IN (317, 318, 319, 320);
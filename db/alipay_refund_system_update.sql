-- ============================================================================
-- 支付宝支付功能及退款系统完整数据库更新脚本
-- 创建日期：2025-09-20
-- 功能说明：
-- 1. 新增支付宝H5支付、当面付支付类型支持
-- 2. 完整的退款管理功能及菜单权限
-- ============================================================================

-- 1. 更新支付结算表注释，支持新的支付类型
-- 支付宝H5支付(6) 和 支付宝当面付(7)
ALTER TABLE `tz_order_settlement`
MODIFY COLUMN `pay_type` int(1) DEFAULT NULL COMMENT '支付方式 1:微信支付 2:支付宝 6:支付宝H5支付 7:支付宝当面付';

-- 2. 更新订单退款表注释，支持新的支付类型
ALTER TABLE `tz_order_refund`
MODIFY COLUMN `pay_type` int(1) DEFAULT NULL COMMENT '支付方式 1:微信支付 2:支付宝 6:支付宝H5支付 7:支付宝当面付';

-- 3. 在订单管理下添加退款管理菜单
INSERT INTO `tz_sys_menu`(`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`) VALUES
(317, 91, '退款管理', 'refund/refund', '', 1, NULL, 2);

-- 4. 添加退款管理的按钮权限
INSERT INTO `tz_sys_menu`(`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`) VALUES
(318, 317, '查看退款列表', '', 'admin:refund:page', 2, '', 1),
(319, 317, '查看退款详情', '', 'admin:refund:info', 2, '', 2),
(320, 317, '审核退款', '', 'admin:refund:audit', 2, '', 3);

-- 5. 更新AUTO_INCREMENT值，确保菜单ID不冲突
ALTER TABLE `tz_sys_menu` AUTO_INCREMENT = 321;

-- ============================================================================
-- 说明：
-- 1. 支付类型扩展：
--    原有：1(微信支付) 2(支付宝)
--    新增：6(支付宝H5支付) 7(支付宝当面付)
--
-- 2. 菜单权限说明：
--    - menu_id 317: 退款管理主菜单，隶属于订单管理(parent_id=91)
--    - menu_id 318-320: 退款管理的三个按钮权限
--    - 权限标识对应AdminRefundController中的@PreAuthorize注解
--
-- 3. 退款表结构说明：
--    - refund_sts: 1=待审核, 2=已同意, 3=已拒绝
--    - return_money_sts: 0=退款处理中, 1=退款成功, -1=退款失败
--    - apply_type: 1=仅退款, 2=退货退款
--
-- 4. 支付渠道退款：
--    - 支付宝(2,6,7)类型：调用真实支付宝退款API
--    - 微信支付(1)类型：当前为模拟退款（可后续扩展）
-- ============================================================================
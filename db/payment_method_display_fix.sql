-- ============================================================================
-- 订单详情页面支付方式显示修复
-- 更新日期：2025-09-25
-- 说明：修复订单详情页面支付方式硬编码为"微信支付"的问题
-- ============================================================================

-- 修复内容：
-- 1. OrderShopDto 添加 payType 字段
-- 2. MyOrderController 在返回订单详情时设置 payType
-- 3. 前端根据 payType 动态显示支付方式名称

-- 支付方式对应关系：
-- 1: 微信支付
-- 2: 支付宝
-- 6: 支付宝H5支付
-- 7: 支付宝当面付

-- 检查订单表中的支付方式数据分布
SELECT
    pay_type,
    COUNT(*) as count,
    CASE pay_type
        WHEN 1 THEN '微信支付'
        WHEN 2 THEN '支付宝'
        WHEN 6 THEN '支付宝H5支付'
        WHEN 7 THEN '支付宝当面付'
        ELSE '未知支付方式'
    END as pay_type_name
FROM tz_order
WHERE pay_type IS NOT NULL
GROUP BY pay_type
ORDER BY pay_type;

-- 检查是否有空的支付方式
SELECT COUNT(*) as null_pay_type_count
FROM tz_order
WHERE pay_type IS NULL;

-- 修复前后对比：
-- 修复前：订单详情页面固定显示"微信支付"
-- 修复后：根据订单实际支付方式动态显示对应名称
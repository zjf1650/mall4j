-- 调试退款查询问题的SQL脚本

-- 1. 查看所有退款记录
SELECT
    refund_id,
    shop_id,
    order_number,
    refund_amount,
    refund_sts,
    apply_time,
    buyer_msg
FROM tz_order_refund
ORDER BY apply_time DESC;

-- 2. 查看具体的退款记录信息
SELECT
    r.refund_id,
    r.shop_id,
    r.order_number,
    r.refund_amount,
    r.refund_sts,
    r.buyer_msg,
    o.shop_id as order_shop_id,
    o.status as order_status
FROM tz_order_refund r
LEFT JOIN tz_order o ON r.order_number = o.order_number
ORDER BY r.apply_time DESC;

-- 3. 检查shop_id是否匹配
SELECT DISTINCT shop_id FROM tz_order_refund;
SELECT DISTINCT shop_id FROM tz_order;

-- 4. 查看管理员用户的shop_id
SELECT user_id, username, shop_id FROM tz_sys_user WHERE username = 'admin';

-- 5. 检查退款记录的详细信息（包括可能为NULL的字段）
SELECT
    refund_id,
    shop_id,
    order_id,
    order_number,
    order_amount,
    refund_amount,
    refund_sts,
    return_money_sts,
    apply_type,
    user_id,
    buyer_msg,
    seller_msg,
    apply_time,
    audit_time,
    flow_trade_no,
    pay_type
FROM tz_order_refund
ORDER BY apply_time DESC
LIMIT 5;
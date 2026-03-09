-- 强一致性库存扣减：原子检查并扣减，防止超卖
-- KEYS[1] 库存 key，如 inventory:stock:{skuId}
-- ARGV[1] 扣减数量（正整数）
-- 返回：扣减后剩余库存（数字）；若库存不足则返回 -1
local key = KEYS[1]
local deduct = tonumber(ARGV[1])
if deduct == nil or deduct <= 0 then
    return -2
end
local current = tonumber(redis.call('GET', key))
if current == nil then
    return -1
end
if current < deduct then
    return -1
end
redis.call('DECRBY', key, deduct)
return current - deduct

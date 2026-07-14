-- 滑动窗口限流 Lua 脚本
-- KEYS[1]: 限流 Key（如 rate_limit:/api/auth/login:127.0.0.1）
-- ARGV[1]: 窗口起始时间戳（毫秒，当前时间 - 窗口大小）
-- ARGV[2]: 当前时间戳（毫秒）
-- ARGV[3]: 最大请求数

-- 1. 删除窗口外的旧记录
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1])

-- 2. 统计窗口内的请求数
local count = redis.call('ZCARD', KEYS[1])

-- 3. 判断是否超限
if count < tonumber(ARGV[3]) then
    -- 未超限，记录本次请求
    redis.call('ZADD', KEYS[1], ARGV[2], ARGV[2])
    -- 设置过期时间为窗口大小的 2 倍（防止 Key 堆积）
    redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[3]) * 2000)
    return 1  -- 允许
else
    return 0  -- 拒绝
end

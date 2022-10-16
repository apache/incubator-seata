--
-- User: tianyu.li
-- Date: 2021/1/19
--
-- init data
local array = {}; local result; local keySize = ARGV[1]; local argSize = ARGV[2];
-- Loop through all keys to see if they can be used , when a key is not available, exit
for i= 1, keySize do
    -- search lock xid
    result = redis.call('HGET',KEYS[i],'xid');
    -- if lock xid is nil
    if (not result)
        -- set 'no' mean There is need to store lock information
        then array[i]='no'
        else
           if (result ~= ARGV[3])
           then
               -- return fail
               return result
           else
               -- set 'yes' mean  There is not need to store lock information
               array[i]= 'yes'
           end
    end
end
-- Loop through array
for i =1, keySize do
    -- if is no ,The lock information is stored
    if(array[i] == 'no')
        then
            -- set xid
            redis.call('HSET',KEYS[i],'xid',ARGV[3]);
            -- set transactionId
            redis.call('HSET',KEYS[i],'transactionId',ARGV[(i-1)*6+4]);
            -- set branchId
            redis.call('HSET',KEYS[i],'branchId',ARGV[(i-1)*6+5]);
            -- set resourceId
            redis.call('HSET',KEYS[i],'resourceId',ARGV[(i-1)*6+6]);
            -- set tableName
            redis.call('HSET',KEYS[i],'tableName',ARGV[(i-1)*6+7]);
            -- set rowKey
            redis.call('HSET',KEYS[i],'rowKey',ARGV[(i-1)*6+8]);
            -- set pk
            redis.call('HSET',KEYS[i],'pk',ARGV[(i-1)*6+9]);
    -- exit if
    end
-- exit for
end
-- set SEATA_GLOBAL_LOCK
redis.call('HSET',KEYS[(keySize+1)],KEYS[(keySize+2)],ARGV[(argSize+0)]);
--  return success
return ARGV[3]

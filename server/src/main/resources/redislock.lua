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
               return 0
           else
               -- set 'yes' mean  There is no need to store lock information
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
            redis.call('HSET',KEYS[i],'xid',ARGV[(i-1)*7+4]);
            -- set transactionId
            redis.call('HSET',KEYS[i],'transactionId',ARGV[(i-1)*7+5]);
            -- set transactionId
            redis.call('HSET',KEYS[i],'branchId',ARGV[(i-1)*7+6]);
            -- set branchId
            redis.call('HSET',KEYS[i],'resourceId',ARGV[(i-1)*7+7]);
            -- set resourceId
            redis.call('HSET',KEYS[i],'tableName',ARGV[(i-1)*7+8]);
            -- set rowKey
            redis.call('HSET',KEYS[i],'rowKey',ARGV[(i-1)*7+9]);
            -- set pk
            redis.call('HSET',KEYS[i],'pk',ARGV[(i-1)*7+10]);
    -- exit if
    end
-- exit for
end
-- set SEATA_GLOBAL_LOCK
redis.call('HSET',KEYS[(keySize+1)],KEYS[(keySize+2)],ARGV[(argSize+0)]);
--  return success
return 1

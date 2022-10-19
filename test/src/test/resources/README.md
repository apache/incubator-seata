## MySQLUpdateJoinTest
##测试表结构
CREATE TABLE `t` (
`id` int NOT NULL,
`a` int DEFAULT NULL,
`c` int DEFAULT NULL,
PRIMARY KEY (`id`),
KEY `a` (`a`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `t1` (
`id` int NOT NULL,
`a` int DEFAULT NULL,
`b` int DEFAULT NULL,
`c` int DEFAULT NULL,
PRIMARY KEY (`id`),
KEY `a` (`a`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
###测试数据
insert into t(id,a,c) values(1,1,1);\
insert into t1(id,a,b,c) values(2,1,2,2)
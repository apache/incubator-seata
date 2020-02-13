DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `user_id` varchar(255) DEFAULT NULL,
   `amount` double(14,2) DEFAULT '0.00',
   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `t_account` VALUES ('1', '1', '4000.00');


DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `order_no` varchar(255) DEFAULT NULL,
    `user_id` varchar(255) DEFAULT NULL,
    `commodity_code` varchar(255) DEFAULT NULL,
    `total` int(11) DEFAULT '0',
    `amount` double(14,2) DEFAULT '0.00',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `t_storage`;
CREATE TABLE `t_storage` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `commodity_code` varchar(255) DEFAULT NULL,
   `name` varchar(255) DEFAULT NULL,
   `total` int(11) DEFAULT '0',
   PRIMARY KEY (`id`),
   UNIQUE KEY `commodity_code` (`commodity_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `t_storage` VALUES ('1', 'C201901140001', '水杯', '1000');

-- SEATA AT 模式需要 UNDO_LOG 表
-- 注意此处0.3.0+ 增加唯一索引 ux_undo_log
CREATE TABLE `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  `ext` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

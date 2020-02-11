
SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for t_account
-- ----------------------------
DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `user_id` varchar(255) DEFAULT NULL,
   `amount` double(14,2) DEFAULT '0.00',
   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of t_account
-- ----------------------------
INSERT INTO `t_account` VALUES ('1', '1', '4000.00');

-- ----------------------------
-- Table structure for t_order
-- ----------------------------
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

-- ----------------------------
-- Records of t_order
-- ----------------------------

-- ----------------------------
-- Table structure for t_storage
-- ----------------------------
DROP TABLE IF EXISTS `t_storage`;
CREATE TABLE `t_storage` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `commodity_code` varchar(255) DEFAULT NULL,
   `name` varchar(255) DEFAULT NULL,
   `total` int(11) DEFAULT '0',
   PRIMARY KEY (`id`),
   UNIQUE KEY `commodity_code` (`commodity_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of t_storage
-- ----------------------------
INSERT INTO `t_storage` VALUES ('1', 'C201901140001', '水杯', '1000');

-- ----------------------------
-- Records of undo_log
-- ----------------------------
SET FOREIGN_KEY_CHECKS=1;

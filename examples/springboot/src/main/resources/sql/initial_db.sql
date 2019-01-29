SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_asset_assign
-- ----------------------------
DROP TABLE IF EXISTS `t_asset_assign`;
CREATE TABLE `t_asset_assign`  (
  `id` varchar(32) NOT NULL COMMENT '',
  `asset_id` varchar(32) NOT NULL COMMENT '',
  `desc` varchar(255)  NULL DEFAULT NULL COMMENT '',
  `status` char(2)  NOT NULL COMMENT '',
  `create_user` varchar(32)  NOT NULL COMMENT '',
  `create_time` datetime(0) NOT NULL,
  `update_user` varchar(32)  NOT NULL COMMENT '',
  `update_time` datetime(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB;

-- ----------------------------
-- Records of t_asset_assign
-- ----------------------------
INSERT INTO `t_asset_assign` VALUES ('14070e0e3cfe403098fa9ca37e8e7e76', 'e2d1c4512d554db9ae4a5f30cbc2e4b1', NULL, '04', '2fb4a37dcc8043e3b6b8da9deba7b2b8', '2019-01-03 17:38:25', '2fb4a37dcc8043e3b6b8da9deba7b2b8', '2019-01-03 17:38:36');

-- ----------------------------
-- Table structure for t_asset
-- ----------------------------
DROP TABLE IF EXISTS `t_asset`;
CREATE TABLE `t_asset`  (
  `id` varchar(32)  NOT NULL COMMENT '',
  `voucher_code` varchar(100)   NOT NULL COMMENT '',
  `amount` decimal(12, 0) NOT NULL COMMENT '',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB;

-- ----------------------------
-- Table structure for undo_log
-- ----------------------------
CREATE TABLE `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  `ext` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_unionkey` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=159;

-- ----------------------------
-- Records of t_asset
-- ----------------------------
INSERT INTO `t_asset` VALUES ('DF001', 'DF001-V-CODE', 100);
SET FOREIGN_KEY_CHECKS = 1;

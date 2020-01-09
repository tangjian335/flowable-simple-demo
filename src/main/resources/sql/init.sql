/*
Navicat MySQL Data Transfer

Source Server         : 192.168.31.100
Source Server Version : 50505
Source Host           : 192.168.31.100:3306
Source Database       : flowable

Target Server Type    : MYSQL
Target Server Version : 50505
File Encoding         : 65001

Date: 2019-04-16 15:09:35
*/


-- ----------------------------
-- Table structure for `bbd_user`
-- ----------------------------
DROP TABLE IF EXISTS `bbd_user`;
CREATE TABLE `bbd_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `role_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of bbd_user
-- ----------------------------
INSERT INTO `bbd_user` VALUES ('1', '唐建', '普通员工');
INSERT INTO `bbd_user` VALUES ('2', '马杲灵', '直接领导');
INSERT INTO `bbd_user` VALUES ('3', '廖海峰', '事业线领导');
INSERT INTO `bbd_user` VALUES ('4', '吴桐', '事业线领导');
INSERT INTO `bbd_user` VALUES ('5', '曾途', '总裁');

-- ----------------------------
-- Table structure for `bbd_vacation_record`
-- ----------------------------
DROP TABLE IF EXISTS `bbd_vacation_record`;
CREATE TABLE `bbd_vacation_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `start` date NOT NULL,
  `end` date NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of bbd_vacation_record
-- ----------------------------

/*
 Navicat Premium Data Transfer

 Source Server         : centos
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : 43.138.28.205:3307
 Source Schema         : device_manage

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 08/12/2025 19:53:43
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for borrow_records
-- ----------------------------
DROP TABLE IF EXISTS `borrow_records`;
CREATE TABLE `borrow_records`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鍊熺敤璁板綍ID',
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID锛堝?鐢ㄤ汉锛',
  `device_id` bigint NOT NULL COMMENT '璁惧?ID',
  `device_instance_id` bigint NULL DEFAULT NULL COMMENT '设备实例ID',
  `quantity` int NOT NULL COMMENT '鍊熺敤鏁伴噺',
  `purpose` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍊熺敤鐩?殑',
  `expected_return_date` date NOT NULL COMMENT '棰勮?褰掕繕鏃ユ湡',
  `actual_return_date` date NULL DEFAULT NULL COMMENT '瀹為檯褰掕繕鏃ユ湡',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '鐘舵?锛歅ENDING-鐢宠?涓?紝APPROVED-宸叉壒鍑嗭紝REJECTED-宸叉嫆缁濓紝RETURNED-宸插綊杩',
  `approve_user_id` bigint NULL DEFAULT NULL COMMENT '瀹℃壒浜篒D锛堢?鐞嗗憳锛',
  `approve_time` datetime NULL DEFAULT NULL COMMENT '瀹℃壒鏃堕棿',
  `approve_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '瀹℃壒澶囨敞',
  `return_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '褰掕繕澶囨敞',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿锛堢敵璇锋椂闂达級',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_device_id`(`device_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_approve_user_id`(`approve_user_id` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  INDEX `idx_device_instance_id`(`device_instance_id` ASC) USING BTREE,
  CONSTRAINT `fk_borrow_records_approve_user` FOREIGN KEY (`approve_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_borrow_records_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_borrow_records_device_instance` FOREIGN KEY (`device_instance_id`) REFERENCES `device_instances` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_borrow_records_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '鍊熺敤璁板綍琛' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of borrow_records
-- ----------------------------
INSERT INTO `borrow_records` VALUES (11, 7, 7, NULL, 1, '出去旅游', '2025-12-09', NULL, 'REJECTED', 2, '2025-12-08 13:17:13', '123', NULL, '2025-12-08 13:06:04', '2025-12-08 13:06:04');
INSERT INTO `borrow_records` VALUES (12, 7, 7, NULL, 1, '123', '2025-12-31', '2025-12-08', 'RETURNED', 2, '2025-12-08 13:08:49', '可以', '', '2025-12-08 13:07:28', '2025-12-08 13:07:28');
INSERT INTO `borrow_records` VALUES (13, 7, 1, 1, 1, '拍景色', '2025-12-27', '2025-12-08', 'RETURNED', 2, '2025-12-08 14:42:11', '', '', '2025-12-08 14:41:55', '2025-12-08 14:41:55');
INSERT INTO `borrow_records` VALUES (16, 2, 1, 1, 1, '123', '2025-12-10', '2025-12-08', 'RETURNED', 2, '2025-12-08 15:06:13', '', '', '2025-12-08 15:06:06', '2025-12-08 15:06:06');
INSERT INTO `borrow_records` VALUES (17, 2, 6, 4, 1, '拍花', '2025-12-09', '2025-12-08', 'RETURNED', 2, '2025-12-08 16:48:53', '', '', '2025-12-08 16:48:41', '2025-12-08 16:48:41');
INSERT INTO `borrow_records` VALUES (18, 2, 2, 2, 1, '123', '2025-12-09', NULL, 'REJECTED', 2, '2025-12-08 16:58:03', '', NULL, '2025-12-08 16:50:50', '2025-12-08 16:50:50');
INSERT INTO `borrow_records` VALUES (19, 2, 6, 4, 1, '玩玩', '2025-12-09', NULL, 'REJECTED', 2, '2025-12-08 16:57:05', '', NULL, '2025-12-08 16:53:16', '2025-12-08 16:53:16');
INSERT INTO `borrow_records` VALUES (20, 2, 6, 4, 1, '123', '2025-12-17', NULL, 'REJECTED', 2, '2025-12-08 16:58:02', '', NULL, '2025-12-08 16:57:37', '2025-12-08 16:57:37');
INSERT INTO `borrow_records` VALUES (21, 2, 6, 4, 1, '1', '2025-12-12', NULL, 'REJECTED', 2, '2025-12-08 17:00:48', '', NULL, '2025-12-08 17:00:37', '2025-12-08 17:00:37');
INSERT INTO `borrow_records` VALUES (22, 1, 2, 2, 1, '124', '2025-12-11', '2025-12-08', 'RETURNED', 1, '2025-12-08 18:28:51', '', '', '2025-12-08 18:28:40', '2025-12-08 18:28:40');
INSERT INTO `borrow_records` VALUES (23, 1, 2, 2, 1, '123123123', '2025-12-10', '2025-12-08', 'RETURNED', 1, '2025-12-08 18:30:01', '', '', '2025-12-08 18:29:07', '2025-12-08 18:29:07');
INSERT INTO `borrow_records` VALUES (24, 2, 2, 2, 1, '12312312', '2026-01-08', NULL, 'PENDING', NULL, NULL, NULL, NULL, '2025-12-08 18:31:21', '2025-12-08 18:31:21');

-- ----------------------------
-- Table structure for categories
-- ----------------------------
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鍒嗙被ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍒嗙被鍚嶇О',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '鍒嗙被鎻忚堪',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '璁惧?鍒嗙被琛' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of categories
-- ----------------------------
INSERT INTO `categories` VALUES (1, '相机', '各类相机设备，包括单反、无反、摄像机等', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `categories` VALUES (2, '镜头', '各类镜头设备，包括定焦、变焦、广角、长焦等', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `categories` VALUES (3, '灯光', '各类灯光设备，包括LED灯、闪光灯、柔光灯等', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `categories` VALUES (4, '线材', '各类线材设备，包括HDMI线、USB线、音频线等', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `categories` VALUES (5, '三脚架', '各类三脚架和稳定器设备', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `categories` VALUES (6, '存储设备', '各类存储设备，包括SD卡、移动硬盘等', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `categories` VALUES (7, '音频设备', '各类音频设备，包括麦克风、录音笔等', '2025-12-08 10:02:18', '2025-12-08 10:02:18');

-- ----------------------------
-- Table structure for device_instances
-- ----------------------------
DROP TABLE IF EXISTS `device_instances`;
CREATE TABLE `device_instances`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备实例ID',
  `device_id` bigint NOT NULL COMMENT '设备ID（关联设备模板）',
  `instance_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备实例名称（如：尼康1、尼康2）',
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存放位置',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常，MAINTENANCE-维修中，SCRAPPED-已报废',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '设备实例描述',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_device_id`(`device_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_instance_name`(`instance_name` ASC) USING BTREE,
  CONSTRAINT `fk_device_instances_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '设备实例表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of device_instances
-- ----------------------------
INSERT INTO `device_instances` VALUES (1, 1, '佳能 EOS R5-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:41:48', '2025-12-08 14:41:48');
INSERT INTO `device_instances` VALUES (2, 2, '索尼 A7M4-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:41:48', '2025-12-08 14:41:48');
INSERT INTO `device_instances` VALUES (4, 6, '佳能 RF 24-70mm f/2.8-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:41:48', '2025-12-08 14:41:48');
INSERT INTO `device_instances` VALUES (5, 7, '索尼 FE 85mm f/1.4 GM-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:41:48', '2025-12-08 14:41:48');
INSERT INTO `device_instances` VALUES (7, 9, '适马 14-24mm f/2.8-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:41:48', '2025-12-08 14:41:48');
INSERT INTO `device_instances` VALUES (8, 10, '爱图仕 LS 300X-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:41:48', '2025-12-08 14:41:48');
INSERT INTO `device_instances` VALUES (11, 13, '爱图仕 Nova P300c-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:41:48', '2025-12-08 14:41:48');
INSERT INTO `device_instances` VALUES (13, 15, 'USB-C 数据线 2米-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:43:34', '2025-12-08 14:43:34');
INSERT INTO `device_instances` VALUES (14, 16, 'XLR 音频线 5米-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:43:34', '2025-12-08 14:43:34');
INSERT INTO `device_instances` VALUES (15, 17, '曼富图 055 三脚架-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:43:34', '2025-12-08 14:43:34');
INSERT INTO `device_instances` VALUES (16, 18, '大疆 RS 3 稳定器-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:43:34', '2025-12-08 14:43:34');
INSERT INTO `device_instances` VALUES (17, 19, '智云 Weebill 3-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:43:34', '2025-12-08 14:43:34');
INSERT INTO `device_instances` VALUES (18, 20, '闪迪 Extreme Pro SD卡 128GB-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:43:34', '2025-12-08 14:43:34');
INSERT INTO `device_instances` VALUES (19, 21, '三星 T7 移动硬盘 1TB-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:43:34', '2025-12-08 14:43:34');
INSERT INTO `device_instances` VALUES (20, 22, '希捷 Backup Plus 移动硬盘 2TB-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:43:34', '2025-12-08 14:43:34');
INSERT INTO `device_instances` VALUES (21, 23, '森海塞尔 MKH 416 麦克风-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:43:34', '2025-12-08 14:43:34');
INSERT INTO `device_instances` VALUES (22, 24, '舒尔 SM58 麦克风-001', '默认位置', 'NORMAL', '自动创建的默认实例', '2025-12-08 14:43:34', '2025-12-08 14:43:34');

-- ----------------------------
-- Table structure for devices
-- ----------------------------
DROP TABLE IF EXISTS `devices`;
CREATE TABLE `devices`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '璁惧?ID',
  `category_id` bigint NOT NULL COMMENT '鍒嗙被ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '璁惧?鍚嶇О',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '璁惧?鍨嬪彿',
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '默认存放位置（设备实例可能有不同位置）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NORMAL' COMMENT '鐘舵?锛歂ORMAL-姝ｅ父锛孧AINTENANCE-缁翠慨涓?紝SCRAPPED-宸叉姤搴',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '璁惧?鎻忚堪',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_name_model`(`name` ASC, `model` ASC) USING BTREE,
  CONSTRAINT `fk_devices_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '璁惧?琛' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of devices
-- ----------------------------
INSERT INTO `devices` VALUES (1, 1, '佳能 EOS R5', 'EOS R5', 'A区-001', 'NORMAL', '全画幅无反相机，4500万像素，支持8K视频录制', '2025-12-08 10:02:18', '2025-12-08 14:36:52');
INSERT INTO `devices` VALUES (2, 1, '索尼 A7M4', 'ILCE-7M4', 'A区-002', 'NORMAL', '全画幅无反相机，3300万像素，支持4K视频录制', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (6, 2, '佳能 RF 24-70mm f/2.8', 'RF 24-70mm f/2.8', 'B区-001', 'NORMAL', '标准变焦镜头，适合多种拍摄场景', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (7, 2, '索尼 FE 85mm f/1.4 GM', 'FE 85mm f/1.4 GM', 'B区-002', 'NORMAL', '人像定焦镜头，大光圈虚化效果', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (9, 2, '适马 14-24mm f/2.8', '14-24mm f/2.8', 'B区-004', 'NORMAL', '超广角变焦镜头，适合风景和建筑拍摄', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (10, 3, '爱图仕 LS 300X', 'LS 300X', 'C区-001', 'NORMAL', 'LED摄影灯，300W功率，支持RGB调色', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (13, 3, '爱图仕 Nova P300c', 'Nova P300c', 'C区-004', 'NORMAL', 'RGB LED面板灯，300W功率，支持无线控制', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (15, 4, 'USB-C 数据线 2米', 'USB-C-2M', 'D区-002', 'NORMAL', '支持USB 3.1 Gen2，传输速度10Gbps', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (16, 4, 'XLR 音频线 5米', 'XLR-5M', 'D区-003', 'NORMAL', '专业音频线，平衡传输，抗干扰', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (17, 5, '曼富图 055 三脚架', '055', 'E区-001', 'NORMAL', '碳纤维三脚架，最大承重8kg', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (18, 5, '大疆 RS 3 稳定器', 'RS 3', 'E区-002', 'NORMAL', '专业相机稳定器，支持自动轴锁', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (19, 5, '智云 Weebill 3', 'Weebill 3', 'E区-003', 'NORMAL', '轻量级稳定器，适合小型相机', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (20, 6, '闪迪 Extreme Pro SD卡 128GB', 'Extreme Pro 128GB', 'F区-001', 'NORMAL', 'UHS-I，读取速度170MB/s，写入速度90MB/s', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (21, 6, '三星 T7 移动硬盘 1TB', 'T7 1TB', 'F区-002', 'NORMAL', 'USB 3.2 Gen2，读取速度1050MB/s', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (22, 6, '希捷 Backup Plus 移动硬盘 2TB', 'Backup Plus 2TB', 'F区-003', 'NORMAL', 'USB 3.0，适合数据备份', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (23, 7, '森海塞尔 MKH 416 麦克风', 'MKH 416', 'G区-001', 'NORMAL', '专业枪式麦克风，适合影视录音', '2025-12-08 10:02:18', '2025-12-08 10:02:18');
INSERT INTO `devices` VALUES (24, 7, '舒尔 SM58 麦克风', 'SM58', 'G区-002', 'NORMAL', '经典动圈麦克风，适合人声录制', '2025-12-08 10:02:18', '2025-12-08 10:02:18');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鐢ㄦ埛ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐢ㄦ埛鍚嶏紙鍞?竴锛',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '瀵嗙爜锛堝姞瀵嗗瓨鍌?級',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐪熷疄濮撳悕',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER' COMMENT '瑙掕壊锛欰DMIN-绠＄悊鍛橈紝USER-鏅??鐢ㄦ埛',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '鐘舵?锛欰CTIVE-婵?椿锛孖NACTIVE-绂佺敤',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  INDEX `idx_role`(`role` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '鐢ㄦ埛琛' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'root', '$2a$10$cPZGMzNQthunR0bPbP.r6O6NqEz12rsTn33dYsj6fdu40fCv2eKsW', '超级管理员', 'ROOT', 'ACTIVE', '2025-12-08 18:14:26', '2025-12-08 18:15:20');
INSERT INTO `users` VALUES (2, 'admin', '$2a$10$b68xPCyu2uuxoo9tVV/XM.HB4vd5EksRJl2E8rTY8g.i7o3TnD3XW', '管理员', 'ADMIN', 'ACTIVE', '2025-12-08 10:02:18', '2025-12-08 18:15:02');
INSERT INTO `users` VALUES (7, 'u1', '$2a$10$m0.sM1YmykZPAeQ/C4Z9S.pwbYRy7F2lVlyo2/mUDSjpliqcOH3MG', '赵杰', 'USER', 'ACTIVE', '2025-12-08 12:47:48', '2025-12-08 14:59:33');

SET FOREIGN_KEY_CHECKS = 1;

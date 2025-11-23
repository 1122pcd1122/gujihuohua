CREATE DATABASE IF NOT EXISTS gujihuohua DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;

USE gujihuohua;

CREATE TABLE `ancient_text` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `title` varchar(255) NOT NULL COMMENT '古籍标题/文件名',
                                `category` varchar(50) DEFAULT NULL COMMENT '类别(史书/志书等)',
                                `raw_content` longtext COMMENT '原始文本',
                                `processed_json` longtext COMMENT '处理后的JSON数据(核心)',
                                `sentence_count` int(11) DEFAULT '0' COMMENT '总句数',
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='古籍语料表';
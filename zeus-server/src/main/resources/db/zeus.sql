CREATE TABLE `zeus_id_alloc`(
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `biz_type` VARCHAR(20) NOT NULL COMMENT '业务',
    `max_id` BIGINT NOT NULL DEFAULT 0 COMMENT '最大id',
    `step` INT NOT NULL DEFAULT 0 COMMENT '增长步长',
    `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `description` VARCHAR(50) COMMENT '业务描述',
    `gmt_create` DATETIME NOT NULL COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_type`(`biz_type`)
)
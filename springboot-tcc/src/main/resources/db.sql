CREATE TABLE `product` (
                           `id` bigint NOT NULL,
                           `product_name` varchar(255) DEFAULT NULL,
                           `price` decimal(10,2) DEFAULT NULL,
                           `num` bigint DEFAULT NULL,
                           PRIMARY KEY (`id`)
);
CREATE TABLE `product_consumption_detail` (
                                              `id` bigint NOT NULL AUTO_INCREMENT,
                                              `product_id` bigint DEFAULT NULL,
                                              `num` bigint DEFAULT NULL,
                                              `order_id` bigint DEFAULT NULL,
                                              `status` int DEFAULT NULL,
                                              `created_time` datetime DEFAULT NULL,
                                              `updated_time` datetime DEFAULT NULL,
                                              PRIMARY KEY (`id`)
);
CREATE TABLE `tcc_order` (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `product_id` bigint DEFAULT NULL,
                             `product_name` varchar(255) DEFAULT NULL,
                             `product_num` bigint DEFAULT NULL,
                             `product_price` decimal(10,2) DEFAULT NULL,
                             `order_amount` decimal(10,2) DEFAULT NULL,
                             `order_status` smallint DEFAULT NULL,
                             `order_time` datetime DEFAULT NULL,
                             PRIMARY KEY (`id`)
);
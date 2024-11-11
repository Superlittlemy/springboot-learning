create table se_order
(
    id               bigint auto_increment
        primary key,
    merchandise_id   bigint         null comment '商品id',
    merchandise_name varchar(255)   null comment '商品名称',
    unit_price       decimal(10, 2) null comment '单价',
    quantity         decimal(10, 2) null comment '数量',
    total_price      decimal(10, 2) null comment '总价',
    status           smallint       null comment '状态：1.正常、2.关闭',
    created_time     datetime       null comment '创建时间'
);
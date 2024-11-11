create table merchandise
(
    id         bigint auto_increment
        primary key,
    name       varchar(255)   null comment '商品名称',
    unit_price decimal(10, 2) null comment '单价',
    quantity   decimal(10, 2) null comment '数量',
    status     smallint       null comment '状态：0.未上架、1.正常销售、9.售罄'
);
create table sale_detail
(
    id               bigint auto_increment
        primary key,
    merchandise_id   bigint         null comment '商品id',
    merchandise_name varchar(255)   null comment '商品名称',
    unit_price       decimal(10, 2) null comment '单价',
    quantity         decimal(10, 2) null comment '数量',
    total_price      decimal(10, 2) null comment '总价',
    account_id       bigint         null comment '账户id',
    status           smallint       null comment '状态：0.待确认、1.正常、2.撤销',
    request_id       varchar(255)   null comment '请求id',
    created_time     datetime       null comment '创建时间'
);
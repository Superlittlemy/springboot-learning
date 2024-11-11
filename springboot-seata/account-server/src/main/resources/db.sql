create table account
(
    id       bigint auto_increment
        primary key,
    username varchar(255)   null comment '账户名',
    balance  decimal(10, 2) null comment '余额'
);
create table expense_detail
(
    id           bigint auto_increment
        primary key,
    account_id   bigint         null comment '账户id',
    amount       decimal(10, 2) null comment '金额',
    created_time datetime       null comment '创建时间'
);
-- auto-generated definition
create table finance_invoice_status
(
    store_id       bigint                  not null comment '店铺id',
    market_code    varchar(20)             not null comment '站点code',
    payment_number varchar(100)            not null comment '付款编号',
    invoice_number varchar(100)            not null comment '发票编号',
    type           varchar(50)             not null comment '类型',
    status         varchar(50)             not null comment '状态值',
    value          varchar(2000)           null comment '值',
    value2         varchar(2000)           null comment '值',
    value3         varchar(2000)           null comment '值',
    create_by      varchar(64) default ' ' not null comment '创建人',
    update_by      varchar(64) default ' ' not null comment '修改人',
    create_time    datetime                null comment '创建时间',
    update_time    datetime                null comment '修改时间',
    constraint uk_finance_invoice_status
        unique (store_id, market_code, payment_number, invoice_number, type)
)
    comment '发票状态表';


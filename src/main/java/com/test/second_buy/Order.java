package com.test.second_buy;

import java.text.SimpleDateFormat;

/**
 * 尽量简短，减少redis读写压力
 *
 * @ClassName Order
 * @Description
 */
public class Order {

    private String oun;//orderUserName下单用户名（或ID）

    private String opn;//orderProductName下单产品名（或ID）

    private String time;

    /**
     * 注意，本ID在本次抢购中必须是唯一ID，不然可能出现最终的下单记录总数和已被下单产品数量不一致（记录的下单量小于出库商品总量）。
     */
    private String id;//本ID只有在生成订单号时起作用，主要是为了区分一次下多个单导致的数据内容一致，从而使得sadd覆盖少订单问题，不使用UUID减轻网络和缓冲IO，使用时间戳有精度不够，容易重复

    //必须要有空的构造函数，否则fastjson转换报错
    public Order() {

    }

    public Order(String orderUserName, String orderProductName, String currectId) {
        this.oun = orderUserName;
        this.opn = orderProductName;
        this.id = currectId;
        this.time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
    }

    public String getOun() {
        return oun;
    }

    public void setOun(String oun) {
        this.oun = oun;
    }

    public String getOpn() {
        return opn;
    }

    public void setOpn(String opn) {
        this.opn = opn;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}


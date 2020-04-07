package com.test.second_buy;

import java.io.Serializable;

public class FastBuyBusinessDTO implements Serializable {

    private String id;//

    private String productId;//商品主键

    private String productAmoutId;//商品剩余量ID   String

    private String orderListId;//关联的下订成功客户列表（缓存中的） 集合


    //扩展规则时候用的，暂时没用
    private int maxTransaction; //本次抢购同一用户最多可以购买几个的上限值

    private int maxTransactionNumber;//同一用户一次最多可以抢购几个商品

    private int maxRepeatBuy;//同一用户是否可以重复购买，如果否则为0.如果可以则大于0，表示可以重复参与本次抢购多少次。如单个用户可以参与本次抢购3次，则这里为3

    private int maxAmout;//最大购买量，既本次活动本商品总量

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductAmoutId() {
        return productAmoutId;
    }

    public void setProductAmoutId(String productAmoutId) {
        this.productAmoutId = productAmoutId;
    }

    public String getOrderListId() {
        return orderListId;
    }

    public void setOrderListId(String orderListId) {
        this.orderListId = orderListId;
    }

    public int getMaxTransaction() {
        return maxTransaction;
    }

    public void setMaxTransaction(int maxTransaction) {
        this.maxTransaction = maxTransaction;
    }

    public int getMaxTransactionNumber() {
        return maxTransactionNumber;
    }

    public void setMaxTransactionNumber(int maxTransactionNumber) {
        this.maxTransactionNumber = maxTransactionNumber;
    }

    public int getMaxRepeatBuy() {
        return maxRepeatBuy;
    }

    public void setMaxRepeatBuy(int maxRepeatBuy) {
        this.maxRepeatBuy = maxRepeatBuy;
    }

    public int getMaxAmout() {
        return maxAmout;
    }

    public void setMaxAmout(int maxAmout) {
        this.maxAmout = maxAmout;
    }


}


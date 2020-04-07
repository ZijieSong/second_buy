package com.test.second_buy;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.UUID;

@Service
public class FastBuyServiceImpl {
    private static Logger logger = LoggerFactory.getLogger("");

    /**
     * @param jedis              第一道过滤流量分离的redis主要用于场景隔离，不影响其他业务，如不需要隔离可以和jedisMain是同一个实例
     * @param jedisMain          主要业务redis实例
     * @param fastBuyBusinessDTO 秒杀场景商品基础信息封装类
     * @param orderUserName      关联下单用户名（或ID）
     * @param orders             本商品下单数量
     * @return
     */
    public String fastBuyProductOrders(Jedis jedis, Jedis jedisMain, FastBuyBusinessDTO fastBuyBusinessDTO, String orderUserName, int orders) {
        //过滤部分恶意脚本，不从redis中获取数据直接和初始化数据判断，redis减轻压力，预防部分恶意刷单。
        //伪判断预防一些恶意脚本
        if (!this.allowProductAmout(fastBuyBusinessDTO.getMaxAmout(), orders)) {
            return "抢购商品不足，抢购失败！";
        }

        //判断下单是否超库存，从redis中获取当前库存
        if (!this.allowFastBuyProduc(fastBuyBusinessDTO.getProductAmoutId(), orders, jedis)) {
            return "抢购商品不足，抢购失败！";
        }

        return this.fastBuyProduct(jedisMain, fastBuyBusinessDTO.getProductAmoutId(), fastBuyBusinessDTO.getOrderListId(), orderUserName, orders, fastBuyBusinessDTO.getProductId());


    }

    /**
     * 初步判断下单量是否超出剩余量
     * return 超
     */
    public boolean allowFastBuyProduc(String productAmoutId, int orders, Jedis jedis) {

        //一阶段过滤流量、尽可能不写REDIS，提高性能
        //判断是否还有库存
        //预拿一次，如果剩余商品已经<=0，直接返回结果过滤流量
        int prdNum = Integer.parseInt(jedis.get(productAmoutId));

        //判断所下的单数量是否在剩余范围内
        return this.allowProductAmout(prdNum, orders);
    }

    /**
     * redis 的 watch原理：
     * 当watch监听某个key的时候，redis DB结构在内部维护了一个dict字典，其key为客户端watch的key，value为链表，每个链表节点都是监听了这个key的client
     * 每个client结构有一个flag属性表明了该client的状态
     * 当服务端收到了写命令，会去判断该写命令修改的key是否在dict字典中，若有则将该key对应的链表中每个client的flag置为dirty
     * 当服务端收到了client的exec命令时，会去查看该client的flag属性，如果是dirty则证明该client watch的key被修改过
     * 则此时将队列中的命令抛弃，否则执行队列中的所有命令
     * （在multi开启事物之后，client发起的命令都会缓存在该client结构的一个队列中）
     */

    /**
     * @param jedis
     * @param amoutId       redis中的存本次活动商品总量的KEY
     * @param ordersId      redis中存本次活动商品抢购成功的用户信息队列的KEY
     * @param orders        下单数量
     * @param orderUserName 购买人
     * @param productId     商品标识唯一ID
     * @return
     */
    public String fastBuyProduct(Jedis jedis, String amoutId, String ordersId, String orderUserName, int orders, String productId) {
        String result = "抢商品失败咯！";
        if (logger.isInfoEnabled()) {
            logger.info(orderUserName + "开始抢票-" + orders + " 张！！");
        }

        //TODO 根据实际业务和测试情况，可以用for限制重试抢票次数
        while (true) {
            int i = 0;
            i++;
            if (logger.isInfoEnabled()) {
                logger.info(orderUserName + "---第" + i + "次抢票");
            }
            try {
                jedis.watch(amoutId, ordersId);// 监视key ，如果在以下事务执行之前key的值被其他命令所改动，那么事务将被打断
                int prdNum = Integer.parseInt(jedis.get(amoutId));
                if (this.allowProductAmout(prdNum, orders)) {
                    //TODO 1、商品规则是否满足根据实际情况加
                    //TODO 2、分析用户规则根据实际情况加

                    Transaction transaction = jedis.multi();
                    transaction.set(amoutId, String.valueOf(prdNum - orders));//如果可以一次购买多张需要修改，对应下面的抢购成功的用户入队列也需要修改。
                    // 把当前抢到商品的用户记录到下单队列中，sadd不允许插入重复内容。读队列smembers，读的时候不会出队列，不会改变队列内容，所以不影响CAS中的写入。获取长度scard
                    //封装成JSON格式后提交队列
                    transaction.sadd(ordersId, this.createOrdersString(orderUserName, productId, orders));
                    List<Object> res = transaction.exec();

                    // 事务提交后如果为null，说明key值在本次事务提交前已经被改变，本次事务不执行。
                    if (res == null || res.isEmpty()) {
                        if (logger.isInfoEnabled()) {
                            logger.info(orderUserName + "---第" + i + "次抢-----没有抢到商品，正在重试");
                        }
                    } else {
                        result = "抢购成功！";
                        if (logger.isInfoEnabled()) {
                            logger.info(orderUserName + "---第" + i + "次抢-----抢到商品-");
                        }
                        break;
                    }

                } else {
                    result = "库存为0，本商品已经被抢空了哦，欢迎下次光临88";
                    break;
                }
            } catch (Exception e) {
                logger.error("抢购出错：" + e);
            } finally {
                jedis.unwatch();

            }
        }

        return result;
    }


    /**
     * 是否在库存范围内
     *
     * @return 范围内true、范围外false
     */
    private boolean allowProductAmout(int prdNum, int orders) {

        //商品已经没有库存
        if (prdNum <= 0) {
            return false;
        }
        //商品当前库存不够支付订单量
        if (prdNum - orders < 0) {
            return false;
        }

        return true;
    }

    /**
     * 转JSON格式
     *
     * @param orderUserName
     * @param productId
     * @param orderCount
     * @return
     */
    public String[] createOrdersString(String orderUserName, String productId, int orderCount) {
        String[] result = new String[orderCount];
        for (int i = 0; i < result.length; i++) {
            result[i] = JSON.toJSONString(new Order(orderUserName, productId, UUID.randomUUID().toString()));
        }

        return result;
    }


}

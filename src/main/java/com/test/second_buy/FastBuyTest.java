package com.test.second_buy;

import com.alibaba.fastjson.JSON;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class FastBuyTest {

    private static Jedis jedis1;
    private static Jedis jedis2;
    private static JedisPool pool;
    private static JedisPoolConfig config;

    //业务数据设置
    private static FastBuyBusinessDTO fastBuyBusinessDTO;
    private final static int MAX_AMOUT = 10;//本次活动总商品量

    //测试线程数据设置
    private static CountDownLatch latch;
    //初始化几个线程等待抢商品
    private final static int THREAD_LENG = 200;

    public void init() {
        initRedisPool();//初始化redis数据
        fastBuyBusinessDTO = new FastBuyBusinessDTO();
        initFastBuy();//初始化业务数据

    }

    public void colseResources() {
        jedis1.close();
        pool.close();
    }

    /**
     * 模拟高并发场景（1秒内，1000的并发量）
     *
     * @throws InterruptedException
     */
    public void fastBuyProductTest() throws InterruptedException {
        System.out.println("开始产品量：" + jedis1.get(fastBuyBusinessDTO.getProductAmoutId()));
        System.out.println("开始订单量：" + jedis1.scard(fastBuyBusinessDTO.getOrderListId()));

        for (int i = 0; i < THREAD_LENG + 1; i++) {
            Thread.sleep(20L);
            String orderUserName = "orderUserName_" + i;//模拟高并发情况下，正常情况的多用户下单
//			String orderUserName = "orderUserName_" + "0";//模拟高并发情况下，单个用户多次下单（如黄牛党）
            int orders = 3;
            int ordersRandom = (int) (1 + Math.random() * (4 - 1 + 1));
            Thread th = new Thread(new TestThread(pool, orderUserName, ordersRandom));
            th.setName("THREADDDDD_" + i);
            System.out.println(th.getName() + "inited...");
            th.start();
            latch.countDown();//业务调用完成，计数器减一
        }
        Thread.sleep(3000);

        //显示订单结果，并把订单结果转换成order对象
        if (true) {
            Set<String> orders = jedis1.smembers(fastBuyBusinessDTO.getOrderListId());

            Iterator<String> it = orders.iterator();
            while (it.hasNext()) {
                Order order = (Order) JSON.parseObject(it.next(), Order.class);
                System.out.println("userid:" + order.getOun() + "-----productId:" + order.getOpn() + "------orderTime:" + order.getTime());
            }
        }

        System.out.println("内存剩余产品：" + jedis1.get(fastBuyBusinessDTO.getProductAmoutId()));
        System.out.println("内存订单量：" + jedis1.scard(fastBuyBusinessDTO.getOrderListId()));
    }

    /**
     * 初始抢购
     */
    public static void initFastBuy() {
        fastBuyBusinessDTO.setId("1");
        fastBuyBusinessDTO.setProductAmoutId("product_amout_id_1");
        fastBuyBusinessDTO.setProductId("product_id_1");
        fastBuyBusinessDTO.setOrderListId("order_list_id_1");
        fastBuyBusinessDTO.setMaxAmout(MAX_AMOUT);

        String key = fastBuyBusinessDTO.getProductAmoutId();
        String clientList = fastBuyBusinessDTO.getOrderListId();// 抢购到商品的顾客列表
        if (jedis1.exists(key)) {
            jedis1.del(key);
        }

        if (jedis1.exists(clientList)) {
            jedis1.del(clientList);
        }
        jedis1.set(key, String.valueOf(MAX_AMOUT));// 初始化
    }

    public static void initRedisPool() {
        jedis1 = new Jedis("127.0.0.1", 6379);
        jedis2 = new Jedis("127.0.0.1", 6379);
        //利用Redis连接池，保证多个线程利用多个连接，充分模拟并发性
        config = new JedisPoolConfig();
        config.setMaxIdle(10);
        config.setMaxWaitMillis(1000);
        config.setMaxTotal(THREAD_LENG + 1);
        pool = new JedisPool(config, "127.0.0.1", 6379);
        //利用ExecutorService 管理线程
//        service = Executors.newFixedThreadPool(THREAD_LENG);
        //CountDownLatch保证主线程在全部线程结束之后退出
        latch = new CountDownLatch(THREAD_LENG);
    }

    public static class TestThread implements Runnable {
        private Jedis cli;
        private JedisPool pool;
        private FastBuyServiceImpl fs = new FastBuyServiceImpl();
        private String orderUserName;
        private int orders;

        public TestThread(JedisPool pool, String orderUserName, int orders) {
            cli = pool.getResource();
            this.pool = pool;
            this.orderUserName = orderUserName;
            this.orders = orders;
        }

        public TestThread(Jedis jedis, String orderUserName, int orders) {
            cli = jedis;
            this.orderUserName = orderUserName;
            this.orders = orders;
        }

        public void run() {
            try {
                latch.await();
                fs.fastBuyProductOrders(cli, cli, fastBuyBusinessDTO, orderUserName, orders);
            } catch (Exception e) {
                pool.close();
            }
        }
    }
}


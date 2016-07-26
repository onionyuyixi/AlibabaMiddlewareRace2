package com.alibaba.middleware.race;

import com.alibaba.middleware.race.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.alibaba.middleware.race.concurrentlinkedhashmap.EvictionListener;
import org.junit.Test;

import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

/**
 * Created by yfy on 7/11/16.
 * Test
 */
public class AppTest {

  @Test
  public void queryBig() throws Exception {
    OrderSystem os = constructBig();

    // queryOrder
    OrderSystem.Result result = os.queryOrder(606092157, Arrays.asList("buyername"));
    assertEquals("晋恿吾", result.get("buyername").valueAsString());

    result = os.queryOrder(604911336, Arrays.asList("buyerid"));
    assertEquals("tp-9d00-3b1cf5d41ff5", result.get("buyerid").valueAsString());

    // queryOrdersByBuyer
    os.queryOrdersByBuyer(1470611363, 1484693606, "ap-ab95-3e7e0ed47717");

    // queryOrdersBySaler
    Iterator<OrderSystem.Result> iter = os.queryOrdersBySaler(
        "almm-8f6a-3e6a9697a0f9",
        "aye-8d0d-57e792eb1371",
        Arrays.asList("a_b_19123"));
    result = iter.next();
    assertEquals("c51c1ce6-8d10-401a-ae5e-4f4023911cf3",
        result.get("a_b_19123").valueAsString());
    assertEquals(587983792, result.orderId());

    iter = os.queryOrdersBySaler(
        "ay-bb53-b150818332f2",
        "al-b702-2c34aeaa78cb",
        Arrays.asList("a_b_19123", "a_o_12490", "a_b_26525", "description"));
    result = iter.next();
    assertEquals(587999455, result.orderId());
    result = iter.next();
    assertEquals(588610606, result.orderId());

    // sumOrdersByGood
    OrderSystem.KeyValue kv = os.sumOrdersByGood("al-950f-5924be431212", "a_g_10839");
    assertEquals(null, kv);

    kv = os.sumOrdersByGood("dd-8ad6-8de99e8d7dad", "amount");
    assertEquals(735, kv.valueAsLong());

    kv = os.sumOrdersByGood("dd-b9e1-77c52c63fffa", "price");
    assertEquals(455880.3135284825, kv.valueAsDouble(), 1e-6);

    kv = os.sumOrdersByGood("al-8162-0492cff4394c", "amount");
    assertEquals(552, kv.valueAsLong());

    kv = os.sumOrdersByGood("dd-a665-acd638b44e92", "price");
    assertEquals(117811.2897340, kv.valueAsDouble(), 1e-6);

  }

  private OrderSystem constructBig() throws Exception {
    OrderSystem os = new OrderSystemImpl();
    os.construct(
        Arrays.asList(fn("order.0.0"), fn("order.0.3"), fn("order.1.1"), fn("order.2.2")),
        Arrays.asList(fn("buyer.0.0"), fn("buyer.1.1")),
        Arrays.asList(fn("good.0.0"), fn("good.1.1"), fn("good.2.2")),
        Arrays.asList(
            "/home/yfy/middleware/prerun_data",
            "/home/yfy/middleware/prerun_data",
            "/home/yfy/middleware/prerun_data"));
    return os;
  }

  private String fn(String file) {
    return "/home/yfy/middleware/prerun_data/" + file;
  }

  @Test
  public void constructMedium() throws Exception {
    OrderSystem os = new OrderSystemImpl();
    os.construct(Arrays.asList("order_records.txt"),
        Arrays.asList("buyer_records.txt"),
        Arrays.asList("good_records.txt"),
        Arrays.asList(
            "/home/yfy/middleware/race2",
            "/home/yfy/middleware/race2",
            "/home/yfy/middleware/race2"));
  }

  @Test
  public void testMedium() throws Exception {
    OrderSystem os = new OrderSystemImpl();
    os.construct(Arrays.asList("order_records.txt"),
        Arrays.asList("buyer_records.txt"),
        Arrays.asList("good_records.txt"),
        Arrays.asList(
            "/home/yfy/middleware/race2",
            "/home/yfy/middleware/race2",
            "/home/yfy/middleware/race2"));

    OrderSystem.Result result;

    result = os.queryOrder(3007847, null);
    assertEquals(3007847, result.orderId());
    assertEquals(true, result.get("done").valueAsBoolean());
    assertEquals(117, result.get("amount").valueAsLong());
    assertEquals("椰子节一路工程授权如何苏子河纯利润，奎松离别剑打扮网上开店慌张四",
        result.get("remark").valueAsString());
    assertEquals(8380.42, result.get("app_order_3334_0").valueAsDouble(), 1e-6);

    assertEquals("一些", result.get("good_name").valueAsString());
    assertEquals(42.9, result.get("price").valueAsDouble(), 1e-6);
    assertEquals(null, result.get("ggg"));
    assertEquals("tm_758d7a5f-c254-4bb8-9587-d211a4327814",
        result.get("salerid").valueAsString());

    assertEquals("376 55715168", result.get("contactphone").valueAsString());
    assertEquals("三寸不烂之舌", result.get("buyername").valueAsString());

    result = os.queryOrder(2982725, Arrays.asList("amount", "hehe", "offprice"));
    assertEquals(2982725, result.orderId());
    assertEquals(220, result.get("amount").valueAsLong());
    assertEquals(null, result.get("hehe"));
    assertEquals(null, result.get("buyerid"));
    assertEquals(null, result.get("yyyy"));

    assertEquals(6.71, result.get("offprice").valueAsDouble(), 1e-6);

    result = os.queryOrder(12345, null);
    assertEquals(null, result);

    Iterator<OrderSystem.Result> iter = os.queryOrdersByBuyer(1408867965, 1508867965,
        "ap_855a4497-5614-401f-97be-45a6c6e8936c");
    int count = 0;
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(33, count);

    iter = os.queryOrdersByBuyer(1466441697, 1470031858,
        "tb_7a1f9032-e715-4c84-abaa-2405e7579408");
//    iter = os.queryOrdersByBuyer(1406441697, 1490031858,
//        "tb_7a1f9032-e715-4c84-abaa-2405e7579408");
    count = 0;
    while (iter.hasNext()) {
      result = iter.next();
      //System.out.println(result.orderId() + " " + result.get("createtime").valueAsLong());
      count++;
    }
    //assertEquals(21, count);

    iter = os.queryOrdersBySaler("", "good_e3111b68-638b-4a5b-89ef-15f522171b9f", null);
    count = 0;
    while (iter.hasNext()) {
      result = iter.next();
      //System.out.println(result.orderId());
      count++;
      if (count == 1) {
        assertEquals(2982453, result.orderId());
        assertEquals(4.21, result.get("offprice").valueAsDouble(), 1e-6);
      }
      if (count == 22) {
        assertEquals(3009294, result.orderId());
        assertEquals("云集", result.get("buyername").valueAsString());
      }
    }
    assertEquals(22, count);

    OrderSystem.KeyValue kv = os.sumOrdersByGood(
        "aliyun_6371c5b3-29e0-48f1-9e1f-602b034122a6", "amount");
    assertEquals(8494, kv.valueAsLong());
    assertEquals(8494, kv.valueAsDouble(), 1e-6);

    kv = os.sumOrdersByGood(
        "aliyun_6371c5b3-29e0-48f1-9e1f-602b034122a6", "price");
    assertEquals(398.08, kv.valueAsDouble(), 1e-6);
  }

  @Test
  public void testSmall() throws Exception {
    OrderSystem os = new OrderSystemImpl();
    os.construct(Arrays.asList("rec.txt"), null, null, null);
    os.queryOrder(345, null);
    os.queryOrder(199, null);
  }

  @Test
  public void util() {
    assertEquals(Long.MAX_VALUE, Util.byte2long(Util.long2byte(Long.MAX_VALUE)));
    assertEquals(Long.MIN_VALUE, Util.byte2long(Util.long2byte(Long.MIN_VALUE)));
    assertEquals(-1, Util.byte2long(Util.long2byte(-1)));
    assertEquals(0, Util.byte2long(Util.long2byte(0)));

    assertEquals(Integer.MAX_VALUE, Util.byte2int(Util.int2byte(Integer.MAX_VALUE)));
    assertEquals(Integer.MIN_VALUE, Util.byte2int(Util.int2byte(Integer.MIN_VALUE)));
    assertEquals(-1, Util.byte2int(Util.int2byte(-1)));
    assertEquals(0, Util.byte2int(Util.int2byte(0)));

    assertEquals(Short.MAX_VALUE, Util.byte2short(Util.short2byte(Short.MAX_VALUE)));
    assertEquals(65535, Util.byte2short(Util.short2byte(-1)));
    assertEquals(0, Util.byte2short(Util.short2byte(0)));
  }

  @Test
  public void move() {
    int a, b;
    long t1, t2;

    t1 = System.currentTimeMillis();
    a = 98739287;
    for (int i = 0; i < 1000000000; i++) {
      a >>= 10;
    }
    System.out.println(a);
    t2 = System.currentTimeMillis();
    System.out.println(t2 - t1);

    t1 = System.currentTimeMillis();
    a = 98739287;
    for (int i = 0; i < 1000000000; i++) {
      a >>>= 10;
    }
    System.out.println(a);
    t2 = System.currentTimeMillis();
    System.out.println(t2 - t1);
  }

  @Test
  public void t() {
    System.out.println((long)0xfffffff << 6);
    System.out.println((int) (0xfffffffffL >> 4));
    byte[][] bufs = new byte[10][];
    System.out.println(bufs.length);
    System.out.println(bufs[0].length);
  }

  @Test
  public void fs() {
    try {
      RandomAccessFile f = new RandomAccessFile("test", "rw");
      f.setLength(400000000);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void disk() {
    try {
      RandomAccessFile f = new RandomAccessFile("test", "rw");
      byte[] buf = new byte[8000];

      Arrays.fill(buf, (byte) 5);
      buf[3] = 99;

      long t0, t1, ts = 0;

      int sum = 0, step = 4000;
      for (long i = 0; i < 16000000000L; i += step) {
        t0 = System.currentTimeMillis();
        for (int j = 0; j < step; j++)
          buf[j] = (byte) (Math.random()*1000);
        f.seek(i + 1400);
        f.write(buf, 0, 300);
        sum += buf[5];
        t1 = System.currentTimeMillis();
        ts += t1 - t0;
        if (i % 8000000 == 0) {
          System.out.println(ts + " ");
          ts = 0;
        }
      }
      System.out.println(sum);

      f.seek(50000);
      System.out.println(f.read());

//      System.out.println(System.currentTimeMillis());
//      f.seek(500);
//      f.write(buf);
//      f.seek(500000);
//      f.read();
//      System.out.println(System.currentTimeMillis());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void col() {
    for (String s : getCol())
      s.length();
  }

  private Collection<String> getCol() {
    System.out.println(1);
    return Arrays.asList("aa", "bb");
  }

  @Test
  public void clhm() {
    EvictionListener<Integer, Integer> listener = new EvictionListener<Integer, Integer>() {
      @Override
      public void onEviction(Integer key, Integer value) {
        System.out.println(key + " " + value);
      }
    };

    ConcurrentLinkedHashMap<Integer, Integer> cache =
        new ConcurrentLinkedHashMap.Builder<Integer, Integer>()
            .maximumWeightedCapacity(5)
            .listener(listener)
            .build();
    for (int i = 0; i < 20; i++)
      cache.put(i, i);
    cache.get(17);
    cache.put(3, 3);

    for (int key : cache.ascendingKeySet())
      System.out.println(key);
  }

  @Test
  public void fill() {
    short[] b = new short[2000000];
    Arrays.fill(b, (short)8);
  }
}

package com.alibaba.middleware.race;

import com.alibaba.middleware.race.cache.ConcurrentCache;
import com.alibaba.middleware.race.index.BgIndex;
import com.alibaba.middleware.race.index.OrderIndex;
import com.alibaba.middleware.race.kvDealer.BuyerKvDealer;
import com.alibaba.middleware.race.kvDealer.GoodKvDealer;
import com.alibaba.middleware.race.kvDealer.IKvDealer;
import com.alibaba.middleware.race.kvDealer.OrderKvDealer;
import com.alibaba.middleware.race.result.BuyerResult;
import com.alibaba.middleware.race.result.GoodResult;
import com.alibaba.middleware.race.result.SimpleResult;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by yfy on 7/13/16.
 * Database
 */
public class Database implements Runnable {

  private List<String> orderFilesList, goodFilesList, buyerFilesList,
      storeFoldersList;

  private static BuyerResultComparator buyerResultComparator;

  private static TupleOrderidComparator tupleOrderidComparator;

  private OrderIndex orderIndex;

  public static BgIndex buyerIndex, goodIndex;

  public Database(Collection<String> orderFiles,
                  Collection<String> buyerFiles,
                  Collection<String> goodFiles,
                  Collection<String> storeFolders) throws Exception {

    buyerResultComparator = new BuyerResultComparator();
    tupleOrderidComparator = new TupleOrderidComparator();

    ConcurrentCache cache = ConcurrentCache.getInstance();

    orderFilesList = new ArrayList<>();
    for (String file : orderFiles) {
      orderFilesList.add(file);
      cache.addFd(file, true);
    }

    goodFilesList = new ArrayList<>();
    for (String file : goodFiles) {
      goodFilesList.add(file);
      cache.addFd(file, true);
    }

    buyerFilesList = new ArrayList<>();
    for (String file : buyerFiles) {
      buyerFilesList.add(file);
      cache.addFd(file, true);
    }

    storeFoldersList = new ArrayList<>();
    for (String folder : storeFolders)
      storeFoldersList.add(folder);
  }

  public void construct() throws Exception {
    buildOrder2OrderHash();
    buildGood2GoodHash();
    buildBuyer2BuyerHash();

    System.out.println("[yfy] buyer num: " + BuyerKvDealer.count);
    System.out.println("[yfy] good num: " + GoodKvDealer.count);
    System.out.flush();
  }

//  public void construct() throws Exception {
//    Thread.sleep(3595000);
//    new Thread(this).start();
//  }

  // construct in another thread
  @Override
  public void run() {
    try {
      buildO2oHash();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void buildO2oHash() throws Exception {
    WriteBuffer writeBuffer = new WriteBuffer(Config.orderIndexSize,
        Config.orderIndexBlockSize, 1500);
    Thread thread = new Thread(writeBuffer);
    orderIndex = new OrderIndex(orderFilesList, fullname0("order.idx"),
        writeBuffer);
    thread.start();
    OrderKvDealer dealer = new OrderKvDealer(orderIndex, buyerIndex, goodIndex);
    for (int i = 0; i < orderFilesList.size(); i++) {
      dealer.setFileId(i);
      readDataFile(orderFilesList.get(i), dealer);
    }
    writeBuffer.finish();
    thread.join();
  }

  private void buildB2oHash() throws Exception {

  }

  private void buildG2oHash() throws Exception {

  }

  private void buildOrder2OrderHash() throws Exception {
    WriteBuffer writeBuffer0 = new WriteBuffer(Config.orderIndexSize,
        Config.orderIndexBlockSize, Config.orderIndexBlockBufSize);
//    WriteBuffer writeBuffer1 = new WriteBuffer(Config.buyerIndexSize,
//        Config.buyerIndexBlockSize, Config.buyerIndexBlockBufSize);
//    WriteBuffer writeBuffer2 = new WriteBuffer(Config.goodIndexSize,
//        Config.goodIndexBlockSize, Config.goodIndexBlockBufSize);
    WriteBuffer writeBuffer1 = new WriteBuffer(1, 1, 1);
    WriteBuffer writeBuffer2 = new WriteBuffer(1, 1, 1);
    Thread thread0 = new Thread(writeBuffer0);
    Thread thread1 = new Thread(writeBuffer1);
    Thread thread2 = new Thread(writeBuffer2);
    orderIndex = new OrderIndex(orderFilesList, fullname0("order.idx"),
        writeBuffer0);
    buyerIndex = new BgIndex(orderFilesList, fullname1("b2o.idx"),
        buyerFilesList, Config.buyerIndexSize, Config.buyerIndexBlockSize,
        writeBuffer1);
    goodIndex = new BgIndex(orderFilesList, fullname2("g2o.idx"),
        goodFilesList, Config.goodIndexSize, Config.goodIndexBlockSize,
        writeBuffer2);
    thread0.start();
    //thread1.start();
    //thread2.start();

    System.out.println(System.currentTimeMillis());
    OrderKvDealer dealer = new OrderKvDealer(orderIndex, buyerIndex, goodIndex);
    for (int i = 0; i < orderFilesList.size(); i++) {
      dealer.setFileId(i);
      readDataFile(orderFilesList.get(i), dealer);
    }
    System.out.println(System.currentTimeMillis());
    System.out.println("[yfy] order num: " + OrderKvDealer.count);
    System.out.println("[yfy] orderid max: " + OrderKvDealer.maxOid +
        " min: " + OrderKvDealer.minOid);

    writeBuffer0.finish();
    writeBuffer1.finish();
    writeBuffer2.finish();
    thread0.join();
    thread1.join();
    thread2.join();
  }

  private void buildGood2GoodHash() throws Exception {
    GoodKvDealer dealer = new GoodKvDealer(goodIndex);
    for (int i = 0; i < goodFilesList.size(); i++) {
      dealer.setFileId(i);
      readDataFile(goodFilesList.get(i), dealer);
    }
  }

  private void buildBuyer2BuyerHash() throws Exception {
    BuyerKvDealer dealer = new BuyerKvDealer(buyerIndex);
    for (int i = 0; i < buyerFilesList.size(); i++) {
      dealer.setFileId(i);
      readDataFile(buyerFilesList.get(i), dealer);
    }
  }

  private String fullname0(String filename) {
    return storeFoldersList.get(0) + '/' + filename;
  }

  private String fullname1(String filename) {
    return storeFoldersList.get(1) + '/' + filename;
  }

  private String fullname2(String filename) {
    return storeFoldersList.get(2) + '/' + filename;
  }

  private void readAio(String filename) throws Exception {
    final int SIZE = 50;
    Path path = Paths.get(filename);
    final AsynchronousFileChannel channel = AsynchronousFileChannel.open(path);
    final ByteBuffer buffer0 = ByteBuffer.allocate(SIZE);
    final ByteBuffer buffer1 = ByteBuffer.allocate(SIZE);
    CompletionHandler<Integer, Object> handler =
        new CompletionHandler<Integer, Object>() {
          public int offset;

          @Override
          public void completed(Integer result, Object att) {
            System.out.println(new String(buffer0.array()));
            offset += SIZE;
            System.out.println(offset);
            buffer0.clear();
            channel.read(buffer0, offset, null, this);
          }

          @Override
          public void failed(Throwable exc, Object att) {
            System.out.println("fail");
          }
        };
    channel.read(buffer0, 0, null, handler);

    System.out.println("other");
    Thread.sleep(3000);
  }

  private void readDataFile(String filename, IKvDealer dealer)
      throws Exception {

    System.out.println("[yfy] filename: " + filename +
        " size: " + new File(filename).length());
    ReadBuffer readBuffer = new ReadBuffer(filename);
    new Thread(readBuffer).start();
    readBuffer.getBuf();

    int b, keyLen = 0, valueLen = 0;
    long offset = 0, count = 0;
    // 0 for read key, 1 for read value, 2 for skip line
    int status = 0;
    byte[] key = new byte[256];
    byte[] value = new byte[105536];

    while ((b = readBuffer.read()) != -1) {
      //System.out.print((char)b);
      count++;
      if (status == 0) {
        if (b == ':') {
          valueLen = 0;
          status = 1;
        } else {
          key[keyLen++] = (byte) b;
        }
      } else if (status == 1) {
        if (b == '\t') {
          int code = dealer.deal(key, keyLen, value, valueLen, offset);
          if (code == 2)
            status = 2;
          else
            status = keyLen = 0;
        } else if (b == '\n') {
          dealer.deal(key, keyLen, value, valueLen, offset);
          offset = count;
          status = keyLen = 0;
        } else {
          value[valueLen++] = (byte) b;
        }
      } else { // status == 2
        if (b == '\n') {
          offset = count;
          status = keyLen = 0;
        }
      }
    }
    dealer.deal(key, keyLen, value, valueLen, offset);
  }

  public ResultImpl queryOrder(long orderId, Collection<String> keys)
      throws Exception {

    if (orderId > Config.orderidMax)
      return null;
    Tuple orderTuple = orderIndex.get(Util.long2byte5(orderId));
    if (orderTuple == null)
      return null;
    ResultImpl result = new ResultImpl(orderTuple, keys);
    //result.printOrderTuple();
    return result;
  }

  public Iterator<OrderSystem.Result> queryOrdersByBuyer(
      long startTime, long endTime, String buyerid) throws Exception {

    //TupleFilter filter = new TupleFilter(startTime, endTime);
    List<Tuple> orderTupleList = buyerIndex.getOrder(buyerid);
    if (orderTupleList.isEmpty())
      return new ArrayList<OrderSystem.Result>().iterator();

    Tuple buyerTuple = buyerIndex.getBg(buyerid);
    SimpleResult buyerResult = new SimpleResult(buyerTuple, null);

    List<OrderSystem.Result> resultList =
        new ArrayList<>(orderTupleList.size());
    for (Tuple tuple : orderTupleList)
      resultList.add(new BuyerResult(tuple, buyerResult));
    Collections.sort(resultList, buyerResultComparator);

    return resultList.iterator();
  }

  public Iterator<OrderSystem.Result> queryOrdersBySaler(
      String goodid, Collection<String> keys) throws Exception {

    List<Tuple> tupleList = goodIndex.getOrder(goodid);
    if (tupleList.isEmpty())
      return new ArrayList<OrderSystem.Result>().iterator();

    Tuple goodTuple = goodIndex.getBg(goodid);
    SimpleResult goodResult = new SimpleResult(goodTuple, keys);

    Collections.sort(tupleList, tupleOrderidComparator);
    List<OrderSystem.Result> resultList = new ArrayList<>();
    for (Tuple tuple : tupleList)
      resultList.add(new GoodResult(tuple, goodResult, keys));
    return resultList.iterator();
  }

  public OrderSystem.KeyValue sumOrdersByGood(
      String goodid, String key) throws Exception {

    boolean asLong = true, asDouble = true, hasKey = false;
    long sumLong = 0;
    double sumDouble = 0;

    Collection<String> keys = Collections.singleton(key);
    List<Tuple> orderTupleList = goodIndex.getOrder(goodid);

    Tuple goodTuple = goodIndex.getBg(goodid);
    SimpleResult goodResult = new SimpleResult(goodTuple, keys);
    OrderSystem.KeyValue kv = goodResult.get(key);
    if (kv != null) {
      long vl = 0;
      double vd = 0;
      try {
        vl = kv.valueAsLong();
      } catch (Exception e) {
        asLong = false;
      }
      try {
        vd = kv.valueAsDouble();
      } catch (Exception e) {
        asDouble = false;
      }
      if (!asLong && !asDouble)
        return null;
      int size = orderTupleList.size();
      return new KeyValueForSum(key, vl * size, vd * size);
    }

    for (Tuple tuple : orderTupleList) {
      long valueLong = 0;
      double valueDouble = 0;
      kv = new ResultImpl(tuple, keys).get(key);

      if (kv == null)
        continue;
      else
        hasKey = true;

      if (asLong) {
        try {
          valueLong = kv.valueAsLong();
          sumLong += valueLong;
        } catch (OrderSystem.TypeException e) {
          asLong = false;
        }
      }

      if (asDouble) {
        try {
          valueDouble = kv.valueAsDouble();
          sumDouble += valueDouble;
        } catch (OrderSystem.TypeException e) {
          asDouble = false;
        }
      }

      if (!asLong && !asDouble)
        return null;
    }

    if (!hasKey)
      return null;

    return new KeyValueForSum(key, sumLong, sumDouble);
  }

  //  private void print(byte[] key, int keyLen, byte[] value, int valueLen) {
//    for (int i = 0; i < keyLen; i++)
//      System.out.print((char) key[i]);
//    System.out.print(':');
//    for (int i = 0; i < valueLen; i++)
//      System.out.print((char) value[i]);
//    System.out.print('\t');
//  }

//  private void readOrderFile(String filename, int fileId) throws Exception {
//    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename));
//    int b, keyLen = 0, valueLen = 0;
//    long offset = 0, count = 0;
//    // 0 for read key, 1 for read value, 2 for skip line
//    int status = 0;
//    byte[] key = new byte[256];
//    byte[] value = new byte[65536];
//    while ((b = bis.read()) != -1) {
//      count++;
//      if (status == 0) {
//        if (b == ':') {
//          valueLen = 0;
//          status = 1;
//        } else {
//          key[keyLen++] = (byte) b;
//        }
//      } else if (status == 1) {
//        if (b == '\t') {
//          //print(key, keyLen, value, valueLen);
//
//          if (expectedKey(key, keyLen, orderidBytes)) {
//            long valueLong = Long.parseLong(new String(value, 0, valueLen));
//            orderHashTable.add(valueLong, fileId, offset);
//            //orderHashTable.get(Util.long2byte(valueLong));
//            status = 2;
//          } else {
//            keyLen = 0;
//            status = 0;
//          }
//        } else if (b == '\n') {
//          //print(key, keyLen, value, valueLen);
//          //System.out.println("offset: " + offset);
//
//          if (expectedKey(key, keyLen, orderidBytes)) {
//            long valueLong = Long.decode(new String(value, 0, valueLen));
//            orderHashTable.add(valueLong, fileId, offset);
//            //orderHashTable.get(Util.long2byte(valueLong));
//          }
//
//          offset = count;
//          keyLen = 0;
//          status = 0;
//        } else {
//          value[valueLen++] = (byte) b;
//        }
//      } else {
//        if (b == '\n') {
//          //System.out.println("offset: " + offset);
//
//          offset = count;
//          keyLen = 0;
//          status = 0;
//        }
//      }
//    }
//  }

//  public void readOrderFile2(String filename, int fileId) throws IOException {
//    orderHashTable = new HashTable("order.hash");
//
//    BufferedReader br = new BufferedReader(new FileReader(filename));
//    String line;
//    long fileOff = 0;  // offset in file
//    while ((line = br.readLine()) != null) {
//      int lineOff = 0, sepPos, tabPos = 0;
//      while (tabPos != -1) {
//        sepPos = line.indexOf(':', lineOff);  // pos of :
//        String key = line.substring(lineOff, sepPos);
//        tabPos = line.indexOf('\t', lineOff);  // pos of tab
//        String value;
//        if (tabPos == -1)
//          value = line.substring(sepPos + 1);
//        else
//          value = line.substring(sepPos + 1, tabPos);
//
//        System.out.println(key + ":" + value);
//
////        if (key.equals("orderid"))
////          orderHashTable.add(value, fileId, fileOff);
//
//        lineOff = tabPos + 1;
//      }
//      fileOff += line.length();
//
//      System.out.println(fileOff);
//    }
//  }

}

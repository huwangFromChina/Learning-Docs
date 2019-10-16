import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.socket.DatagramPacket;
import javafx.util.Pair;
import model.MetricPackage;
import util.CRCUtil;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;


/**
 * @author Gavin
 * 2019/8/29 11:10
 */
public class ServerNetProtocol {

    private BlockingDeque<DatagramPacket> rcvQueue = new LinkedBlockingDeque<>(); //接收到的数据分片
    private BlockingDeque<Pair<ByteBuf, InetSocketAddress>> ackQueue = new LinkedBlockingDeque<>(); //需要发送的ack数据包
    private static final int PACKAGE_OVERHEAD = 40; //报头字节数
    private static final long TIME_OUT = 60_000L; //超时时间
    private static final int PACKAGE_MTU = 24;//每个分片的数据部分的字节数
    private Map<String, MetricPackage> packageMap = new HashMap<>();
    private MetricServer server;

    public ServerNetProtocol(MetricServer server) {
        this.server = server;
        new Thread(this::receive, "onReceive").start();
        new Thread(this::sendAck, "onSendAck").start();
    }

    public void addData(DatagramPacket datagramPacket) {
        rcvQueue.add(datagramPacket);
    }

    private void receive() {
        while (true) {
            try {
                DatagramPacket datagramPacket = rcvQueue.poll(30, TimeUnit.SECONDS);
                if (datagramPacket == null) {
                    flushReceivePackages();
                    continue;
                }
                ByteBuf byteBuf = datagramPacket.content();
                if (byteBuf.readableBytes() < PACKAGE_OVERHEAD || byteBuf.readableBytes() > PACKAGE_MTU + PACKAGE_OVERHEAD)
                    continue;
                String uniqueCode = byteBuf.readCharSequence(32, StandardCharsets.UTF_8).toString();
                int currentSegment = byteBuf.readInt();
                int segmentNumbers = byteBuf.readInt();
                byte[] bytes = new byte[byteBuf.readableBytes()];
                byteBuf.getBytes(PACKAGE_OVERHEAD, bytes);
                System.out.println("接收到数据分片：" + uniqueCode + "," + currentSegment + "," + segmentNumbers);
                ByteBuf ack = PooledByteBufAllocator.DEFAULT.buffer(20);
                if (!packageMap.containsKey(uniqueCode)) {
                    if (currentSegment != 0) {
                        byteBuf.release();
                        datagramPacket.release();
                        ack.release();
                        continue;  //超时的数据分片，丢掉
                    }
                    MetricPackage metricPackage = new MetricPackage();
                    metricPackage.setActTime(System.currentTimeMillis());
                    metricPackage.setCurrentSegment(0);
                    metricPackage.setUniqueCode(uniqueCode);
                    metricPackage.setSegmentNumbers(segmentNumbers);
                    metricPackage.addRcvList(bytes);

                    //初始化ack ByteBuf
                    ack.writeBytes(uniqueCode.getBytes());
                    ack.writeInt(currentSegment);
                    ackQueue.add(new Pair<>(ack, datagramPacket.sender()));
                    packageMap.put(uniqueCode, metricPackage);
//                    datagramPacket.release();
                } else {
                    MetricPackage metricPackage = packageMap.get(uniqueCode);
                    if (currentSegment < metricPackage.getCurrentSegment()) {
                        byteBuf.release();
//                        datagramPacket.release();
                        continue;//过时的数据分片（segment丢失）
                    } else if (currentSegment == metricPackage.getCurrentSegment()) {
                        //初始化ack ByteBuf
                        ack.writeBytes(uniqueCode.getBytes());
                        ack.writeInt(currentSegment);
                        ackQueue.add(new Pair<>(ack, datagramPacket.sender()));

                        byteBuf.release();
//                        datagramPacket.release();
                        continue;//重复的数据分片（ack丢失）
                    }
                    metricPackage.setCurrentSegment(currentSegment);
                    metricPackage.addRcvList(bytes);
                    metricPackage.updateActTime();

                    //初始化ack ByteBuf
                    ack.writeBytes(uniqueCode.getBytes());
                    ack.writeInt(currentSegment);
                    ackQueue.add(new Pair<>(ack, datagramPacket.sender()));

//                    datagramPacket.release();
                }
                flushReceivePackages();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void flushReceivePackages() {
        Iterator<Map.Entry<String, MetricPackage>> iterator = packageMap.entrySet().iterator();
        while (iterator.hasNext()) {
            try {
                MetricPackage value = iterator.next().getValue();
                //数据分片已全部接收
                if (value.getCurrentSegment() == value.getSegmentNumbers() - 1) {
                    System.out.println("****result: " + CRCUtil.byteListToString(value.getRcvList()));
                    //接收完毕的数据将在内存中存放TIME_OUT时间，防止数据重发重写
                    if (System.currentTimeMillis() - value.getActTime() > TIME_OUT)
                        iterator.remove();
                }
                //数据超时，丢弃这条数据
                if (System.currentTimeMillis() - value.getActTime() > TIME_OUT)
                    iterator.remove();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendAck() {
        while (true) {
            try {
                Pair<ByteBuf, InetSocketAddress> pair = ackQueue.take();
                server.output(pair.getKey(), pair.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

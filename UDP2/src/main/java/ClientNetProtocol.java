import util.CRCUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gavin
 * 2019/8/29 14:49
 */
public class ClientNetProtocol {
    private final BlockingDeque<String> sendQueue = new LinkedBlockingDeque<>(); //需要发送的数据
    private final Object lock = new Object();
    private DatagramSocket socket;
    private InetSocketAddress address;
    private final static int retryTimes = 5; //数据重发次数
    private final static int MTU = 24;
    private final static long TIME_OUT = 10_000L; //超时时间
    private StringBuffer currentMsg = new StringBuffer();
    private AtomicInteger currentSegment = new AtomicInteger(-1);

    public ClientNetProtocol(int port) throws Exception {
        socket = new DatagramSocket(port);
        address = new InetSocketAddress("127.0.0.1", 10010);
        new Thread(this::send, "onSend").start();
        new Thread(this::receive, "onReceive").start();
    }

    public void addMessage(String value) {
        sendQueue.add(value);
    }

    private void send() {
        while (true) {
            try {
                String value = sendQueue.take();
                String md5 = CRCUtil.MD5(value); //计算MD5
                List<byte[]> bytes = CRCUtil.encode(value.getBytes(StandardCharsets.UTF_8), MTU, md5.getBytes()); //切割byte数组，封装报头
                System.out.println("切割成" + bytes.size() + "个分片");
                currentMsg = new StringBuffer(md5);
                int sendTime = 0;
                for (int i = 0; i < bytes.size(); ) {
                    synchronized (lock) {
                        if (sendTime >= retryTimes)
                            break; //超过重发次数限制，跳过此条数据
                        byte[] aByte = bytes.get(i);
                        DatagramPacket packet = new DatagramPacket(aByte, aByte.length, address);
                        socket.send(packet);
                        System.out.println("发送数据分片：" + CRCUtil.byteArrayToString(aByte, 0, 32) + ","
                                + CRCUtil.byteArrayToInt(aByte, 32, 4) + ","
                                + CRCUtil.byteArrayToInt(aByte, 36, 4));
                        currentSegment.set(i);
                        try {
                            //发送数据后阻塞此线程，直到超时时间或者收到ack确认包
                            lock.wait(TIME_OUT);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        //超时的话重发，收到ACK的话发送下一条报文
                        if (currentSegment.get() == i + 1) {
                            i++;
                            System.out.println("发送下一条数据");
                            sendTime = 0;
                        } else {
                            System.out.println("重发当前这条数据");
                            sendTime++;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void receive() {
        while (true) {
            try {
                byte[] data = new byte[36];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                String md5 = CRCUtil.byteArrayToString(data, 0, 32);
                int segment = CRCUtil.byteArrayToInt(data, 32, 4);
                System.out.println("接收到ack:" + md5 + "," + segment);
                if (md5.equals(currentMsg.toString()) && segment == currentSegment.get()) {
                    synchronized (lock) {
                        currentSegment.addAndGet(1);
//                        System.out.println("唤醒send thread");
                        lock.notify();
                    }
                } else
                    System.out.println("收到错误的ack包");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

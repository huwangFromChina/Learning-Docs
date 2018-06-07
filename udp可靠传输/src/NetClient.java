import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class NetClient {

    public static void main(String args[]) {
        new NetClient().startToWork("DingTalk_v4.2.8.38.exe");
    }

    private FileOperator fileOperator = new FileOperator();
    private DatagramSocket socket;
    private InetAddress ia;
    private int version = 0;

    public File startToWork(String fileName) {
        try {
            socket = new DatagramSocket(10001);
            ia = InetAddress.getByName("localhost");
        } catch (Exception e) {
            System.out.println("client init error:" + e);
        }

        //申请发送文件数据
        sendRequest(socket, ia, fileName);

        //获取文件信息
        int fileLength = getFileMess(socket, ia, fileName);
        int size = (fileLength / (1024 * 10)) + 1;

        //发送确认包
        sendFileMessAck(socket, fileName, ia);

        //开始接收文件数据
        int[] datas = new int[size];
        byte[][] bs = new byte[size][1024 * 10];
        System.out.println("client start to get File");
        while (getFile(socket, datas, bs)) {
        }

        //申请重发文件数据
        while (requestResend(size, datas, fileName, socket, ia)) {
            System.out.println("client start to get resend file");
            while (getFile(socket, datas, bs)) {
            }
        }

        //拼接文件
        System.out.println("拼接文件");
        File file = fileOperator.mergeByteToFile(bs, fileName,fileLength);
        socket.close();
        return file;
    }

    public boolean requestResend(int size, int[] datas, String fileName, DatagramSocket socket, InetAddress ia) {
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < size; i++) {
            if (datas[i] == 0) {
                sb.append(i + ".");
            }
        }
        if (sb.toString().equals("")) return false;
        sb.delete(sb.length() - 1, sb.length());
        try {
            String requestMess = "RESEND:" + fileName + ":" + sb.toString() + ":" + version + ":";
            System.out.println("client ask resend request:" + requestMess);
            byte[] bytes = requestMess.getBytes();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, ia, 10000);
            socket.send(packet);
            version++;
        } catch (IOException e) {
            System.out.println("client ask resend request error:" + e.getMessage());
            requestResend(size, datas, fileName, socket, ia);
        }
        return true;
    }

    public boolean getFile(DatagramSocket socket, int[] datas, byte[][] bs) {
        try {
            socket.setSoTimeout(1000 * 10);
            byte[] bytes = new byte[1024 * 11];
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            socket.receive(packet);
            byte[] file = new byte[1024 * 10];
            for (int i = 0; i < 1024 * 10; i++) file[i] = bytes[i];
            int position = bytesToInt(bytes, 1024 * 10);
            datas[position] = 1;
            bs[position] = file;
            return true;
        } catch (SocketTimeoutException e) { //超过10s未收取到数据视为数据传输完毕
            System.out.println("client get file timeout:" + e.getMessage());
            return false;
        } catch (IOException e) {
            System.out.println("client get file error:" + e.getMessage());
            return true;
        }
    }

    public void sendFileMessAck(DatagramSocket socket, String fileName, InetAddress ia) {
        try {
            String ack = "ACK:FILEMESS:" + fileName + ":blank";
            System.out.println("client send fileMess Ack:" + ack);
            byte[] bytes = ack.getBytes();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, ia, 10000);
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("client send fileMess Ack error:" + e.getMessage());
            sendFileMessAck(socket, fileName, ia);
        }
    }

    public void sendRequest(DatagramSocket socket, InetAddress ia, String fileName) {
        try {
            String requestMess = "ALL:" + fileName + ":";
            System.out.println("client send request:" + requestMess);
            byte[] bytes = requestMess.getBytes();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, ia, 10000);
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("client send request error:" + e.getMessage());
            sendRequest(socket, ia, fileName);
        }
    }

    public int getFileMess(DatagramSocket socket, InetAddress ia, String fileName) {
        int nums;
        try {
            byte[] receives = new byte[50];
            DatagramPacket receivePacket = new DatagramPacket(receives, receives.length);
            socket.setSoTimeout(1000);
            socket.receive(receivePacket);
            byte[] receiveFileName = new byte[46];
            nums = bytesToInt(receives, 0);
            for (int i = 4; i < 50; i++) receiveFileName[i - 4] = receives[i];
            System.out.println("client receive File Message:" + new String(receiveFileName) + "size:" + nums);
        } catch (SocketTimeoutException e) {
            System.out.println("client get filemess timeout error:" + e.getMessage());
            sendRequest(socket, ia, fileName);
            return getFileMess(socket, ia, fileName);
        } catch (IOException e) {
            System.out.println("client get filemess io error:" + e.getMessage());
            return getFileMess(socket, ia, fileName);
        }
        return nums;
    }

    public int bytesToInt(byte[] src, int offset) {
        int value;
        value = ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24));
        return value;
    }

}

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public class NetServer {

    public static void main(String args[]) {
        new NetServer().startToWork();
    }

    private InetAddress inetAddress;
    private DatagramSocket socket;
    private boolean isContinue = true;
    private Integer lastRequestVersion = null;

    public void startToWork() {
        try {
            socket = new DatagramSocket(10000);
            inetAddress = InetAddress.getByName("localhost");
        } catch (Exception e) {
            System.out.println("server exception:" + e.getMessage());
        }
        byte[] request = new byte[1024 * 30];
        DatagramPacket requestPacket = new DatagramPacket(request, request.length);
        try {
            socket.setSoTimeout(1000 * 60 * 10);
            while (isContinue) {
                System.out.println("等待接收指令");
                socket.receive(requestPacket);
                String mess = new String(request);
                System.out.println("server receive command:" + mess);
                String[] directives = mess.split(":");
                switch (directives[0]) {
                    case "ALL":  //申请发送文件
                        sendFileMess(socket, directives[1]);  //把文件的基本信息（长度，文件名）发送到客户端
                        break;
                    case "RESEND":  //申请重发文件
                        sendFile(socket, directives[1], directives[2], directives[3]);    //将客户端要求重发的数据包发送到客户端
                        break;
                    case "ACK":       //确认数据包
                        if (directives[1].equals("FILEMESS"))
                            sendAllFile(socket, directives[2]);  //接收到客户端的确认报文，开始发送文件数据包
                        break;
                    default:
                        break;
                }
            }
        } catch (SocketTimeoutException e) {
            System.out.println("超过10min没有接收到数据，断开连接");
            socket.close();
        } catch (SocketException e) {
            System.out.println("udp error");
            socket.close();
        } catch (IOException e) {
            System.out.println("io Exception");
            socket.close();
        }
    }

    public void sendFileMess(DatagramSocket socket, String fileName) {
        try {
            System.out.println("server sendFileMess");
            File file = new File(FileOperator.FILEPATH + fileName);
            int fileLength = (int) file.length();
            byte[] bytes = intToBytes(fileLength);
            byte[] fileNameBytes = fileName.getBytes();
            byte[] response = new byte[bytes.length + fileNameBytes.length];
            System.arraycopy(bytes, 0, response, 0, bytes.length);
            System.arraycopy(fileNameBytes, 0, response, bytes.length, fileNameBytes.length);
            DatagramPacket responsePacket = new DatagramPacket(response, response.length, inetAddress, 10001);
            socket.send(responsePacket);
        } catch (IOException e) {
            System.out.println("send fileMess io Exception:" + e);
            sendFileMess(socket, fileName);
        }
    }

    public void sendAllFile(DatagramSocket socket, String fileName) {
        try {
            System.out.println("server sendAllFile");
            File file = new File(FileOperator.FILEPATH + fileName);
            InputStream inputStream = new FileInputStream(file);
            for (int i = 0; i < file.length() / (1024 * 10) + 1; i++) {
                byte[] content = new byte[1024 * 10];
                int len;
                if ((len = inputStream.read(content)) != -1) {
                    byte[] head = intToBytes(i);
                    byte[] response = new byte[content.length + head.length];
                    System.arraycopy(content, 0, response, 0, content.length);
                    System.arraycopy(head, 0, response, content.length, head.length);
                    DatagramPacket responsePacket = new DatagramPacket(response, response.length, inetAddress, 10001);
                    socket.send(responsePacket);
                }
            }
        } catch (IOException e) {
            System.out.println("sendAllFile io Exception：" + e);
        }
    }

    public void sendFile(DatagramSocket socket, String fileName, String asks, String version) {
        if (lastRequestVersion != null && Integer.parseInt(version) <= lastRequestVersion) {
            System.out.println("ignore resend file request");
            return;
        }
        try {
            System.out.println("server resend file,fileName:" + fileName + ",parts:" + asks + ",version:" + version);
            File file = new File(FileOperator.FILEPATH + fileName);
            InputStream inputStream = new FileInputStream(file);
            String[] parts = asks.split("\\.");
            System.out.println("length:" + (file.length() / (1024 * 10)) + ",parts:" + parts.length + ",asks:" + asks);
            int j = 0;
            for (int i = 0; i < (file.length() / (1024 * 10) + 1); i++) {
                byte[] content = new byte[1024 * 10];
                int len;
                if ((len = inputStream.read(content)) != -1) {
                    if (j < parts.length && Integer.parseInt(parts[j]) == i) {
                        byte[] head = intToBytes(i);
                        byte[] response = new byte[content.length + head.length];
                        System.arraycopy(content, 0, response, 0, content.length);
                        System.arraycopy(head, 0, response, content.length, head.length);
                        DatagramPacket responsePacket = new DatagramPacket(response, response.length, inetAddress, 10001);
                        socket.send(responsePacket);
                        System.out.println("server resend file part:" + i);
                        j++;
                    } else if (j >= parts.length) break;
                }
            }
        } catch (IOException e) {
            System.out.println("sendFile io Exception");
        }
    }

    public byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }
}

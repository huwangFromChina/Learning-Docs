package util;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CRCUtil {

    /**
     * /**
     * 获取验证码byte数组，基于Modbus CRC16的校验算法
     */
    public static byte[] getCrc16(byte[] arr_buff, int start, int len) {

        // 预置 1 个 16 位的寄存器为十六进制FFFF, 称此寄存器为 CRC寄存器。
        int crc = 0xFFFF;
        int i, j;
        for (i = start; i < len; i++) {
            // 把第一个 8 位二进制数据 与 16 位的 CRC寄存器的低 8 位相异或, 把结果放于 CRC寄存器
            crc = ((crc & 0xFF00) | (crc & 0x00FF) ^ (arr_buff[i] & 0xFF));
            for (j = 0; j < 8; j++) {
                // 把 CRC 寄存器的内容右移一位( 朝低位)用 0 填补最高位, 并检查右移后的移出位
                if ((crc & 0x0001) > 0) {
                    // 如果移出位为 1, CRC寄存器与多项式A001进行异或
                    crc = crc >> 1;
                    crc = crc ^ 0xA001;
                } else
                    // 如果移出位为 0,再次右移一位
                    crc = crc >> 1;
            }
        }
        return intTo2Bytes(crc);
    }

    /**
     * 将int转换成byte数组，低位在前，高位在后
     * 改变高低位顺序只需调换数组序号
     */
    public static byte[] intTo2Bytes(int value) {
        byte[] src = new byte[2];
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    public static byte[] intTo4Bytes(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }

    public static int byteArrayToInt(byte[] bytes, int start, int len) {
        int value = 0;
        for (int i = start; i < len + start; i++) {
            int shift = (3 - i) * 8;
            value += (bytes[i] & 0xFF) << shift;
        }
        return value;
    }

    public static String byteArrayToString(byte[] bytes, int start, int len) {
        byte[] result = new byte[len];
        int j = 0;
        for (int i = start; i < len + start; i++)
            result[j++] = bytes[i];
        return new String(result);
    }

    public static int byteArrayToInt(byte[] bytes) {
        return byteArrayToInt(bytes, 0, bytes.length);
    }

    public static void bytesCopy(byte[] source, int sourceStart, byte[] target, int targetStart, int len) {
        for (int i = sourceStart; i < len; i++)
            target[targetStart++] = source[i];
    }

    public static String MD5(String key) throws Exception {
        char hexDigits[] = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        byte[] btInput = key.getBytes();
        // 获得MD5摘要算法的 MessageDigest 对象
        MessageDigest mdInst = MessageDigest.getInstance("MD5");
        // 使用指定的字节更新摘要
        mdInst.update(btInput);
        // 获得密文
        byte[] md = mdInst.digest();
        // 把密文转换成十六进制的字符串形式
        int j = md.length;
        char str[] = new char[j * 2];
        int k = 0;
        for (int i = 0; i < j; i++) {
            byte byte0 = md[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(str);
    }

    public static List<byte[]> encode(byte[] data, int MTU, byte[] md5) {
        //分割字节数组
        int sourceDataLength = data.length;
        int height = sourceDataLength % MTU == 0 ? sourceDataLength / MTU : sourceDataLength / MTU + 1;
        List<byte[]> byteArray = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            byte[] byteItem;
            if (i == height - 1) {
                //到了最后一行
                byteItem = new byte[sourceDataLength - MTU * i + 40];
            } else
                byteItem = new byte[MTU + 40];
            for (int j = 0; j < byteItem.length - 40; j++)
                byteItem[40 + j] = data[MTU * i + j];
            byteArray.add(byteItem);
        }

        //封装报头
        byte[] totalSegment = CRCUtil.intTo4Bytes(byteArray.size());
        for (int i = 0; i < byteArray.size(); i++) {
            byte[] currentSegment = CRCUtil.intTo4Bytes(i);
            bytesCopy(md5, 0, byteArray.get(i), 0, 32);
            bytesCopy(currentSegment, 0, byteArray.get(i), 32, 4);
            bytesCopy(totalSegment, 0, byteArray.get(i), 36, 4);
        }
        return byteArray;
    }

    public static String byteListToString(List<Byte> rcvList) {
        byte[] result = new byte[rcvList.size()];
        for (int i = 0; i < rcvList.size(); i++)
            result[i] = rcvList.get(i);
        return new String(result);
    }
}
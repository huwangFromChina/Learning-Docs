package model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gavin
 * 2019/8/29 10:37
 */
public class MetricPackage {
    private int segmentNumbers; //数据包的总分片数
    private int currentSegment = -1; //当前分片编号
    private List<Byte> rcvList = new ArrayList<>();
    private String uniqueCode; //标识此份数据包的唯一标识
    private long actTime; //数据包的接收时间

    public String getUniqueCode() {
        return uniqueCode;
    }

    public void setUniqueCode(String uniqueCode) {
        this.uniqueCode = uniqueCode;
    }

    public long getActTime() {
        return actTime;
    }

    public void setActTime(long actTime) {
        this.actTime = actTime;
    }


    public int getSegmentNumbers() {
        return segmentNumbers;
    }

    public void setSegmentNumbers(int segmentNumbers) {
        this.segmentNumbers = segmentNumbers;
    }

    public int getCurrentSegment() {
        return currentSegment;
    }

    public void setCurrentSegment(int currentSegment) {
        this.currentSegment = currentSegment;
    }

    public void updateActTime() {
        this.actTime = System.currentTimeMillis();
    }

    public List<Byte> getRcvList() {
        return rcvList;
    }

    public void addRcvList(byte[] bytes) {
        for (byte b : bytes)
            this.rcvList.add(b);
    }
}

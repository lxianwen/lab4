package src;

public class Transport {
    private byte[] payload;
    private int seqNum;
    private int ackNum;
    private boolean syn;
    private boolean ack;
    private boolean fin;

    public Transport() {
        this.seqNum = 0;
        this.ackNum = 0;
        this.syn = false;
        this.ack = false;
        this.fin = false;
    }

    // Getters and setters
    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public void setAckNum(int ackNum) {
        this.ackNum = ackNum;
    }

    public void setSyn(boolean syn) {
        this.syn = syn;
    }

    public void setAck(boolean ack) {
        this.ack = ack;
    }

    public void setFin(boolean fin) {
        this.fin = fin;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    // 添加 Getters
    public boolean isSyn() {
        return syn;
    }

    public boolean isAck() {
        return ack;
    }

    public boolean isFin() {
        return fin;
    }

    // 添加模拟的数据包创建和发送方法
    public Packet createPacket(byte[] data, int srcAddr, int srcPort,
            int destAddr, int destPort) {
        // 创建传输层头部
        byte[] header = new byte[20]; // 20字节的头部

        // 设置源端口和目标端口
        header[0] = (byte) (srcPort >> 8);
        header[1] = (byte) srcPort;
        header[2] = (byte) (destPort >> 8);
        header[3] = (byte) destPort;

        // 设置序列号
        header[4] = (byte) (seqNum >> 24);
        header[5] = (byte) (seqNum >> 16);
        header[6] = (byte) (seqNum >> 8);
        header[7] = (byte) seqNum;

        // 设置标志位
        header[8] = (byte) ((syn ? 1 : 0) |
                (ack ? 2 : 0) |
                (fin ? 4 : 0));

        // 组合头部和数据
        byte[] payload = new byte[header.length + (data != null ? data.length : 0)];
        System.arraycopy(header, 0, payload, 0, header.length);
        if (data != null) {
            System.arraycopy(data, 0, payload, header.length, data.length);
        }

        // 修改 Packet 构造函数调用
        byte[] packetData = new byte[payload.length];
        System.arraycopy(payload, 0, packetData, 0, payload.length);
        return new Packet(packetData, seqNum);
    }

    public void sendPacket(Packet packet) {
        // 简化版本，只打印跟踪信息
        if (syn)
            System.out.print("S");
        else if (ack)
            System.out.print(":");
        else if (fin)
            System.out.print("F");
        else
            System.out.print(".");
    }
}
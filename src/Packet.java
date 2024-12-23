package src;

public class Packet {
    private byte[] data;
    private int seqNum;
    private int ackNum;
    private boolean isSYN;
    private boolean isACK;
    private boolean isFIN;

    public Packet(byte[] data, int seqNum) {
        this.data = data;
        this.seqNum = seqNum;
        this.isSYN = false;
        this.isACK = false;
        this.isFIN = false;
    }

    // Getters and setters
    public byte[] getData() {
        return data;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public int getAckNum() {
        return ackNum;
    }

    public void setAckNum(int ackNum) {
        this.ackNum = ackNum;
    }

    public boolean isSYN() {
        return isSYN;
    }

    public boolean isACK() {
        return isACK;
    }

    public boolean isFIN() {
        return isFIN;
    }

    public void setSYN(boolean syn) {
        isSYN = syn;
    }

    public void setACK(boolean ack) {
        isACK = ack;
    }

    public void setFIN(boolean fin) {
        isFIN = fin;
    }
}
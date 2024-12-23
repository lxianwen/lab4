package src;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import src.Packet;

public class Connection {
    // 连接状态枚举
    public enum State {
        CLOSED, // 初始状态
        LISTEN, // 服务器等待连接
        SYN_SENT, // 客户端已发送SYN
        SYN_RECEIVED, // 服务器收到SYN并回复
        ESTABLISHED, // 连接建立
        FIN_WAIT, // 等待关闭
        CLOSING // 正在关闭
    }

    private State state;
    private int localAddr;
    private int remoteAddr;
    private int localPort;
    private int remotePort;
    private int sequenceNum;
    private int ackNum;

    private static final int MIN_TIMEOUT = 100; // 最小超时时间(毫秒)
    private static final int MAX_TIMEOUT = 3000; // 最大超时时间(毫秒)
    private int currentTimeout = 1000; // 当前超时时间
    private long lastRTT = 0; // 最后一次RTT时间
    private long sendTime = 0; // 包发送时间

    private Packet lastSentPacket; // 最后发送的数据包
    private boolean waitingForAck; // 是否在等待确认
    private Timer retransmissionTimer; // 重传定时器

    // 添加缓冲区相关常量
    private static final int BUFFER_SIZE = 1024 * 256; // 256KB缓冲区
    private static final int MAX_SEGMENT_SIZE = 1024; // 最大分段大小

    // 添加滑动窗口相关字段
    private static final int WINDOW_SIZE = 4; // 窗口大小
    private Queue<Packet> sendWindow; // 发送窗口
    private int windowBase; // 窗口基序号
    private int nextSeqNum; // 下一个序列号

    // 修改缓冲区实现
    private class Buffer {
        private byte[] data;
        private int head;
        private int tail;
        private int size;

        public Buffer() {
            this.data = new byte[BUFFER_SIZE];
            this.head = 0;
            this.tail = 0;
            this.size = 0;
        }

        public synchronized boolean write(byte[] buf, int offset, int length) {
            if (size + length > BUFFER_SIZE) {
                return false; // 缓冲区已满
            }

            // 写入数据
            for (int i = 0; i < length; i++) {
                data[tail] = buf[offset + i];
                tail = (tail + 1) % BUFFER_SIZE;
            }
            size += length;
            return true;
        }

        public synchronized int read(byte[] buf, int offset, int length) {
            if (size == 0) {
                return 0; // 缓冲区为空
            }

            int bytesToRead = Math.min(length, size);
            for (int i = 0; i < bytesToRead; i++) {
                buf[offset + i] = data[head];
                head = (head + 1) % BUFFER_SIZE;
            }
            size -= bytesToRead;
            return bytesToRead;
        }

        public synchronized boolean isFull() {
            return size == BUFFER_SIZE;
        }

        public synchronized boolean isEmpty() {
            return size == 0;
        }

        public synchronized int available() {
            return BUFFER_SIZE - size;
        }
    }

    // 替换原来的Queue为Buffer
    private Buffer sendBuffer;
    private Buffer receiveBuffer;

    private Transport transport;

    // 用于标识连接的构造函数
    public Connection(int localAddr, int localPort, int remoteAddr, int remotePort) {
        this.localAddr = localAddr;
        this.localPort = localPort;
        this.remoteAddr = remoteAddr;
        this.remotePort = remotePort;
        this.state = State.CLOSED;
        this.sequenceNum = 0;
        this.ackNum = 0;
        this.sendBuffer = new Buffer();
        this.receiveBuffer = new Buffer();
        this.waitingForAck = false;
        this.sendWindow = new LinkedList<>();
        this.windowBase = 0;
        this.nextSeqNum = 0;
        this.transport = new Transport();
    }

    // 获取连接状态
    public State getState() {
        return state;
    }

    // 设置连接状态
    public void setState(State state) {
        this.state = state;
    }

    // 添加获取连接信息的方法
    public String getConnectionInfo() {
        return String.format("本地[%d:%d] -> 远程[%d:%d] 状态:%s",
                localAddr, localPort, remoteAddr, remotePort, state);
    }

    // 添加序列号管理方法
    public void incrementSequenceNum(int amount) {
        this.sequenceNum += amount;
    }

    public void incrementAckNum(int amount) {
        this.ackNum += amount;
    }

    // 获取当前序列号和确认号
    public int getSequenceNum() {
        return sequenceNum;
    }

    public int getAckNum() {
        return ackNum;
    }

    // 更新超时时间
    private void updateTimeout(long rtt) {
        // 使用TCP的超时计算公式
        double alpha = 0.125;
        double beta = 0.25;

        if (lastRTT == 0) {
            lastRTT = rtt;
        }

        // 计算平滑RTT
        long smoothedRTT = (long) ((1 - alpha) * lastRTT + alpha * rtt);
        // 计算RTT偏差
        long deviation = (long) ((1 - beta) * Math.abs(lastRTT - rtt));
        // 更新超时时间
        currentTimeout = (int) Math.min(MAX_TIMEOUT,
                Math.max(MIN_TIMEOUT, smoothedRTT + 4 * deviation));

        lastRTT = smoothedRTT;
    }

    // 添加错误处理
    public enum ErrorType {
        BUFFER_FULL,
        BUFFER_EMPTY,
        CONNECTION_CLOSED,
        TIMEOUT,
        INVALID_STATE,
        WINDOW_FULL
    }

    public static class ConnectionException extends Exception {
        private ErrorType type;

        public ConnectionException(ErrorType type, String message) {
            super(message);
            this.type = type;
        }

        public ErrorType getType() {
            return type;
        }
    }

    // 修改发送数据包方法
    public synchronized boolean sendPacket(byte[] data) throws ConnectionException {
        if (state != State.ESTABLISHED) {
            throw new ConnectionException(ErrorType.INVALID_STATE,
                    "Connection not established");
        }

        if (sendWindow.size() >= WINDOW_SIZE) {
            throw new ConnectionException(ErrorType.WINDOW_FULL,
                    "Send window is full");
        }

        if (!sendBuffer.write(data, 0, data.length)) {
            throw new ConnectionException(ErrorType.BUFFER_FULL,
                    "Send buffer is full");
        }

        // 创建并发送数据包
        Packet packet = new Packet(data, nextSeqNum);
        sendWindow.offer(packet);
        lastSentPacket = packet;
        sendTime = System.currentTimeMillis();
        nextSeqNum++;

        startRetransmissionTimer();
        return true;
    }

    // 添加接收数据方法
    public synchronized int receiveData(byte[] buf, int offset, int length)
            throws ConnectionException {
        if (state != State.ESTABLISHED) {
            throw new ConnectionException(ErrorType.INVALID_STATE,
                    "Connection not established");
        }

        int bytesRead = receiveBuffer.read(buf, offset, length);
        if (bytesRead == 0) {
            throw new ConnectionException(ErrorType.BUFFER_EMPTY,
                    "Receive buffer is empty");
        }

        return bytesRead;
    }

    // 添加处理接收数据包的方法
    public synchronized void handleReceivePacket(Packet packet)
            throws ConnectionException {
        if (state != State.ESTABLISHED) {
            throw new ConnectionException(ErrorType.INVALID_STATE,
                    "Connection not established");
        }

        // 检查序列号
        if (packet.getSeqNum() == ackNum) {
            // 按序到达的包
            byte[] data = packet.getData();
            if (!receiveBuffer.write(data, 0, data.length)) {
                throw new ConnectionException(ErrorType.BUFFER_FULL,
                        "Receive buffer is full");
            }
            ackNum++;
            // 发送确认
            sendAck(ackNum);
        } else {
            // 乱序包，重发上一个ACK
            sendAck(ackNum);
        }
    }

    private void sendAck(int ackNum) {
        // 实现发送ACK的逻辑
        System.out.print(":"); // 打印确认包跟踪字符
    }

    // 修改处理确认包方法以支持滑动窗口
    public synchronized void handleAck(int ackNum) {
        if (ackNum > windowBase) {
            long rtt = System.currentTimeMillis() - sendTime;
            updateTimeout(rtt);

            // 移动窗口
            while (windowBase < ackNum) {
                sendWindow.poll(); // 移除已确认的包
                windowBase++;
            }

            // 如果窗口中还有包，重新启动定时器
            if (!sendWindow.isEmpty()) {
                startRetransmissionTimer();
            } else {
                stopRetransmissionTimer();
            }
        }
    }

    // 修改启动重传定时器方法
    private void startRetransmissionTimer() {
        if (retransmissionTimer != null) {
            retransmissionTimer.cancel();
        }
        retransmissionTimer = new Timer();
        retransmissionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                retransmitPacket();
            }
        }, currentTimeout); // 使用当前的超时时间
    }

    // 停止重传定时器
    private void stopRetransmissionTimer() {
        if (retransmissionTimer != null) {
            retransmissionTimer.cancel();
            retransmissionTimer = null;
        }
    }

    // 重传数据包
    private synchronized void retransmitPacket() {
        if (!sendWindow.isEmpty()) {
            System.out.print("!"); // 打印重传标记
            // 重新发送窗口中的所有包
            for (Packet packet : sendWindow) {
                // 实际发送包的代码
            }
            startRetransmissionTimer();
        }
    }

    // 添加关闭方法
    public void close() {
        stopRetransmissionTimer(); // 停止重传定时器
        if (retransmissionTimer != null) {
            retransmissionTimer.cancel();
            retransmissionTimer = null;
        }
        state = State.CLOSED;
    }

    // 修改发送数据包方法
    private void sendPacket(byte[] data, boolean isSyn, boolean isAck, boolean isFin) {
        transport.setSeqNum(sequenceNum);
        transport.setAckNum(ackNum);
        transport.setSyn(isSyn);
        transport.setAck(isAck);
        transport.setFin(isFin);
        transport.setPayload(data);

        Packet packet = transport.createPacket(data, localAddr, localPort,
                remoteAddr, remotePort);
        transport.sendPacket(packet);
    }
}
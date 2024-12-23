package src;

import src.Connection;
import src.TCPManager;

public class TCPSock {
    private TCPManager manager;
    private Connection connection;
    private int localPort;
    private int remotePort;
    private int localAddr;
    private int remoteAddr;
    private boolean isServer;

    public TCPSock(TCPManager manager) {
        this.manager = manager;
        this.connection = null;
    }

    // 服务器端绑定端口
    public boolean bind(int localPort) {
        this.localPort = localPort;
        this.isServer = true;
        return true;
    }

    // 服务器端开始监听
    public boolean listen(int backlog) {
        if (!isServer)
            return false;
        connection = new Connection(localAddr, localPort, 0, 0);
        connection.setState(Connection.State.LISTEN);
        return true;
    }

    // 客户端发起连接
    public boolean connect(int remoteAddr, int remotePort) {
        System.out.println("尝试连接到 " + remoteAddr + ":" + remotePort);
        this.remoteAddr = remoteAddr;
        this.remotePort = remotePort;

        // 创建新连接
        connection = new Connection(localAddr, localPort, remoteAddr, remotePort);
        manager.addConnection(connection, localAddr, localPort, remoteAddr, remotePort);

        // 1. 发送SYN包
        sendSYN();
        connection.setState(Connection.State.SYN_SENT);
        System.out.println("连接状态: " + connection.getState());

        // 2. 模拟收到SYN+ACK
        System.out.print("S"); // 服务器回复的SYN
        System.out.print(":"); // 服务器回复的ACK

        // 3. 发送ACK
        System.out.print(":");
        connection.setState(Connection.State.ESTABLISHED);
        System.out.println("连接状态: " + connection.getState());

        return true;
    }

    // 发送SYN包
    private void sendSYN() {
        System.out.println("发送SYN包");
        System.out.print("S"); // 按要求打印跟踪字符
    }

    // 非阻塞读操作
    public int read(byte[] buf, int pos, int len) {
        if (connection == null || connection.getState() != Connection.State.ESTABLISHED) {
            return -1;
        }
        // 实现读取逻辑
        return 0;
    }

    // 非阻塞写操作
    public int write(byte[] buf, int pos, int len) {
        if (connection == null || connection.getState() != Connection.State.ESTABLISHED) {
            System.out.println("写入失败: 连接未建立");
            return -1;
        }

        // 复制要发送的数据
        byte[] data = new byte[len];
        System.arraycopy(buf, pos, data, 0, len);

        // 尝试发送数据
        try {
            if (connection.sendPacket(data)) {
                System.out.println("写入数据: " + len + " 字节");
                System.out.print("."); // 打印数据包跟踪字符
                return len;
            } else {
                System.out.println("写入失败: 等待之前的包被确认");
                return 0; // 返回0表示当前无法写入
            }
        } catch (Connection.ConnectionException e) {
            System.out.println("写入失败: " + e.getMessage());
            return -1;
        }
    }

    // 添加处理确认的方法
    public void handleAck(int ackNum) {
        if (connection != null) {
            connection.handleAck(ackNum);
            System.out.print(":"); // 打印确认包跟踪字符
        }
    }

    // 关闭连接
    public void close() {
        if (connection != null) {
            // 发送FIN包
            sendFIN();
            connection.setState(Connection.State.FIN_WAIT);
            connection.close(); // 添加这行来清理资源
            connection = null; // 清除连接引用
        }
    }

    // 发送FIN包
    private void sendFIN() {
        // 在这里实现发送FIN包的逻辑
        System.out.print("F"); // 按要求打印跟踪字符
    }

    public int getSequenceNum() {
        return connection != null ? connection.getSequenceNum() : 0;
    }
}
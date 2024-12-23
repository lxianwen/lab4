package src;

import java.util.HashMap;
import java.util.Map;

public class TCPManager {
    private Map<String, Connection> connections;

    public TCPManager() {
        this.connections = new HashMap<>();
    }

    // 创建新的 TCP Socket
    public TCPSock socket() {
        return new TCPSock(this);
    }

    // 添加新连接
    public void addConnection(Connection connection, int localAddr, int localPort,
            int remoteAddr, int remotePort) {
        String key = getConnectionKey(localAddr, localPort, remoteAddr, remotePort);
        connections.put(key, connection);
    }

    // 获取连接
    public Connection getConnection(int localAddr, int localPort,
            int remoteAddr, int remotePort) {
        String key = getConnectionKey(localAddr, localPort, remoteAddr, remotePort);
        return connections.get(key);
    }

    // 移除连接
    public void removeConnection(int localAddr, int localPort,
            int remoteAddr, int remotePort) {
        String key = getConnectionKey(localAddr, localPort, remoteAddr, remotePort);
        connections.remove(key);
    }

    // 生成连接的唯一键
    private String getConnectionKey(int localAddr, int localPort,
            int remoteAddr, int remotePort) {
        return String.format("%d:%d-%d:%d", localAddr, localPort, remoteAddr, remotePort);
    }
}
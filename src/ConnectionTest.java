package src;

import src.TCPManager;
import src.TCPSock;

public class ConnectionTest {
    public static void main(String[] args) throws InterruptedException {
        testBasicConnection();
        testDataTransfer();
        testRetransmission();
        testBufferOperations();
    }

    private static void testBasicConnection() throws InterruptedException {
        // 创建TCP管理器
        TCPManager serverManager = new TCPManager();
        TCPManager clientManager = new TCPManager();

        // 创建服务器socket
        TCPSock serverSock = serverManager.socket();
        // 绑定服务器端口21
        serverSock.bind(21);
        // 开始监听
        serverSock.listen(1);
        System.out.println("服务器开始监听端口21");

        // 创建客户端socket
        TCPSock clientSock = clientManager.socket();
        // 客户端连接到服务器
        boolean connected = clientSock.connect(1, 21); // 假设服务器地址为1

        if (connected) {
            System.out.println("客户端连接成功");

            // 测试数据传输
            byte[] testData = "Hello, Server!".getBytes();
            int written = clientSock.write(testData, 0, testData.length);
            System.out.println("客户端发送数据: " + written + " 字节");

            // 等待3秒，让��时重传发生几次（因为TIMEOUT是1秒）
            System.out.println("等待3秒，观察重传...");
            Thread.sleep(3000);

            // 然后发送ACK来停止重传
            System.out.println("发送ACK...");
            clientSock.handleAck(1);

            // 关闭连接
            clientSock.close();
            serverSock.close();
        } else {
            System.out.println("连接失败");
        }
    }

    private static void testDataTransfer() {
        System.out.println("\n=== 测试数据传输 ===");
        TCPManager manager = new TCPManager();
        TCPSock sock = manager.socket();

        try {
            // 创建大量测试数据
            byte[] largeData = new byte[100 * 1024]; // 100KB
            for (int i = 0; i < largeData.length; i++) {
                largeData[i] = (byte) (i % 256);
            }

            sock.bind(8000);
            sock.listen(1);

            // 建立连接
            TCPManager clientManager = new TCPManager();
            TCPSock clientSock = clientManager.socket();
            if (clientSock.connect(0, 8000)) { // 连接到服务器
                System.out.println("连接建立成功，开始传输数据");

                // 模拟发送大量数据
                int totalSent = 0;
                while (totalSent < largeData.length) {
                    int toSend = Math.min(1024, largeData.length - totalSent);
                    try {
                        int sent = clientSock.write(largeData, totalSent, toSend);
                        if (sent > 0) {
                            totalSent += sent;
                            System.out.println("已发送: " + totalSent + " 字节");
                            clientSock.handleAck(totalSent);
                        }
                    } catch (Exception e) {
                        System.out.println("缓冲区已满，等待清空...");
                        Thread.sleep(500); // 等待缓冲区清空
                        continue;
                    }
                    Thread.sleep(50);
                }

                clientSock.close();
            }
        } catch (Exception e) {
            System.out.println("数据传输测试异常: " + e.getMessage());
        } finally {
            sock.close();
        }
    }

    private static void testRetransmission() {
        System.out.println("\n=== 测试重传机制 ===");
        TCPManager serverManager = new TCPManager();
        TCPManager clientManager = new TCPManager();

        try {
            // 创建并启动服务器
            TCPSock serverSock = serverManager.socket();
            serverSock.bind(8001);
            serverSock.listen(1);

            // 创建客户端并连接
            TCPSock clientSock = clientManager.socket();
            if (clientSock.connect(0, 8001)) {
                System.out.println("连接建立成功，开始测试重传");

                // 发送数据但不确认，触发重传
                byte[] data = "Test retransmission".getBytes();
                clientSock.write(data, 0, data.length);

                // 等待观察重传
                System.out.println("等待5秒观察重传...");
                Thread.sleep(5000);

                // 最后发送确认
                System.out.println("发送ACK...");
                clientSock.handleAck(1);

                clientSock.close();
            }
            serverSock.close();
        } catch (Exception e) {
            System.out.println("重传测试异常: " + e.getMessage());
        }
    }

    private static void testBufferOperations() {
        System.out.println("\n=== 测试缓冲区操作 ===");
        TCPManager serverManager = new TCPManager();
        TCPManager clientManager = new TCPManager();

        try {
            // 创建并启动服务器
            TCPSock serverSock = serverManager.socket();
            serverSock.bind(8002);
            serverSock.listen(1);

            // 创建客户端并连接
            TCPSock clientSock = clientManager.socket();
            if (clientSock.connect(0, 8002)) {
                System.out.println("连接建立成功，开始测试缓冲区");

                // 测试缓冲区写满
                byte[] largeData = new byte[128 * 1024]; // 128KB
                try {
                    clientSock.write(largeData, 0, largeData.length);
                } catch (Exception e) {
                    System.out.println("预期的缓冲区已满异常: " + e.getMessage());
                }

                // 测试读空缓冲区
                byte[] readBuffer = new byte[1024];
                try {
                    int read = serverSock.read(readBuffer, 0, readBuffer.length);
                    System.out.println("读取到: " + read + " 字节");
                } catch (Exception e) {
                    System.out.println("预期的缓冲区为空异常: " + e.getMessage());
                }

                clientSock.close();
            }
            serverSock.close();
        } catch (Exception e) {
            System.out.println("缓冲区测试异常: " + e.getMessage());
        }
    }
}
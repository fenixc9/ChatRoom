import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * server
 */
public class SocketService {
    private static final int PORT = 9999;
    private CopyOnWriteArrayList<Chat> all = new CopyOnWriteArrayList();
    private boolean isRuning = false;

    public static void main(String[] args) {
        SocketService socketService = new SocketService();
        socketService.start();
    }

    private void start() {
        ServerSocket ss = null;
        isRuning = true;
        System.out.println("server started!");
        try {
            ss = new ServerSocket(PORT);
            while (isRuning) {
                Socket client = ss.accept();
                System.out.println("new client has connected!");
                new Thread(new Register(client)).start();
            }
        } catch (IOException e) {
            isRuning = false;
            System.out.println("SocketService: " + e.toString());
        }

    }

    /**
     * 注册
     */
    private class Register implements Runnable {
        Socket client;

        public Register(Socket socket) {
            client = socket;
        }

        @Override
        public void run() {
            boolean registeSuccess = false;
            try {
                while (!registeSuccess) {
                    DataInputStream dis = new DataInputStream(client.getInputStream());
                    DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                    String s = dis.readUTF();
                    registeSuccess = true;
                    if (all.size() > 0) {
                        for (Chat channal : all) {
                            if (channal.clientName.equals(s)) {
                                dos.writeUTF("[system]:your name has been registed!");
                                registeSuccess = false;
                            }
                        }
                    }
                    if (registeSuccess) {
                        Chat channel = new Chat(client, s);
                        all.add(channel);
                        System.out.println("new client has registedSuccess!");
                        new Thread(channel).start();
                    }
                }
            } catch (IOException e) {

            }
        }
    }

    /**
     * 聊天
     */
    private class Chat implements Runnable {
        DataInputStream dis;
        DataOutputStream dos;
        private String clientName;
        private boolean isRunning;

        public Chat(Socket client, String name) {
            try {
                System.out.println("Chat init ...");
                isRunning = true;
                dis = new DataInputStream(client.getInputStream());
                dos = new DataOutputStream(client.getOutputStream());
                this.clientName = name;
                this.send("welcome to Hao's chatroom");
                sendOthers(this.clientName + " has entered chatroom", true);
            } catch (IOException e) {
                isRunning = false;
            }
        }

        private void send(String msg) {
            if (null == msg && !msg.equals("")) {
                return;
            }
            try {
                dos.writeUTF(msg);
                dos.flush();
            } catch (IOException e) {
                try {
                    if (dis!=null)
                        dis.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    if (dos!=null)
                    dos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                System.out.println("SocketService send :" + e.toString());
                isRunning = false;
                all.remove(this);//移除自身
            }
        }

        /**
         * 发送
         * @param msg
         * @param isSys
         */
        private void sendOthers(String msg, boolean isSys) {
            //单聊以@/用户名后加空格
            if (msg.startsWith("@/")) {
                boolean findTar = false;
                for (Chat channal : all) {
                    if (channal.clientName.equals(msg.split(" ")[0].substring(2, msg.split(" ")[0].length()))) {
                        findTar = true;
                        channal.send("[" + this.clientName + "]" + " whisper：" + msg.substring(msg.indexOf(" "), msg.length()));
                        send("you whisper to [" + channal.clientName + "]:" + msg.substring(msg.indexOf(" "), msg.length()));
                    }
                }
                if (!findTar) {
                    send("[system]：message send fail :user not found");
                }
                return;

            }
            //群发
            for (Chat other : all) {
                if (isSys) {//系统信息
                    other.send("[system]：" + msg);
                } else {
                    other.send("[" + this.clientName + "]" + "：" + msg);
                }
            }
        }

        private String receive() {
            try {
                return dis.readUTF();
            } catch (IOException e) {
                return "-exit";
            }
        }

        @Override
        public void run() {
            while (isRunning) {
                String receive = receive();
                if (receive.startsWith("-exit")){
                    sendOthers(this.clientName + " has left the chatroom", true);
                    all.remove(this);
                    isRunning = false;
                    break;
                }
                if (receive != null && receive.length() > 0) {
                    sendOthers(receive, false);
                }
            }
        }
    }
}
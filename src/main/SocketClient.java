package main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * client
 */
public class SocketClient {
    private static final String CMD_EXIT = "-exit";
    private static final int PORT = 9999;
    private boolean isRunning = false;
    private DataOutputStream dos;
    private String lastReceivedMsg;
    Pattern ipPattern = Pattern.compile("(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)");
    private String lastScanMsg = "";
    private static final int STATE_INPUT_HOST = 123;
    private static final int STATE_INPUT_NAME = 95;
    private static final int STATE_INPUT_MSG = 417;
    private int currentState = 0;
    Socket socket = null;
    public static void main(String[] args) {
        SocketClient client = new SocketClient();
        try {
            client.start();
        } catch (IOException e) {
            System.out.println("SocketClient  :" + e.toString());
        }
    }

    private class Scan implements Runnable {

        public Scan() {
            currentState = STATE_INPUT_HOST;
        }

        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            while (isRunning) {
                lastScanMsg = scanner.nextLine();
                if (lastScanMsg!=null && lastScanMsg.length()>0 ){
                    switch (currentState){
                        case STATE_INPUT_HOST:
                            setTheHost(lastScanMsg);
                            break;
                        case STATE_INPUT_NAME:
                            setName(lastScanMsg);
                            break;
                        case STATE_INPUT_MSG:
                            sendMsg(lastScanMsg);
                            break;
                    }
                }
            }
        }

        private void setName(String name) {
            if (name==null || name.length()==0){
                System.out.println("name cant be null");
                return;
            }
            try {
                dos.writeUTF(name);
                dos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendMsg(String msg) {
            try {
                dos.writeUTF(msg);
                dos.flush();
                cmdCheck(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void setTheHost(String host) {
            if (!ipPattern.matcher(host).matches()){
                System.out.println("illegal host,please input the right host：");
            }else{
                System.out.println("connecting...");
                try {
                    socket = new Socket(host,PORT);
                    new Thread(new Receive(socket)).start();
                    dos = new DataOutputStream(socket.getOutputStream());
                    currentState = STATE_INPUT_NAME;
                    System.out.println("connect to server success!please intput your name:");
                } catch (IOException e) {
                    System.out.println("connect failed,please make sure the server is running and input the right host：");
                }
            }
        }
    }

    /**
     * ex command
     * @param msg
     * @return
     */
    private boolean cmdCheck(String msg){
        switch (msg){
            case CMD_EXIT:
                isRunning = false;
                try {
                    socket.close();
                } catch (IOException e) {
                    try {
                        dos.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                return false;
            default:
                return true;
        }

    }

    private void start() throws IOException {
        isRunning = true;
        System.out.println("welcome to the Chatroom \nplease input the server host:");
        new Thread(new Scan()).start();
    }

    private class Receive implements Runnable {
        private DataInputStream is;

        public Receive(Socket socket) {
            try {
                is = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                System.out.println("client receive init" + e.toString());
                isRunning = false;
            }

        }

        public String receive() {
            lastReceivedMsg = "";
            try {
                lastReceivedMsg = is.readUTF();
            } catch (IOException e) {
                isRunning = false;
                try {
                    dos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                System.out.println("disconnected");
            }
            return lastReceivedMsg;
        }

        @Override
        public void run() {
            while (isRunning) {
                String receive = receive();
                if (receive != null && receive.length() > 0){
                    if (receive.equals("welcome to Mario's chatroom")){
                        currentState = STATE_INPUT_MSG;
                    }
                    System.out.println(receive);
                }
            }
        }
    }

}
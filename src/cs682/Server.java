package cs682;

import ZooKeeper.Zk;
import model.ChatData;
import model.User;
import org.apache.log4j.net.SocketServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

/**
 * Created by jordan on 1/31/18.
 */
public class Server extends Thread {
    public static int PORT;
    private static volatile Boolean isAlive = true;
    private static User user;
    private ServerSocket socket;
    private ChatData chatData;
    private Client client;

    //using isAlive from cs601
    public Server(User user, ChatData cd){
        this.user = user;
        this.chatData = cd;
        this.PORT = user.getPort();
        try {
            this.socket = new ServerSocket(PORT);
        }catch(IOException e){
            System.out.println("connection closed");
        }
    }
    public void addClient(Client client){
        this.client = client;
    }

    public void run(){
        try{
            while(isAlive){
                System.out.println("Server: Waiting for connection...");
                Socket connectionSocket = socket.accept();
                System.out.println("incoming connection");

                Thread chatThread = new Thread(new ChatHandler(connectionSocket, user, chatData));
                chatThread.start();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public void exitProgram(){
        isAlive = false;
        try {
            socket.close();
        }catch (IOException e){

        }
    }

}

package cs682;

import ZooKeeper.Zk;
import model.ChatData;
import model.DataServerMessages;
import model.User;

import org.apache.zookeeper.ZooKeeper;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by jordan on 2/4/18.
 */
public class Chat {

    private static String name;
    private static String ipAddress;
    private static String port;
    private static String udpPort;
    private static Zk zk;


    public static void main(String[] args) {
        boolean running =true;
        parseArgs(args);
        User thisUser= new User(name,ipAddress, Integer.parseInt(port));

        connectToZk();

        DataServerMessages.Chat message = DataServerMessages.Chat.newBuilder()
                .setMessage("hello this is a testtttttttttttttttttttt").setFrom("jordan").setIsBcast(true).build();
        ChatData cd = new ChatData();
        cd.addMessage(message);
        PacketHandler ph = new PacketHandler(Integer.parseInt(udpPort), cd);
        Thread phThread = new Thread(ph);
        phThread.start();

        Client cl = new Client(thisUser, cd, zk, ph);
        Server s = new Server(thisUser, cd);
        cl.addServer(s);
        s.addClient(cl);

        Thread serverThread = new Thread(s);
        serverThread.start();

        Thread clientThread =new Thread(cl);
        clientThread.start();



    }

    private static void connectToZk() {
        DataServerMessages.ZKData userData = DataServerMessages.ZKData.newBuilder()
                .setIp(ipAddress)
                .setPort(port)
                .setUdpport(udpPort).build();
        zk = new Zk(name, userData);
        zk.connectToZk();
        zk.joinGroup();
    }

    private static void parseArgs(String[] args) {
        for(int i =0; i<args.length; i++){

            if(args[i].equals("-user")){
                name = args[i+1];
            }else if(args[i].equals("-port")){

                try{
                    // https://stackoverflow.com/questions/9340989/javainetaddress-to-string-conversion
                    InetAddress address = InetAddress.getLocalHost();
                    ipAddress = address.getHostAddress();

                }catch(UnknownHostException e){
                    System.out.println("couldn't get the ipaddress");
                }

                port = args[i+1];
            }else if( args[i].equals("-udpport")){
                udpPort = args[i+1];
            }
        }
    }


}

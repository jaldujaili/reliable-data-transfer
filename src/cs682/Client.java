package cs682;

import ZooKeeper.Zk;
import model.ChatData;
import model.DataServerMessages;
import model.User;

import java.io.*;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

/**
 * Created by jordan on 2/1/18.
 */
public class Client extends Thread{

    private User user;
    private Zk zk;
    private volatile boolean isHelping = true;
    private ChatData chatData;
    private Server server;
    private BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));// for the user input
    private PacketHandler packetHandler;


    public Client(User user, ChatData cd, Zk zk, PacketHandler ph){
        this.user = user;
        this.zk= zk;
        this.chatData = cd;
        this.packetHandler = ph;
    }

    /*
    gets the server so it can be used
    param server
     */
    public void addServer(Server server){
        this.server = server;
    }

    public void run(){
        while(isHelping) {
            helping();
        }
    }
    /*
    Get user input to do more commands
     */
    public void helping(){

        if(!isHelping){
            return;
        }
        try {
            System.out.println("\nWhat would you like do? Type help to see options");
            String in = userInput.readLine();
            if(in.equals("help")){
                seeHelp();
            }else if (in.equals("send")){
                connectToUser();
            }else if (in.equals("list")){
                getListOfOthers();
            }else if (in.equals("broadcast")){
                broadCast();
            }else if (in.equals("read")) {
                readMessages();
            }else if (in.equals("download")) {
                getMessages();
            }else if (in.equals("clear messages")){
                    deleteMessages();
            }else if (in.equals("exit")){
                isHelping = false;
                server.exitProgram();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void seeHelp(){
        System.out.println("send, to send a message " +
                "\nlist, to list others\nbroadcast, to broadcast a message to all\nread, to see broadcast messages\ndownload, to download bcast");
    }
    public void deleteMessages(){
        chatData.clearHistory();
    }
    public void readMessages(){
        chatData.showMessage();
    }

    public void getMessages(){
        zk.populateGroup();
        System.out.println("who would you like to download from?");
        checkUser("download");

    }

    public void getListOfOthers(){
            zk.populateGroup();
    }
    public void connectToUser()throws IOException{

        zk.populateGroup();
        System.out.println("who would you like to send to? OR type \"back\" to go back");
        checkUser("message");


    }

    private void checkUser(String st) {
        boolean isConnecting=true;
        while(isConnecting){
            String username="";
            try{
                username= new BufferedReader(new InputStreamReader(System.in)).readLine();
            }catch(IOException e){
                e.printStackTrace();
            }
            if(username.equals("back")){
                isConnecting =false;
                return;
            }
            User checkedUser = zk.checkUsername(username);
            if(checkedUser==null){
                System.out.println("oops, try typing name again");
            }else{
                isConnecting = false;
                if(st.equals("message")){
                    System.out.println("send a message to: " + username);

                    sendMessage(checkedUser, user);
                }else if(st.equals("download")){
                    System.out.println("download from: " + username);

                    int thisUDP = user.getUdpPort();
                    int otherUserUDP = checkedUser.getUdpPort();
                    try {
//                        DatagramSocket socket = new DatagramSocket(thisUDP);
                        packetHandler.sendReq(checkedUser);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }catch (IOException e){
                        e.printStackTrace();
                    }

                }

            }
        }
    }

    public void sendMessage(User user, User me){

            try {
                String message = new BufferedReader(new InputStreamReader(System.in)).readLine();
                DataServerMessages.Chat cmessage = DataServerMessages.Chat.newBuilder().setFrom(me.getName())
                        .setMessage(message).setIsBcast(false).build();

                Thread th = new Thread() {
                    DataServerMessages.Chat messages;
                    User user;

                    Thread init(DataServerMessages.Chat messages, User user) {
                        this.messages = messages;
                        this.user = user;
                        return this;
                    }

                    @Override
                    public void run() {

                        try (Socket sock = new Socket(user.getIpaddress(), user.getPort()); //connecting to localhost
                             final OutputStream outstream = sock.getOutputStream();
                             InputStream instream = sock.getInputStream()) {


                            messages.writeDelimitedTo(outstream);
                            DataServerMessages.Reply reply = DataServerMessages.Reply.parseDelimitedFrom(instream);
                            String rep = reply.getMessage()+ " "+ reply.getStatus();
                            System.out.println(rep);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.init(cmessage, user);
                th.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void broadCast(){
        zk.populateGroup();
        System.out.println("type a message to all");
        List<User> users = zk.getUsers();
        String message = "";

            try {
                message = new BufferedReader(new InputStreamReader(System.in)).readLine();

            } catch (IOException e) {
                e.printStackTrace();
            }
            DataServerMessages.Chat cmessage = DataServerMessages.Chat.newBuilder().setFrom(user.getName())
                    .setMessage(message).setIsBcast(true).build();

            for (User user : users) {
                System.out.println(user.getName());

                Thread th = new Thread() {
                    DataServerMessages.Chat messages;
                    User user;

                    Thread init(DataServerMessages.Chat messages, User user) {
                        this.messages = messages;
                        this.user = user;
                        return this;
                    }

                    @Override
                    public void run() {

                        try (Socket sock = new Socket(user.getIpaddress(), user.getPort()); //connecting to localhost
                             final OutputStream outstream = sock.getOutputStream();
                             InputStream instream = sock.getInputStream()) {

                            messages.writeDelimitedTo(outstream);
                            sock.close();
                        } catch (IOException e) {
//                            System.out.println("couldn't send to: "+user.getName());
                        }

                    }
                }.init(cmessage, user);
                th.start();
            }

    }
}



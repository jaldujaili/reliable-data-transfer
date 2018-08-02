package cs682;

import jdk.internal.util.xml.impl.Input;
import model.ChatData;
import model.DataServerMessages;
import model.User;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * Created by jordan on 1/31/18.
 */
public class ChatHandler implements Runnable{

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private User user;
    private ChatData chatData;


    public ChatHandler(Socket socket, User user, ChatData chatData) throws IOException{
        this.socket = socket;
        this.user = user;
        this.chatData = chatData;
    }

    public void readCached(){
        chatData.showMessage();
    }

    public void run(){
        try{
            in = socket.getInputStream();
            out = socket.getOutputStream();

            DataServerMessages.Chat message = DataServerMessages.Chat.parseDelimitedFrom(in);
            String sender = message.getFrom();
            String mess = message.getMessage();
            boolean isBroadcast = message.getIsBcast();

            System.out.println(sender+" said: "+ mess+ "\n broadcast: "+ isBroadcast);
            if(isBroadcast){
//                String cached = sender+" sent: "+mess;

                chatData.addMessage(message);
            }

            DataServerMessages.Reply reply = DataServerMessages.Reply.newBuilder().setStatus(200).setMessage("Ok").build();
            socket.close();

        }catch(IOException e){
            e.printStackTrace();
        }
    }
}

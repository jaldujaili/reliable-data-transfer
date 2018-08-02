package cs682;

import com.google.protobuf.ByteString;
import model.ChatData;
import model.DataServerMessages;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * Created by jordan on 2/16/18.
 */
public class UDPSender implements Runnable{
    private int myUdpPort;
    private int otherUdpPort;
    private ChatData chatData;
    private DatagramSocket socket;
    List<DataServerMessages.Data> packets;
    private int seqNum;
    private int packetNum;
    private int firstSeqNum;
    private InetAddress ip;
    private Stack<DataServerMessages.Data> ackStack;
    private int expectedPac=1;
    PacketHandler parent;

    public UDPSender(int udpPort, int otherUdpPort, ChatData chatData, PacketHandler parent, DatagramSocket socket,InetAddress ad) {
        this.myUdpPort = udpPort;
        this.otherUdpPort =  otherUdpPort;
        this.chatData = chatData;
        this.packets = new ArrayList<>();
        this.parent = parent;
        this.socket = socket;
        this.ip = ad;
        ackStack = new Stack<>();
    }

    public void ackReceived(DataServerMessages.Data packet){
        System.out.println("R- expected: "+expectedPac+" got: "+packet.getSeqNo());

        synchronized (this) {
            notify();
        }
    }
    public void initializeDownload(){
        createWindow();
    }

    // creates a list of packets
    public void createPacketList(byte[] item){
        byte[] newPacket = new byte[10];
        int j = 0;
        int seqNum=1;
        for(int i =0; i<item.length; i++){
            if(i!=0 && i%10==0){
                ByteString st = ByteString.copyFrom(newPacket);
                DataServerMessages.Data data = DataServerMessages.Data.newBuilder()
                        .setSeqNo(seqNum).setData(st).setType(DataServerMessages.Data.packetType.DATA).build();
                seqNum++;
                packets.add(data);
                newPacket = new byte[10];
                j=0;
            }
            newPacket[j] = item[i];
            j++;
            if( i==item.length-1){
                ByteString st = ByteString.copyFrom(newPacket);
                DataServerMessages.Data data = DataServerMessages.Data.newBuilder().setSeqNo(seqNum).setType(DataServerMessages.Data.packetType.DATA).setData(st).setIsLast(true).build();
                seqNum++;
                packets.add(data);
            }
        }
        System.out.println("S- created packetlist size: "+ packets.size());
    }
    //setus up window
    public void createWindow(){
        try {
            DataServerMessages.History history = DataServerMessages.History.newBuilder().addAllHistory(chatData.returnMessageList()).build();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);
            history.writeDelimitedTo(outStream);
            byte[] item = outStream.toByteArray();
            createPacketList(item);
            sendFirstSet();

        } catch (SocketException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //starts the window
    private void sendFirstSet()throws IOException{
        int len = 4;
        if (packets.size() < len){
            len = packets.size();
        }
        firstSeqNum = 0;
        for(packetNum = 0; packetNum<len; packetNum++){
            DataServerMessages.Data packet = packets.get(packetNum);
            sendPacket(packet);
        }
    }

    //setup up the sending of packts
    private void sendPacket(DataServerMessages.Data packet) throws IOException{
        Random random = new Random();
        int rand = random.nextInt(100)+1;
        if(rand <25){
            System.out.println("S- ======Did not send: "+packet.getSeqNo());
        }else {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);
            packet.writeDelimitedTo(outStream);
            byte[] item = outStream.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(item, item.length, ip, otherUdpPort);
            socket.send(datagramPacket);
        }
    }

    //moves the window to new packets
    private void moveWindow(int lastAck) {
        System.out.println("S- Got ack number: "+ seqNum+ " resetting timer");
        packetNum = seqNum + 4;
        if((packets.size()-seqNum+1) < seqNum +5 ){
            packetNum = (packets.size()-(seqNum+1));
        }
        for(int i = seqNum+1; i<packetNum; i++){
            DataServerMessages.Data packet = packets.get(packetNum);
            try {
                sendPacket(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        firstSeqNum = lastAck;
    }

    // resends the packets when ack isnt received
    private void resendWindow() {
        int i = firstSeqNum;
        if(i > 0){
            i--;
        }
        for(i = firstSeqNum; i<packetNum; i++){

            DataServerMessages.Data packet = packets.get(i);
            try {
                sendPacket(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void run() {
        synchronized (this){
            int reset=0;
            while(reset!=7 || !packets.isEmpty()){
                try{
                    this.wait(1000);
                    if(ackStack.isEmpty()){
                        resendWindow();
                        reset++;

                    }else{
                        DataServerMessages.Data p = ackStack.pop();
                        if(!p.getIsLast()){
                            moveWindow(p.getSeqNo());
                        }else {
                            finish();
                        }
                    }

                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
    private void finish(){
        this.parent.deRegisterSender(this);
    }
}

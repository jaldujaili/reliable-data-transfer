package cs682;

import com.google.protobuf.ByteString;

import model.ChatData;
import model.DataServerMessages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by jordan on 2/16/18.
 */
public class UDPReceiver implements PacketListener, Runnable{

    private ConcurrentLinkedQueue<DataServerMessages.Data> packetQueue;
    private PacketHandler parent;
    private boolean isLast = false;
    private int expectedPac = 1;
    private InetAddress address;
    private int port;
    private DatagramSocket socket;
    private ByteString bstring = ByteString.EMPTY;
    private ChatData cd;

    public UDPReceiver(PacketHandler parent, ChatData cd, InetAddress address, int port, DatagramSocket socket){
        this.packetQueue = new ConcurrentLinkedQueue();
        this.parent = parent;
        this.cd = cd;
        this.address = address;
        this.port = port;
        this.socket = socket;
    }

    // syn to add packet
    public void addPacket(DataServerMessages.Data packet){
        synchronized (this) {
            System.out.println("R- expected: "+expectedPac+" got: "+packet.getSeqNo());
            if(expectedPac == packet.getSeqNo()){
                packetQueue.add(packet);
                sendAck(packet);
                expectedPac++;
                this.notify();
            }

        }
    }
    // sends ack by converting to bytearray
    private void sendAck(DataServerMessages.Data packet) {
        System.out.println("R- sending ack: "+packet.getSeqNo()+ " last: "+packet.getIsLast());
        DataServerMessages.Data request;//for sending a request
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);
        request = DataServerMessages.Data.newBuilder().setType(DataServerMessages.Data.packetType.ACK)
                .setSeqNo(packet.getSeqNo()).setIsLast(packet.getIsLast()).build();
        try {
            request.writeDelimitedTo(outStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] item = outStream.toByteArray();
        DatagramPacket datagramPacket = new DatagramPacket(item, item.length, address, port);
        try {
            Random random = new Random();
            int rand = random.nextInt(100)+1;
            if(rand < 10){
                System.out.println("R- ======Did not send ack: "+packet.getSeqNo());
            }else{
                socket.send(datagramPacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        synchronized (this){
            while(!isLast) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (!packetQueue.isEmpty()) {
                    DataServerMessages.Data pac = packetQueue.poll();
                    if(pac.getIsLast()){
                        isLast = true;
                    }
                    ByteString packet = pac.getData();
                    bstring = bstring.concat(packet);
                }
            }
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bstring.toByteArray());
                DataServerMessages.History history = DataServerMessages.History.parseDelimitedFrom(inputStream);
                List<DataServerMessages.Chat> tList = history.getHistoryList();
                cd.replaceHistory(tList);
            } catch (IOException e) {
                e.printStackTrace();
            }
            finish();
        }
    }
    private void finish(){
        this.parent.deRegisterReviever(this);
    }

}

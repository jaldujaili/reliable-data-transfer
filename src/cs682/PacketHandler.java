package cs682;

import model.ChatData;
import model.DataServerMessages;
import model.User;

import javax.sound.midi.Receiver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jordan on 2/20/18.
 */
public class PacketHandler implements Runnable{
    Map<String, UDPReceiver> receiverMap;
    Map<String, UDPSender> senderMap;
    int myPort;
    int port;
    InetAddress address;
    ChatData chatData;
    DatagramSocket socket;

    public PacketHandler(int udpPort, ChatData chatData){
        this.receiverMap = new HashMap<>();
        this.senderMap = new HashMap<>();
        this.myPort = udpPort;
        this.chatData = chatData;
        try {
            this.socket = new DatagramSocket(myPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] data = new byte[1024];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        boolean running = true;

        while(running){
            try{
                socket.receive(packet);
                address = packet.getAddress();
                port = packet.getPort();
                String addressStr = String.valueOf(address);
                String portStr = String.valueOf(port);
                byte[] rcvdData = packet.getData();
                ByteArrayInputStream instream = new ByteArrayInputStream(rcvdData);
                DataServerMessages.Data protopkt = DataServerMessages.Data.parseDelimitedFrom(instream);
                DataServerMessages.Data.packetType type = protopkt.getType();

                if(type == DataServerMessages.Data.packetType.REQUEST){
                    if(!senderMap.containsKey(addressStr+portStr) && !chatData.returnMessageList().isEmpty()) {
                        UDPSender s = new UDPSender(myPort, port, chatData, this, socket,address);
                        senderMap.put(addressStr + portStr, s);
                        Thread sThread = new Thread(s);
                        sThread.start();
                        s.initializeDownload();
                    }
                }else if(type == DataServerMessages.Data.packetType.ACK){
                    if(senderMap.containsKey(addressStr+portStr)){

                        senderMap.get(addressStr+portStr).ackReceived(protopkt);
                    }
                }else if(type == DataServerMessages.Data.packetType.DATA){
                    if(!receiverMap.containsKey(addressStr+portStr)){
                        UDPReceiver r = new UDPReceiver(this, chatData, address, port, socket);
                        Thread rThread = new Thread(r);
                        rThread.start();
                        r.addPacket(protopkt);
                        receiverMap.put(addressStr+portStr, r);

                    }
                    receiverMap.get(addressStr+portStr).addPacket(protopkt);
                }

            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    //sends the req  when user tries to download
    public void sendReq(User user) throws IOException {
        DataServerMessages.Data request = DataServerMessages.Data.newBuilder().setTypeValue(0).build();
        ByteArrayOutputStream outstream = new ByteArrayOutputStream(1024);
        request.writeDelimitedTo(outstream);

        byte[] item = outstream.toByteArray();
        InetAddress ipAddress = InetAddress.getByName(user.getIpaddress());
        int port = user.getUdpPort();

        DatagramPacket datagramPacket = new DatagramPacket(item, item.length, ipAddress, port);
        socket.send(datagramPacket);
    }
    public void deRegisterReviever(UDPReceiver recv){
        this.receiverMap.remove(recv);
        System.out.println("finsihed receiving");
    }
    public void deRegisterSender(UDPSender send){
        this.senderMap.remove(send);
        System.out.println("finsihed sending");
    }
}

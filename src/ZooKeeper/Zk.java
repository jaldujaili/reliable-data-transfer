package ZooKeeper;


import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;
import model.DataServerMessages;
import model.User;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jordan on 1/31/18.
 */
public class Zk {

    public static final int PORT = 2181;
    public static final String HOST = "mc01.cs.usfca.edu";

//    public static final int PORT = 3050;
//    public static final String HOST = "localhost";
//    private static User user;
    private static List<User> users = new ArrayList();
    private ZooKeeper zk;
    private String group="/CS682_Chat";
//    private String group="/zkjo";
    private String member;
    private DataServerMessages.ZKData data;


    public Zk(String name, DataServerMessages.ZKData zkData) {
        this.member = name;
        this.data = zkData;

    }


    public void connectToZk(){
        //Connect to ZK instance
        try {
            final CountDownLatch connectedSignal = new CountDownLatch(1);
            zk = new ZooKeeper(HOST + ":" + PORT, 1000, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        connectedSignal.countDown();
                    }
                }
            });
            System.out.println("Connecting to zookeeper...");
            connectedSignal.await();
            System.out.println("connected to zk");

        }catch(IOException e){
            e.printStackTrace();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public void createNewGroup(){
        //Create a new group
        //Note this will be completed for you for your project assignments!
        try {
            zk.create(group, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            System.out.println("Group " + group + " created");
        } catch(KeeperException ke) {
            System.out.println("Unable to create new group " + group + "...maybe it already exists?");
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public void joinGroup(){
        //to join a group
        try {
            String createdPath = zk.create(group + "/"+member,
//            String createdPath = zk.create(group +member,

                    data.toByteArray(),  //probably should be something more interesting here...
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL);
            System.out.println("Joined group " + group +"/"+ member);
//            populateGroup();
        } catch(KeeperException ke) {
            System.out.println("Unable to join group " + group + " as " + member);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public void populateGroup(){
        //to list members of a group
        try {
            List<String> children = zk.getChildren(group, false);
            for(String child: children) {
                String name = child;

                //get data about each child
                Stat s = new Stat();
                byte[] raw = zk.getData(group + "/" + child, false, s);
                if(raw != null) {
                    DataServerMessages.ZKData returnedData=null;
                    try {
                        returnedData = DataServerMessages.ZKData.parseFrom(raw);
                    } catch (InvalidProtocolBufferException e) {
//                        System.out.println("invalid buffer");
                    }
                    String ipAddress = "100.20.20.134";
                    int port=8080;

                    if(returnedData!=null){
                        ipAddress = returnedData.getIp();

                        Pattern p = Pattern.compile("^[0-9]{4}$");
                        Matcher m = p.matcher(returnedData.getPort());
                        boolean matches = m.matches();

                        if(matches){
                            port = Integer.parseInt(returnedData.getPort());
                        }
                    }


                    User newUser = new User(name,ipAddress,port);
                    if(returnedData!=null && returnedData.getUdpport()!=null && !returnedData.getUdpport().equals("")){
                        newUser.setUdpPort(Integer.parseInt(returnedData.getUdpport()));

                    }
                    users.add(newUser);
//                    System.out.println("-----");
//                    System.out.println(newUser.getName());
//                    System.out.println(newUser.getIpaddress());
//                    System.out.println(newUser.getPort());
                } else {
                    System.out.println("\tNO DATA");
                }
            }
        } catch(KeeperException ke) {
            System.out.println("Unable to list members of group " + group);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }


    public User checkUsername(String username){
        for(User user : users){
            if(username.equals(user.getName())){
                return user;
            }
        }
        return null;
    }

    public List<User> getUsers(){
        return users;
    }

}

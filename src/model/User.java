package model;

/**
 * Created by jordan on 2/4/18.
 */
public class User {
    private String name;
    private String ipaddress;
    private int port;
    private int udpPort;

    public User(String name, String ipaddress, int port) {

        this.name = name;
        this.ipaddress = ipaddress;
        this.port = port;

    }

    public String getName() {
        return name;
    }

    public int getUdpPort(){
        return udpPort;
    }
    public void setUdpPort(int port){
        this.udpPort = port;
    }
    public String getIpaddress() {
        return ipaddress;
    }

    public int getPort() {
        return port;
    }


    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", ipaddress='" + ipaddress + '\'' +
                ", port='" + port + '\'' +
                '}';
    }
}

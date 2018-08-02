package cs682;

import model.DataServerMessages;

/**
 * Created by jordan on 2/20/18.
 */
public interface PacketListener {

    public void addPacket(DataServerMessages.Data packet);
}

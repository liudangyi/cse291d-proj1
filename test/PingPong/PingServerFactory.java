package test.PingPong;

import rmi.Stub;
import java.net.InetSocketAddress;

/**
 * Created by dangyi on 2/2/17.
 */
public class PingServerFactory {
    static PingServer makePingServer() {
        return Stub.create(PingServer.class, new InetSocketAddress("server", 30000));
    }
}

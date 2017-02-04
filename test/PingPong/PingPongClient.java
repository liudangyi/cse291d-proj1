package test.PingPong;

import rmi.RMIException;
import rmi.Stub;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Created by dangyi on 2/2/17.
 */
public class PingPongClient {
    public static void main(String[] argv) throws UnknownHostException, RMIException {
        PingServerFactory pingServerFactory = Stub.create(PingServerFactory.class,
                new InetSocketAddress("server", 30000));
        PingServer pingServer = pingServerFactory.makePingServer();
        int fails = 0;

        for (int i = 0; i < 4; i++) {
            try {
                String pong = pingServer.ping(i);
                if (!pong.equals("Pong" + i)) {
                    throw new RMIException("Asset false");
                }
            } catch (RMIException e) {
                fails += 1;
                e.printStackTrace();
            }
        }
        System.out.println("4 Tests Completed, " + fails + " Tests Failed");
    }
}

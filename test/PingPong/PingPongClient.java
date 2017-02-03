package test.PingPong;

import rmi.RMIException;

/**
 * Created by dangyi on 2/2/17.
 */
public class PingPongClient {
    public static void main(String[] argv) {
        PingServer pingServer = PingServerFactory.makePingServer();
        int fails = 0;

        for (int i = 0; i < 4; i++) {
            try {
                pingServer.ping(i);
            } catch (RMIException e) {
                fails += 1;
                e.printStackTrace();
            }
        }
        System.out.println("4 Tests Completed, " + fails + " Tests Failed");
    }
}

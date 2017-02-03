package test.PingPong;

import rmi.RMIException;
import rmi.Skeleton;
import rmi.Stub;

import java.net.InetSocketAddress;

/**
 * Created by dangyi on 2/2/17.
 */
public class PingPongServer {

    private static class PingServerImp implements PingServer {
        @Override
        public String ping(int idNumber) throws RMIException {
            return "Pong" + idNumber;
        }
    }

    public static void main(String[] argv) {
        Skeleton<PingServer> skeleton = new Skeleton<PingServer>(
                PingServer.class,
                new PingServerImp(),
                new InetSocketAddress(30000)
        );
        try {
            skeleton.start();
        } catch (RMIException e) {
            e.printStackTrace();
        }
    }
}

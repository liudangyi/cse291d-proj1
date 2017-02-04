package test.PingPong;

import rmi.RMIException;
import rmi.Skeleton;
import rmi.Stub;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Created by dangyi on 2/2/17.
 */
public class PingPongServer {

    private static class PingServerImplementation implements PingServer {
        @Override
        public String ping(int idNumber) throws RMIException {
            System.out.println("ping called with " + idNumber);
            return "Pong" + idNumber;
        }
    }

    private static class PingServerFactoryImplementation implements PingServerFactory {
        @Override
        public PingServer makePingServer() throws RMIException, UnknownHostException {
            System.out.println("makePingServer called");
            Skeleton<PingServer> server = new Skeleton<>(PingServer.class, new PingServerImplementation());
            server.start();
            System.out.println("Create a pingServer on " + server.getAddress());
            return Stub.create(PingServer.class, server);
        }
    }

    public static void main(String[] argv) {
        Skeleton<PingServerFactory> skeleton = new Skeleton<>(
                PingServerFactory.class,
                new PingServerFactoryImplementation(),
                new InetSocketAddress(30000)
        );
        try {
            skeleton.start();
        } catch (RMIException e) {
            e.printStackTrace();
        }
    }
}

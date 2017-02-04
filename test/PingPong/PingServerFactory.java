package test.PingPong;

import rmi.RMIException;

import java.net.UnknownHostException;

/**
 * Created by dangyi on 2/4/17.
 */
public interface PingServerFactory {
    PingServer makePingServer() throws RMIException, UnknownHostException;
}

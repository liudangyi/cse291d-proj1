package test.PingPong;

import rmi.RMIException;

/**
 * Created by dangyi on 2/2/17.
 */
public interface PingServer {
    String ping(int idNumber) throws RMIException;
}

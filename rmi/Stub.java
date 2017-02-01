package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.UnknownHostException;

import com.sun.istack.internal.Nullable;
import rmi.Skeleton.*;

/**
 * RMI stub factory.
 * <p>
 * <p>
 * RMI stubs hide network communication with the remote server and provide a
 * simple object-like interface to their users. This class provides methods for
 * creating stub objects dynamically, when given pre-defined interfaces.
 * <p>
 * <p>
 * The network address of the remote server is set when a stub is created, and
 * may not be modified afterwards. Two stubs are equal if they implement the
 * same interface and carry the same remote server address - and would
 * therefore connect to the same skeleton. Stubs are serializable.
 */
public abstract class Stub {
    /**
     * Creates a stub, given a skeleton with an assigned address.
     * <p>
     * <p>
     * The stub is assigned the address of the skeleton. The skeleton must
     * either have been created with a fixed address, or else it must have
     * already been started.
     * <p>
     * <p>
     * This method should be used when the stub is created together with the
     * skeleton. The stub may then be transmitted over the network to enable
     * communication with the skeleton.
     *
     * @param c        A <code>Class</code> object representing the interface
     *                 implemented by the remote object.
     * @param skeleton The skeleton whose network address is to be used.
     * @return The stub created.
     * @throws IllegalStateException If the skeleton has not been assigned an
     *                               address by the user and has not yet been
     *                               started.
     * @throws UnknownHostException  When the skeleton address is a wildcard and
     *                               a port is assigned, but no address can be
     *                               found for the local host.
     * @throws NullPointerException  If any argument is <code>null</code>.
     * @throws Error                 If <code>c</code> does not represent a remote interface
     *                               - an interface in which each method is marked as throwing
     *                               <code>RMIException</code>, or if an object implementing
     *                               this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton) throws UnknownHostException {
        if (c == null || skeleton == null) {
            throw new NullPointerException();
        }

        InetSocketAddress address = skeleton.getAddress();
        if (address.getHostName().equals("0.0.0.0")) {
            String localHostName = InetAddress.getLocalHost().getHostName();
            address = new InetSocketAddress(localHostName, address.getPort());
        }
        return create(c, address);
    }

    /**
     * Creates a stub, given a skeleton with an assigned address and a hostname
     * which overrides the skeleton's hostname.
     * <p>
     * <p>
     * The stub is assigned the port of the skeleton and the given hostname.
     * The skeleton must either have been started with a fixed port, or else
     * it must have been started to receive a system-assigned port, for this
     * method to succeed.
     * <p>
     * <p>
     * This method should be used when the stub is created together with the
     * skeleton, but firewalls or private networks prevent the system from
     * automatically assigning a valid externally-routable address to the
     * skeleton. In this case, the creator of the stub has the option of
     * obtaining an externally-routable address by other means, and specifying
     * this hostname to this method.
     *
     * @param c        A <code>Class</code> object representing the interface
     *                 implemented by the remote object.
     * @param skeleton The skeleton whose port is to be used.
     * @param hostname The hostname with which the stub will be created.
     * @return The stub created.
     * @throws IllegalStateException If the skeleton has not been assigned a
     *                               port.
     * @throws NullPointerException  If any argument is <code>null</code>.
     * @throws Error                 If <code>c</code> does not represent a remote interface
     *                               - an interface in which each method is marked as throwing
     *                               <code>RMIException</code>, or if an object implementing
     *                               this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton, String hostname) {
        if (c == null || skeleton == null || hostname == null) {
            throw new NullPointerException();
        }

        return create(c, new InetSocketAddress(hostname, skeleton.getAddress().getPort()));
    }

    /**
     * Creates a stub, given the address of a remote server.
     * <p>
     * <p>
     * This method should be used primarily when bootstrapping RMI. In this
     * case, the server is already running on a remote host but there is
     * not necessarily a direct way to obtain an associated stub.
     *
     * @param c       A <code>Class</code> object representing the interface
     *                implemented by the remote object.
     * @param address The network address of the remote skeleton.
     * @return The stub created.
     * @throws NullPointerException If any argument is <code>null</code>.
     * @throws Error                If <code>c</code> does not represent a remote interface
     *                              - an interface in which each method is marked as throwing
     *                              <code>RMIException</code>, or if an object implementing
     *                              this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, InetSocketAddress address) {
        if (c == null || address == null) {
            throw new NullPointerException();
        }

        if (!c.isInterface()) {
            throw new Error(c.getName() + "is not an interface");
        }
        for (Method method : c.getMethods()) {
            boolean found = false;

            for (Class<?> exception : method.getExceptionTypes()) {
                if (exception == RMIException.class) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new Error();
            }
        }

        StubInvocationHandler handler = new StubInvocationHandler(c, address);

        return (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[]{c}, handler);
    }

    private static class StubInvocationHandler implements InvocationHandler {

        private InetSocketAddress address;
        private Class klass;

        StubInvocationHandler(Class c, InetSocketAddress address) {
            this.klass = c;
            this.address = address;
        }

        @Override
        public Object invoke(Object proxy, Method method, @Nullable Object[] args) throws Throwable {

            Response response;

            try {
                Socket socket = new Socket();
                socket.connect(address);

                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

                if (args == null) {
                    args = new Object[]{};
                }
                Request request = new Request(method.getName(), args, method.getParameterTypes());

                outputStream.writeObject(request);

                Object obj = inputStream.readObject();
                if (!(obj instanceof Response)) {
                    throw new RMIException("invalid response object");
                }

                response = (Response) obj;

            } catch (IOException e) {
                throw new RMIException(e);
            }

            if (response.status == Response.Status.NORMAL) {
                return response.result;
            } else {
                throw (Throwable) response.result;
            }
        }
    }
}

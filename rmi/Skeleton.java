package rmi;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * RMI skeleton
 * <p>
 * <p>
 * A skeleton encapsulates a multithreaded TCP server. The server's clients are
 * intended to be RMI stubs created using the <code>Stub</code> class.
 * <p>
 * <p>
 * The skeleton class is parametrized by a type variable. This type variable
 * should be instantiated with an interface. The skeleton will accept from the
 * stub requests for calls to the methods of this interface. It will then
 * forward those requests to an object. The object is specified when the
 * skeleton is constructed, and must implement the remote interface. Each
 * method in the interface should be marked as throwing
 * <code>RMIException</code>, in addition to any other exceptions that the user
 * desires.
 * <p>
 * <p>
 * Exceptions may occur at the top level in the listening and service threads.
 * The skeleton's response to these exceptions can be customized by deriving
 * a class from <code>Skeleton</code> and overriding <code>listen_error</code>
 * or <code>service_error</code>.
 */
public class Skeleton<T> {

    private Class<T> klass;
    private T object;
    private InetSocketAddress address;
    private boolean isRunning;
    private ServerSocket serverSocket;

    /**
     * Creates a <code>Skeleton</code> with no initial server address. The
     * address will be determined by the system when <code>start</code> is
     * called. Equivalent to using <code>Skeleton(null)</code>.
     * <p>
     * <p>
     * This constructor is for skeletons that will not be used for
     * bootstrapping RMI - those that therefore do not require a well-known
     * port.
     *
     * @param c      An object representing the class of the interface for which the
     *               skeleton server is to handle method call requests.
     * @param server An object implementing said interface. Requests for method
     *               calls are forwarded by the skeleton to this object.
     * @throws Error                If <code>c</code> does not represent a remote interface -
     *                              an interface whose methods are all marked as throwing
     *                              <code>RMIException</code>.
     * @throws NullPointerException If either of <code>c</code> or
     *                              <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server) throws Error, NullPointerException {
        this(c, server, null);
    }

    /**
     * Creates a <code>Skeleton</code> with the given initial server address.
     * <p>
     * <p>
     * This constructor should be used when the port number is significant.
     *
     * @param c       An object representing the class of the interface for which the
     *                skeleton server is to handle method call requests.
     * @param server  An object implementing said interface. Requests for method
     *                calls are forwarded by the skeleton to this object.
     * @param address The address at which the skeleton is to run. If
     *                <code>null</code>, the address will be chosen by the
     *                system when <code>start</code> is called.
     * @throws Error                If <code>c</code> does not represent a remote interface -
     *                              an interface whose methods are all marked as throwing
     *                              <code>RMIException</code>.
     * @throws NullPointerException If either of <code>c</code> or
     *                              <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server, InetSocketAddress address) {
        if (c == null || server == null) {
            throw new NullPointerException();
        }
        if (!c.isInterface()) {
            throw new Error();
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

        klass = c;
        object = server;
        isRunning = false;
        this.address = address;
    }

    /**
     * Called when the listening thread exits.
     * <p>
     * <p>
     * The listening thread may exit due to a top-level exception, or due to a
     * call to <code>stop</code>.
     * <p>
     * <p>
     * When this method is called, the calling thread owns the lock on the
     * <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
     * calling <code>start</code> or <code>stop</code> from different threads
     * during this call.
     * <p>
     * <p>
     * The default implementation does nothing.
     *
     * @param cause The exception that stopped the skeleton, or
     *              <code>null</code> if the skeleton stopped normally.
     */
    protected void stopped(Throwable cause) {
    }

    /**
     * Called when an exception occurs at the top level in the listening
     * thread.
     * <p>
     * <p>
     * The intent of this method is to allow the user to report exceptions in
     * the listening thread to another thread, by a mechanism of the user's
     * choosing. The user may also ignore the exceptions. The default
     * implementation simply stops the server. The user should not use this
     * method to stop the skeleton. The exception will again be provided as the
     * argument to <code>stopped</code>, which will be called later.
     *
     * @param exception The exception that occurred.
     * @return <code>true</code> if the server is to resume accepting
     * connections, <code>false</code> if the server is to shut down.
     */
    protected boolean listen_error(Exception exception) {
        return false;
    }

    /**
     * Called when an exception occurs at the top level in a service thread.
     * <p>
     * <p>
     * The default implementation does nothing.
     *
     * @param exception The exception that occurred.
     */
    protected void service_error(RMIException exception) {
    }

    /**
     * Starts the skeleton server.
     * <p>
     * <p>
     * A thread is created to listen for connection requests, and the method
     * returns immediately. Additional threads are created when connections are
     * accepted. The network address used for the server is determined by which
     * constructor was used to create the <code>Skeleton</code> object.
     *
     * @throws RMIException When the listening socket cannot be created or
     *                      bound, when the listening thread cannot be created,
     *                      or when the server has already been started and has
     *                      not since stopped.
     */
    public synchronized void start() throws RMIException {
        if (isRunning)
            throw new RMIException("server is already running");

        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(address);
            isRunning = true;

            // If address is null, we're assigned a random port. We'll save the port
            // number in address in case we restart (and still use the same port).
            if (address == null) {
                address = new InetSocketAddress(serverSocket.getLocalPort());
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Executor executor = Executors.newCachedThreadPool();

                    while (isRunning) try {
                        Socket socket = serverSocket.accept();
                        executor.execute(new RequestHandler(socket, Skeleton.this));
                    } catch (IOException e) {
                        if (isRunning && !listen_error(e)) {
                            stop();
                            stopped(e);
                            return;
                        }
                    }
                    stopped(null);
                }
            }).start();
        } catch (IOException e) {
            throw new RMIException("IOException: " + e.getMessage());
        }
    }

    /**
     * Stops the skeleton server, if it is already running.
     * <p>
     * <p>
     * The listening thread terminates. Threads created to service connections
     * may continue running until their invocations of the <code>service</code>
     * method return. The server stops at some later time; the method
     * <code>stopped</code> is called at that point. The server may then be
     * restarted.
     */
    public synchronized void stop() {
        if (!isRunning)
            return;

        try {
            serverSocket.close();
            isRunning = false;
        } catch (IOException e) {
            throw new RuntimeException("close server socket error");
        }
    }

    static class Request implements Serializable {
        String methodName;
        List<Object> arguments;
        List<Class> argumentTypes;

        Request(String methodName, Object[] arguments, Class[] argumentTypes) {
            if (arguments == null) {
                arguments = new Objects[]{};
            }
            this.methodName = methodName;
            this.arguments = Arrays.asList(arguments);
            this.argumentTypes = Arrays.asList(argumentTypes);
        }
    }

    static class Response implements Serializable {
        enum Status {
            NORMAL, EXCEPTION, ERROR
        }

        Status status;
        Object result;

        Response(Status status, Object result) {
            this.status = status;
            this.result = result;
        }
    }

    private static class RequestHandler implements Runnable {

        private final Socket socket;
        private final Skeleton<?> skeleton;

        RequestHandler(Socket socket, Skeleton<?> skeleton) {
            this.socket = socket;
            this.skeleton = skeleton;
        }

        @Override
        public void run() {
            try {
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                Response response;

                try {
                    Object obj = inputStream.readObject();
                    if (!(obj instanceof Skeleton.Request)) {
                        throw new RMIException("invalid request object");
                    }

                    Request request = (Request) obj;
                    Object[] args = request.arguments.toArray();
                    Class[] argTypes = (Class[]) request.argumentTypes.toArray();

                    Method method = skeleton.klass.getMethod(request.methodName, argTypes);
                    try {
                        Object result = method.invoke(skeleton.object, args);
                        response = new Response(Response.Status.NORMAL, result);
                    } catch (InvocationTargetException e) {
                        response = new Response(Response.Status.EXCEPTION, e.getTargetException());
                    }

                } catch (ClassNotFoundException | RMIException | NoSuchMethodException | IllegalAccessException e) {
                    RMIException rmiException = new RMIException(e);
                    response = new Response(Response.Status.ERROR, rmiException);
                    skeleton.service_error(rmiException);
                }
                outputStream.writeObject(response);
                socket.close();
            } catch (IOException e) {
                // We just ignore any IOException since it's unlikely that we can inform the client of this.
                // In most cases, the client will also get an IOException.
                // e.printStackTrace();
                skeleton.service_error(new RMIException(e));
            }

        }

    }

    public InetSocketAddress getAddress() {
        if (address == null)
            throw new IllegalStateException();

        return address;
    }

}

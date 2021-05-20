package ElectronicAssetTradingPlatform.Server;

import ElectronicAssetTradingPlatform.Database.DBConnectivity;
import ElectronicAssetTradingPlatform.Database.UsersDataSource;
import ElectronicAssetTradingPlatform.Users.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkServer {
    private static final int PORT = 10000;
    /**
     * this is the timeout inbetween accepting clients, not reading from the socket itself.
     */
    private static final int SOCKET_ACCEPT_TIMEOUT = 100;

    /**
     * This timeout is used for the actual clients.
     */
    private static final int SOCKET_READ_TIMEOUT = 5000;

    private AtomicBoolean running = new AtomicBoolean(true);

    /**
     * The connection to the database where everything is stored.
     */
    private Connection database;

    // Exception codes: https://sqlite.org/rescode.html
    private static final int UNIQUE_CONSTRAINT_EXCEPTION_CODE = 19;

    /**
     * Returns the port the server is configured to use
     *
     * @return The port number
     */
    public static int getPort() {
        return PORT;
    }

    /**
     * Starts the server running on the default port
     */
    public void start() throws IOException {
        // Connect to the database
        database = DBConnectivity.getInstance();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setSoTimeout(SOCKET_ACCEPT_TIMEOUT);
            // The server is no longer running
            while (running.get()) {
                try {
                    final Socket socket = serverSocket.accept();
                    socket.setSoTimeout(SOCKET_READ_TIMEOUT);

                    // We have a new connection from a client. Use a runnable and thread to handle
                    // the client. The lambda here wraps the functional interface (Runnable).
                    final Thread clientThread = new Thread(() -> {
                        try {
                            handleConnection(socket);
                        } catch (IOException | ClassNotFoundException e) {
                            // We will report an IOException by printing the stack trace, but we
                            // will not shut down the server. An IOException can happen due to a
                            // client malfunction (or malicious client)
                            e.printStackTrace();
                        }
                    });
                    clientThread.start();
                } catch (SocketTimeoutException ignored) {
                    // Do nothing. A timeout is normal- we just want the socket to
                    // occasionally timeout so we can check if the server is still running
                }
            }
        } catch (IOException e) {
            // If we get an error starting up, show an error dialog then exit
            e.printStackTrace();
            System.exit(1);
        }

        // Close down the server
        System.exit(0);
    }

    /**
     * Handles the connection received from ServerSocket
     * @param socket The socket used to communicate with the currently connected client
     */
    private void handleConnection(Socket socket) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream =
                        new ObjectInputStream(socket.getInputStream())) {
            // Read: Command, and parameter object
            NetworkCommands command = (NetworkCommands) objectInputStream.readObject();
            try {
                /*
                 * Remember this is happening on separate threads for each client. Therefore access to the database
                 * must be thread-safe in some way. The easiest way to achieve thread safety is to just put a giant
                 * lock around all database operations, in this case with a synchronized block on the database object.
                 */
                switch (command) {
                    case RETRIEVE_USER -> {
                        // Get input
                        String username = (String) objectInputStream.readObject();

                        User out;
                        synchronized (database) {
                            out = UsersDataSource.getInstance().getUser(username);
                        }

                        // Write output
                        try (
                                ObjectOutputStream objectOutputStream =
                                        new ObjectOutputStream(socket.getOutputStream())
                        ) {
                            objectOutputStream.writeObject(out);
                        }
                    }
                    case STORE_USER -> {
                        // Get input
                        User user = (User) objectInputStream.readObject();

                        synchronized (database) {
                            // Save to db
                            UsersDataSource.getInstance().insertUser(user);
                        }

                        // Write success output
                        try (
                                ObjectOutputStream objectOutputStream =
                                        new ObjectOutputStream(socket.getOutputStream())
                        ) {
                            objectOutputStream.writeObject("Added user.");
                        }
                    }
                }
            } catch (SQLException e) {
                // Write error output
                try (
                        ObjectOutputStream objectOutputStream =
                                new ObjectOutputStream(socket.getOutputStream())
                ) {
                    if (e.getErrorCode() == UNIQUE_CONSTRAINT_EXCEPTION_CODE)
                        objectOutputStream.writeObject("It already exists.");
                    else objectOutputStream.writeObject("It could not be found.");
                }
            } catch (User.UserTypeException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Requests the server to shut down
     */
    public void shutdown() {
        // Shut the server down
        running.set(false);
    }

}

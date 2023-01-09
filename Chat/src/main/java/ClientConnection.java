import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientConnection implements Runnable{
    private static Socket clientSocket = null;
    private static PrintStream outputStream = null;
    private static BufferedReader inputStream = null;
    private static BufferedReader inputLine = null;
    private static boolean closed = false;

    public static void connectClient() {

        int port = 8080;
        String host = "localhost";

        try {
            clientSocket = new Socket(host, port);
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            outputStream = new PrintStream(clientSocket.getOutputStream());
            inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Host not found " + host);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        if (clientSocket != null && outputStream != null && inputStream != null) {
            try {

                new Thread(new ClientConnection()).start();
                while (!closed) {
                    outputStream.println(inputLine.readLine().trim());
                }
                outputStream.close();
                inputStream.close();
                clientSocket.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }
    public void run() {
        String input;
        try {
            while ((input = inputStream.readLine()) != null) {
                System.out.println(input);
            }
            closed = true;
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}

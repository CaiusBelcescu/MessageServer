import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {
    private ServerSocket serverSocket;
    public Server(ServerSocket serverSocket){
        this.serverSocket=serverSocket;
    }
    public void startServer(){
        try{
            while(!serverSocket.isClosed()){
                Socket socket= serverSocket.accept();
                System.out.println("A client has connected!");
                ClientHandler clientHandler = new ClientHandler(socket);

                Thread thread=new Thread(clientHandler);
                thread.start();
            }
        }catch(IOException e){

        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket =new ServerSocket(1234);
        Server server = new Server(serverSocket);
        System.out.println("Server is running!");
        server.startServer();
    }
}

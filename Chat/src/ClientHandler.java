import javax.imageio.IIOException;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable{

    public static ArrayList<ClientHandler> clientHandlers= new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            clientHandlers.add(this);
            broadcastMessage("Sever: " + clientUsername + "has entered the chat");
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()){
            try{
                messageFromClient  = bufferedReader.readLine();
                if(messageFromClient.contains("->")) {
                    String[] stringToSend = messageFromClient.split("->");
                    messageFromClient  = stringToSend[1];
                    String nameToSend =  stringToSend[0];
                    String[] nameToSend2 = nameToSend.split(":");
                    nameToSend = nameToSend2[1];

                    broadcastMessage(messageFromClient,nameToSend);
                }else{
                    broadcastMessage(messageFromClient);
                }
            }catch (IOException e){
                //closeEverything(socket,bufferedReader,bufferedWriter);
                break;
            }
        }
    }

    public void broadcastMessage(String messageToSend, String nameToSend) {
        if(messageToSend == null) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }else {
            for( ClientHandler clientHandler : clientHandlers){
                try {
                    if(clientHandler.clientUsername.equals(nameToSend.trim())){
                        messageToSend = "Private message from " + clientUsername + ":" + messageToSend;
                        clientHandler.bufferedWriter.write(messageToSend);
                        clientHandler.bufferedWriter.newLine();
                        clientHandler.bufferedWriter.flush();
                    }
                }catch (IOException e){
                    closeEverything(socket,bufferedReader,bufferedWriter);
                }
            }
        }
    }

    public void broadcastMessage(String messageToSend) {
        if(messageToSend == null) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }else {
            for( ClientHandler clientHandler : clientHandlers){
                try {
                    if(!clientHandler.clientUsername.equals(clientUsername)){
                        clientHandler.bufferedWriter.write(messageToSend);
                        clientHandler.bufferedWriter.newLine();
                        clientHandler.bufferedWriter.flush();
                    }
                }catch (IOException e){
                    closeEverything(socket,bufferedReader,bufferedWriter);
                }
            }
        }
    }

    public void removeClientHandler(){
        clientHandlers.remove(this);
        broadcastMessage("Server: "+ clientUsername +" has left the chat!");
        //clientHandlers.remove(this);
    }

    public void closeEverything(Socket socket,BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        removeClientHandler();
        try {
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(bufferedWriter != null){
                bufferedWriter.close();
            }
            if(socket != null){
                socket.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}

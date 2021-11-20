package com.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{
    
    ArrayList<ConnectionHandler> connections;
    ServerSocket server;
    boolean done;
    ExecutorService pool;

    public Server(){

        connections = new ArrayList<>();
        done = false;

    }

    @Override
    public void run() {
        
        try {

            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();

            while(!done){
            
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }

        } catch (IOException e) {
            shutdown();
        }
        
    }

    public void broadcast(String message, String nickClient){

        for(ConnectionHandler connectionHandler: connections){

            if(connectionHandler.nickname != null && connectionHandler.nickname != nickClient){

                    connectionHandler.sendMessage(message);
            }
        
        }
    }

    public void invioAlSingolo(String nomeDestinatario, String messaggio, String nomeMittente){

        boolean verifica = false;
        for(ConnectionHandler connectionHandler: connections){

            if(connectionHandler.nickname != null && connectionHandler.nickname.equals(nomeDestinatario)){

                    connectionHandler.sendMessage(nomeMittente + ": " + messaggio);
                    verifica = true;
            }
        
        }
        
        if(!verifica){

            for(ConnectionHandler connectionHandler: connections){

                if(connectionHandler.nickname != null && connectionHandler.nickname.equals(nomeMittente)){
    
                        connectionHandler.sendMessage("Ci dispiace il client che stai cercando di raggiungere non esiste.\n");
                        listaCLientConessi(connectionHandler.nickname);
                }
            
            }
        }
        
    } 

    public boolean nomeGiaEsistente(String nick){

        for(int i = 0; i < connections.size() - 1; i++){

           if(connections.get(i).nickname.equals(nick)){

                return true;
           }
        }

        return false;
    }

    public void shutdown(){

        try{

            done = true;
            pool.shutdown();
            if(!server.isClosed()){

                server.close();
            }

            for(ConnectionHandler connectionHandler: connections){

                connectionHandler.shutdown();
            }

        }catch(IOException e){


        }
    }
    
    public void listaCLientConessi(String nick){

        String lista = "Lista client connessi: ";

        for(ConnectionHandler connectionHandler: connections){

            if(connectionHandler.nickname != null){

                lista += connectionHandler.nickname + ", ";
            }

        }
        
        for(ConnectionHandler connectionHandler: connections){

            if(connectionHandler.nickname != null){

                if(connectionHandler.nickname.equals(nick)){

                    connectionHandler.sendMessage(lista);
                }
            }

        }
    }

    class ConnectionHandler implements Runnable{
    
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
    
        public ConnectionHandler(Socket client){
    
            this.client = client;
            
        }
    
        @Override
        public void run(){
    
            try {
                
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                
                
                do {
                    
                    out.println("Please enter a nickname: ");
                    nickname = in.readLine();

                    if(nomeGiaEsistente(nickname)){

                        out.print("Il nome è già esistente\n");
                    }

                } while (nomeGiaEsistente(nickname));

                System.out.println(nickname + " connected!");
                broadcast(nickname + " joined the chat!", nickname);

                listaCLientConessi(nickname);

                String message;
                while((message = in.readLine()) != null ){
    
                    if(message.startsWith("/nick ")){
    
                        String[] messageSplit = message.split(" ", 2);
                        if(messageSplit.length == 2){
    
                            broadcast(nickname + " renamed themselves to " + messageSplit[1], nickname);
                            System.out.println(nickname + " renamed themselves to " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Succesfully changed nickname to " + nickname);
                        
                        }else{
    
                            out.println("No nickname provided!");
                        }
    
                    }else if(message.startsWith("/list")){

                        listaCLientConessi(nickname);

                    }else if(message.startsWith("@")){

                        String nomeDestinatario = message.substring(1, message.indexOf(" "));
                        String messaggioDaInviare = message.substring(message.indexOf(" "));
                        invioAlSingolo(nomeDestinatario, messaggioDaInviare, nickname);

                    }else if(message.startsWith("/quit")){
    
                        System.out.println(nickname + " left the chat!");
                        broadcast(nickname + " left the chat!", nickname);
                        shutdown();
                        connections.remove(this);

                    }else if(connections.size() == 1){

                        connections.get(0).out.println("Sei da solo, ci dispiace!");
                    }else{
    
                        broadcast(nickname + ": " + message, nickname);
                    }
                }
    
            } catch (IOException e) {
                shutdown();
            }
        }
    
        public void sendMessage(String message){
            
            out.println(message);
        }
    
        public void shutdown(){
    
            try{
                in.close();
                out.close();
    
                if(!client.isClosed()){
    
                    client.close();
                }
    
            }catch(IOException e){
    
    
            }
            
        }
    
    }
    
   public static void main(String[] args) {
        
        Server server = new Server();
        server.run();
    } 
}

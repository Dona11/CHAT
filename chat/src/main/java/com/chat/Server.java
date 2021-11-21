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
    
    ArrayList<ConnectionHandler> clientConnessi;
    ServerSocket server;
    boolean done;
    ExecutorService pool;

    public Server(){

        clientConnessi = new ArrayList<>();
        done = false;

    }

    @Override
    public void run() {
        
        try {

            server = new ServerSocket(5678);
            pool = Executors.newCachedThreadPool();

            while(!done){
            
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                clientConnessi.add(handler);
                pool.execute(handler);
            }

        } catch (IOException e) {
            chiudi();
        }
        
    }

    public void messaggioTutti(String messaggio, String nickClient){

        for(ConnectionHandler connectionHandler: clientConnessi){

            if(connectionHandler.nickname != null && connectionHandler.nickname != nickClient){

                    connectionHandler.inviaMessaggio(messaggio);
            }
        
        }
    }

    public void messaggioSingolo(String nomeDestinatario, String messaggio, String nomeMittente){

        boolean verifica = false;
        for(ConnectionHandler connectionHandler: clientConnessi){

            if(connectionHandler.nickname != null && connectionHandler.nickname.equals(nomeDestinatario)){

                    connectionHandler.inviaMessaggio(nomeMittente + ": " + messaggio);
                    verifica = true;
            }
        
        }
        
        if(!verifica){

            for(ConnectionHandler connectionHandler: clientConnessi){

                if(connectionHandler.nickname != null && connectionHandler.nickname.equals(nomeMittente)){
    
                        connectionHandler.inviaMessaggio("Ci dispiace il client che stai cercando di raggiungere non esiste.\n");
                        listaCLientConessi(connectionHandler.nickname);
                }
            
            }
        }
        
    } 

    public boolean contolloNome(String nick){

        for(int i = 0; i < clientConnessi.size(); i++){

           if(clientConnessi.get(i).nickname.equals(nick)){

                return true;
           }
        }

        return false;
    }

    public boolean nomeGiaEsistente(String nick){

        for(int i = 0; i < clientConnessi.size() - 1; i++){

           if(clientConnessi.get(i).nickname.equals(nick)){

                return true;
           }
        }

        return false;
    }

    public void chiudi(){

        try{

            done = true;
            pool.shutdown();;
            if(!server.isClosed()){

                server.close();
            }

            for(ConnectionHandler connectionHandler: clientConnessi){

                connectionHandler.chiudi();
            }

        }catch(IOException e){

        }
    }
    
    public void listaDeiComandi(String nick){

        String comandi = "Digita: /nome + il tuo nuovo nickname per cambiare il tuo attuale\nDigita: /listaComandi per visualizzare tutti i comandi\nDigita: /listaClienti per visualizzare tutti i client connessi\nDigita: /esci per uscire dalla chat\nDigita: @+nickname se vuoi mandare un messaggio privato a qualcuno\nDigita: /mta+messaggio per mandare un mesagio a tutti";
        
        for(ConnectionHandler connectionHandler: clientConnessi){

            if(connectionHandler.nickname != null){

                if(connectionHandler.nickname.equals(nick)){

                    connectionHandler.inviaMessaggio(comandi);
                }
            }

        }
    }

    public void listaCLientConessi(String nick){

        String lista = "Lista client connessi: ";

        for(ConnectionHandler connectionHandler: clientConnessi){

            if(connectionHandler.nickname != null){

                lista += connectionHandler.nickname + ", ";
            }

        }
        
        for(ConnectionHandler connectionHandler: clientConnessi){

            if(connectionHandler.nickname != null){

                if(connectionHandler.nickname.equals(nick)){

                    connectionHandler.inviaMessaggio(lista);
                }
            }

        }
    }

    class ConnectionHandler implements Runnable{
    
        private Socket client;
        private BufferedReader input;
        private PrintWriter output;
        private String nickname;
    
        public ConnectionHandler(Socket client){
    
            this.client = client;
            
        }
    
        @Override
        public void run(){
    
            try {
                
                output = new PrintWriter(client.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                
                
                do {
                    
                    output.println("Inserisci un nickname: ");
                    nickname = input.readLine();

                    if(nomeGiaEsistente(nickname)){

                        output.print("Il nome è già esistente\n");
                    }

                } while (nomeGiaEsistente(nickname));

                System.out.println(nickname + " si è connesso!");
                messaggioTutti(nickname + " si è unito alla chat!", nickname);

                listaCLientConessi(nickname);

                String messaggio;
                while((messaggio = input.readLine()) != null ){
    
                    if(messaggio.startsWith("/nome ")){

                        String[] messaggioDiviso = messaggio.split(" ", 2);

                        if(contolloNome(messaggioDiviso[1])){

                            output.println("Nome già utilizzato, reinserire il comando con un altro nome");

                        }else if(messaggioDiviso.length == 2 && messaggioDiviso[1] != ""){
    
                            messaggioTutti(nickname + " ha cambiato nome in " + messaggioDiviso[1], nickname);
                            System.out.println(nickname + " ha cambiato nome in " + messaggioDiviso[1]);
                            nickname = messaggioDiviso[1];
                            output.println("Nome correttamente cambiato in " + nickname);
                        
                        }else{
    
                            output.println("Nessun nickname inserito!");
                        }

                    }else if(messaggio.startsWith("/listaComandi")){

                        listaDeiComandi(nickname);

                    }else if(messaggio.startsWith("/listaClient")){

                        listaCLientConessi(nickname);

                    }else if(messaggio.startsWith("@")){

                        String nomeDestinatario = messaggio.substring(1, messaggio.indexOf(" "));
                        String messaggioDaInviare = messaggio.substring(messaggio.indexOf(" "));
                        messaggioSingolo(nomeDestinatario, messaggioDaInviare, nickname);

                    }else if(messaggio.startsWith("/esci")){
    
                        System.out.println(nickname + " è uscito dalla chat!");
                        messaggioTutti(nickname + " è uscito dalla chat!", nickname);
                        chiudi();
                        clientConnessi.remove(this);

                    }else if(clientConnessi.size() == 1){

                        clientConnessi.get(0).output.println("Sei da solo, ci dispiace!");

                    }else if(messaggio.startsWith("/mta ")){
    
                        String[] messaggioDiviso = messaggio.split(" ", 2);

                        messaggioTutti(nickname + ": " + messaggioDiviso[1], nickname);

                    }else{

                        output.println("Comando non disponibile");
                        listaDeiComandi(nickname);
                    }
                }
    
            } catch (IOException e) {
                chiudi();
            }
        }
    
        public void inviaMessaggio(String message){
            
            output.println(message);
        }
    
        public void chiudi(){
    
            try{
                input.close();
                output.close();
    
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

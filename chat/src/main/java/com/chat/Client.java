package com.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable{

    Socket client;
    BufferedReader input;
    PrintWriter output;
    boolean done;


    @Override
    public void run(){

        try{

            client = new Socket("localhost", 5678);
            output = new PrintWriter(client.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(client.getInputStream()));
            

            InputHandler inputHandler = new InputHandler();
            Thread t = new Thread(inputHandler);
            t.start();

            String messaggio;
            while((messaggio = input.readLine()) != null){

                System.out.println(messaggio);
            }
        }catch(IOException e){
            chiudi();
        }
    }

    public void chiudi(){

        done = true;
        try {
            
            input.close();
            output.close();

            if(!client.isClosed()){
                client.close();
            }

        } catch (IOException e) {
            
        }
    }

    class InputHandler implements Runnable{

    
        @Override
        public void run(){
    
            try{
    
                BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
    
                while(!done){
    
                    String messaggio = buffer.readLine();
                    
                    if(messaggio.equals("/quit")){
                        output.println(messaggio);
                        buffer.close();
                        chiudi();
    
                    }else{
    
                        output.println(messaggio);
                    }
                }
    
            }catch(IOException e){
    
                chiudi();
            }
        }
    
        
    }

    public static void main(String[] args){

        Client client = new Client();
        client.run();
    }
}


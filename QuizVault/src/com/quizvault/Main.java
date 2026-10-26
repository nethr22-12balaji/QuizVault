package com.quizvault;

import com.quizvault.web.QuizWebServer;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Starting QuizVault Web Server...");
            QuizWebServer.startServer(8080);
            System.out.println("Server is running!");
            System.out.println("To play, open a web browser and go to: http://localhost:8080");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
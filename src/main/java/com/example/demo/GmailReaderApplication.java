package com.example.demo;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Collections;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;

@SpringBootApplication
public class GmailReaderApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(GmailReaderApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting mail Reader...");
        
        // Setup Gmail service
        Gmail gmail = getGmailService();
        
        // Get last 200 emails
        var messages = gmail.users().messages().list("me").setMaxResults(200L).execute().getMessages();
        
        System.out.println("Last 200 Emails:\n");
        
        int count = 1;
        for (Message msg : messages) {
            // Get email details
            Message email = gmail.users().messages().get("me", msg.getId()).execute();
            
            String from = "";
            String subject = "";
            
            // Extract sender and subject
            for (MessagePartHeader header : email.getPayload().getHeaders()) {
                if ("From".equals(header.getName())) {
                    from = header.getValue();
                }
                if ("Subject".equals(header.getName())) {
                    subject = header.getValue();
                }
            }
            
            System.out.println(count + ". From: " + from);
            System.out.println("   Subject: " + subject);
            System.out.println();
            count++;
        }
        
        System.out.println("Gmail reading completed!");
    }
    
    private Gmail getGmailService() throws Exception {
        // Load credentials from resources folder
        ClassPathResource resource = new ClassPathResource("credentials.json");
        GoogleClientSecrets secrets = GoogleClientSecrets.load(
            GsonFactory.getDefaultInstance(), 
            new InputStreamReader(resource.getInputStream())
        );
        
        // Create auth flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            secrets,
            Collections.singletonList(GmailScopes.GMAIL_READONLY)
        )
        .setDataStoreFactory(new FileDataStoreFactory(new File("tokens")))
        .build();
        
        // Authorize with fixed port
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
            .setPort(8080)
            .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        
        // Build Gmail service
        return new Gmail.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
        .setApplicationName("Mail Finder")
        .build();
    }
}
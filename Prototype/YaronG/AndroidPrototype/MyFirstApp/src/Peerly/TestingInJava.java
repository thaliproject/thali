/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Peerly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yarong
 */
public class TestingInJava {
    public static SimpleHTTPServer server;
    
    public static void main(String[] args) 
    {
        int port = 8090;
        server = new SimpleHTTPServer(port, (SimpleHTTPServer.SimpleRequestHandler) new JavaSimpleHTTPServerRequestHandler());
        try
        {
            server.start();
        }
        catch (IOException ioe)
        {
            throw new RuntimeException();
        }     
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        try {
            bufferedReader.readLine();
        } catch (IOException ex) {
            Logger.getLogger(TestingInJava.class.getName()).log(Level.SEVERE, null, ex);
        }
        server.stop();
    }
}

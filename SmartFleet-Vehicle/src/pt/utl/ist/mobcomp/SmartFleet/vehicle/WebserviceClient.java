package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;

import android.os.Handler;
import android.util.Log;

public class WebserviceClient implements Runnable{

    private String serverIpAddress = "10.0.2.2";
    
    private int serverPort;
    
    private String vID;
    
    private Double lat;
    
    private Double lon;
    
    private Integer alt;
    
    private String type;
    
    private String dest;
   
    private String pList;
    
    private Double bat;
    
    private Long time;
    
    private boolean connected = false;

    //private Handler handler = new Handler();

    public WebserviceClient()
    {
    	serverPort = 1000;
    }
        
    
    public void contactVehicle(String type, String vID, String serverIpAddress, int serverPort, Double lat, Double lon, Integer alt, String dest, String pList, Double bat, Long time)
    {
    	this.type = type;
    	this.serverIpAddress = serverIpAddress;
    	this.serverPort = serverPort;
    	this.vID = vID;
    	this.lat = lat;
    	this.lon = lon;
    	this.alt = alt;
    	this.dest = dest;
    	this.pList = pList;
    	this.bat = bat;
    	this.time = time;
    	
    	Log.d("Call", "Success " +serverIpAddress + " Port" + serverPort);
    }

        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
                Log.d("ClientActivity", "C: Connecting...");
                Socket socket = new Socket(serverAddr, serverPort);
                connected = true;
                while (connected) {
                    try {
                        Log.d("ClientActivity", "C: Sending command.");
                        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                                    .getOutputStream())), true);
                         
                            out.println(type+";"+vID+";"+serverIpAddress+";"+serverPort+";"+lat+";"+lon+";"+alt+";"+dest+";"+pList+";"+bat+";"+time);
                            
                            Log.d("ClientActivity", "C: Sent.");
                            connected = false;
                    } catch (Exception e) {
                        Log.e("ClientActivity", "S: Error", e);
                    }
                }
                socket.close();
                Log.d("ClientActivity", "C: Closed.");
            } catch (Exception e) {
                Log.e("ClientActivity", "C: Error", e);
                connected = false;
            }
        }
}
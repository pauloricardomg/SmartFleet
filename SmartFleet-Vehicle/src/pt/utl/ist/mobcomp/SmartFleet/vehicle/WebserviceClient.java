package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import android.util.Log;

public class WebserviceClient implements Runnable{

    //private String serverIpAddress = "10.0.2.2";
    
    //private int serverPort;
    private String vID;
    private Double lat;
    private Double lon;
    private Integer alt;
    private String type;
    private String dest;
    private String pList;
    private Double bat;
    private Long time;
	private String ip;
	private int port;

    public WebserviceClient(String type, String ip, int port, String vID, Double lat, Double lon, Integer alt, String dest, String pList, Double bat, Long time)
    {
    	
    	this.ip = ip;
    	this.port = port;
    	this.type = type;
    	this.vID = vID;
    	this.lat = lat;
    	this.lon = lon;
    	this.alt = alt;
    	this.dest = dest;
    	this.pList = pList;
    	this.bat = bat;
    	this.time = time;
    }

    public void run() {
    	try {
    		InetAddress serverAddr = InetAddress.getByName(ip);
    		Log.d("ClientActivity", "C: Connecting...");
    		Socket socket = new Socket(serverAddr, port);
    		try {
    			Log.d("ClientActivity", "C: Sending command.");
    			PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

    			out.println(type+";"+vID+";"+lat+";"+lon+";"+alt+";"+dest+";"+pList+";"+bat+";"+time);

    			Log.d("ClientActivity", "C: Sent.");
    		} catch (Exception e) {
    			Log.e("ClientActivity", "S: Error", e);
    		}
    		socket.close();
    		Log.d("ClientActivity", "C: Closed.");
    	} catch (Exception e) {
    		Log.e("ClientActivity", "C: Error", e);
    	}
    }
}
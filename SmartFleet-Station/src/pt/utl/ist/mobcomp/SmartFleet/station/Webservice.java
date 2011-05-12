package pt.utl.ist.mobcomp.SmartFleet.station;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import android.content.Intent;
import android.os.Handler;
import android.util.Log;

public class Webservice implements Runnable {

	public static String SERVERIP = "10.0.2.2";
	
	private Register displayUI;

	// designate a port
	public static final int SERVERPORT = 8010;

	private Handler handler = new Handler();

	private ServerSocket serverSocket;

	Webservice(Register object ){
			//SERVERIP = getLocalIpAddress();
		displayUI = object;
	}

	public void run() {
		try {
			if (SERVERIP != null) {
				
				try{
				serverSocket = new ServerSocket(SERVERPORT);
				}
				catch(IOException e){
					System.out.println("Exception starting socket "+ e.getMessage());
				}
				System.out.println("The address is: " + serverSocket.getInetAddress().toString());
				System.out.println("The port is: "+ serverSocket.getLocalPort());
				System.out.println("starting socket ");
				
				while (true) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							displayUI.server_thread.setText("Listening on IP: " + SERVERIP);
						}
					});
					// listen for incoming clients
					Socket client = serverSocket.accept();
					handler.post(new Runnable() {
						@Override
						public void run() {
							displayUI.server_thread.setText("Connected.");
						}
					});

					try {
						BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
						String line = null;
						while ((line = in.readLine()) != null) {
							Log.d("ServerActivity", line);
							System.out.println("Received " + line);
							if (line.compareTo("left")==0)
							{
								Log.d("ServerActivity", "HERE");
								//displayUI.getControlBack();	
								displayUI.finishActivity(0);
								 
							}
							else
							{
								final String vehicleID = line.split(";")[0];
								final String partyNames = line.split(";")[1];
								handler.post(new Runnable() {
									@Override
									public void run() {

										displayUI.receiveDetails(partyNames, vehicleID);	
										//displayUI.callForBoarding();

									}
								});
							}
						}
						//break;
					} catch (Exception e) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								displayUI.server_thread.setText("Oops. Connection interrupted. Please reconnect your phones.");
							}
						});
						e.printStackTrace();
					}
				}
			} else {
				handler.post(new Runnable() {
					@Override
					public void run() {
						displayUI.server_thread.setText("Couldn't detect internet connection.");
					}
				});
			}
		} catch (Exception e) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					displayUI.server_thread.setText("Error");
				}
			});
			e.printStackTrace();
		}
	}
	
	public void closeSocket(){
		try{
		serverSocket.close();
		}catch (IOException e){
		  System.out.println("Socket close failed!! "+ e.getMessage());
		}
	}
	
	public boolean isClosed(){
		if(serverSocket.isClosed()){
			System.out.println("Socket is closed!! ");
			return true;
		}
		else
			return false;
	}

	// gets the ip address of your phone's network
	private String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) { return inetAddress.getHostAddress().toString(); }
				}
			}
		} catch (SocketException ex) {
			Log.e("ServerActivity", ex.toString());
		}
		return null;
	}

}


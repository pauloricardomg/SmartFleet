package pt.utl.ist.mobcomp.SmartFleet.vehicle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class Webservice implements Runnable {

	public static String SERVERIP = "10.0.2.2";

	private VehicleActivity displayUI;

	final private String myvID;

	private String myAlt;

	private Integer count;

	private VehicleInfo[] info;

	Thread client_socket;

	// designate a port
	public static final int SERVERPORT = 8010;

	WebserviceClient service_request;

	private Handler handler = new Handler();

	private ServerSocket serverSocket;

	HashMap<String, VehicleInfo> wordcount;

	private LocationManager lm;
	
	Webservice(VehicleActivity object,LocationManager lm){
		//SERVERIP = getLocalIpAddress();
		displayUI = object;
		info= new VehicleInfo[15];
		wordcount = new HashMap<String, VehicleInfo>();
		count =0;
		myvID = object.getId();
		this.lm = lm;
	}

	public void run() {
		try {

			service_request = new WebserviceClient();
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
							//		displayUI.server_thread.setText("Listening on IP: " + SERVERIP);
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

							String value = line.split(";")[0];
							final String type = value;

							final String vID = line.split(";")[1];
 
							Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
							double lon = location.getLongitude();
							double lat = location.getLatitude();
							
							Integer alt = displayUI.getAlt();
							String dest = displayUI.getDestination();
							String pList = displayUI.getPassengerList();
							Double bat = displayUI.getBattery();
							
							
							if (value.compareToIgnoreCase("warn") == 0){
								if ( wordcount.containsKey(vID))
								{
								/*	handler.post(new Runnable() {
										@Override
										public void run() {
											Toast.makeText(displayUI,"Vehicle "+ myvID+"warn. ALERT!!", Toast.LENGTH_LONG).show();		
										}
									});*/
									VehicleInfo tempInfo = wordcount.get(vID);
									if(tempInfo == null)
										Log.d("ServerActivity", "null");
								String contactIP = tempInfo.ipAddress;
								Integer contactPort = tempInfo.getPort();
								//contact other vehicle with your ID and alt
								Long time = System.currentTimeMillis();
								service_request.contactVehicle("recvwarn",myvID, contactIP, contactPort, lat, lon, alt, dest, pList, bat, time); //TODO: no need to send ip and port (here it is wrong) 
								client_socket = new Thread(service_request);
								client_socket.start(); 
								}

							}

							else if (value.compareToIgnoreCase("inrange") == 0){
								//send to client all our information with time stamp
								//at client: send info
								handler.post(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(displayUI,"Vehicle "+ vID+"in range", Toast.LENGTH_LONG).show();		
									}
								});
								
								String contactIP = line.split(";")[2];

								String Port = line.split(";")[3];
								int contactPort = Integer.parseInt(Port);
								
								Long time = System.currentTimeMillis();
								
								info[count]= new VehicleInfo(contactIP,contactPort);
								wordcount.put(vID, info[count]);
								count ++;
								
									service_request.contactVehicle("recvinrange",myvID, contactIP, contactPort, lat, lon, alt, dest, pList, bat, time); //TODO: no need to send ip and port
									client_socket = new Thread(service_request);
									client_socket.start(); 

									
									final Double lat_final = lat;
									final Double lon_final = lon;
									final String vID_final = vID;
									
									displayUI.updateNewVehiclePos(vID_final,lat_final, lon_final);		
									
							}

							else if (value.compareToIgnoreCase("outrange") == 0){
								// Send info about lat and lon of new vehicle and stop
								final String vID_final = vID;
								handler.post(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(displayUI,"Vehicle "+ vID+"outrange", Toast.LENGTH_LONG).show();		
									}
								});
										displayUI.stopNewVehicle(vID_final);		
							}

							else if (value.compareToIgnoreCase("recvwarn") == 0){
								//onreceive compare altitude  
								
								handler.post(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(displayUI,"Vehicle "+ myvID+"RECEIVED warn", Toast.LENGTH_LONG).show();		
									}
								});
								if(alt == Integer.parseInt(line.split(";")[6])){

									Log.d("RecvWarn", myvID +" "+vID);
									
									if(Integer.parseInt(myvID) < Integer.parseInt(vID) ){
										Log.d("RecvWarn", "Change Altitude");
										handler.post(new Runnable() {
											@Override
											public void run() {
												Toast.makeText(displayUI,"Vehicle "+ myvID+"moved up by 100 mts", Toast.LENGTH_LONG).show();		
											}
										});
										final Integer altitude = alt+100;
										displayUI.setAlt(altitude);	
									}
								}

							}


							else if (value.compareToIgnoreCase("recvinrange") == 0){
								//on receive store
								//if battery is zero, mark vehicle as missing.

							}


							Log.d("ServerActivityHERE", value);

							handler.post(new Runnable() {
								@Override
								public void run() {
									displayUI.server_thread.setText("Received " + type);		
								}
							});
						}
						//	break;
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


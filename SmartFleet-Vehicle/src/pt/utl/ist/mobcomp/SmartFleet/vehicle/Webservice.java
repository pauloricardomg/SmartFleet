package pt.utl.ist.mobcomp.SmartFleet.vehicle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class Webservice implements Runnable {

	public static String SERVERIP = "10.0.2.2";

	private VehicleActivity vehicleActivity;

	final private String myvID;

	// designate a port
	public static final int SERVERPORT = 8010;

	private Handler handler = new Handler();

	private ServerSocket serverSocket;

	private LocationManager lm;
	
	Webservice(VehicleActivity object,LocationManager lm){
		vehicleActivity = object;
		myvID = object.getId();
		this.lm = lm;
	}

	public void run() {
		try {

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
				// listen for incoming clients
				Socket client = serverSocket.accept();
				handler.post(new Runnable() {
					@Override
					public void run() {
						vehicleActivity.server_thread.setText("Connected.");
					}
				});

				try {
					BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
					String line = null;
					while ((line = in.readLine()) != null) {
						Log.d("ServerActivity", line);
						System.out.println("Received " + line);

						String[] split = line.split(";");
						final String type = split[0];
						final String otherVehicleID = split[1];

						if (type.compareToIgnoreCase("warn") == 0){
							VehicleInfo info = vehicleActivity.getLearnedVehicles().get(otherVehicleID);
							if (info != null)
							{
								Log.d("RecvWarn", myvID +" "+otherVehicleID);
//								handler.post(new Runnable() {
//									@Override
//									public void run() {
//										Toast.makeText(displayUI,"Vehicle "+ myvID+"RECEIVED warn", Toast.LENGTH_LONG).show();		
//									}
//								});
								
								Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
								double lon = location.getLongitude();
								double lat = location.getLatitude();

								Integer alt = vehicleActivity.getAlt();
								String dest = vehicleActivity.getDestination();
								String pList = vehicleActivity.getPassengerList();
								Double bat = vehicleActivity.getBattery();
								Runnable recvwarn = new WebserviceClient("recvwarn", info.getIpAddress(), info.getPort(), myvID, lat, lon, alt, dest, pList, bat, System.currentTimeMillis());
								new Thread(recvwarn).start();
							}

						} else if (type.compareToIgnoreCase("inrange") == 0){
							//send to client all our information with time stamp
							//at client: send info
							handler.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(vehicleActivity,"Vehicle "+ otherVehicleID+" is in range.", Toast.LENGTH_LONG).show();		
								}
							});

							String contactIP = split[2];
							int contactPort = Integer.parseInt(split[3]);
							
							VehicleInfo vehicleInfo = vehicleActivity.getLearnedVehicles().get(otherVehicleID);
							if(vehicleInfo == null){
								vehicleInfo = new VehicleInfo(otherVehicleID, contactIP, contactPort);
								vehicleActivity.getLearnedVehicles().put(otherVehicleID, vehicleInfo);
							}
							
							vehicleActivity.getInRange().add(otherVehicleID);

						} else if (type.compareToIgnoreCase("outrange") == 0){
							// Send info about lat and lon of new vehicle and stop
							handler.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(vehicleActivity,"Vehicle "+ otherVehicleID+" is out of range.", Toast.LENGTH_LONG).show();		
								}
							});
							vehicleActivity.getInRange().remove(otherVehicleID);	
						} else if (type.compareToIgnoreCase("recvwarn") == 0){
							//onreceive compare altitude  

							updateVehicleInfo(split, otherVehicleID);
							String otherAlt = split[4];
							if(vehicleActivity.getAlt() == Integer.parseInt(otherAlt)){

								Log.d("RecvWarn", myvID +" "+otherVehicleID);

								if(Integer.parseInt(myvID) < Integer.parseInt(otherVehicleID) ){
									Log.d("RecvWarn", "Change Altitude");
									handler.post(new Runnable() {
										@Override
										public void run() {
											Toast.makeText(vehicleActivity,"Vehicle " + otherVehicleID + " was too close. Moving up 100 mts", Toast.LENGTH_LONG).show();		
										}
									});
									vehicleActivity.raiseAltitude();
								}
							}

						} else if (type.compareToIgnoreCase("gossip") == 0){
							//on receive store
							//if battery is zero, mark vehicle as missing.
							updateVehicleInfo(split, otherVehicleID);
						}

						Log.d("ServerActivityHERE", type);

						handler.post(new Runnable() {
							@Override
							public void run() {
								vehicleActivity.server_thread.setText("Received " + type);		
							}
						});
					}
					//	break;
				} catch (Exception e) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							vehicleActivity.server_thread.setText("Oops. Connection interrupted. Please reconnect your phones.");
						}
					});
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					vehicleActivity.server_thread.setText("Error");
				}
			});
			e.printStackTrace();
		}
	}

	private void updateVehicleInfo(String[] split, String otherVehicleID) {
		VehicleInfo vehicleInfo = vehicleActivity.getLearnedVehicles().get(otherVehicleID);
		if(vehicleInfo != null){
			//out.println(type+";"+vID+";"+lat+";"+lon+";"+alt+";"+dest+";"+pList+";"+bat+";"+time);
			vehicleInfo.setLat(new Double(split[2]));
			vehicleInfo.setLon(new Double(split[3]));
			vehicleInfo.setAlt(new Integer(split[4]));
			vehicleInfo.setDest(split[5]);
			vehicleInfo.setpList(split[6]);
			vehicleInfo.setBat(new Double(split[7]));
			vehicleInfo.setTime(new Long(split[8]));
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


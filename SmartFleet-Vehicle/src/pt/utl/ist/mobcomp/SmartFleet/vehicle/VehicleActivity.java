package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import pt.utl.ist.mobcomp.SmartFleet.util.HTTPClient;
import pt.utl.ist.mobcomp.SmartFleet.util.LookupUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;

public class VehicleActivity extends Activity implements LocationListener{

	TextView show;
	TextView server_thread;
	Thread server_socket, client_socket;
	Webservice service;
	
	GossipSender sender;
	
	WebserviceClient service_request;
	LastSeenVehicleInfo lastSeen;
	 /** Called when the activity is first created. */
	
	String id;
	String initialLat;
	String initialLon;
	String serverIP;
	String serverPort;
	String myIp;
	String myPort;
	String gpsPort;
	String emulatorPort;
	String capacity;
	Integer alt;
	
	String destination;
	String passengerList;
	double battery;
	
	LocationManager locationManager;
	List<StationInfo> activeStations;
	boolean atStation;
	
	public static HashMap<String, VehicleInfo> learnedVehicles = new HashMap<String, VehicleInfo>();
	public static List<String> inRange = new ArrayList<String>();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		Properties prop = LookupUtils.readPropertiesFile(this.getResources().getAssets(), "vehicle.conf");
		id = prop.getProperty("id");
		initialLat = prop.getProperty("lat");
		initialLon = prop.getProperty("lon");
		serverIP = prop.getProperty("server_ip");
		serverPort = prop.getProperty("server_port");
		gpsPort = prop.getProperty("gps_server_port");
		emulatorPort = prop.getProperty("emulator_port");
		capacity = prop.getProperty("capacity");
		myIp = prop.getProperty("vehicle_ip");
		myPort = prop.getProperty("vehicle_port");
        atStation = false;
        alt = 0;
		
        show = (TextView) this.findViewById(R.id.server_thread);
        server_thread = (TextView) this.findViewById(R.id.show);
        
        //Try to register vehicle.
        register_vehicle();
        
        activeStations = LookupUtils.lookupStations("http://" + serverIP + ":" + serverPort);
        
		// Get the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		
		//Start a thread that listens to incoming connections from the server.
		service = new Webservice(this, locationManager);
		server_socket = new Thread(service);
		server_socket.start();
		
		sender = new GossipSender(this, locationManager);
		new Thread(sender).start();
    }


	public static StationInfo getStation(Location location, List<StationInfo> activeStations){
    	
    	for (StationInfo station : activeStations) {
			Location stationLocation = station.getLocation();
			if(stationLocation != null && location.distanceTo(stationLocation) < 1){
				return station;
			}
		}
    	
    	return null;
    }

    
	@Override
	public void onLocationChanged(Location location) {
		StationInfo station;
		if((station = getStation(location, activeStations)) != null){
			if(!atStation){
				arrivedAtStation(station);
			}
		} else {
			//Moved away from the station
			atStation = false;
			showNavigationScreen(location);
			//show.setText("Lat: " + String.valueOf(location.getLatitude()) + "Lon: " + String.valueOf(location.getLongitude()));
		}		
	}


	public void arrivedAtStation(StationInfo station) {
		atStation = true;
		show.setText("Vehicle arrived at station: " + station.getName());
		// when a vehicle arrives at a  transportation station, it takes communicates with 			
		// the central server to indicate its state, information about other vehicles that it 			
		//learned along the way and about any missing vehicles it may have found
		//do{
		//	partiesToEmbark = server.arrivedAtStation(blabla);
		//	if(partiesToEmbark > 0)
		//		break
		//		else
		//			sleep(whatever)
		//} while(true);
		//	currentParties.append(partiesToEmbark)
		//	rechargeBattery()
		//	nextDest = selectNextDest()
			//  Action: display next stop on screenppppp0poooooooooooooooooooo9
		//	sleep(1min) // Each stop takes a total time of 1 minute
		//	this.alt = server.leftStation(blablabla) //TODO: altitude is updated here
		//	gpsEmulator.moveTo(vehicleId, nextDest)	
	}
    
	/* Request updates at startup */
	@Override
	protected void onResume() {
		super.onResume();
		
		Location actualLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if(actualLocation != null){
			StationInfo station = getStation(actualLocation, activeStations);
			if(station != null && !atStation){
				arrivedAtStation(station);
			}
		}
		
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 1, this);
		
		// TODO: battery draining loop
	}
	
	public void onDestroy(){
		super.onDestroy();
		try{
			service.closeSocket();
			
		}catch(Exception e){
			e.printStackTrace();
		}
		sender.stop();
		System.out.println("ONDESTROY");
	}

	/* Remove the locationlistener updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(this);
	}
	
	//Parse a given xml	
	private String parsexml (String response)
		throws XmlPullParserException, IOException
	{
		
		String value = null;
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	    XmlPullParser xpp = factory.newPullParser();
	    byte[] bytes = response.getBytes();
	    InputStream xml = new ByteArrayInputStream(bytes);
	    xpp.setInput(xml, null);
	    
	    boolean printing = false;
	     
	    while (true) {
	         int event = xpp.next();
	         if (event == XmlPullParser.START_TAG) {
	             String name = xpp.getName();
	             if (name.equals("Status")) printing = true;
	         }
	         else if (event == XmlPullParser.END_TAG) {
	             String name = xpp.getName();
	             if (name.equals("Status")) printing = false;
	         }
	         else if (event == XmlPullParser.TEXT) {
	             if (printing) 
	            	 value = xpp.getText();
	         }
	         else if (event == XmlPullParser.END_DOCUMENT) {
	             break;
	         } 
	      }	    
		return value;
		
	}
	
	
	
	
private String contact_server(String url)
{
	
	//Make a get Request to the server
	String response = null;
	try {
	    response =  HTTPClient.executeHttpGet(url);
	} catch (Exception e) {
	    e.printStackTrace();
	  }

 //Parse the received xml to see if the registration was successful or not.	
	try {
		response = parsexml(response);
	} catch (XmlPullParserException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}
	
	return response;
}

private String validate (String response)
{
	
	if (response == null)
		return "TIME OUT";
	else if(response.equals("1"))
		return "SUCCESS";
	else
		return "FAIL";
		
}

public void showNavigationScreen(Location location){
	Intent intent = new Intent(this, NavigationActivity.class);
	intent.putExtra("stations",(Serializable)this.activeStations);
	intent.putExtra("vehicleID",this.id);
	intent.putExtra("position", location);
	startActivity(intent);
}

public void updateNewVehiclePos(String vId, Double lat, Double lon)
{
	// start intent to call navigation map with whatever values are required
}

public void stopNewVehicle (String vID)
{
	// start intent to call and stop displaying the new vehicle on map
}

private void register_vehicle(){
	String url,response=null,status;

	//REGISTER VEHICLE ON GPS SERVER (maybe this will be bundled with central server, but for now a separate server)
	try{
		url = String.format("http://" + this.serverIP + ":" + this.gpsPort + "/RegisterVehicle?id=" + 
				this.id + ";lat=" +  this.initialLat + ";lon=" + this.initialLon + ";ip=" + myIp +
				";port=" + myPort + ";emulatorPort=" + emulatorPort);
		response = contact_server(url);
	}catch(Exception e){
		System.out.println("Exception here: "+ e.getMessage());
	}
	status = validate(response);
	
	if(status.equals("SUCCESS")){
		response = null;

		try{
			url = String.format("http://" + this.serverIP + ":" + this.serverPort + "/RegisterVehicle?id=" + 
					this.id + ";capacity=" + this.capacity + ";lat=" +  this.initialLat + ";lon=" + this.initialLon);
			response = contact_server(url);
			}catch(Exception e){
				System.out.println("Exception here: "+ e.getMessage());
			}
		status = validate(response);
	}	
	
	if (status == "SUCCESS"){
		show.setText("Vehicle Registration Success!!");
	} else if (status == "FAIL")
		show.setText("Vehicle already registered!");
	else
		show.setText("Vehicle registration request timed out!! Please relaunch");

}

@Override
public void onProviderDisabled(String provider) {
	// TODO Auto-generated method stub
	
}

@Override
public void onProviderEnabled(String provider) {
	// TODO Auto-generated method stub
	
}

@Override
public void onStatusChanged(String provider, int status, Bundle extras) {
	// TODO Auto-generated method stub
	
}

/**
 * @return the alt
 */
public Integer getAlt() {
	return alt;
}


public void raiseAltitude(){
	alt += 100;
	try{
		String url = String.format("http://" + this.serverIP + ":" + this.gpsPort + "/ChangeAltitude?vehicleID=" + 
				this.id + ";alt=" +  this.alt);
		contact_server(url);
	}catch(Exception e){
		System.out.println("Exception here: "+ e.getMessage());
	}
	//CONSUME BATTERY
}

public void lowerAltitude(){
	alt -= 100;
	//CONSUME BATTERY
}


/**
 * @return the destination
 */
public String getDestination() {
	return destination;
}


/**
 * @param destination the destination to set
 */
public void setDestination(String destination) {
	this.destination = destination;
}


/**
 * @return the battery
 */
public double getBattery() {
	return battery;
}


/**
 * @param battery the battery to set
 */
public void setBattery(double battery) {
	this.battery = battery;
}


/**
 * @return the passengerList
 */
public String getPassengerList() {
	return passengerList;
}


/**
 * @return the id
 */
public String getId() {
	return id;
}


public HashMap<String, VehicleInfo> getLearnedVehicles() {
	return learnedVehicles;
}


public List<String> getInRange() {
	return inRange;
}

}

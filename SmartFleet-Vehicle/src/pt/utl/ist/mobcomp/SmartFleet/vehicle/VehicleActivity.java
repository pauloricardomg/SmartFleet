package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import pt.utl.ist.mobcomp.SmartFleet.bean.PartyInfo;
import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import pt.utl.ist.mobcomp.SmartFleet.util.HTTPClient;
import pt.utl.ist.mobcomp.SmartFleet.util.LookupUtils;
import pt.utl.ist.mobcomp.SmartFleet.util.XmlUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

public class VehicleActivity extends Activity implements LocationListener{

	TextView show;
	TextView server_thread;
	EditText wpname;
	EditText wdest;
	EditText wbat;
	
	
	Thread server_socket, client_socket;
	Webservice service;
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
	
	String destLat;
	String destLon;
	
	String destination;
	String passengerList;
	double battery;
	
	List<PartyInfo> passengersForDest;
	
	HashMap<String, List<PartyInfo>> destToPassenger;
	
	LocationManager locationManager;
	List<StationInfo> activeStations;
	boolean atStation;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nextstop);
        
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
        
        
		
        destToPassenger = new HashMap<String, List<PartyInfo>>();
        passengersForDest = new LinkedList<PartyInfo>();
        
        show = (TextView) this.findViewById(R.id.server_thread);
        server_thread = (TextView) this.findViewById(R.id.show);
        
        wdest = (EditText) this.findViewById(R.id.wdest);
        wpname = (EditText) this.findViewById(R.id.wpname);
        wbat = (EditText) this.findViewById(R.id.wbat);
        
        
        //Try to register vehicle.
        register_vehicle();
        
        activeStations = LookupUtils.lookupStations("http://" + serverIP + ":" + serverPort);
        
		// Get the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		
		//Start a thread that listens to incoming connections from the server.
		service = new Webservice(this, locationManager);
		server_socket = new Thread(service);
		server_socket.start();
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

	private String getServerAddress() {
		return "http://" + serverIP + ":" + serverPort;
	}
	
	
	
	
	public void arrivedAtStation(StationInfo station) {
		atStation = true;
		show.setText("Vehicle arrived at station: " + station.getName());
		
		//TODO: when a vehicle arrives at a  transportation station, it takes communicates with 			
		// the central server to indicate its state, information about other vehicles that it 			
		//learned along the way and about any missing vehicles it may have found
		
		
		// Also disembark passengers for this destination upon Arrival
		disembarkPassengers();
		
		//Get string of passengers in vehicle after others disembarked, to be passed in arrived station get call
		String passengersInVehicle = getPassengersInVehicle();
		
		
		// move to altitude 0 
		String url,response=null;
		try{
			url = String.format("http://" + this.serverIP + ":" + this.gpsPort + "/ChangeAltitude?vehicleID=" + 
					this.id + ";alt=" +  this.alt );
			response = contact_server(url);
		}catch(Exception e){
			System.out.println("Exception here: "+ e.getMessage());
		}
		
		//then set alt 0;
		setAlt(0);
		
		do{
			//Call arrived at station method on central server
			
			try{
				if (passengersInVehicle!=null)
					url = String.format(getServerAddress() + "/ArrivedAtStation?vehicleID=" + id + ";" + "stationID=" + station.getId() + ";" +"freeSeats="+ capacity+ ";" +"parties="+passengersInVehicle+ ";" +"ts="+System.currentTimeMillis());
				else
					url = String.format(getServerAddress() + "/ArrivedAtStation?vehicleID=" + id + ";" + "stationID=" + station.getId() + ";" +"freeSeats="+ capacity+ ";" +"ts="+System.currentTimeMillis());
				response = contactServerNoParsing(url);
			} catch(Exception e){
				show.setText("Exception: " + e.getMessage());
				System.out.println("Exception "+ e.getMessage());
			}
			
			if(response!=null){

				try {
					passengersForDest =XmlUtils.parsePartyInfos(response);
				} catch (XmlPullParserException e) {
					// TODO Auto-generated catch block
					System.out.println("Exception: " + e.getMessage());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Exception: " + e.getMessage());
				}
			}
			
			if (passengersForDest.size()>0){
				break;
			}
			else{
				try {
					Thread.currentThread();
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					System.out.println("Exception: " + e.getMessage());
				}
			} 
		} while(true);


		//TODO:	rechargeBattery()

		//choose next destination from among the destinations for passengers
		selectNextDest();

		//Update this.passengerlist for the selected destination. Will be used to notify on arrival
		updatePartyForNextDest();
		
		// Action: display next stop on screen
		wdest.setText(this.destination);
		wpname.setText(this.passengerList);
		wbat.setText("TODO");
		
		// Each stop takes a total time of 1 minute. For now 20 seconds.
		try {
			Thread.currentThread();
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		
		//Call leave station
		try{
			url = String.format(getServerAddress() + "/LeaveStation?vehicleID=" + id + ";" + "stationID=" + station.getId() + ";" +"dest="+destination+ ";" + "ts="+System.currentTimeMillis());
			response = contact_server(url);
			
		} catch(Exception e){
			show.setText("Exception: " + e.getMessage());
			System.out.println("Exception "+ e.getMessage());
		}
		
		//Set Altitude
		setAlt((Integer.parseInt(response))); 


		//Change Altitude
		try{
			url = String.format("http://" + this.serverIP + ":" + this.gpsPort + "/ChangeAltitude?vehicleID=" + 
					this.id + ";alt=" +  this.alt );
			response = contact_server(url);
		}catch(Exception e){
			System.out.println("Exception here: "+ e.getMessage());
		}
		
		//Move to the destination
		try{
			url = String.format("http://" + this.serverIP + ":" + this.gpsPort + "/MoveTo?vehicleID=" + 
					this.id + ";lat=" +  this.destLat + ";lon=" + this.destLon );
			response = contact_server(url);
		}catch(Exception e){
			System.out.println("Exception here: "+ e.getMessage());
		}
		
	}
	
	
	

	public void updatePartyForNextDest()
	{
		for (int i=0; i< passengersForDest.size(); i++)
        {
                if(passengersForDest.get(i).getDestination().compareTo(this.destination) == 0 )
                {
                	if(this.passengerList == null)
                		this.passengerList = passengersForDest.get(i).getName();
                	else
                		this.passengerList.concat(","+passengersForDest.get(i).getName());
                }
        }
	}

	public String getPassengersInVehicle()
	{
		String passengersInVehicle = null;
		for (int i=0; i< passengersForDest.size(); i++)
		{
			if (passengersInVehicle == null)
				passengersInVehicle = passengersForDest.get(i).getName();
			else
				passengersInVehicle.concat(","+passengersForDest.get(i).getName());
        }
		return passengersInVehicle;
	}
	
	
	public void selectNextDest()
	{
		//TODO: move to the closest Dest. 
		
		//For now move to the first Dest
		destination = passengersForDest.get(0).getDestination();
		destLat =  passengersForDest.get(0).getLat();
		destLon =  passengersForDest.get(0).getLon();
	}
	
	
	public void disembarkPassengers()
	{
		for (int i=0; i< passengersForDest.size(); i++)
        {
                if(passengersForDest.get(i).getDestination().compareTo(this.destination) == 0 )
                {
                	passengersForDest.remove(i);
                }
        }
		
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
		System.out.println(" ONDESTROY ");
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
	
private String contactServerNoParsing(String url)
{
	String response = null;
	try {
	    response =  HTTPClient.executeHttpGet(url);
	} catch (Exception e) {
	    e.printStackTrace();
	  }
	
	return response;
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
	intent.putExtra("destination", destination);
	startActivityForResult(intent, 0);
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


/**
 * @param alt the alt to set
 */
public void setAlt(Integer alt) {
	this.alt = alt;
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


}

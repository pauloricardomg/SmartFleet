package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import pt.utl.ist.mobcomp.SmartFleet.util.HTTPClient;
import pt.utl.ist.mobcomp.SmartFleet.util.LookupUtils;
import pt.utl.ist.mobcomp.SmartFleet.vehicle.R;
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
	LocationManager locationManager;
	List<StationInfo> activeStations;
	boolean atStation;
	
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
		
        show = (TextView) this.findViewById(R.id.show);
        
        //Try to register vehicle.
        register_vehicle();
        
        activeStations = LookupUtils.lookupStations("http://" + serverIP + ":" + serverPort);
        
		// Get the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
		//	server.leftStation(blablabla)
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

}

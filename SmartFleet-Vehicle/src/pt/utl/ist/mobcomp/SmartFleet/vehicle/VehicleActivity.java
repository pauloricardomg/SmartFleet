package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import pt.utl.ist.mobcomp.SmartFleet.bean.PartyInfo;
import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import pt.utl.ist.mobcomp.SmartFleet.util.HTTPClient;
import pt.utl.ist.mobcomp.SmartFleet.util.LookupUtils;
import pt.utl.ist.mobcomp.SmartFleet.util.XmlUtils;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
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
	
	VehicleEngine engine;
	
	private BatteryManager battMan;
	
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
	
	Double destLat;
	Double destLon;
	
	String destination;
	String passengerList;
	double battery;
	
	List<PartyInfo> passengersForDest;
	
	HashMap<String, List<PartyInfo>> destToPassenger;
	
	LocationManager locationManager;
	List<StationInfo> activeStations;
	boolean atStation;
	
	ProgressDialog dialog;
	
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
		
        battMan = BatteryManager.createInstance(serverIP, gpsPort, id);
        
        destination = "";
        dialog = null;
        
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
		
		engine = new VehicleEngine(this, locationManager);
		new Thread(engine).start();
    }


	public static StationInfo getStation(Location location, List<StationInfo> activeStations){
    	
		if(activeStations != null){
	    	for (StationInfo station : activeStations) {
				Location stationLocation = station.getLocation();
				if(stationLocation != null && location.distanceTo(stationLocation) < 100){ //TODO: Change later 
					return station;
				}
			}
		}
    	
    	return null;
    }

    
	@Override
	public void onLocationChanged(Location location) {
		if(activeStations != null){
			//show.setText("Distance: " + location.distanceTo(activeStations.get(0).getLocation()));
		}
		StationInfo station;
		if((station = getStation(location, activeStations)) != null){
			if(!atStation){
				if(dialog != null){
					dialog.cancel();
					dialog = null;
				}
				engine.stopMoving();
				arrivedAtStation(station);
			}
		} else if(!destination.equals("")){
			engine.startMoving();
			//Moved away from the station
			atStation = false;
			showNavigationScreen(location);
			//show.setText("Lat: " + String.valueOf(location.getLatitude()) + "Lon: " + String.valueOf(location.getLongitude()));
		} else if(dialog == null){
			dialog = ProgressDialog.show(VehicleActivity.this, "", "Please move vehicle to station..", true);
		}
	}

	private String getServerAddress() {
		return "http://" + serverIP + ":" + serverPort;
	}
	
	
public void arrivedAtDestNotStation() {
		
	    ProgressDialog diag = ProgressDialog.show(VehicleActivity.this, "", "Calculating next Destination..", true);
		
		atStation = false;
		//show.setText("Vehicle arrived at station: " + station.getName());
		
		//TODO: when a vehicle arrives at a  transportation station, it takes communicates with 			
		// the central server to indicate its state, information about other vehicles that it 			
		//learned along the way and about any missing vehicles it may have found
		
		// Also disembark passengers for this destination upon Arrival
		disembarkPassengers();
		
		//then set alt 0;
		while(battMan.getAlt() > 0){
			battMan.lowerAltitude();
		}
		
		//choose next destination from among the destinations for passengers
		Boolean nextDest =selectNextDest();

		if(nextDest){
			
			diag.setMessage("Preparing to go to "+ this.destination);
			
		//Update this.passengerlist for the selected destination. Will be used to notify on arrival
		updatePartyForNextDest();
		
		//raise altitude to 100m and start moving
		battMan.raiseAltitude();
		
		diag.cancel();
		
		String url, response=null;
		//Move to the destination
		try{
			url = String.format("http://" + this.serverIP + ":" + this.gpsPort + "/MoveTo?vehicleID=" + 
					this.id + ";lat=" +  this.destLat + ";lon=" + this.destLon );
			response = contact_server(url);
		}catch(Exception e){
			System.out.println("Exception here: "+ e.getMessage());
		}
		
		}
		
		else    // go back to first station
		{
			diag.setMessage("No passsengers in vehicle. Moving to Station: "+activeStations.get(0).getName() );
			
			String lat =activeStations.get(0).getLat();
			String lon = activeStations.get(0).getLon();
			
			destLat = Double.parseDouble(lat);
			destLon = Double.parseDouble(lon);
			this.destination = activeStations.get(0).getName();
			
			//Update this.passengerlist for the selected destination. Will be used to notify on arrival
			updatePartyForNextDest();

			battMan.raiseAltitude();
				
			diag.cancel();
			
			String url, response=null;
			//Move to the destination
			try{
				url = String.format("http://" + this.serverIP + ":" + this.gpsPort + "/MoveTo?vehicleID=" + 
						this.id + ";lat=" +  lat + ";lon=" + lon );
				response = contact_server(url);
			}catch(Exception e){
				System.out.println("Exception here: "+ e.getMessage());
			}
			
		}
		
		
	//	dialog = ProgressDialog.show(VehicleActivity.this, "", "Contacting station " + station.getName() + " for passengers. Please wait...", true);
		
	//	new WaitPartiesFromStation().execute(station);
//		atStation = true;
//		show.setText("Vehicle arrived at station: " + station.getName());
//		
//		//TODO: when a vehicle arrives at a  transportation station, it takes communicates with 			
//		// the central server to indicate its state, information about other vehicles that it 			
//		//learned along the way and about any missing vehicles it may have found
//		
//		// Also disembark passengers for this destination upon Arrival
//		disembarkPassengers();
//		
//		//then set alt 0;
//		while(this.alt > 0){
//			lowerAltitude();
//		}
//		
//		ProgressDialog dialog = ProgressDialog.show(VehicleActivity.this, "", "Contacting station " + station.getName() + " for passengers. Please wait...", true);
	}
	
	
	public void arrivedAtStation(StationInfo station) {
		
		
		atStation = true;
	//	show.setText("Vehicle arrived at station: " + station.getName());
		
		//TODO: when a vehicle arrives at a  transportation station, it takes communicates with 			
		// the central server to indicate its state, information about other vehicles that it 			
		//learned along the way and about any missing vehicles it may have found
		
		// Also disembark passengers for this destination upon Arrival
		disembarkPassengers();
		
		//then set alt 0;
		while(battMan.getAlt() > 0){
			battMan.lowerAltitude();
		}
		
		dialog = ProgressDialog.show(VehicleActivity.this, "", "Contacting station " + station.getName() + " for passengers. Please wait...", true);
		
		new WaitPartiesFromStation().execute(station);
//		atStation = true;
//		show.setText("Vehicle arrived at station: " + station.getName());
//		
//		//TODO: when a vehicle arrives at a  transportation station, it takes communicates with 			
//		// the central server to indicate its state, information about other vehicles that it 			
//		//learned along the way and about any missing vehicles it may have found
//		
//		// Also disembark passengers for this destination upon Arrival
//		disembarkPassengers();
//		
//		//then set alt 0;
//		while(this.alt > 0){
//			lowerAltitude();
//		}
//		
//		ProgressDialog dialog = ProgressDialog.show(VehicleActivity.this, "", "Contacting station " + station.getName() + " for passengers. Please wait...", true);
	}
	
	public void leaveStation(StationInfo station){
		
		String url,response=null,status;
		
		//Call leave station
		try{
			url = String.format(getServerAddress() + "/LeaveStation?vehicleID=" + id + ";" + "stationID=" + station.getId() + ";" +"dest="+destination+ ";" + "ts="+System.currentTimeMillis());
			response = contact_server(url);
			
		} catch(Exception e){
			show.setText("Exception: " + e.getMessage());
			System.out.println("Exception "+ e.getMessage());
		}
		
		//Set Altitude
		int desiredAltitude = Integer.parseInt(response);
		desiredAltitude = desiredAltitude/100;
		for(int i=0; i<desiredAltitude; i++){
			battMan.raiseAltitude();
		}
		
		//Move to the destination
		try{
			url = String.format("http://" + serverIP + ":" + gpsPort + "/MoveTo?vehicleID=" + 
					id + ";lat=" +  destLat + ";lon=" + destLon );
			response = contact_server(url);
		}catch(Exception e){
			System.out.println("Exception here: "+ e.getMessage());
		}
		
	}
	
	public void updatePartyForNextDest()
	{
		this.passengerList = null;
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
	
	public String getPassengersNotForNextDest()
	{
		String passengersNotForNextDest=null;
		for (int i=0; i< passengersForDest.size(); i++)
        {
                if(passengersForDest.get(i).getDestination().compareTo(this.destination) != 0 )
                {
                	if(passengersNotForNextDest == null)
                		passengersNotForNextDest = passengersForDest.get(i).getName();
                	else
                		passengersNotForNextDest.concat(","+passengersForDest.get(i).getName());
                }
        }
		
		return passengersNotForNextDest;
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
	
	
	public Boolean selectNextDest()
	{
		//TODO: move to the closest Dest. 
		if (passengersForDest.size()>0)
		{
		//For now move to the first Dest
		destination = passengersForDest.get(0).getDestination();
		destLat =  new Double(passengersForDest.get(0).getLat());
		destLon =  new Double(passengersForDest.get(0).getLon());
		return true;
		}
		
		else 
			return false; 
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
			
			else {
			
				arrivedAtDestNotStation();
			}
		}
		
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 1, this);
	}
	
	public void onDestroy(){
		if(dialog != null){
			dialog.cancel();
			dialog = null;
		}
		super.onDestroy();
		try{
			service.closeSocket();
			
		}catch(Exception e){
			e.printStackTrace();
		}
		engine.stop();
		System.out.println(" ONDESTROY ");
	}

	/* Remove the locationlistener updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(this);
	}
	
	//Parse a given xml	
	private static String parsexml (String response)
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
	
	
public static String contact_server(String url)
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
	intent.putExtra("lat", destLat);
	intent.putExtra("lon", destLon);
	intent.putExtra("pList", passengerList);
	String allPassengers= getPassengersInVehicle();
	intent.putExtra("allPassengers",allPassengers);
	String passengersNotForNextDest =getPassengersNotForNextDest();
	intent.putExtra("passengersNotForNextDest",passengersNotForNextDest);
	startActivityForResult(intent, 0);
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
	}
	else if (status == "FAIL")
		show.setText("Vehicle already registered!");
	else
		show.setText("Vehicle registration request timed out!! Please relaunch");

}

public String getServerIP() {
	return serverIP;
}


public String getGpsPort() {
	return gpsPort;
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

class WaitPartiesFromStation extends AsyncTask<StationInfo, String, Boolean> {
	

	private StationInfo station;
	
    protected Boolean doInBackground(StationInfo... args) {
    	station = args[0];
		
		String url,response=null;
		
		//Get string of passengers in vehicle after others disembarked, to be passed in arrived station get call
		String passengersInVehicle = getPassengersInVehicle();
		
		try{
			for (VehicleInfo inf : VehicleManager.getInstance().getLearnedVehicles().values()) {
				if(inf.getLat() != null){
					url = String.format(getServerAddress() + "/Update?vid=" + inf.getvID() + ";lat=" + inf.getLat() + 
							";lon=" + inf.getLon() + ";alt=" + inf.getAlt() + ";dest=" + inf.getDest() + 
							";plist=" + inf.getpList() + ";bat=" + inf.getBat() + ";ts=" + inf.getTime());	
					contact_server(url);
				}
			}
		}catch(Exception e){
			System.out.println("Exception here: "+ e.getMessage());
		}
		
		do{
			//Call arrived at station method on central server
			
			try{
				if (passengersInVehicle!=null)
					url = String.format(getServerAddress() + "/ArrivedAtStation?vehicleID=" + id + ";" + "stationID=" + station.getId() + ";" +"freeSeats="+ capacity+ ";" +"parties="+passengersInVehicle+ ";" +"ts="+System.currentTimeMillis());
				else
					url = String.format(getServerAddress() + "/ArrivedAtStation?vehicleID=" + id + ";" + "stationID=" + station.getId() + ";" +"freeSeats="+ capacity+ ";" +"ts="+System.currentTimeMillis());
				response = contactServerNoParsing(url);
			} catch(Exception e){
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
					publishProgress("");
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					System.out.println("Exception: " + e.getMessage());
				}
			} 
		} while(true);
		
		return true;
    }

    protected void onProgressUpdate(String... progress) {
		dialog.setMessage("Station has no parties to embark. Waiting..");
    }

    protected void onPostExecute(Boolean result) {
		dialog.cancel();
    	
		battMan.rechargeAll();

		//choose next destination from among the destinations for passengers
		selectNextDest();

		//Update this.passengerlist for the selected destination. Will be used to notify on arrival
		updatePartyForNextDest();
		
		// Action: display next stop on screen
		wdest.setText(destination);
		wpname.setText(passengerList);
		wbat.setText(battMan.getBatteryLevel()*1000 + "W");
		
		new WaitEmbark().execute(station);
    }
}

class WaitEmbark extends AsyncTask<StationInfo, String, Boolean> {

	private StationInfo info;
	
	@Override
	protected Boolean doInBackground(StationInfo... params) {
		info = params[0];
		// Each stop takes a total time of 1 minute. For now 20 seconds.
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
		
		
		return true;
	}
	
    protected void onProgressUpdate(String... progress) {
		dialog.setMessage("Station has no parties to embark. Waiting..");
    }
	
	
    protected void onPostExecute(Boolean result) {
    	//show.setText("Arrived here");
		leaveStation(info);
    }
}

}

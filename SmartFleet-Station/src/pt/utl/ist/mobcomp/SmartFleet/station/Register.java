package pt.utl.ist.mobcomp.SmartFleet.station;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import pt.utl.ist.mobcomp.SmartFleet.util.HTTPClient;
import pt.utl.ist.mobcomp.SmartFleet.util.LookupUtils;
import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class Register extends Activity implements OnClickListener{


	String pname, pcount, pdest;
	EditText name;
	EditText dest;
	EditText count;
	TextView show;
	TextView server_thread;
	Button done;
	Thread st, thrd;
	Webservice service;
	InfoSocket info; 
	//List<StationInfo> activeStations;
	private static final long STATIONS_QUERY_TIME = 10000L;
	//ArrayAdapter<String> adapter;
	
	String id;
	String stationName;
	String lat;
	String lon;
	String stationIP;
	String port;
	String serverIP;
	String serverPort;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		name = (EditText) this.findViewById(R.id.name);
		dest = (EditText) this.findViewById(R.id.dest);
		count = (EditText) this.findViewById(R.id.count);
		show = (TextView) this.findViewById(R.id.show);
		server_thread = (TextView) this.findViewById(R.id.server_thread);
		done = (Button)this.findViewById(R.id.done);
		done.setOnClickListener(this);
		info = new InfoSocket();
		
		Properties prop = LookupUtils.readPropertiesFile(this.getResources().getAssets(), "station.conf");
		id = prop.getProperty("id");
		stationName = prop.getProperty("name");
		lat = prop.getProperty("lat");
		lon = prop.getProperty("lon");
		stationIP = prop.getProperty("station_ip");
		port = prop.getProperty("port");
		serverIP = prop.getProperty("server_ip");
		serverPort = prop.getProperty("server_port");
		
		//try to register station 
		register_station();

		System.out.println("ONCREATE");

	}


	public void onRestart(){
		super.onRestart();
		System.out.println("ON RESTART");
	}



	public void onResume(){
		super.onResume();	

		//Start a thread that listens to incoming connections from the server.
		service = new Webservice(this);
		st = new Thread(service);
		st.start();
		
		System.out.println("ONRESUME");

	}


	
	
	private String getServerAddress() {
		return "http://" + serverIP + ":" + serverPort;
	}

	public void onPause(){
		super.onPause();
		System.out.println("ONPAUSE");
		if (thrd != null)
			thrd.interrupt();
		thrd = null;
	}

	public void onStop(){
		super.onStop();
		try{
			service.closeSocket();
		}catch(Exception e){
			e.printStackTrace();
		}
		if (thrd != null)
			thrd.interrupt();
		thrd = null;
		System.out.println("ONSTOP");
	}

	/*	public void onDestroy(){
		super.onDestroy();
		//st.stop();
		try{
		service.closeSocket();
		}catch(Exception e){
			e.printStackTrace();
		}
		System.out.println("ONDESTROY");
	}*/

	@Override
	public void onClick(View v) {
		
		pname = name.getText().toString();
		pcount = count.getText().toString();
		pdest = dest.getText().toString();

		Double lat = 0.0;
		Double lon = 0.0;
		
//		Address location = getLocation(pdest);
//		if(location != null){
//			lat = location.getLatitude();
//			lon = location.getLongitude();
//		}
		
		JSONObject locationInfo = getLocationInfo(pdest);
		if(locationInfo != null){
			Location loc = getLocation(locationInfo);
			lat = loc.getLatitude();
			lon = loc.getLongitude();
		}
		
		//Try to register party.
		String url,response=null,status;
		try{
			url = String.format(getServerAddress() + "/RegisterParty?stationID=" + id + ";partyName=%s;numPassengers=%s;dest=%s;destLat=%s;destLon=%s",pname,pcount,pdest,lat.toString(),lon.toString());
			response = contact_server(url);
		} catch(Exception e){
			show.setText("Exception: " + e.getMessage());
			System.out.println("Exception "+ e.getMessage());
		}
		status = validate(response);

		if (status == "SUCCESS")
			show.setText("Party Registration Success!!");
		else if (status == "FAIL")
			show.setText("Party already registered!");
		else
			show.setText("Server could not be contacted, Please re-submit!!");

	}
	
	public Address getLocation(String dest){
        Geocoder geoCoder = new Geocoder(this);    
        try {
            List<Address> addresses = geoCoder.getFromLocationName(dest, 1);
            if (addresses.size() > 0) {
            	return addresses.get(0);
            }  
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

	private String contact_server(String url) throws Exception
	{

		//Make a get Request to the server
		String response = HTTPClient.executeHttpGet(url);

		if(response != null){
			//Parse the received xml to see if the registration was successful or not.	
			return parsexml(response);
		}
		
		return null;
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


	private void register_station()
	{
		String url,response=null,status;
		try{
			url = String.format(getServerAddress() + "/RegisterStation?id=" + id + ";name=" + stationName + ";lat=" + lat + ";lon=" + lon + ";ip=" + stationIP + ";port=" + port);
			response = contact_server(url);
		} catch(Exception e){
			System.out.println("Exception "+ e.getMessage());
		}
		status =validate(response);

		if (status == "SUCCESS")
			show.setText("Station Registration Success!!");
		else if (status == "FAIL")
			show.setText("Station already registered!");
		else
			show.setText("Server could not be contacted. Please close and relaunch.");

	}


	public void callForBoarding(){
		Intent intent = new Intent(this, Announce.class);
		intent.putExtra("names",info.getAnn_pname());
		intent.putExtra("vehicleID",info.getAnn_vehicleID());
		startActivity(intent);
	}

	public void receiveDetails(String name, String vid){
		info.setAnn_pname(name);
		info.setAnn_vehicleID(vid);
		callForBoarding();
	}
	
	public static JSONObject getLocationInfo(String address) {

		address = address.replaceAll(" ", "+");
		HttpGet httpGet = new HttpGet("http://maps.google."
				+ "com/maps/api/geocode/json?address=" + address
				+ "ka&sensor=false");
		HttpClient client = new DefaultHttpClient();
		HttpResponse response;
		StringBuilder stringBuilder = new StringBuilder();

		try {
			response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			InputStream stream = entity.getContent();
			int b;
			while ((b = stream.read()) != -1) {
				stringBuilder.append((char) b);
			}
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = new JSONObject(stringBuilder.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return jsonObject;
	}
	
	public static Location getLocation(JSONObject jsonObject) {

		Double lon = new Double(0);
		Double lat = new Double(0);

		try {

			lon = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
				.getJSONObject("geometry").getJSONObject("location")
				.getDouble("lng");

			lat = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
				.getJSONObject("geometry").getJSONObject("location")
				.getDouble("lat");

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Location loc = new Location(LocationManager.GPS_PROVIDER);
		loc.setLatitude(lat);
		loc.setLongitude(lon);
		
		return loc;
	}
}

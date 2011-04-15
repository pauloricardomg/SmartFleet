package pt.utl.ist.mobcomp.SmartFleet.station;
import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import pt.utl.ist.mobcomp.SmartFleet.util.HTTPClient;
import pt.utl.ist.mobcomp.SmartFleet.util.XmlUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class Register extends Activity implements OnClickListener{


	String pname, pcount, pdest;
	EditText name;
	Spinner dest;
	EditText count;
	TextView show;
	TextView server_thread;
	Button done;
	Thread st, thrd;
	Webservice service;
	InfoSocket info; 
	List<StationInfo> activeStations;
	private static final long STATIONS_QUERY_TIME = 10000L;
	ArrayAdapter<String> adapter;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		name = (EditText) this.findViewById(R.id.name);
		dest = (Spinner) this.findViewById(R.id.dest);
		count = (EditText) this.findViewById(R.id.count);
		show = (TextView) this.findViewById(R.id.show);
		server_thread = (TextView) this.findViewById(R.id.server_thread);
		done = (Button)this.findViewById(R.id.done);
		done.setOnClickListener(this);
		info = new InfoSocket();

		//Set the spinner in the beginning
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dest.setAdapter(adapter);
		
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

		//Start a thread that constantly polls for new stations
		if(thrd == null){
			thrd = new Thread(getDestination());
			thrd.start();
		}

		System.out.println("ONRESUME");

	}

	
	
	private Runnable getDestination(){

		return new Runnable() {
			public void run() {
				while (!Thread.interrupted()) {
					activeStations = lookupStations("http://192.168.1.78:8080/GetAllStations");
					while (activeStations!=null){
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								List<String> destination = getStationList(activeStations);
								adapter.clear();
								for (int i=0;i<destination.size();i++) {

										adapter.add(destination.get(i));
									}
							}
						});
						
					}
					try {
						Thread.sleep(STATIONS_QUERY_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
	}
	
	
	public void updateSpinner(){

		adapter.notifyDataSetChanged();
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
		pdest = dest.getSelectedItem().toString();

		//Try to register party.
		String url,response=null,status;
		try{
			url = String.format("http://192.168.1.78:8080/RegisterParty?stationID=12345;partyName=%s;numPassengers=%s;dest=%s",pname,pcount,pdest);
			response = contact_server(url);
		} catch(Exception e){
			System.out.println("Exception "+ e.getMessage());
		}
		status = validate(response);

		if (status == "SUCCESS")
			show.setText("Party Registration Success!!");
		else if (status == "FAIL")
			show.setText("Party already registered!");
		else
			show.setText("Request timed out, Please re-submit!!");

	}


	private List<String> getStationList(List<StationInfo> infos){
		List<String> list = new LinkedList<String>();

		if(infos != null){
			for (StationInfo info : infos) {
				list.add(info.getName());
			}
		}

		return list;
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


	private List<StationInfo> lookupStations(String url)
	{

		//Make a get Request to the server
		String response = null;
		try {
			response = HTTPClient.executeHttpGet(url);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		//Parse the received xml to see if the registration was successful or not.	
		List<StationInfo> result = null;
		try {
			result = XmlUtils.parsexml(response);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}



	private String contact_server(String url)
	{

		//Make a get Request to the server
		String response = null;
		try {
			response = HTTPClient.executeHttpGet(url);
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


	private void register_station()
	{
		String url,response=null,status;
		try{
			url = String.format("http://192.168.1.78:8080/RegisterStation?id=12345;name=Alameda;lat=53.123456;lon=22.1234567;ip=127.0.0.1;port=4001");
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
			show.setText("Request timed out!! Please close and relaunch.");

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

}

package pt.utl.ist.mobcomp.SmartFleet.station;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import pt.utl.ist.mobcomp.SmartFleet.util.HTTPClient;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Register extends Activity implements OnClickListener{


	String sid, sname, slat, slon, sport, server_ip, server_port;
	String pname, pcount, pdest;
	EditText name;
	EditText dest;
	EditText count;
	TextView show;
	TextView server_thread;
	Button done;

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

		// read_configuration();


		//Start a thread that listens to incoming connections from the server.
		Thread st = new Thread(new webservice(this));
		st.start();



		//Register the station at the server.
		register_station();
	}

	@Override
	public void onClick(View v) {

		pname = name.getText().toString();
		pcount = count.getText().toString();
		pdest = dest.getText().toString();

		//Try to register party.
		String url,response=null,status;
		url = String.format("http://192.168.1.78:8080/RegisterParty?stationID=12345;partyName=%s;numPassengers=%s;dest=%s",pname,pcount,pdest);

		response = contact_server(url);
		status = validate(response);

		if (status == "SUCCESS")
			show.setText("Party Registration Success!!");
		else if (status == "FAIL")
			show.setText("Party already registered!");
		else
			show.setText("Request timed out, Please re-submit!!");

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
		url = String.format("http://192.168.1.78:8080/RegisterStation?id=12345;name=Alameda;lat=53.123456;lon=22.1234567;ip=127.0.0.1;port=4001");
		response = contact_server(url);
		status =validate(response);

		if (status == "SUCCESS")
			show.setText("Station Registration Success!!");
		else if (status == "FAIL")
			show.setText("Station already registered!");
		else
			show.setText("Request timed out");

	}

	private void read_configuration(){

		DataInputStream dis = null;
		String result = null, buffer;
		try { 
			File f = new File("station.conf");
			FileInputStream fis = new FileInputStream(f); 
			BufferedInputStream bis = new BufferedInputStream(fis); 
			dis = new DataInputStream(bis);
			while((buffer=dis.readLine())!=null){
				result=result+buffer;
			}
			tokenize(result);
			dis.close();
		}catch (IOException e) { 
			// catch io errors from FileInputStream or readLine() 
			System.out.println("Uh oh, got an IOException error: " + e.getMessage()); 

		}
	}

	private void tokenize(String result){
		// show.setText(result);
		StringTokenizer st = new StringTokenizer(result, "=");
		st.nextToken();
		sid=st.nextToken();
		st.nextToken();
		sname=st.nextToken();
		st.nextToken();
		slat=st.nextToken();
		st.nextToken();
		slon=st.nextToken();
		st.nextToken();
		sport=st.nextToken();
		st.nextToken();
		server_ip=st.nextToken();
		st.nextToken();
		server_port=st.nextToken();


	}
	
	
}

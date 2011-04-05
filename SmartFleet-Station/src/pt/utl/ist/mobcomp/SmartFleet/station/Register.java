package pt.utl.ist.mobcomp.SmartFleet.station;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;

public class Register extends Activity implements OnClickListener{
	
	TextView name;
	TextView dest;
	TextView count;
	TextView show;
	Button done;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        name = (EditText) this.findViewById(R.id.name);
        dest = (EditText) this.findViewById(R.id.dest);
        count = (EditText) this.findViewById(R.id.count);
        show = (TextView) this.findViewById(R.id.show);
        
         done = (Button)this.findViewById(R.id.done);
         done.setOnClickListener(this);
    }
    
	@Override
	public void onClick(View v) {
		
	//FOR TESTING PURPOSE
	//	Uri uri = Uri.parse("http://192.168.1.78:8080/RegisterParty?stationID=1;partyName=partyName;numPassengers=2;dest=abc");
	//	 Intent intent = new Intent(Intent.ACTION_VIEW, uri);
	//	 startActivity(intent);
		
		String sname, scount, sdest;
		
		sname = name.getText().toString();
		scount = (String) count.getText().toString();
		sdest = (String) dest.getText().toString();
		
		String url;
		url = String.format("http://192.168.1.78:8080/RegisterParty?stationID=1;partyName=%s;numPassengers=%s;dest=%s",sname,scount,sdest);
		
	//	 Make a get Request to the server.
		String response = null;
		try {
		    response = Client.executeHttpGet(url);
		} catch (Exception e) {
		    e.printStackTrace();
		  }
		
		try {
			response = parsexml(response);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (response.equals("1")){
			show.setText("Success!!");
		}
		else
			show.setText("Exists already!");

	}
	
	String parsexml (String response)
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
	         } // end else if
	      }  // end while	    
		return value;
		
		}

}

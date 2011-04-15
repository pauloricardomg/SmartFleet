package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class PickUpPassengers extends Activity{

	TextView show;
	 /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        show = (TextView) this.findViewById(R.id.show);
        
      //Try to register vehicle.
        register_vehicle();
		
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
	    response =  Client.executeHttpGet(url);
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

private void register_vehicle(){
	String url,response=null,status;
	try{
	url = String.format("http://194.210.224.249:8080/RegisterVehicle?id=vehicle001;capacity=5;lat=42.438917;lon=42.438917");
	response = contact_server(url);
	}catch(Exception e){
		System.out.println("Exception here: "+ e.getMessage());
	}
	status = validate(response);
	 if (status == "SUCCESS")
			show.setText("Vehicle Registration Success!!");
		else if (status == "FAIL")
			show.setText("Vehicle already registered!");
		else
			show.setText("Vehicle registration request timed out!! Please relaunch");
 
}

}

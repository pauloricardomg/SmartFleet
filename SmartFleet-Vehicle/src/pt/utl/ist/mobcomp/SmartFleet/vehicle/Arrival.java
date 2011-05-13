package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;

public class Arrival extends Activity {

	EditText dest;
	EditText partyNames;
	String destination;
	String pList;
	Boolean isStation;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.arrival);
        
        dest = (EditText) this.findViewById(R.id.dest);
        partyNames =(EditText) this.findViewById(R.id.partyNames);
        
        Bundle extras = getIntent().getExtras();
        destination = extras.getString("destination");
        pList = extras.getString("passenger");
        isStation = extras.getBoolean("station");
              
        dest.setText(destination);
        partyNames.setText(pList);
        
        new WaitDisembark().execute("");
       
	}
	
	class WaitDisembark extends AsyncTask<String, String, Boolean> {

		@Override
		protected Boolean doInBackground(String... params) {
			// Each stop takes a total time of 1 minute. For now 20 seconds.
			try {
				Thread.sleep(8000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
			
			
			return true;
		}
		
	    protected void onProgressUpdate(String... progress) {
	    }
		
		
	    protected void onPostExecute(Boolean result) {
	        
	        if(isStation)
	        {
	        	Intent intent1 = new Intent();
		    	setResult(1, intent1);
		        finish();
			}
	        
	        else
	        {
	        	Intent intent1 = new Intent();
		    	setResult(0, intent1);
		        finish();
	        }
	    }
	}
}

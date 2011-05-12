package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;

public class Arrival extends Activity {

	EditText dest;
	EditText partyNames;
	String destination;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.arrival);
        
        dest = (EditText) this.findViewById(R.id.dest);
        partyNames =(EditText) this.findViewById(R.id.partyNames);
        
        Bundle extras = getIntent().getExtras();
        destination = extras.getString("destination");
        
        dest.setText(destination);
	}
}

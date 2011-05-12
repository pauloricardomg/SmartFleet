package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.util.List;

import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

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

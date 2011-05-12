package pt.utl.ist.mobcomp.SmartFleet.station;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;


public class Announce extends Activity implements OnClickListener{
    /** Called when the activity is first created. */
	
	String party_names;
	String vehicleID;
	EditText pnames;
	EditText vid;
	Button back;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display);
        pnames =(EditText)this.findViewById(R.id.pnames);
        vid =(EditText)this.findViewById(R.id.vid);
        back = (Button)this.findViewById(R.id.back_button);
		back.setOnClickListener(this);
        
            //get the Bundle out of the Intent...
            Bundle extras = getIntent().getExtras();
            party_names = extras.getString("names") ;
            vehicleID = extras.getString("vehicleID");
            
       pnames.setText(party_names);
       vid.setText(vehicleID);
    }

    public void onClick(View v) {
    	Intent intent = new Intent();
    	setResult(RESULT_OK, intent);
        finish();
    }
    
}
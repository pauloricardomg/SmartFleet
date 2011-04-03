package pt.utl.ist.mobcomp.SmartFleet.station;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;

public class Register extends Activity implements OnClickListener{
	
	Button done;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
         done = (Button)this.findViewById(R.id.done);
         done.setOnClickListener(this);      
    }

	@Override
	public void onClick(View v) {
		Intent intent = new Intent(this, Announce.class);
		startActivity(intent);

	}
}
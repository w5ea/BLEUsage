package cn.way.wandroid.bluetooth.leusage;

import cn.way.bleusage.R;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

/**
 * @author Wayne
 * @2015年2月4日
 */
public class BleConnectionActivity extends Activity {
	public static final String EXTRA_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.page_connection);
		String deviceAddress = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);
		if (deviceAddress!=null) {
			
		}else{
			Toast.makeText(this, "no address", Toast.LENGTH_LONG).show();
		}
	}
}

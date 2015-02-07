package cn.way.wandroid.bluetooth.leusage;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.widget.Toast;
import cn.way.bleusage.R;
import cn.way.wandroid.bluetooth.BleManager;
import cn.way.wandroid.bluetooth.BleManager.BleAvailableException;

/**
 * @author Wayne
 * @2015年2月4日
 */
public class BleServerActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.page_server);
		try {
			BleManager bleManager = BleManager.instance(this);
			bleManager.createServer(new BluetoothGattServerCallback() {
				@Override
				public void onConnectionStateChange(BluetoothDevice device,
						int status, int newState) {
					super.onConnectionStateChange(device, status, newState);
					Toast.makeText(getApplicationContext(), "state="+newState, 0).show();
				}
				@Override
				public void onServiceAdded(int status,
						BluetoothGattService service) {
					super.onServiceAdded(status, service);
					Toast.makeText(getApplicationContext(), "state="+status, 0).show();
				}
			});
		} catch (BleAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

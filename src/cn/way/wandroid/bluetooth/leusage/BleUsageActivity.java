package cn.way.wandroid.bluetooth.leusage;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import cn.way.bleusage.R;
import cn.way.wandroid.bluetooth.BleManager;
import cn.way.wandroid.bluetooth.BleManager.BleSupportException;
import cn.way.wandroid.bluetooth.BleManager.DeviceState;
import cn.way.wandroid.bluetooth.BleManager.DeviceStateListener;

public class BleUsageActivity extends Activity {

	private BleManager bluetoothManager;

	@Override
	protected void onDestroy() {
		if (bluetoothManager!=null) {
			bluetoothManager.release();
		}
		super.onDestroy();
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.page_bleusage);
		try {
			bluetoothManager = BleManager.instance(this);
			bluetoothManager.setDeviceStateListener(new DeviceStateListener() {
				@Override
				public void onStateChanged(DeviceState state) {
					switch (state) {
					case TURNING_ON:
						getActionBar().setTitle("正在开启蓝牙");
						break;
					case ON:
						getActionBar().setTitle("蓝牙已经开启");
						FragmentTransaction ft = getFragmentManager().beginTransaction();
						FriendsFragment hf = new FriendsFragment();
						hf.setBluetoothManager(bluetoothManager);
						ft.replace(R.id.bluetooth_page_main_root, hf);
						ft.commit();
						break;
					case OFF:
						break;
					case TURNING_OFF:
						break;
					}
				}
			});
			bluetoothManager.enable();
		} catch (BleSupportException e) {
			Toaster.instance(this).setup(e.toString()).show();
		}
	}
}

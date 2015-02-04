package cn.way.wandroid.bluetooth.leusage;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.widget.Toast;
import cn.way.bleusage.R;
import cn.way.wandroid.bluetooth.BleManager;
import cn.way.wandroid.bluetooth.BleManager.BleAvailableException;
import cn.way.wandroid.bluetooth.BleManager.DeviceState;
import cn.way.wandroid.bluetooth.BleManager.DeviceStateListener;

public class BleUsageActivity extends Activity {

	private BleManager bluetoothManager;
	private FriendsFragment friendsFragment;
	private DeviceStateListener deviceStateListener = new DeviceStateListener() {
		@Override
		public void onStateChanged(DeviceState state) {
			Toast.makeText(getApplicationContext(), "蓝牙状态："+state, Toast.LENGTH_SHORT).show();
			switch (state) {
			case TURNING_ON:
				getActionBar().setTitle("正在开启蓝牙");
				break;
			case ON:
				getActionBar().setTitle("蓝牙已经开启");
				if (friendsFragment==null) {
					FragmentTransaction ft = getFragmentManager().beginTransaction();
					friendsFragment = new FriendsFragment();
					friendsFragment.setBluetoothManager(bluetoothManager);
					ft.replace(R.id.bluetooth_page_main_root, friendsFragment);
					ft.commit();
				}
				break;
			case OFF:
				getActionBar().setTitle("蓝牙已经关闭");
				break;
			case TURNING_OFF:
				getActionBar().setTitle("正在关闭蓝牙");
				break;
			}
		}
	};
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
			bluetoothManager.setDeviceStateListener(deviceStateListener);
			bluetoothManager.enable();
		} catch (BleAvailableException e) {
			Toaster.instance(this).setup(e.toString()).show();
		}
	}
	@Override
	protected void onStop() {
		super.onStop();
		bluetoothManager.pause();
	}
	@Override
	protected void onResume() {
		super.onResume();
		bluetoothManager.resume();
	}
}

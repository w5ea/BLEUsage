package cn.way.wandroid.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;

/**
 * 1.add permissions. 
 * <uses-permission android:name="android.permission.BLUETOOTH" /> 
 * <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
 * 2.declare the ble feature
 * <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
 * @author Wayne
 */
public class BLEManager {
	public static final int REQUEST_CODE__ENABLE_BT = 1001;
	public static final int REQUEST_CODE__DISCOVERABLE_BT = 1002;
	private static BLEManager manager;

	public static BLEManager instance(Context context)
			throws BleSupportException {
		if (!BLEManager.isBleSupported(context)) {
			throw new BleSupportException();
		}
		if (context == null) {
			return null;
		}
		if (manager == null) {
			manager = new BLEManager(context.getApplicationContext());
		}
		return manager;
	}

	public static boolean isBleSupported(Context context) {
		//判断设备支持BLE
		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			return false;
		}
		//尝试获取蓝牙适配器实例
		BluetoothAdapter mBluetoothAdapter = getBluetoothAdapter(context);
		if (mBluetoothAdapter == null) {
			return false;
		} else {
			return true;
		}
	}
	public static BluetoothManager getBluetoothManager(Context context){
		return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
	}
	public static BluetoothAdapter getBluetoothAdapter(Context context){
		BluetoothManager bluetoothManager = getBluetoothManager(context);
		if (bluetoothManager!=null) {
			return bluetoothManager.getAdapter();
		}
		return null;
	}
	public static boolean isBluetoothEnabled() {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();
		if (!mBluetoothAdapter.isEnabled()) {
			return false;
		}
		return true;
	}

	public static void requestEnable(Activity activity) {
		Intent enableBtIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_ENABLE);
		activity.startActivityForResult(enableBtIntent, REQUEST_CODE__ENABLE_BT);
	}

	public static void requestDiscoverable(Activity activity) {
		Intent discoverableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		// maximum duration is 300 seconds,default is 120 seconds.
		discoverableIntent.putExtra(
				BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		activity.startActivityForResult(discoverableIntent,
				REQUEST_CODE__DISCOVERABLE_BT);
	}

	public static String getBondState(BluetoothDevice bd) {
		/**
		 * {@link #BOND_NONE}, {@link #BOND_BONDING}, {@link #BOND_BONDED}.
		 */
		String state = "未配对";
		if (bd.getBondState() == BluetoothDevice.BOND_BONDING) {
			state = "正在配对";
		}
		if (bd.getBondState() == BluetoothDevice.BOND_BONDED) {
			state = "已配对";
		}
		return state;
	}

	// //////////////////////////////////////////////////////////////////////////////////////////
	
	private BluetoothAdapter mBluetoothAdapter;
	private Context context;
	private DeviceStateListener deviceStateListener;
	
	public void release() {
		// Make sure we're not doing discovery anymore
		cancelLeScan();
		// Unregister broadcast listeners
		if (this.context != null && mReceiver != null)
			this.context.unregisterReceiver(mReceiver);
	}

	/**
	 * @param context
	 */
	private BLEManager(Context context) {
		super();
		// 监听蓝牙设置的开关状态
		IntentFilter filter = new IntentFilter(
				BluetoothAdapter.ACTION_STATE_CHANGED);
		context.registerReceiver(mReceiver, filter);

		mBluetoothAdapter = getBluetoothAdapter(context);
	}
//	public interface Pauseable{
//		void pause();
//		void resume();
//	}
	public enum DeviceState {
		OFF, TURNING_ON, ON, TURNING_OFF
	}
	
	public interface DeviceStateListener {
		/**
		 * @param state
		 *            {@link #STATE_OFF}, {@link #STATE_TURNING_ON},
		 *            {@link #STATE_ON}, {@link #STATE_TURNING_OFF},
		 */
		void onStateChanged(DeviceState state);
	}

	private void onDeviceStateChanged(int state) {
		if (deviceStateListener != null) {
			DeviceState ds = DeviceState.OFF;
			if (state == BluetoothAdapter.STATE_TURNING_ON) {
				ds = DeviceState.TURNING_ON;
			}
			if (state == BluetoothAdapter.STATE_ON) {
				ds = DeviceState.ON;
			}
			if (state == BluetoothAdapter.STATE_TURNING_OFF) {
				ds = DeviceState.TURNING_OFF;
			}
			deviceStateListener.onStateChanged(ds);
		}
	}

	public boolean isEnabled() {
		if (!mBluetoothAdapter.isEnabled()) {
			return false;
		}
		return true;
	}

	//开启蓝牙
	public void enable() {
		if (!mBluetoothAdapter.isEnabled()) {
			mBluetoothAdapter.enable();
		} else {
			onDeviceStateChanged(BluetoothAdapter.STATE_ON);
		}
	}
	//关闭蓝牙
	public void disable(){
		if (mBluetoothAdapter.isEnabled()) {
			mBluetoothAdapter.disable();
		} else {
			onDeviceStateChanged(BluetoothAdapter.STATE_OFF);
		}
	}
	
	

	// The BroadcastReceiver that listens for discovered devices and
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// Device Adapter state changed
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
						BluetoothAdapter.STATE_OFF);
				onDeviceStateChanged(state);
			}
		}
	};

	public DeviceStateListener getDeviceStateListener() {
		return deviceStateListener;
	}

	public void setDeviceStateListener(DeviceStateListener deviceStateListener) {
		this.deviceStateListener = deviceStateListener;
	}

	/**
	 * Device does not support Ble
	 * @author Wayne
	 */
	public static class BleSupportException extends Exception {
		private static final long serialVersionUID = -5168381347847106511L;
		@Override
		public String getMessage() {
			return "当前设备不支持BLE";
		}
	}

//查找//////////////////////////////////////////////////////////////////////////////////
	public interface ScanListener {
		/**
		 * the list of devices changed, update view should be done.
		 * @param devices
		 */
		void onDevicesChanged(Collection<BluetoothDevice> devices);
		void onFinished();
	}

	private ScanListener scanListener;
	private Hashtable<String, BluetoothDevice> devices = new Hashtable<String, BluetoothDevice>();

	private BluetoothDevice addDevice(BluetoothDevice device) {
		if (device != null) {
			//如果已经存在相同设备（地址相同）则替换,并返回旧设备
			return devices.put(device.getAddress(), device);
		}
		return null;
	}
	
	// Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            
        }
    };
	/**
	 * scan the nearby devices
	 * involves an inquiry scan of about 12 seconds
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public void startLeScan(ScanListener l) {
		this.scanListener = l;
		cancelLeScan();
		// Request discover from BluetoothAdapter
		mBluetoothAdapter.startLeScan(mLeScanCallback);
	}

	public void cancelLeScan() {
		if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		}
	}

	public boolean isDiscovering() {
		return mBluetoothAdapter.isDiscovering();
	}

	/**
	 * Get a set of currently paired devices
	 * @return
	 */
	public Set<BluetoothDevice> getBondedDevices() {
		return BluetoothAdapter.getDefaultAdapter().getBondedDevices();
	}

	public ScanListener getDiscoveryListener() {
		return scanListener;
	}

	public void setDiscoveryListener(ScanListener discoveryListener) {
		this.scanListener = discoveryListener;
	}
	
//IO//////////////////////////////////////////////////////////////////////////////////
	
}

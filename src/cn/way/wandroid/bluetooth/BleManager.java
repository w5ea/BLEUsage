package cn.way.wandroid.bluetooth;

import java.util.Collection;
import java.util.Hashtable;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;

/**
 * 1.add permissions. 
 * <uses-permission android:name="android.permission.BLUETOOTH" /> 
 * <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
 * 2.declare the ble feature
 * <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
 * @author Wayne
 */
public class BleManager {
	public static final int REQUEST_CODE__ENABLE_BT = 1001;
	public static final int REQUEST_CODE__DISCOVERABLE_BT = 1002;
	private static BleManager manager;

	public static BleManager instance(Context context)
			throws BleSupportException {
		if (!BleManager.isBleSupported(context)) {
			throw new BleSupportException();
		}
		if (context == null) {
			return null;
		}
		if (manager == null) {
			manager = new BleManager(context.getApplicationContext());
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
		scanListener = null;
		// Make sure we're not doing discovery anymore
		stopScan();
		// Unregister broadcast listeners
		if (this.context != null && mReceiver != null)
			this.context.unregisterReceiver(mReceiver);
	}

	/**
	 * @param context
	 */
	private BleManager(Context context) {
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
	// Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 1000*10;
    private boolean isScanning;
    private Handler scnaHandler = new Handler();
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
            addDevice(device);
            scnaHandler.post(new Runnable() {
				@Override
				public void run() {
					if (scanListener!=null) {
						scanListener.onDevicesChanged(devices.values());
					}
				}
			});
        }
    };
	/**
	 * scan the nearby devices
	 * @return
	 */
	public void startScan(ScanListener l) {
		if (isScanning) {
			return;
		}
		this.scanListener = l;
		stopScan();
		isScanning = true;
		// Request discover from BluetoothAdapter
		mBluetoothAdapter.startLeScan(mLeScanCallback);
		scnaHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
            	stopScan();
            }
        }, SCAN_PERIOD);
	}

	public void stopScan() {
		if (!isScanning) {
			return;
		}
		isScanning = false;
		mBluetoothAdapter.stopLeScan(mLeScanCallback);
		if (scanListener!=null) {
        	scanListener.onFinished();
		}
	}

	public ScanListener getScanListener() {
		return scanListener;
	}

	public void setDiscoveryListener(ScanListener discoveryListener) {
		this.scanListener = discoveryListener;
	}
	
//IO//////////////////////////////////////////////////////////////////////////////////
	public Connection createConnection(){
		Connection conn = new Connection();
		return conn;
	}
	public enum ConnectionState {
		DISCONNECTED, // 未连接或连接断开
		CONNECTING, // 正在试图去连接对方或接受连接
		CONNECTED// 已经连接
	}
	public interface BluetoothConnectionListener {
		/**
		 * @param state 当前状态
		 */
		void onConnectionStateChanged(ConnectionState state);
	}
	class Connection{
		private String serverDeviceName;
	    private String serverDeviceAddress;

		protected ConnectionState state = ConnectionState.DISCONNECTED;
		public void disconnect(){
			
		}
		public void connect(){
			
		}
		public void connect(BluetoothDevice device){
			this.serverDeviceName = device.getName();
			this.serverDeviceAddress = device.getAddress();
		}
		public void connect(String serverDeviceAddress){
			this.serverDeviceAddress = serverDeviceAddress;
		}
		public void connect(String serverDeviceName,String serverDeviceAddress){
			this.serverDeviceName = serverDeviceName;
			this.serverDeviceAddress = serverDeviceAddress;
		}
		public void read(){
			
		}
		public void write(String chars){
			write(chars.getBytes());
		}
		public void write(byte[] data) {
			if (data==null||data.length==0) {
				return;
			}
	        // Synchronize a copy of the ConnectedThread
	        synchronized (this) {
	            if (state != ConnectionState.CONNECTED) return;
	        }
	        // Perform the write unsynchronized
	        
	    }
		public String getServerDeviceName() {
			return serverDeviceName;
		}
		public void setServerDeviceName(String serverDeviceName) {
			this.serverDeviceName = serverDeviceName;
		}
	}
	public static UUID UUID_SERVICE =
	            UUID.fromString("wayne");
	public static UUID UUID_C1 =
			UUID.fromString("green");
	public void createServer(){
		BluetoothGattServer server = getBluetoothManager(context).openGattServer(context, new BluetoothGattServerCallback() {
			@Override
			public void onConnectionStateChange(BluetoothDevice device,
					int status, int newState) {
				super.onConnectionStateChange(device, status, newState);
			}

			@Override
			public void onServiceAdded(int status, BluetoothGattService service) {
				super.onServiceAdded(status, service);
			}

			@Override
			public void onCharacteristicReadRequest(BluetoothDevice device,
					int requestId, int offset,
					BluetoothGattCharacteristic characteristic) {
				super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
			}

			@Override
			public void onCharacteristicWriteRequest(BluetoothDevice device,
					int requestId, BluetoothGattCharacteristic characteristic,
					boolean preparedWrite, boolean responseNeeded, int offset,
					byte[] value) {
				super.onCharacteristicWriteRequest(device, requestId, characteristic,
						preparedWrite, responseNeeded, offset, value);
			}

			@Override
			public void onExecuteWrite(BluetoothDevice device, int requestId,
					boolean execute) {
				super.onExecuteWrite(device, requestId, execute);
			}
			
		});
		BluetoothGattService service = new BluetoothGattService(UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);
//		BluetoothGa/ttCharacteristic c = new BluetoothGattCharacteristic(UUID_C1, properties, permissions)
//		service.addCharacteristic(characteristic)
		server.addService(service);
	}
}

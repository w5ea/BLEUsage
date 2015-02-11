package cn.way.wandroid.bluetooth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;

/**
 * 1.add permissions. <uses-permission
 * android:name="android.permission.BLUETOOTH" /> <uses-permission
 * android:name="android.permission.BLUETOOTH_ADMIN" /> 2.declare the ble
 * feature <uses-feature android:name="android.hardware.bluetooth_le"
 * android:required="true" />
 * 
 * @author Wayne
 */
public class BleManager {
	public static final int REQUEST_CODE__ENABLE_BT = 1001;
	public static final int REQUEST_CODE__DISCOVERABLE_BT = 1002;
	private static BleManager manager;

	public static BleManager instance(Context context)
			throws BleAvailableException {
		if (!BleManager.isBleAvailable(context)) {
			throw new BleAvailableException();
		}
		if (context == null) {
			return null;
		}
		if (manager == null) {
			manager = new BleManager(context);
		}
		return manager;
	}

	public static boolean isBleAvailable(Context context) {
		// 判断设备支持BLE
		if (!context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			return false;
		}
		// 尝试获取蓝牙适配器实例
		BluetoothAdapter mBluetoothAdapter = getBluetoothAdapter(context);
		if (mBluetoothAdapter == null) {
			return false;
		} else {
			return true;
		}
	}

	public static BluetoothManager getBluetoothManager(Context context) {
		return (BluetoothManager) context
				.getSystemService(Context.BLUETOOTH_SERVICE);
	}

	public static BluetoothAdapter getBluetoothAdapter(Context context) {
		BluetoothManager bluetoothManager = getBluetoothManager(context);
		if (bluetoothManager != null) {
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

	private Context context;
	private DeviceStateListener deviceStateListener;

	public void release() {
		scanListener = null;
		stopScan();
		unregisterBroadcast();
		manager = null;
	}

	/**
	 * @param context
	 */
	private BleManager(Context context) {
		super();
		this.context = context.getApplicationContext();
		this.isPaused = false;
		// 监听蓝牙设置的开关状态
		registerBroadcast();
	}

	private IntentFilter intentFilterStateChanged;

	private void registerBroadcast() {
		if (this.context == null) {
			return;
		}

		if (intentFilterStateChanged == null) {
			intentFilterStateChanged = new IntentFilter(
					BluetoothAdapter.ACTION_STATE_CHANGED);
		}
		if (mReceiver == null) {
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (isPaused) {
						return;
					}
					String action = intent.getAction();
					// Device Adapter state changed
					if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
						int state = intent.getIntExtra(
								BluetoothAdapter.EXTRA_STATE,
								BluetoothAdapter.STATE_OFF);
						onDeviceStateChanged(state);
					}
				}
			};
		}
		this.context.registerReceiver(mReceiver, intentFilterStateChanged);

	}

	private void unregisterBroadcast() {
		// Unregister broadcast listeners
		if (this.context != null && mReceiver != null) {
			this.context.unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}

	private boolean isPaused;

	public void pause() {
		// Make sure we're not doing discovery anymore
		stopScan();
		isPaused = true;
	}

	public void resume() {
		isPaused = false;
	}

	// public static interface Pauseable {
	// void pause();
	// void resume();
	// }

	public enum DeviceState {
		OFF, TURNING_ON, ON, TURNING_OFF
	}

	public interface DeviceStateListener {
		/**
		 * 如果执行bleManager.pause()方法，回调将不会执行
		 * 
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
		BluetoothAdapter adapter = getBluetoothAdapter(context);
		if (adapter != null && adapter.isEnabled()) {
			return true;
		}
		return false;
	}

	// 开启蓝牙
	public void enable() {
		BluetoothAdapter adapter = getBluetoothAdapter(context);
		if (adapter == null) {
			return;
		}
		resume();
		if (!adapter.isEnabled()) {
			adapter.enable();
		} else {
			onDeviceStateChanged(BluetoothAdapter.STATE_ON);
		}
	}

	// 关闭蓝牙
	public void disable() {
		BluetoothAdapter adapter = getBluetoothAdapter(context);
		if (adapter == null) {
			return;
		}
		if (adapter.isEnabled()) {
			adapter.disable();
		} else {
			onDeviceStateChanged(BluetoothAdapter.STATE_OFF);
		}
	}

	private BroadcastReceiver mReceiver;

	public DeviceStateListener getDeviceStateListener() {
		return deviceStateListener;
	}

	public void setDeviceStateListener(DeviceStateListener deviceStateListener) {
		this.deviceStateListener = deviceStateListener;
	}

	/**
	 * Ble is not available
	 * 
	 * @author Wayne
	 */
	public static class BleAvailableException extends Exception {
		private static final long serialVersionUID = -1795246380456026532L;

		@Override
		public String getMessage() {
			return "当前设备BLE不可用";
		}
	}

	// 查找//////////////////////////////////////////////////////////////////////////////////
	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 3000 * 10;
	private boolean isScanning;
	public boolean isScanning() {
		return isScanning;
	}

	private Handler scnaHandler = new Handler();

	public interface ScanListener {
		/**
		 * the list of devices changed, update view should be done.
		 * 
		 * @param devices
		 */
		void onDevicesChanged(Collection<BluetoothDevice> devices);

		void onFinished();
	}

	private ScanListener scanListener;
	private Hashtable<String, BluetoothDevice> devices = new Hashtable<String, BluetoothDevice>();

	private BluetoothDevice addDevice(BluetoothDevice device) {
		if (device != null) {
			// 如果已经存在相同设备（地址相同）则替换,并返回旧设备
			return devices.put(device.getAddress(), device);
		}
		return null;
	}

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			addDevice(device);
			scnaHandler.post(new Runnable() {
				@Override
				public void run() {
					if (scanListener != null) {
						scanListener.onDevicesChanged(devices.values());
					}
				}
			});
		}
	};

	/**
	 * scan the nearby devices
	 * 
	 * @return
	 */
	public void startScan(ScanListener l) {
		BluetoothAdapter adapter = getBluetoothAdapter(context);
		if (adapter == null) {
			return;
		}
		if (isScanning) {
			return;
		}
		this.scanListener = l;
		stopScan();
		isScanning = true;
		// Request discover from BluetoothAdapter
		adapter.startLeScan(mLeScanCallback);
		scnaHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				stopScan();
			}
		}, SCAN_PERIOD);
	}

	public void stopScan() {
		BluetoothAdapter adapter = getBluetoothAdapter(context);
		if (adapter == null) {
			return;
		}
		if (!isScanning) {
			return;
		}
		isScanning = false;
		adapter.stopLeScan(mLeScanCallback);
		if (scanListener != null) {
			scanListener.onFinished();
		}
	}

	public ScanListener getScanListener() {
		return scanListener;
	}

	public void setDiscoveryListener(ScanListener discoveryListener) {
		this.scanListener = discoveryListener;
	}

	// TODO
	// IO//////////////////////////////////////////////////////////////////////////////////
	private ArrayList<Connection> connections = new ArrayList<BleManager.Connection>();

	public Connection createConnection() {
		Connection conn = new Connection();
		connections.add(conn);
		return conn;
	}

	public void removeConnection(Connection conn) {
		if (conn != null && connections.contains(conn)) {
			conn.disconnect();
			connections.remove(conn);
		}
	}

	public enum ConnectionState {
		DISCONNECTED, // 未连接或连接断开
		DISCONNECTING, // 正在断开连接
		CONNECTING, // 正在试图去连接对方或接受连接
		CONNECTED// 已经连接
	}

	public class Connection {
		private Handler gattHandler = new Handler();
		private String serverDeviceName;
		private String serverDeviceAddress;
		private BluetoothGatt mBluetoothGatt;
		public ConnectionState state = ConnectionState.DISCONNECTED;

		public ConnectionState getState() {
			return state;
		}
		public BluetoothGatt getBluetoothGatt(){
			return mBluetoothGatt;
		}
		private BluetoothGattCallback gattCallback;
		private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
			@Override
			public void onConnectionStateChange(BluetoothGatt gatt, int status,
					int newState) {
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					state = ConnectionState.CONNECTED;
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					state = ConnectionState.DISCONNECTED;
				}else if (newState == BluetoothProfile.STATE_CONNECTING){
					state = ConnectionState.CONNECTING;
				}else if (newState == BluetoothProfile.STATE_DISCONNECTING){
					state = ConnectionState.DISCONNECTING;
				}
				if (gattCallback != null) {
					final BluetoothGatt fgatt = gatt;
					final int fstatus = status;
					final int fnewState = newState;
					gattHandler.post(new Runnable() {
						@Override
						public void run() {
							gattCallback
							.onConnectionStateChange(fgatt, fstatus, fnewState);
						}
					});
				}
			}

			@Override
			public void onServicesDiscovered(BluetoothGatt gatt, int status) {
				super.onServicesDiscovered(gatt, status);
				if (gattCallback != null) {
					final BluetoothGatt fgatt = gatt;
					final int fstatus = status;
					gattHandler.post(new Runnable() {
						@Override
						public void run() {
							gattCallback.onServicesDiscovered(fgatt, fstatus);
						}
					});
				}
			}

			@Override
			public void onCharacteristicRead(BluetoothGatt gatt,
					BluetoothGattCharacteristic characteristic, int status) {
				super.onCharacteristicRead(gatt, characteristic, status);
				if (gattCallback != null&&status == BluetoothGatt.GATT_SUCCESS) {
					final BluetoothGatt fgatt = gatt;
					final int fstatus = status;
					final BluetoothGattCharacteristic fcharacteristic = characteristic;
					
					gattHandler.post(new Runnable() {
						@Override
						public void run() {
							gattCallback.onCharacteristicRead(fgatt, fcharacteristic,
									fstatus);
						}
					});
					
				}
			}

			@Override
			public void onCharacteristicWrite(BluetoothGatt gatt,
					BluetoothGattCharacteristic characteristic, int status) {
				super.onCharacteristicWrite(gatt, characteristic, status);
				if (gattCallback != null&&status == BluetoothGatt.GATT_SUCCESS) {
					final BluetoothGatt fgatt = gatt;
					final int fstatus = status;
					final BluetoothGattCharacteristic fcharacteristic = characteristic;
					
					gattHandler.post(new Runnable() {
						@Override
						public void run() {
							gattCallback.onCharacteristicWrite(fgatt, fcharacteristic,
									fstatus);
						}
					});
					
				}
			}

			@Override
			public void onCharacteristicChanged(BluetoothGatt gatt,
					BluetoothGattCharacteristic characteristic) {
				super.onCharacteristicChanged(gatt, characteristic);
				if (gattCallback != null) {
					final BluetoothGatt fgatt = gatt;
					final BluetoothGattCharacteristic fcharacteristic = characteristic;
					gattHandler.post(new Runnable() {
						@Override
						public void run() {
							gattCallback.onCharacteristicChanged(fgatt, fcharacteristic);
						}
					});
				}
			}

			@Override
			public void onDescriptorRead(BluetoothGatt gatt,
					BluetoothGattDescriptor descriptor, int status) {
				super.onDescriptorRead(gatt, descriptor, status);
				if (gattCallback != null&&status == BluetoothGatt.GATT_SUCCESS) {
					final BluetoothGatt fgatt = gatt;
					final int fstatus = status;
					final BluetoothGattDescriptor fdescriptor = descriptor;
					gattHandler.post(new Runnable() {
						@Override
						public void run() {
							gattCallback.onDescriptorRead(fgatt, fdescriptor, fstatus);
						}
					});
				}
			}

			@Override
			public void onDescriptorWrite(BluetoothGatt gatt,
					BluetoothGattDescriptor descriptor, int status) {
				super.onDescriptorWrite(gatt, descriptor, status);
				if (gattCallback != null&&status == BluetoothGatt.GATT_SUCCESS) {
					final BluetoothGatt fgatt = gatt;
					final int fstatus = status;
					final BluetoothGattDescriptor fdescriptor = descriptor;
					gattHandler.post(new Runnable() {
						@Override
						public void run() {
							gattCallback.onDescriptorWrite(fgatt, fdescriptor, fstatus);
						}
					});
				}
			}

			@Override
			public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
				super.onReliableWriteCompleted(gatt, status);
				if (gattCallback != null&&status == BluetoothGatt.GATT_SUCCESS) {
					final BluetoothGatt fgatt = gatt;
					final int fstatus = status;
					gattHandler.post(new Runnable() {
						@Override
						public void run() {
							gattCallback.onReliableWriteCompleted(fgatt, fstatus);
						}
					});
				}
			}

			@Override
			public void onReadRemoteRssi(BluetoothGatt gatt, int rssi,
					int status) {
				super.onReadRemoteRssi(gatt, rssi, status);
				if (gattCallback != null&&status == BluetoothGatt.GATT_SUCCESS) {
					final BluetoothGatt fgatt = gatt;
					final int frssi = rssi;
					final int fstatus = status;
					gattHandler.post(new Runnable() {
						@Override
						public void run() {
							gattCallback.onReadRemoteRssi(fgatt, frssi, fstatus);
						}
					});
					
				}
			}

//			@Override
//			public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
//				super.onMtuChanged(gatt, mtu, status);
//				if (gattCallback != null) {
//					gattCallback.onMtuChanged(gatt, mtu, status);
//				}   
//			}

		};

		public void disconnect() {
			if (mBluetoothGatt != null) {
				gattCallback = null;
				mBluetoothGatt.disconnect();
				mBluetoothGatt.close();
				mBluetoothGatt = null;
			}
		}

		public boolean connect(BluetoothDevice device) {
			return connect(device, false);
		}

		public boolean connect(BluetoothDevice device, boolean autoConnect) {
			if (device == null) {
				return false;
			}
			return connect(device.getAddress(), false);
		}

		public boolean connect(String serverDeviceAddress) {
			return connect(serverDeviceAddress, false);
		}

		public boolean connect(String serverDeviceAddress, boolean autoConnect) {
			if (this.serverDeviceAddress != null
					&& serverDeviceAddress.equals(this.serverDeviceAddress)
					&& mBluetoothGatt != null) {
				if (this.state == ConnectionState.CONNECTED) {
					return true;
				} else {
					if (mBluetoothGatt.connect()) {
						this.state = ConnectionState.CONNECTING;
						if (gattCallback != null)gattCallback.onConnectionStateChange(mBluetoothGatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTING);
						return true;
					} else {
						this.state = ConnectionState.DISCONNECTED;
						if (gattCallback != null)gattCallback.onConnectionStateChange(mBluetoothGatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED);
						
						return false;
					}
				}
			}
			disconnect();
			this.serverDeviceAddress = serverDeviceAddress;

			BluetoothAdapter adapter = getBluetoothAdapter(context);
			if (adapter == null) {
				this.state = ConnectionState.DISCONNECTED;
				if (gattCallback != null)gattCallback.onConnectionStateChange(mBluetoothGatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED);
				return false;
			}

			final BluetoothDevice device = adapter
					.getRemoteDevice(this.serverDeviceAddress);
			if (device == null) {
				this.state = ConnectionState.DISCONNECTED;
				if (gattCallback != null)gattCallback.onConnectionStateChange(mBluetoothGatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED);
				
				return false;
			}
			this.serverDeviceName = device.getName();
			mBluetoothGatt = device.connectGatt(context, autoConnect, mGattCallback);
			this.state = ConnectionState.CONNECTING;
			if (gattCallback != null)gattCallback.onConnectionStateChange(mBluetoothGatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTING);
			return true;
		}

		public void read() {

		}

		public void write(String chars) {
			write(chars.getBytes());
		}

		public void write(byte[] data) {
			if (data == null || data.length == 0) {
				return;
			}
			// Synchronize a copy of the ConnectedThread
			synchronized (this) {
				if (state != ConnectionState.CONNECTED)
					return;
			}
			// Perform the write unsynchronized
		}

		public String getServerDeviceName() {
			return serverDeviceName;
		}

		public void setServerDeviceName(String serverDeviceName) {
			this.serverDeviceName = serverDeviceName;
		}

		public BluetoothGattCallback getGattCallback() {
			return gattCallback;
		}

		public void setGattCallback(BluetoothGattCallback gattCallback) {
			this.gattCallback = gattCallback;
		}
	}

	// TODO //////////////////////////////////////////////////////////////////
	@SuppressLint("NewApi") //request API 21
	public void testServer(String serviceUuid) {
		BluetoothLeAdvertiser advertiser = getBluetoothAdapter(context)
				.getBluetoothLeAdvertiser();
		if (advertiser != null) {
			AdvertiseData data = new AdvertiseData.Builder().addServiceUuid(
					ParcelUuid.fromString(serviceUuid)).build();

			AdvertiseSettings settings = new AdvertiseSettings.Builder()
					.setConnectable(true).build();

			advertiser.startAdvertising(settings, data,
					new AdvertiseCallback() {
						@Override
						public void onStartSuccess(
								AdvertiseSettings settingsInEffect) {
							super.onStartSuccess(settingsInEffect);
						}
					});
		}
	}

	public ArrayList<BluetoothGattServer> gattServers = new ArrayList<BluetoothGattServer>();

	public void createServer(BluetoothGattServerCallback callback) {
		BluetoothGattServer server = getBluetoothManager(context)
				.openGattServer(context, callback);
		if (server != null) {
			gattServers.add(server);
		}
	}

	public ArrayList<Connection> getConnections() {
		return connections;
	}

	public void setConnections(ArrayList<Connection> connections) {
		this.connections = connections;
	}
}

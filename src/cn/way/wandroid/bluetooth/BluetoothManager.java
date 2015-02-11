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
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

/**
 * 1.add permissions. <uses-permission
 * android:name="android.permission.BLUETOOTH" /> <uses-permission
 * android:name="android.permission.BLUETOOTH_ADMIN" />
 * 
 * @author Wayne
 */
public class BluetoothManager {
	public static int REQUEST_ENABLE_BT = 1001;
	public static int REQUEST_DISCOVERABLE_BT = 1002;
	private static BluetoothManager manager;

	public static BluetoothManager instance(Context context)
			throws BluetoothSupportException {
		if (!BluetoothManager.isBluetoothSupported()) {
			throw new BluetoothSupportException();
		}
		if (context == null) {
			return null;
		}
		if (manager == null) {
			manager = new BluetoothManager(context.getApplicationContext());
		}
		return manager;
	}

	public static boolean isBluetoothSupported() {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			return false;
		} else {
			return true;
		}
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
		activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	}

	public static void requestDiscoverable(Activity activity) {
		Intent discoverableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		// maximum duration is 300 seconds,default is 120 seconds.
		discoverableIntent.putExtra(
				BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		activity.startActivityForResult(discoverableIntent,
				REQUEST_DISCOVERABLE_BT);
	}

	public static String getBondState(BluetoothDevice bd) {
		/**
		 * {@link #BOND_NONE}, {@link #BOND_BONDING}, {@link #BOND_BONDED}.
		 */
		String state = "未配对";
		if (bd.getBondState() == BluetoothDevice.BOND_BONDING) {
			state = "正在配对...";
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
		cancelDiscovery();
		// Unregister broadcast listeners
		if (this.context != null && mReceiver != null)
			this.context.unregisterReceiver(mReceiver);
	}

	/**
	 * 
	 * @param context
	 */
	private BluetoothManager(Context context) {
		super();
		// 监听蓝牙设置的开关状态
		IntentFilter filter = new IntentFilter(
				BluetoothAdapter.ACTION_STATE_CHANGED);
		context.registerReceiver(mReceiver, filter);

		// Register for broadcasts when a device is discovered
		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		context.registerReceiver(mReceiver, filter);

		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		context.registerReceiver(mReceiver, filter);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
	}

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

	public void enable() {
		if (!mBluetoothAdapter.isEnabled()) {
			mBluetoothAdapter.enable();
		} else {
			onDeviceStateChanged(BluetoothAdapter.STATE_ON);
		}
	}

	/**
	 * scan the nearby devices
	 * 
	 * @return
	 */
	public void startDiscovery(DiscoveryListener l) {
		this.discoveryListener = l;
		cancelDiscovery();
		// Request discover from BluetoothAdapter
		mBluetoothAdapter.startDiscovery();
	}

	public void cancelDiscovery() {
		if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		}
	}

	public boolean isDiscovering() {
		return mBluetoothAdapter.isDiscovering();
	}

	/**
	 * Get a set of currently paired devices
	 * 
	 * @return
	 */
	public Set<BluetoothDevice> getBondedDevices() {
		return BluetoothAdapter.getDefaultAdapter().getBondedDevices();
	}

	public DiscoveryListener getDiscoveryListener() {
		return discoveryListener;
	}

	public void setDiscoveryListener(DiscoveryListener discoveryListener) {
		this.discoveryListener = discoveryListener;
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

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's already paired, skip it, because it's been listed
				// already
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					addDevice(device);
					if (discoveryListener != null) {
						discoveryListener.onDevicesChanged(devices.values());
					}
				}
			}
			// discovery is finished
			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				if (discoveryListener != null) {
					discoveryListener.onFinished();
				}
			}
		}
	};

	public interface DiscoveryListener {
		/**
		 * the list of devices changed, update view should be done.
		 * 
		 * @param devices
		 */
		void onDevicesChanged(Collection<BluetoothDevice> devices);

		void onFinished();
	}

	private DiscoveryListener discoveryListener;
	private Hashtable<String, BluetoothDevice> devices = new Hashtable<String, BluetoothDevice>();

	private void addDevice(BluetoothDevice device) {
		if (device != null) {
			devices.put(device.getAddress(), device);
		}
	}

	public DeviceStateListener getDeviceStateListener() {
		return deviceStateListener;
	}

	public void setDeviceStateListener(DeviceStateListener deviceStateListener) {
		this.deviceStateListener = deviceStateListener;
	}

	/**
	 * Device does not support Bluetooth
	 * @author Wayne
	 */
	public static class BluetoothSupportException extends Exception {
		private static final long serialVersionUID = -5168381347847106511L;

		@Override
		public String getMessage() {
			return "当前设备不支持蓝牙";
		}
	}

////////////////////////////////////////////////////////////////////////////////////
	//TODO:准备把服务单独做为一个功能对象，不做为Connection对象的子类。增加一对多连接功能
	private BluetoothServerConnection serverConnection;

	/**
	 * @return the serverConnection
	 */
	public BluetoothServerConnection getServerConnection() {
		if (serverConnection==null) {
			serverConnection = new BluetoothServerConnection();
		}
		return serverConnection;
	}
	public BluetoothClientConnection createClientConnection(){
		BluetoothClientConnection conn = new BluetoothClientConnection();
		return conn;
	}
	
	public interface BluetoothConnectionListener {
		/**
		 * @param state 当前状态
		 * @param errorCode 连接错误码
		 */
		void onConnectionStateChanged(ConnectionState state, int errorCode);
		void onDataReceived(byte[] data);
	}

	public static final int ConnectionErrorCodeNode = 0;// 没有错误
	public static final int ConnectionErrorCodeException = 1;// 未知错误
	public static final int ConnectionErrorCodeOnOnly = 2;// 已经有一个连接，不能创建更多连接
	public static final int ConnectionErrorCodeNoDevice = 3;// 远程设备为空
	public enum ConnectionState {
		DISCONNECTED, // 未连接或连接断开
		CONNECTING, // 正在试图去连接对方或接受连接
		CONNECTED// 已经连接
	}

	@SuppressLint("HandlerLeak")
	private abstract class BluetoothConnection {
		protected static final int MSG_CONNECTION_STATE = 0;
		protected static final int MSG_READ = 1;
		protected static final int MSG_WRITE = 2;
		protected final Handler mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (getBluetoothConnectionListener()==null) {
					return;
				}
				switch (msg.what) {
				case MSG_CONNECTION_STATE:
					getBluetoothConnectionListener()
					.onConnectionStateChanged(state,msg.arg1);
					break;
				case MSG_READ:
					getBluetoothConnectionListener().onDataReceived((byte[]) msg.obj);
					break;
				case MSG_WRITE:
					break;
				default:
					break;
				}
			}
		};
		
		protected static final String NAME_SECURE = "BluetoothSecure";
		protected static final String NAME_INSECURE = "BluetoothInsecure";
		protected UUID uuid;
		protected boolean isSecure;// true is secure or insecure
		protected ConnectionState state = ConnectionState.DISCONNECTED;

		protected BluetoothConnectionListener bluetoothConnectionListener;

		protected Thread connectedThread;// 连接后的通讯线程

		protected Object lock = BluetoothConnection.this;

		/**
		 * 断开连接
		 * @param needCallbak true 执行回调 false则不执行
		 */
		protected void disconnect(boolean needCallbak){
			if (!needCallbak) {//取消执行回调
				setBluetoothConnectionListener(null);
			}
		};
		
		protected abstract InputStream getInputStream() throws IOException;
		protected abstract OutputStream getOutputStream() throws IOException;
		
		protected void startDataReceiving() {
			changeState(ConnectionState.CONNECTED,ConnectionErrorCodeNode);
			mBluetoothAdapter.cancelDiscovery();
			
			connectedThread = new Thread(new Runnable() {
				
				@Override 
				public void run() {
					byte[] buffer = new byte[1024];
					int byteCount;
		            // Keep listening to the InputStream while connected
		            while (true) {
		                try {
		                    // Read from the InputStream
		                    byteCount = getInputStream().read(buffer);
		                    ByteBuffer bb = ByteBuffer.wrap(buffer, 0, byteCount);
		                    // Send the obtained bytes to the UI Activity
		                    mHandler.obtainMessage(MSG_READ,bb.array()).sendToTarget();
		                } catch (IOException e) {
		                	//TODO
		                    //connectionLost();
		                    // Start the service over to restart listening mode
		                    //BluetoothChatService.this.start();
		                	changeState(ConnectionState.DISCONNECTED,ConnectionErrorCodeException);
		                    break;
		                }
		            }
				}
			});
			connectedThread.start();
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
	        try {
				getOutputStream().write(data);
				mHandler.obtainMessage(MSG_WRITE, data)
                .sendToTarget();
			} catch (IOException e) {
				e.printStackTrace();
				changeState(ConnectionState.DISCONNECTED,ConnectionErrorCodeException);
			}
	    }
		
		protected synchronized void changeState(ConnectionState state,int errorCode) {
			this.state = state;
			if (getBluetoothConnectionListener() != null) {
				mHandler.obtainMessage(MSG_CONNECTION_STATE, errorCode, -1);
			}
		}
		public synchronized ConnectionState getState() {
			return state;
		}
		
		public BluetoothConnectionListener getBluetoothConnectionListener() {
			return bluetoothConnectionListener;
		}

		public void setBluetoothConnectionListener(
				BluetoothConnectionListener bluetoothConnectionListener) {
			this.bluetoothConnectionListener = bluetoothConnectionListener;
		}
	}
	/**
	 * 服务器连接，支持多个客户端陆续连接，目前仅支持一对一连接
	 * @author Wayne
	 * @2015年1月10日
	 */
	public class BluetoothServerConnection extends BluetoothConnection{
		@Override
		public void disconnect(boolean needCallbak) {
			super.disconnect(needCallbak);
			try {
				if (serverSocket != null){
					serverSocket.close();
					serverSocket = null;
				}
				if (socket != null) {
					socket.close();
					socket = null;
				}
			} catch (IOException e) {}
			changeState(ConnectionState.DISCONNECTED,ConnectionErrorCodeNode);
		}
		
		protected BluetoothSocket socket;
		protected BluetoothServerSocket serverSocket;
		protected Thread listeningThread;// 等待客户端连接线程
		
		
		
		public void connect(UUID uuid,boolean isSecure,BluetoothConnectionListener l) {
			disconnect(true);
			setBluetoothConnectionListener(l);
			this.isSecure = isSecure;
			this.uuid = uuid;
			if (this.state == ConnectionState.CONNECTING) {
				return;
			}
			
			try {//创建一个ServerSocket
				if (isSecure) {
					this.serverSocket = mBluetoothAdapter
							.listenUsingRfcommWithServiceRecord(NAME_SECURE,
									uuid);
				} else {
					this.serverSocket = mBluetoothAdapter
							.listenUsingInsecureRfcommWithServiceRecord(
									NAME_INSECURE, uuid);
				}
			} catch (IOException e) {
				this.serverSocket = null;
				//创建失败，执行状态回调
				changeState(ConnectionState.DISCONNECTED, ConnectionErrorCodeException);
			}
			
			//创建成功，开启一个线程 不断尝试接受连接直到连接完成
			changeState(ConnectionState.CONNECTING, ConnectionErrorCodeNode);
			
			listeningThread = new Thread(new Runnable() {
				@Override
				public void run() {
					//直到连接成功才停止接受连接请求
					while (state != ConnectionState.CONNECTED) {
						try {
							//进入等待对方连接状态，会阻塞线程，所以开一下子线程
							socket = serverSocket.accept();
						} catch (IOException e) {
							// 尝试连接异常退出
							socket = null;
							changeState(ConnectionState.DISCONNECTED, ConnectionErrorCodeException);
							break;
						}
						if (socket != null) {
							synchronized (lock) {
								switch (state) {
								case CONNECTING:
									//连接完成，进入交换数据状态
									startDataReceiving();
									break;
								case CONNECTED:
									try {
										// 目前只要求有一个连接，所以有已经存在连接则关闭当前得到的Socket
										socket.close();
										socket = null;
									} catch (IOException e) {
										//关闭失败
									}
									//保持当前连接状态不变，但返回错误信息
									changeState(ConnectionState.CONNECTED, ConnectionErrorCodeOnOnly);
									break;
								default:
									break;
								}
							}
						}
					}
				}
			});
			listeningThread.start();
		}
		@Override
		protected InputStream getInputStream() throws IOException {
			// TODO Auto-generated method stub
			return socket.getInputStream();
		}
		@Override
		protected OutputStream getOutputStream() throws IOException {
			// TODO Auto-generated method stub
			return socket.getOutputStream();
		}
		public BluetoothDevice getClientDevice(){
			if (socket!=null) {
				return socket.getRemoteDevice();
			}
			return null;
		}
	}
	/**
	 * 客户端连接只能连接到一个服务器
	 * @author Wayne
	 * @2015年1月10日
	 */
	public class BluetoothClientConnection extends BluetoothConnection{
		@Override
		public void disconnect(boolean needCallbak) {
			super.disconnect(needCallbak);
			try {
				if (socket != null) {
					socket.close();
					socket = null;
				}
			} catch (IOException e) {}
			changeState(ConnectionState.DISCONNECTED,ConnectionErrorCodeNode);
		}
		protected BluetoothSocket socket;
		protected Thread connectingThread;// 连接服务器线程
		private BluetoothDevice remoteServerDevice;// 准备或已经连接的远程设备
		/**
		 * 创建一个连接
		 * 
		 * @param device 不可为空。通过device创建Socket并尝试连接（ConnectionState.CONNECTING）
		 * @param isSecure
		 *            true 创建安全连接，false 创建不安全连接
		 */
		public void connect(UUID uuid, BluetoothDevice device, boolean isSecure,BluetoothConnectionListener l) {
			setBluetoothConnectionListener(null);
			disconnect(true);
			setBluetoothConnectionListener(l);
			this.uuid = uuid;
			this.isSecure = isSecure;
			this.remoteServerDevice = device;
			if (device != null) {
				doConnecting();
			}else{
				changeState(ConnectionState.DISCONNECTED,ConnectionErrorCodeNoDevice);
			}
		}
		private void doConnecting() {
			try {
				if (isSecure) {
					this.socket = remoteServerDevice
							.createRfcommSocketToServiceRecord(uuid);
				} else {
					this.socket = remoteServerDevice
							.createInsecureRfcommSocketToServiceRecord(uuid);
				}
			} catch (IOException e) {
				changeState(ConnectionState.DISCONNECTED,ConnectionErrorCodeException);
			}
			changeState(ConnectionState.CONNECTING,ConnectionErrorCodeNode);
			
			connectingThread = new Thread(new Runnable() {
				@Override
				public void run() {
		            try {
		            	//这个连接操作是同步的。所以在线程中执行
		                socket.connect();
		            } catch (IOException e) {
		                try {
		                    socket.close();
		                } catch (IOException e2) {
		                    //未成功关闭
		                }
		                changeState(ConnectionState.DISCONNECTED,ConnectionErrorCodeException);
		                return;
		            }
		            //如果连接未出现异常，则说明连接成功，可以进入数据交换了
		            synchronized (lock) {
		            	connectingThread = null;
		            }
		            startDataReceiving();
				}
			});
			connectingThread.start();
		}
		@Override
		protected InputStream getInputStream() throws IOException {
			// TODO Auto-generated method stub
			return socket.getInputStream();
		}
		@Override
		protected OutputStream getOutputStream() throws IOException {
			// TODO Auto-generated method stub
			return socket.getOutputStream();
		}
	}
	
}

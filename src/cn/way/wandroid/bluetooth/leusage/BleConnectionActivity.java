package cn.way.wandroid.bluetooth.leusage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import cn.way.bleusage.R;
import cn.way.bleusage.SampleGattAttributes;
import cn.way.wandroid.bluetooth.BleManager;
import cn.way.wandroid.bluetooth.BleManager.BleAvailableException;
import cn.way.wandroid.bluetooth.BleManager.ConnectionState;

/**
 * @author Wayne
 * @2015年2月4日
 */
public class BleConnectionActivity extends Activity {
	private BleManager bleManager;
	
	public static final String EXTRA_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	private String mDeviceAddress;
	
	private TextView mDataField;
	private EditText mDataSource;
	private View connectBtn;
	private ExpandableListView mGattServicesList;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    @Override
    protected void onPause() {
    	super.onPause();
    	if(bleManager!=null&&conn!=null){
    		conn.disconnect();
    	}
    }
    @Override
    protected void onResume() {
    	super.onResume();
    	if (bleManager!=null) {
    		doConnect();
		}
    }
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.page_connection);
		mDeviceAddress = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);
		if (mDeviceAddress!=null) {
			try {
				bleManager = BleManager.instance(this);
				((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
				mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
				mGattServicesList.setOnChildClickListener(servicesListClickListner);
				mDataField = (TextView) findViewById(R.id.data_value);
				mDataSource = (EditText) findViewById(R.id.dataSource);
				connectBtn = findViewById(R.id.connectBtn);
				connectBtn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						doConnect();
					}
				});
			} catch (BleAvailableException e) {
				e.printStackTrace();
				toast(e.getMessage());
			}
		}else{
			toast("no address");
		}
		
	}
	private void toast(String text){
		Toaster.instance(this).setup(text).show();
	}
	public static UUID getUUID(){
//		int minValue = 1;//0x0001
//		int maxValue = Short.MAX_VALUE*2+1;//0xFFFF
		int value = 0x1800;
		String uuid = new cn.way.wandroid.bluetooth.UUID(Integer.toHexString(value),true).toString();
		return UUID.fromString(uuid);
	}
	public final static UUID UUID_C_TEST = getUUID();
	private String TAG = "test";
	private BleManager.Connection conn;
	private void doConnect(){
		if (bleManager==null) {
			return;
		}
		if (conn==null) {
			conn = bleManager.createConnection();
			conn.setGattCallback(new BluetoothGattCallback() {
				@Override
				public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
					updateConnectionState();
					connectBtn.setEnabled(newState==BluetoothGatt.STATE_DISCONNECTED);
					if (newState==BluetoothGatt.STATE_CONNECTED) {
						Log.w(TAG, "开始查找服务:" +
								gatt.discoverServices());
						toast("正在查找服务，请稍候");
					}
					if (newState==BluetoothGatt.STATE_DISCONNECTED) {
						clearUI();
						Log.w(TAG, "STATE_DISCONNECTED:" +
								conn.getState());
					}
					if (conn.getState()==ConnectionState.CONNECTING) {
						toast("正在创建连接，请稍候");
					}
				}
				
				@Override
				public void onServicesDiscovered(BluetoothGatt gatt, int status) {
					if (status == BluetoothGatt.GATT_SUCCESS) {
						Log.w(TAG, "查找服务完成:" + gatt.getServices());
						displayGattServices(gatt.getServices());
					} else {
						Log.w(TAG, "未找到服务: " + status);
					}
				}
				
				@Override
				public void onCharacteristicRead(BluetoothGatt gatt,
						BluetoothGattCharacteristic characteristic,
						int status) {
					if (status == BluetoothGatt.GATT_SUCCESS) {
						readData(characteristic);
					}
				}
				
				@Override
				public void onCharacteristicChanged(BluetoothGatt gatt,
						BluetoothGattCharacteristic characteristic) {
					readData(characteristic);
				}
			});
		}
		if (conn.state==ConnectionState.DISCONNECTED) {
			conn.connect(mDeviceAddress);
		}
	}
	private void readData(BluetoothGattCharacteristic characteristic){
		if (characteristic==null) {
			return;
		}
    	String value = characteristic.getStringValue(0);
    	displayData(value);
	}
	private void writeData(BluetoothGattCharacteristic characteristic){
		if (characteristic==null) {
			return;
		}
		currentCharacteristic.setValue(mDataSource.getText().toString());
        conn.getBluetoothGatt().writeCharacteristic(currentCharacteristic);
        toast("write data:"+mDataSource.getText());
	}
	private BluetoothGattCharacteristic currentCharacteristic;
	private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                    	displayData("reading...");
                    	currentCharacteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = currentCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                conn.getBluetoothGatt().setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            //执行读取操作
                            conn.getBluetoothGatt().readCharacteristic(currentCharacteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                            writeData(currentCharacteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                            writeData(currentCharacteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = currentCharacteristic;
                            conn.getBluetoothGatt().setCharacteristicNotification(
                            		currentCharacteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
    };
    
    private void updateConnectionState(){
    	Log.i(TAG, "Connection State : "+conn.getState());
    	getActionBar().setTitle("Connection State : "+conn.getState());
    }
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }
    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }
    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
        toast("连接已经断开");
    }
}

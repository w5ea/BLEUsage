package cn.way.wandroid.bluetooth.leusage;

import java.util.ArrayList;
import java.util.Collection;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cn.way.bleusage.DeviceScanActivity;
import cn.way.bleusage.R;
import cn.way.wandroid.bluetooth.BleManager;
import cn.way.wandroid.bluetooth.BleManager.DeviceState;
import cn.way.wandroid.bluetooth.BleManager.DeviceStateListener;
import cn.way.wandroid.bluetooth.BleManager.ScanListener;

@SuppressLint({ "InflateParams", "NewApi" })
public class FriendsFragment extends Fragment {
	private ViewGroup view;//主视图
	private ListView foundListView;//附近设备列表
	private ArrayAdapter<BluetoothDevice> adapterFound;
	private ArrayList<BluetoothDevice> nearbyDevices = new ArrayList<BluetoothDevice>();
	private BleManager bluetoothManager;
	
	private Button searchBtn;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.bluetooth_im_page_friends, null);
		
		view.findViewById(R.id.demoPage).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), DeviceScanActivity.class));
			}
		});
		searchBtn = (Button) view.findViewById(R.id.searchBtn);
		searchBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doDescovery();
			}
		});
		
		//////////////////////////////////////////////////////////////////////////////////////
		
		foundListView = (ListView) view.findViewById(R.id.im_discoveries_list);
		adapterFound = new ArrayAdapter<BluetoothDevice>(getActivity(), 0){
			@Override
			public int getCount() {
				return nearbyDevices.size();
			}
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = convertView;
				if (view == null) {
					view = getActivity().getLayoutInflater().inflate(R.layout.bluetooth_im_list_friends_cell, null);
				}
				ViewHolder holder = (ViewHolder) view.getTag();
				if (holder==null) {
					holder = new ViewHolder();
					holder.nameTV = (TextView) view.findViewById(R.id.name);
					holder.stateTV = (TextView) view.findViewById(R.id.state);
					view.setTag(holder);
				}
				BluetoothDevice bd = nearbyDevices.get(position);
				holder.nameTV.setText(bd.getName()+"");
				holder.stateTV.setText(bd.getAddress());
				return view;
			}
			class ViewHolder{
				TextView nameTV;//可设置备注
				TextView stateTV;//对好友可见|对附近所有人可见
			}
		};
		foundListView.setAdapter(adapterFound);
		foundListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				BluetoothDevice bd = nearbyDevices.get(position);
				deviceConnectToServer(bd);
			}
		});
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (view!=null&&view.getParent()!=null) {
			((ViewGroup)view.getParent()).removeView(view);
		}
		return view;
	}
	@Override
	public void onResume() {
		super.onResume();
	}
	private void deviceConnectToServer(BluetoothDevice bd){
		if(getBluetoothManager()!=null){
			Toast.makeText(getActivity(), "准备创建连接："+bd, 0).show();
			
		}else{
			Toast.makeText(getActivity(), "无法连接："+bd, 0).show();
		}
	}

	private void doDescovery(){
		if (!getBluetoothManager().isEnabled()) {
			getBluetoothManager().setDeviceStateListener(new DeviceStateListener() {
				@Override
				public void onStateChanged(DeviceState state) {
					if (state == DeviceState.ON) {
						doDescovery();
					}
					Toaster.instance(getActivity()).setup("state : "+state).show();
					if (state == DeviceState.OFF) {
						searchBtn.setText("查找");
					}
				}
			});
			getBluetoothManager().enable();
			return;
		}
		if (getBluetoothManager()!=null) {
			searchBtn.setText("正在查找...");
			getBluetoothManager().startScan(new ScanListener() {
				@Override
				public void onFinished() {
					if (bluetoothManager!=null) {
						searchBtn.setText("查找");
					}
				}
				@Override
				public void onDevicesChanged(Collection<BluetoothDevice> devices) {
					if (bluetoothManager!=null) {
						nearbyDevices.clear();
						nearbyDevices.addAll(devices);
						adapterFound.notifyDataSetChanged();
					}
				}
			});
		}
	}
	
	public BleManager getBluetoothManager() {
		return bluetoothManager;
	}
	public void setBluetoothManager(BleManager manager) {
		this.bluetoothManager = manager;
	}
}

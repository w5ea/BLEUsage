package cn.way.wandroid.bluetooth;

import java.util.UUID;

import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;

/**
 * @author Wayne
 * @2015年2月4日
 */
public class GattServiceFactory {
	public static UUID UUID_SERVICE = UUID
			.fromString("50000001-0000-1000-8000-00805F9B34FB");
	public static UUID UUID_C1 = UUID
			.fromString("60000002-0000-1000-8000-00805F9B34FB");

	public static BluetoothGattService createToiService(BluetoothGattServer server) {
		BluetoothGattService service = new BluetoothGattService(UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);
//		BluetoothGa/ttCharacteristic c = new BluetoothGattCharacteristic(UUID_C1, properties, permissions)
//		service.addCharacteristic(characteristic)
		server.addService(service);
		return service;
	}
}

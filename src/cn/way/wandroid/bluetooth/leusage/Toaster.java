package cn.way.wandroid.bluetooth.leusage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

@SuppressLint("ShowToast")
public class Toaster {
	private static Toaster toaster;
	private Context context;
	public static Toaster instance(Context context){
		if (toaster==null) {
			toaster = new Toaster(context);
		}
		return toaster;
	}
	
	private Toaster(Context context) {
		super();
		//使用context.getApplicationContext()
		this.context = context.getApplicationContext();
		toast = new Toast(context);
		toast.setDuration(Toast.LENGTH_SHORT);
	}
	protected Toast toast;
	protected Toast toastResult;
	public Toast setup(String text) {
		return this.setup(text, Gravity.CENTER, 0, 0);
	}
	public Toast setup(String text,int gravity) {
		return this.setup(text, gravity, 0, 0);
	}
	public Toast setup(String text,int gravity, int xOffset, int yOffset) {
		cancel();
		//TODO toast.setView();
		toastResult = Toast.makeText(context, text, toast.getDuration());
		toastResult.setGravity(gravity, xOffset, yOffset);
		return toastResult;
	}
	public void show(){
		if(toastResult!=null)toastResult.show();
	}
	public void cancel(){
		try {
			if(toastResult!=null){
				toastResult.cancel();
				toastResult = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
//	public Toast getToast(){
//		return toast;
//	}
	public void setDuration(boolean shortOrLong){
		toast.setDuration(shortOrLong?Toast.LENGTH_SHORT:Toast.LENGTH_LONG);
	}
}

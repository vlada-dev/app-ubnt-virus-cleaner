package virusfixer.ubnt.com.ubntvirusremoval.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import virusfixer.ubnt.com.ubntvirusremoval.model.Device;
import virusfixer.ubnt.com.ubntvirusremoval.R;


public class DeviceAdapter extends BaseAdapter
{
	private final Context context;
	private final int mBlueColor;
	private List<Device> values;


	// the context is needed to inflate views in getView()
	public DeviceAdapter(Context context, List<Device> deviceList)
	{
		this.context = context;
		this.values = deviceList;
		mBlueColor = context.getResources().getColor(R.color.blueColor);;
	}





	@Override
	public int getCount()
	{
		return values.size();
	}


	@Override
	public Device getItem(int position)
	{
		return values.get(position);
	}


	@Override
	public long getItemId(int position)
	{
		return position;
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		DeviceHolder holder;

		if(convertView == null)
		{
			convertView = LayoutInflater.from(context).inflate(R.layout.adapter_device, parent, false);
			holder = new DeviceHolder();
			holder.ip = (TextView) convertView.findViewById(R.id.title);
			holder.status = (TextView) convertView.findViewById(R.id.status);
			holder.firmware = (TextView) convertView.findViewById(R.id.firmware);
			convertView.setTag(holder);
		}
		else
		{
			holder = (DeviceHolder) convertView.getTag();
		}
		Device dev = values.get(position);
		holder.ip.setText(dev.getIp());
		if (!dev.hasWarningStatus()) {
			holder.status.setTextColor(mBlueColor);
		} else {
			holder.status.setTextColor(Color.RED);
		}
		holder.status.setText(dev.getStatusText());
		String fw = dev.getFirmwareVersion();
		if (fw == null || fw.length() == 0) {
			holder.firmware.setText("Unknown");
		} else {
			holder.firmware.setText(fw);
		}

		return convertView;
	}


	static class DeviceHolder
	{
		TextView ip, status, firmware;
	}
}


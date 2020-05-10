package com.example.kn_project_app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class ListOfDevices extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_devices);

        ListView listView = (ListView)findViewById(R.id.listOfDevices);

        ArrayList<Device> devices = new ArrayList<>();
        devices.add(new Device(R.drawable.access_point, "Watykan", "Access Point", 2137, "21:37:11:09:17:18:10:05"));
        devices.add(new Device(R.drawable.phone, "Huawei", "Client", 2005, "10:05:12:39:15:65:14:35"));
        devices.add(new Device(R.drawable.phone, "Xiaomi", "Access Point", 204, "22:27:41:59:54:21:12:55"));
        CustomAdapter customAdapter = new CustomAdapter(devices);
        listView.setAdapter(customAdapter);
    }

    class CustomAdapter extends BaseAdapter{

        ArrayList<Device> devices;

        public CustomAdapter(ArrayList<Device> devices) {
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.device_list_custom_layout, null);

            Device device = (Device)this.getItem(position);

            ImageView imageView = (ImageView)convertView.findViewById(R.id.imageView);
            TextView manufacturer = (TextView)convertView.findViewById(R.id.manufacturer);
            TextView deviceType = (TextView)convertView.findViewById(R.id.type);
            TextView channel = (TextView)convertView.findViewById(R.id.channel);
            TextView macAddress = (TextView)convertView.findViewById(R.id.macAddress);

            imageView.setImageResource(device.getImageNumber());
            manufacturer.setText(device.getManufacturer());
            deviceType.setText(device.getDeviceType());
            channel.setText("Channel: " + Integer.toString(device.getChannel()));
            macAddress.setText(device.getMacAddress());

            return convertView;
        }

    }

    class Device {
        int imageNumber;
        String manufacturer;
        String deviceType;
        int channel;
        String macAddress;

        public Device(int imageNumber, String manufacturer, String deviceType, int channel, String macAddress) {
            this.imageNumber = imageNumber;
            this.manufacturer = manufacturer;
            this.deviceType = deviceType;
            this.channel = channel;
            this.macAddress = macAddress;
        }

        public int getImageNumber() {
            return imageNumber;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public String getDeviceType() {
            return deviceType;
        }

        public int getChannel() {
            return channel;
        }

        public String getMacAddress() {
            return macAddress;
        }
    }
}

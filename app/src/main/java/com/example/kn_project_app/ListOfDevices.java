package com.example.kn_project_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ListOfDevices extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_devices);

        getSupportActionBar().setTitle("Devices");

        ListView listView = (ListView)findViewById(R.id.listOfDevices);

        Intent intent = getIntent();
        CustomAdapter customAdapter;
        ArrayList<Device> devices = intent.getParcelableArrayListExtra("devices");
        Device accessPoint = intent.getExtras().getParcelable("accessPoint");
        if (accessPoint != null) {
            customAdapter = new CustomAdapter(devices, accessPoint);
        }
        else {
            customAdapter = new CustomAdapter(devices);
        }
        listView.setAdapter(customAdapter);
    }

    class CustomAdapter extends BaseAdapter{

        ArrayList<Device> devices;
        Device accessPoint;

        public CustomAdapter(ArrayList<Device> devices, Device accessPoint) {
            this.devices = devices;
            this.accessPoint = accessPoint;
        }

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

            final Device device = (Device)this.getItem(position);

            ImageView imageView = (ImageView)convertView.findViewById(R.id.imageView);
//            TextView manufacturer = (TextView)convertView.findViewById(R.id.manufacturer);
            TextView name = (TextView)convertView.findViewById(R.id.name);
            //TextView deviceType = (TextView)convertView.findViewById(R.id.type);
            TextView channel = (TextView)convertView.findViewById(R.id.channel);
            TextView macAddress = (TextView)convertView.findViewById(R.id.macAddress);
            TextView freqmhz = (TextView)convertView.findViewById(R.id.freqmhz);

            imageView.setImageResource(device.getImageNumber());
//            manufacturer.setText(device.getManufacturer());
            name.setText(device.getName());
            //deviceType.setText(device.getDeviceType());
            channel.setText("Channel: " + device.getChannel());
            macAddress.setText(device.getMacAddress());
            freqmhz.setText(device.getFreqmhz());

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (device.getDeviceType().equals("Access Point")) {
                        ArrayList<Device> tempDevices = new ArrayList<>();
                        for (Device deviceInList : devices) {
                            try {
                                if (device.getBssid().equals(deviceInList.getBssid()) && deviceInList.getDeviceType().equals("Client"))
                                    tempDevices.add(deviceInList);
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                        Toast.makeText(ListOfDevices.this, "Selected: " + device.getBssid(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ListOfDevices.this, ListOfDevices.class);
                        intent.putParcelableArrayListExtra("devices", tempDevices);
                        intent.putExtra("accessPoint",device);
                        startActivity(intent);
                    }
                    if (device.getDeviceType().equals("Client")){
                        Intent intent = new Intent(ListOfDevices.this, AttackToggle.class);
                        intent.putExtra("device",device);
                        intent.putExtra("accessPoint", accessPoint);
                        for(Device deviceInList: devices){
                            if(device.getBssid().equals(deviceInList.getBssid()) && deviceInList.getDeviceType().equals("Access Point")) {
                                intent.putExtra("accessPoint", deviceInList);
                            }
                        }
                        startActivity(intent);
                    }
                }
            });

            return convertView;
        }
    }
}

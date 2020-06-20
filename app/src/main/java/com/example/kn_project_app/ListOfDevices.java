package com.example.kn_project_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ListOfDevices extends AppCompatActivity implements MyCallback {

    private boolean mShouldUnbind;
    private SshOperations mBoundService;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        handler = new Handler();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_devices);

        getSupportActionBar().setTitle("Devices");

        ListView listView = (ListView)findViewById(R.id.listOfDevices);

        Intent intent = getIntent();
        CustomAdapter customAdapter;
        ArrayList<Device> devices = intent.getParcelableArrayListExtra("devices");
        Device accessPoint = intent.getExtras().getParcelable("accessPoint");
        int attackNumber = intent.getExtras().getInt("attackNumber");
        if (accessPoint != null) {
            customAdapter = new CustomAdapter(devices, attackNumber, accessPoint);
        }
        else {
            customAdapter = new CustomAdapter(devices, attackNumber);
        }
        listView.setAdapter(customAdapter);
    }

    @Override
    protected void onStart(){
        super.onStart();
        doBindService();
    };

    class CustomAdapter extends BaseAdapter{

        ArrayList<Device> devices;
        Device accessPoint;
        int attackNumber;

        public CustomAdapter(ArrayList<Device> devices, int attackNumber, Device accessPoint) {
            this.devices = devices;
            this.attackNumber = attackNumber;
            this.accessPoint = accessPoint;
        }

        public CustomAdapter(ArrayList<Device> devices, int attackNumber) {
            this.devices = devices;
            this.attackNumber = attackNumber;
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
            TextView name = (TextView)convertView.findViewById(R.id.name);
            TextView channel = (TextView)convertView.findViewById(R.id.channel);
            TextView macAddress = (TextView)convertView.findViewById(R.id.macAddress);
            TextView freqmhz = (TextView)convertView.findViewById(R.id.freqmhz);

            imageView.setImageResource(device.getImageNumber());
            name.setText(device.getName());
            channel.setText("Channel: " + device.getChannel());
            macAddress.setText(device.getMacAddress());
            freqmhz.setText(device.getFreqmhz());

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (device.getDeviceType().equals("Access Point")) {
                        Toast.makeText(ListOfDevices.this, "Selected: " + device.getBssid(), Toast.LENGTH_SHORT).show();
                        if (attackNumber == 1) {
                            ArrayList<Device> tempDevices = new ArrayList<>();
                            for (Device deviceInList : devices) {
                                try {
                                    if (device.getBssid().equals(deviceInList.getBssid()) && deviceInList.getDeviceType().equals("Client"))
                                        tempDevices.add(deviceInList);
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                            Intent intent = new Intent(ListOfDevices.this, ListOfDevices.class);
                            intent.putParcelableArrayListExtra("devices", tempDevices);
                            intent.putExtra("attackNumber", attackNumber);
                            intent.putExtra("accessPoint", device);
                            startActivity(intent);
                        }
                        if (attackNumber == 2){
                            Intent intent = new Intent(ListOfDevices.this, AttackToggleAccessPoint.class);
                            intent.putExtra("device",device);
                            intent.putExtra("attackNumber",attackNumber);
                            startActivity(intent);
                        }
                        if (attackNumber == 3){
                            Intent intent = new Intent(ListOfDevices.this, AttackToggleAccessPoint.class);
                            intent.putExtra("device",device);
                            intent.putExtra("attackNumber",attackNumber);
                            startActivity(intent);
                        }
                    }
                    if (device.getDeviceType().equals("Client")){
                        if (attackNumber == 1) {
                            Intent intent = new Intent(ListOfDevices.this, AttackToggle.class);
                            intent.putExtra("device", device);
                            intent.putExtra("accessPoint", accessPoint);
                            for (Device deviceInList : devices) {
                                if (device.getBssid().equals(deviceInList.getBssid()) && deviceInList.getDeviceType().equals("Access Point")) {
                                    intent.putExtra("accessPoint", deviceInList);
                                }
                            }
                            startActivity(intent);
                        }
                    }
                }
            });

            return convertView;
        }
    }

    // ---------------------------------------
    // ------ CONNECT TO SSH OPERATIONS ------
    // ---------------------------------------

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((SshOperations.LocalBinder)service).getService();
            mBoundService.setMainActivity(ListOfDevices.this);
            Toast.makeText(ListOfDevices.this,"TEMP: SSH Connection successfully transformed.", Toast.LENGTH_SHORT).show();
        }
        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(ListOfDevices.this, "Goodbye SSHOperationClass", Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        if (bindService(new Intent(ListOfDevices.this, SshOperations.class), mConnection, Context.BIND_AUTO_CREATE)) {
            mShouldUnbind = true;
        } else {
            Log.e("MY_APP_TAG", "Error: The requested service doesn't " + "exist, or this client isn't allowed access to it.");
        }
    }

    void doUnbindService() {
        if (mShouldUnbind) {
            unbindService(mConnection);
            mShouldUnbind = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    // ---------------------------------------
    // -------- MY CALLBACK METHODS ----------
    // ---------------------------------------

    public void changeAWSStatus(String text) {}
    public void updateText(String text){}

    // ---------------------------------------
    // - BACKGROUND TASK FOR SENDING COMMAND -
    // ---------------------------------------

    private class executeCommand extends AsyncTask<Integer, Void, Void> {
        MyCallback mainActivity;
        String command;
        String toastText;
        String stringToStatus;
        executeCommand (MyCallback mainActivity, String command, String toastText, String stringToStatus){
            this.mainActivity = mainActivity;
            this.command = command;
            this.toastText = toastText;
            this.stringToStatus = stringToStatus;
        }
        @Override
        protected Void doInBackground(Integer... params) {
            mBoundService.sendCommandToAWS(command);
            mainActivity.updateText(stringToStatus);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ListOfDevices.this, toastText, Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    }

}

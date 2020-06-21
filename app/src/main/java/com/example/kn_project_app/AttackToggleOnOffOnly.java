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

public class AttackToggleOnOffOnly extends AppCompatActivity implements MyCallback {

    Button startAttack;
    Button endAttack;
    TextView status;

    private boolean mShouldUnbind;
    private SshOperations mBoundService;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        handler = new Handler();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attack_toggle_on_off_only);

        startAttack = findViewById(R.id.startFour);
        endAttack = findViewById(R.id.endFour);

        status = findViewById(R.id.statusOfAttack);

        getSupportActionBar().setTitle("Attack 4");

        startAttack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("WALACH","KLIKNIETO");
                new AttackToggleOnOffOnly.executeCommand(AttackToggleOnOffOnly.this, "touch watykanyczk","ATAK CZWARTY","Started").execute();
            }
        });

        endAttack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AttackToggleOnOffOnly.executeCommand(AttackToggleOnOffOnly.this,"rm watykanczyk","ATAK CZWARTY OFF","Ended").execute();
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
            mBoundService.setMainActivity(AttackToggleOnOffOnly.this);
            Toast.makeText(AttackToggleOnOffOnly.this,"TEMP: SSH Connection successfully transformed.", Toast.LENGTH_SHORT).show();
        }
        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(AttackToggleOnOffOnly.this, "Goodbye SSHOperationClass", Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        if (bindService(new Intent(AttackToggleOnOffOnly.this, SshOperations.class), mConnection, Context.BIND_AUTO_CREATE)) {
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
                    Toast.makeText(AttackToggleOnOffOnly.this, toastText, Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    }

}

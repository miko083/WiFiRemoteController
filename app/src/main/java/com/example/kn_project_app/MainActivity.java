package com.example.kn_project_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;




public class MainActivity extends AppCompatActivity implements MyCallback {

    private TextView statusAWS;
    private CardView attack1, kismet, terminal;
    private boolean mShouldUnbind;
    private SshOperations mBoundService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setTitle("Raspberry PI Connector");

        terminal = findViewById(R.id.terminal);
        statusAWS = findViewById(R.id.statusAWS);
        attack1 = findViewById(R.id.attack1);
        kismet = findViewById(R.id.kismet);

        terminal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBoundService.getStatusFromAWS()) {
                    Intent intent = new Intent(MainActivity.this, SendCommandActivity.class);
                    startActivity(intent);
                } else
                    Toast.makeText(MainActivity.this, "Connect first to AWS.", Toast.LENGTH_SHORT).show();
            }
        });

        attack1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBoundService.getStatusFromAWS()) {
                    //new executeCommand(MainActivity.this, "cat example-01.kismet.netxml").execute();
                    //new executeCommand(MainActivity.this, "cat $(ls /root/.airodump/recent-0* | sort | tail -1)").execute();
                    new executeCommand(MainActivity.this, "cat $(ls /tmp/recent-0* | sort | tail -1)").execute();
                } else
                    Toast.makeText(MainActivity.this, "Connect first to AWS.", Toast.LENGTH_SHORT).show();

            }
        });

        kismet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBoundService.getStatusFromAWS()) {
                    Intent intent = new Intent(MainActivity.this, KismetWebsite.class);
                    startActivity(intent);
                } else
                    Toast.makeText(MainActivity.this, "Connect first to AWS.", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onStart(){
        super.onStart();
        doBindService();
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.connectToAWS: {
                mBoundService.setMainActivity(this);
                startService(new Intent(this, SshOperations.class));
                return true;
            }
            case R.id.disconnectAWS:{
                changeAWSStatus("Offline");
                mBoundService.disconnectAWS();
                Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    // ---------------------------------------
    // -------- MY CALLBACK METHODS ----------
    // ---------------------------------------

    public void changeAWSStatus(String text){
        statusAWS.setText("AWS Status: " + text);
    }
    public void updateText(String text){}

    // ---------------------------------------
    // ------ CONNECT TO SSH OPERATIONS ------
    // ---------------------------------------

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((SshOperations.LocalBinder)service).getService();
            mBoundService.setMainActivity(MainActivity.this);
            Toast.makeText(MainActivity.this,"TEMP: SSH Connection successfully transformed.", Toast.LENGTH_SHORT).show();
        }
        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(MainActivity.this, "Goodbye SSHOperationClass", Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        if (bindService(new Intent(MainActivity.this, SshOperations.class), mConnection, Context.BIND_AUTO_CREATE)) {
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
        executeCommand (MyCallback mainActivity, String command){
            this.mainActivity = mainActivity;
            this.command = command;
        }
        @Override
        protected Void doInBackground(Integer... params) {
            String list = mBoundService.sendCommandToAWS(command);
            ArrayList<Device> devices = DeviceConverter.convertFromXmlToDevice(list);
            Intent intent = new Intent(MainActivity.this, ListOfDevices.class);
            intent.putParcelableArrayListExtra("devices", devices);
            intent.putExtra("ifDeviceList", false);
            startActivity(intent);
            return null;
            }
    }
}
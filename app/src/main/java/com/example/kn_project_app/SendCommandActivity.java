package com.example.kn_project_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class SendCommandActivity extends AppCompatActivity implements MyCallback{

    Button sendToAWS;
    EditText toAWSCom;
    TextView fromAWSCom;
    private boolean mShouldUnbind;
    private SshOperations mBoundService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_command);

        getSupportActionBar().setTitle("AWS Command Line");

        sendToAWS = findViewById(R.id.sendToAWS);
        toAWSCom = findViewById(R.id.et_toAWSCom);
        fromAWSCom = findViewById(R.id.tv_fromAWSCom);
        fromAWSCom.setMovementMethod(new ScrollingMovementMethod());

        sendToAWS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("TEST",toAWSCom.getText().toString());
                new sendCommandToAWS(toAWSCom.getText().toString(), SendCommandActivity.this).execute();
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
        ((TextView)findViewById(R.id.tv_fromAWSCom)).setText(text); }

    // ---------------------------------------
    // ------ CONNECT TO SSH OPERATIONS ------
    // ---------------------------------------

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((SshOperations.LocalBinder)service).getService();
            Toast.makeText(SendCommandActivity.this,"TEMP: SSH Connection successfully transformed.", Toast.LENGTH_SHORT).show();
        }
        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(SendCommandActivity.this, "Goodbye SSHOperationClass", Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        if (bindService(new Intent(SendCommandActivity.this, SshOperations.class), mConnection, Context.BIND_AUTO_CREATE)) {
            mShouldUnbind = true;
        } else {
            Log.e("MY_APP_TAG", "Error: The requested service doesn't " +
                    "exist, or this client isn't allowed access to it.");
        }
    }

    void doUnbindService() {
        if (mShouldUnbind) {
            unbindService(mConnection);
            mShouldUnbind = false;
        }
    }

    // ---------------------------------------
    // - BACKGROUND TASK FOR SENDING COMMAND -
    // ---------------------------------------

    private class sendCommandToAWS extends AsyncTask<Integer, Void, Void> {
        String command;
        MyCallback myCallback;

        sendCommandToAWS(String command, MyCallback myCallback){
            this.command = command;
            this.myCallback = myCallback;
        }

        @Override
        protected Void doInBackground(Integer... params) {
            myCallback.updateText(mBoundService.sendCommandToAWS(command));
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }
}

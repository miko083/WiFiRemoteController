package com.example.kn_project_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.jcraft.jsch.JSchException;

public class KismetWebsite extends AppCompatActivity implements MyCallback {
    private Uri uri;
    private boolean mShouldUnbind;
    private SshOperations mBoundService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kismet_website);

        getSupportActionBar().hide();

        uri = Uri.parse("http://localhost:54321"); // missing 'http://' will cause crashed
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    @Override
    protected void onStart(){
        super.onStart();
        doBindService();
        try {
            mBoundService.openKismet("ubuntu", "proxy-vm.ddns.net", 22);
        } catch (JSchException e) {
            Log.d("KISMET_ERROR", e.getMessage());
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        mBoundService.closeKismet();
    }

    // ---------------------------------------
    // -------- MY CALLBACK METHODS ----------
    // ---------------------------------------

    public void changeAWSStatus(String text) {}
    public void updateText(String text){}

    // ---------------------------------------
    // ------ CONNECT TO SSH OPERATIONS ------
    // ---------------------------------------

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((SshOperations.LocalBinder)service).getService();
            mBoundService.setMainActivity(KismetWebsite.this);
            Toast.makeText(KismetWebsite.this,"TEMP: SSH Connection successfully transformed.", Toast.LENGTH_SHORT).show();
        }
        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(KismetWebsite.this, "Goodbye SSHOperationClass", Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        if (bindService(new Intent(KismetWebsite.this, SshOperations.class), mConnection, Context.BIND_AUTO_CREATE)) {
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
}

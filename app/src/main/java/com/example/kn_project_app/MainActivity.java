package com.example.kn_project_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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


        // -------------------------
        // --- EXAMPLE ARRAY LIST --
        // -------------------------

        ArrayList<Device> temp = new ArrayList<>();
        temp.add(new Device(R.drawable.access_point, "Watykan", "Access Point", 2137, "21:37:11:09:17:18:10:05"));
        temp.add(new Device(R.drawable.phone, "Huawei", "Client", 2005, "10:05:12:39:15:65:14:35"));
        temp.add(new Device(R.drawable.phone, "Xiaomi", "Client", 2137, "22:27:41:59:54:21:12:55"));
        temp.add(new Device(R.drawable.phone, "Apple", "Client", 2137, "24:29:48:51:54:25:12:55"));

        final ArrayList<Device> devices = temp;

        attack1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ListOfDevices.class);
                intent.putParcelableArrayListExtra("devices", devices);
                startActivity(intent);
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

    // ---- ZIOMAL TU JEST PRZESZLOSC. TERAZ WSZYSTKO DZIEJE SIE W SSH_OPERATIONS, KLASA KTORA JEST SERVICE CZYLI DZIALA CIAGLE W TLE ----

    /*
    private class toAWS extends AsyncTask<Integer, Void, Void> {
        MyCallback myCallback;
        toAWS(MyCallback callback) {
            myCallback = callback;
        }
        @Override
        protected Void doInBackground(Integer... params) {
            myCallback.connectToAWS();
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    if(myCallback.getStatusFromAWS()) {
                        Toast.makeText(MainActivity.this, "Connected to AWS", Toast.LENGTH_SHORT).show();
                        myCallback.changeAWSStatus("Online");
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                        myCallback.changeAWSStatus("Offline");
                    }
                }
            });
            return null;
        }
    }
    /*
    private static class SSHTask extends AsyncTask<Integer, Void, Void> {
        private byte[] privateKey;
        private String command;
        private WeakReference<MainActivity> activityReference;
        MyCallback myCallback = null;
        SSHTask(byte[] privateKey, String command, MainActivity context, MyCallback callback) {
            this.privateKey = privateKey;
            this.command = command;
            this.activityReference = new WeakReference<>(context);
            this.myCallback = callback;
        }
        @Override
        protected Void doInBackground(Integer... params) {
            // Zestawianie tunelu ssh do kismeta:
            // Tworzy na androidzie socket localhost:54321, który prowadzi do kismeta
            // Po zestawieniu tunelu wystarczy otworzuć w przeglądarce telefonu: localhost:54321
            // Login/hasło do kismeta, to kismet/kismet
            //TODO: Zrobić zestawianie i zamykanie kismetTunnel za pomocą przycisku
            SshOperations kismetTunnel = new SshOperations(privateKey);
            try {
                kismetTunnel.connectToKismet();
            } catch (Exception e1) {Log.d("SSH1 ERROR: ", e1.getMessage());}
            // Zestawianie tunelu ssh do raspberry:
            // Tworzy na androidzie socket localhost:54322, który prowadzi do ssh raspberry
            // Ten tunel jest wykorzystywany przez obiekt sshOperations
            // TODO: przenieść to do onCreate() lub zbindować pod przycisk
            SshOperations raspberryTunnel = new SshOperations(privateKey);
            try {
                raspberryTunnel.connectToRaspberry();
            } catch (Exception e1) {Log.d("SSH2 ERROR: ", e1.getMessage());}
            SshOperations sshOperations = new SshOperations(privateKey);
            try {
                // UPDATE: moduł nie łączy się do proxy-vm.ddns.net:22, tylko do localhost:54322
                //TODO: naprawić problem "E/SpannableStringBuilder: SPAN_EXCLUSIVE_EXCLUSIVE spans cannot have a zero length"
                sshOperations.open("root", "localhost", 54322);
                String ret = sshOperations.runCommand(command);
                sshOperations.close();
                Log.d("COMMAND OUTPUT", ret);
                if (myCallback != null)
                    myCallback.updateMyText(ret);
            } catch (Exception e1) {Log.d("SSH3 ERROR: ", e1.getMessage());}
            return null;
        }
        protected Void onPostExecute () {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing())
                return null;
            return null;
        }
    }
     */

}
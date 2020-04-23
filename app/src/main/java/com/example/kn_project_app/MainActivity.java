package com.example.kn_project_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements MyCallback {

    private TextView textView;
    Button sendToAWS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText toAWSCom = findViewById(R.id.et_toAWSCom);

        sendToAWS = findViewById(R.id.sendToAWS);
        textView = findViewById(R.id.tv_fromAWSCom);

        byte[] temp = null;
        try {
            InputStream inputStream = getAssets().open("Mikolaj_Key.pem");
            temp = IOUtils.toByteArray(inputStream);
        } catch (IOException e1){
            Log.d("No Asset", e1.getMessage());
        }

        final byte[] privateKey = temp;
        Log.d("PRIVATEKEY: ", Arrays.toString(privateKey));

        sendToAWS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SSHTask(privateKey, toAWSCom.getText().toString(), MainActivity.this, MainActivity.this).execute();
            }
        });

    }

    @Override
    public void updateMyText(String text) {
        ((TextView)findViewById(R.id.tv_fromAWSCom)).setText(text);
    }

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
            SshOperations sshOperations = new SshOperations(privateKey);
            try {
                sshOperations.open();
                String ret = sshOperations.runCommand(command);
                sshOperations.close();
                Log.d("COMMAND OUTPUT", ret);
                if (myCallback != null)
                    myCallback.updateMyText(ret);

            } catch (Exception e1) {Log.d("SSH ERROR: ", e1.getMessage());}
            return null;
        }

        protected Void onPostExecute () {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing())
                return null;
            return null;
        }
    }
}


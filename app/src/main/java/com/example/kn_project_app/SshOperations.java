package com.example.kn_project_app;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class SshOperations extends Service {

    private Session sessionAWS;
    private Session sessionRaspberry;
    private Session sessionKismet;

    private byte[] privateKey;
    private MyCallback mainActivity;
    Handler handler;

    @Override
    public void onCreate() {
        handler = new Handler();
        byte[] temp = null;
        try {
            InputStream inputStream = getAssets().open("Private_Key.pem");
            temp = IOUtils.toByteArray(inputStream);

        } catch (IOException e1){
            Log.d("No Asset", e1.getMessage());
        }

        this.privateKey = temp;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MYSERVICE","onStartCommand");
        new connectToAWS(mainActivity).execute();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        disconnectAWS();
        Toast.makeText(this, "Disconnected from AWS", Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------
    // -------- AWS CONNECTING STUFF ---------
    // ---------------------------------------

    public boolean connectToAWS (String username, String host, int port) throws JSchException {

        sessionAWS = open(username, host,port);
        sessionAWS.setPortForwardingL(54322, "localhost", 50022);

        sessionRaspberry = open("root", "localhost", 54322);

        Log.d("TUNNEL: ","Conneted to Kismet" );
        Log.d("CONNECTION: ","Connected to AWS");

        return true;
    }

    public void openKismet (String username, String host, int port) throws JSchException {
        sessionKismet = open(username, host, port);
        sessionKismet.setPortForwardingL(54321,"localhost",52501);
    }

    public void closeKismet(){
        sessionKismet.disconnect();
    }

    public Session open(String username, String host, int port) throws JSchException{
        JSch jSch = new JSch();
        jSch.addIdentity("", privateKey, null, null);
        Session session = jSch.getSession(username, host, port);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        return session;
    }


    public void disconnectAWS(){
        if (sessionAWS != null)
            sessionAWS.disconnect();
        if (sessionKismet != null)
            sessionKismet.disconnect();
        if (sessionRaspberry != null)
            sessionRaspberry.disconnect();
    }

    public boolean getStatusFromAWS(){
        if (sessionAWS == null)
            return false;
        return sessionAWS.isConnected();
    }

    public String sendCommandToAWS (String command){
        if(getStatusFromAWS()) {
            Log.d("SENT COMMAND: ", command);
            String ret = "";
            ChannelExec channelExec = null;
            try {
                channelExec = (ChannelExec) sessionRaspberry.openChannel("exec");
            } catch (JSchException e) {
                e.printStackTrace();
            }

            channelExec.setCommand(command);
            channelExec.setInputStream(null);

            InputStream in = null;
            try {
                in = channelExec.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                channelExec.connect();
            } catch (JSchException e) {
                e.printStackTrace();
            }
            try {
                ret = getChannelOutput(channelExec, in);
            } catch (IOException e) {
                e.printStackTrace();
            }
            channelExec.disconnect();

            return ret;
        }
        return "Failed";
    }

    private String getChannelOutput(Channel channel, InputStream in) throws IOException{
        byte[] buffer = new byte[1024];
        StringBuilder stringBuilder = new StringBuilder();

        while (true){
            while (in.available() > 0) {
                int i = in.read(buffer,0,1024);
                if (i < 0)
                    break;
                stringBuilder.append(new String(buffer,0,i));
            }
            if (channel.isClosed())
                break;
            try {
                Thread.sleep(1000);
            } catch (Exception e1) {Log.d("THREAD: ", e1.getMessage());}
        }
        return stringBuilder.toString();
    }

    // ---------------------------------------
    // ------- ACTIVITIES CONNECTOR ----------
    // ---------------------------------------

    public void setMainActivity(MyCallback mainActivity){
        this.mainActivity = mainActivity;
    }

    public class LocalBinder extends Binder {
        SshOperations getService() {
            return SshOperations.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    // ---------------------------------------
    //  BACKGROUND TASK FOR CONNECTING COMMAND
    // ---------------------------------------

    private class connectToAWS extends AsyncTask<Integer, Void, Void> {
        MyCallback mainActivity;
        connectToAWS(MyCallback mainActivity){
            this.mainActivity = mainActivity;
        }
        @Override
        protected Void doInBackground(Integer... params) {
            if(!getStatusFromAWS()) {
                try {
                    connectToAWS("ubuntu", "proxy-vm.ddns.net", 22);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SshOperations.this, "Connected to AWS", Toast.LENGTH_SHORT).show();
                            mainActivity.changeAWSStatus("Online");
                        }
                    });
                    sendCommandToAWS("tmux kill-session -t kismet; tmux kill-session -t airodump; tmux kill-session -t deAuth; tmux kill-session -t fakeProbe; tmux kill-session -t fakeAuth; tmux kill-session -t beaconFlood; airmon-ng stop wlan1; tmux new-session -d -s airodump 'airmon-ng start wlan1 && airodump-ng wlan1 --output-format netxml -w /tmp/recent'");
                } catch (JSchException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SshOperations.this, "Failed", Toast.LENGTH_SHORT).show();
                            mainActivity.changeAWSStatus("Offline");
                        }
                    });
                    e.printStackTrace();
                }
            }
            else
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SshOperations.this, "You are already connected to AWS.", Toast.LENGTH_SHORT).show();
                    }
                });
            return null;
        }
    }
}

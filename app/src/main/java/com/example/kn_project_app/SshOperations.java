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
            InputStream inputStream = getAssets().open("Mikolaj_Key.pem");
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

        //endCommandToAWS("airmon-ng start wlan1 && airodump-ng wlan1 --output-format netxml -w /tmp/recent > /dev/null 2>&1");

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
                //channelExec = (ChannelExec) sessionAWS.openChannel("exec");
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
                    sendCommandToAWS("tmux new-session -d -s launchApp 'airmon-ng start wlan1 >/dev/null 2>&1 && airodump-ng wlan1 --output-format netxml -w /tmp/recent' >/dev/null 2>&1");
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

    // ---------------------------------------
    // ----------- TODO TASKS  ---------------
    // ---------------------------------------
    /*

    public void connectToKismet() throws JSchException {
        this.open("ubuntu", "proxy-vm.ddns.net", 22);
        // SSH tunnel parameter to the kismet:
        int tunnelLocalPort = 54321;
        String tunnelRemoteHost = "localhost";
        int tunnelRemotePort = 52501;
        session.setPortForwardingL(tunnelLocalPort, tunnelRemoteHost, tunnelRemotePort);

        Log.d("TUNNEL: ","Conneted to Kismet" );
    }

    //metoda wywołuje open() zestawiając połączenie do proxy-vm.ddns.net
    //następnie zestawia tunel: (android) localhost:54322 --> (VM) localhost:50022
    //TODO: połączyć metody connectToKismet() i connectToRaspberry() w jedną
    public void connectToRaspberry() throws JSchException {
        this.open("ubuntu", "proxy-vm.ddns.net", 22);
        // SSH tunnel parameter to the kismet:
        int tunnelLocalPort = 54322;
        String tunnelRemoteHost = "localhost";
        int tunnelRemotePort = 50022;
        session.setPortForwardingL(tunnelLocalPort, tunnelRemoteHost, tunnelRemotePort);

        Log.d("TUNNEL: ","Conneted to Kismet" );
    }


    public String runCommand (String command) throws  JSchException, IOException {
        String ret = "";
        if (!session.isConnected())
            throw new RuntimeException("Connect First");

        ChannelExec channelExec = null;
        channelExec = (ChannelExec) session.openChannel("exec");

        channelExec.setCommand(command);
        channelExec.setInputStream(null);

        //PrintStream out = new PrintStream(channelExec.getOutputStream());
        InputStream in = channelExec.getInputStream();

        channelExec.connect();

        ret = getChannelOutput(channelExec, in);
        channelExec.disconnect();
        Log.d("SENT COMMAND: ", command);

        return ret;
    }



    public void close(){
        session.disconnect();
        Log.d("CONNECTION: ", "LOGOUT FROM AWS");
    }

    private static void executeCommand(byte[] privateKey, String command) {

        JSch sshClient = new JSch();
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        try {
            Log.d("PRIVATEKEY: ", Arrays.toString(privateKey));
            sshClient.addIdentity("", privateKey, null, null);
            com.jcraft.jsch.Session session = sshClient.getSession("ubuntu", "ec2-3-17-16-182.us-east-2.compute.amazonaws.com");
            session.setConfig(config);
            session.connect();
            ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(command);
            channelExec.connect();
            channelExec.start();
        } catch (JSchException e2) {
            Log.d("SSH Error", e2.getMessage());
        }
    }

     */
}

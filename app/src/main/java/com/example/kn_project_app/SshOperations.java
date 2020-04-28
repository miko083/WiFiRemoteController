package com.example.kn_project_app;

import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;

public class SshOperations {
    private Session session;
    private byte[] privateKey;


    public SshOperations(byte[] privateKey) {
        this.privateKey = privateKey;
    }

    //Update: jako argument trzeba podać username, host i port
    public void open(String username, String host, int port) throws JSchException {
        JSch jSch = new JSch();
        jSch.addIdentity("", privateKey, null, null);
        session = jSch.getSession(username, host, port);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        Log.d("CONNNECTION: ","Conneted to AWS" );
    }

    //metoda wywołuje open() zestawiając połączenie do proxy-vm.ddns.net
    //następnie zestawia tunel: (android) localhost:54321 --> (VM) localhost:52501
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
}

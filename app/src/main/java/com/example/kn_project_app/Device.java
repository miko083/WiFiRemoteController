package com.example.kn_project_app;

import android.os.Parcel;
import android.os.Parcelable;

public class Device implements Parcelable {
    private int imageNumber;
    private String name;
    private String deviceType;
    private String channel;
    private String macAddress;
    private String freqmhz;
    private String bssid;

    public Device(int imageNumber, String name, String deviceType, String channel, String macAddress, String freqmhz, String bssid) {
        this.imageNumber = imageNumber;
        this.name = name;
        this.deviceType = deviceType;
        this.channel = channel;
        this.macAddress = macAddress;
        this.freqmhz = freqmhz;
        this.bssid = bssid;
    }

    protected Device(Parcel in) {
        imageNumber = in.readInt();
        name = in.readString();
        deviceType = in.readString();
        channel = in.readString();
        macAddress = in.readString();
        freqmhz = in.readString();
        bssid = in.readString();
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    public int getImageNumber() {
        return imageNumber;
    }

    public String getName() {
        return name;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getChannel() {
        return channel;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getFreqmhz(){
        return freqmhz;
    }

    public String getBssid(){
        return bssid;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(imageNumber);
        dest.writeString(name);
        dest.writeString(deviceType);
        dest.writeString(channel);
        dest.writeString(macAddress);
        dest.writeString(freqmhz);
        dest.writeString(bssid);
    }
}

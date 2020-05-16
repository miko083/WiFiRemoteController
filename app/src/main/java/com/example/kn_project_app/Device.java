package com.example.kn_project_app;

import android.os.Parcel;
import android.os.Parcelable;

public class Device implements Parcelable {
    private int imageNumber;
    private String manufacturer;
    private String deviceType;
    private int channel;
    private String macAddress;

    public Device(int imageNumber, String manufacturer, String deviceType, int channel, String macAddress) {
        this.imageNumber = imageNumber;
        this.manufacturer = manufacturer;
        this.deviceType = deviceType;
        this.channel = channel;
        this.macAddress = macAddress;
    }

    protected Device(Parcel in) {
        imageNumber = in.readInt();
        manufacturer = in.readString();
        deviceType = in.readString();
        channel = in.readInt();
        macAddress = in.readString();
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

    public String getManufacturer() {
        return manufacturer;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public int getChannel() {
        return channel;
    }

    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(imageNumber);
        dest.writeString(manufacturer);
        dest.writeString(deviceType);
        dest.writeInt(channel);
        dest.writeString(macAddress);
    }
}

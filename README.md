# Wi-Fi Remote Controller (Project for Kolo Naukowe)

App which allows to connect to Raspberry PI (with Linux installed) in different network through AWS service. Please use your own key and EC2 instance.

## Features

### Attacks

#### Deauthentication

Disconnect users from access point.

#### Fake Authentication DoS

Assign fake clients to Access Point which will cause a frezze or reset of access point.

#### Fake Probe Response

Respond to all probes and enable beaconing of probed ESSID.

#### Beacon Flood

Send beacon frames to show fake acess points nearby Raspberry PI.

### Kismet Webiste

Open kismet website (forwarded from Raspberry PI).

### Terminal

Run commands directly on the Raspberry PI.

## Screenshot

### Main Menu

![Main menu](https://github.com/miko083/WiFiRemoteController/blob/master/images/main_menu.png)

## Authors

* [Paweł Czaja](https://github.com/GitHub-Pawel) - Kali Linux scirpts, linking them to Android App 
* [Mikołaj Stoch](https://github.com/miko083) - Android App

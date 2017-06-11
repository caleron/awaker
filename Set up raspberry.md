## Prerequisites
* Raspberry Pi 2/3 (instructions here are for 3)
* micro SD card min. 16GB 
* micro SD card reader

## Installing Raspbian
 * Download the [Raspbian lite image](https://www.raspberrypi.org/downloads/raspbian/)
 * Download and install [Etcher](https://etcher.io/)
 * Flash the image on your sd card with Etcher
 * insert the sd card into your raspberry, attach monitor, keyboard and power supply, boot
 * login with pi/raspberry 
   * keyboard layout is probably us, therefore type raspberrz for the password (keep that in mind)

### Setting up Raspbian
#### raspi-config
 * type `sudo raspi-config`
 * choose `Boot Options` and activate console startup (maybe with autologin)
 * under `Localization Options` select your time zone and wifi country
 * activate SSH, SPI and I2C in `Interfacing Options`
   
#### Change keyboard layout
 * `sudo nano /etc/default/keyboard`
 * change `us` to `de` (or your own layout)
 * Save: Ctrl + X, type Y, Enter
   
#### Connect to your wlan (detailed instructions [here](https://www.raspberrypi.org/documentation/configuration/wireless/wireless-cli.md)
 * `sudo ifconfig wlan0 up`
 * `sudo nano /etc/wpa_supplicant/wpa_supplicant.conf`
 * enter the following at the end of the file:

```
      network={
          ssid="<the wlan name (essid)>"
          psk="<wlan password>"
      }
```

 * wait some time, use `sudo wpa_cli reconfigure` or reboot
 * check if connected with `iwconfig` (ESSID should be there)
 * get the ip with `ifconfig` (second line "inet addr") and memorize it
 
#### Set up remote access
* shutdown raspberry, remove keyboard, monitor and place it where you want
* let it boot again with only the power supply attached
* install and start [Putty](http://www.putty.org/) on your PC
* connect to the memorized ip and login
* do the same with [WinSCP](https://winscp.net/eng/docs/lang:de)
 
#### Update all the software
* `sudo apt-get update`
* `sudo apt-get dist-upgrade`
* `sudo apt-get upgrade`

#### Install some stuff
* Java 8 JDK: `sudo apt-get install oracle-java8-jdk`
* Git: `sudo apt-get install git`
* create /home/pi/rf24 (with mkdir) and cd in there
* download [RF24](https://tmrh20.github.io/RF24/RPi.html and associates (and awaker_master)
  * `git clone https://github.com/nRF24/RF24`
  * `git clone https://github.com/nRF24/RF24Network`
  * `git clone https://github.com/nRF24/RF24Mesh`
  * `git clone https://github.com/caleron/awaker_master`
* build them all
  * cd into each dir and execute `sudo make install`
  
## Setting up Awaker 
* clone this repository
* open in IntelliJ IDEA
* build the Artifact
* create a "awaker" directory in the raspberry home directory
* copy the contents of the artifact output to the raspberry
* create a "web" directory in the awaker directory and copy the contents of the [WebAwaker](https://github.com/caleron/WebAwaker) project in there

## Start!
Get into the awaker directory  (`cd /home/pi/awaker`) and finally start this: `sudo java -jar Awaker.jar`

## Autostart
* create a script with `sudo nano /home/pi/start.sh`
* type in the following contents:
```bash
#!/bin/sh
cd /home/pi/awaker
sudo java -jar Awaker.jar &
```
* save the file with Ctrl + X and Y
* type `sudo nano /etc/rc.local`
* add the line `/home/pi/start.sh` before the line `exit 0`
* save again with Ctrl + X and Y
* That's it!

## Advanced
* auto-mount usb drives: https://raspberrypi.stackexchange.com/questions/41959/automount-various-usb-stick-file-systems-on-jessie-lite

## Prerequisites
* Raspberry Pi 2/3
* micro SD card min. 16GB 
* micro SD card reader

## Install Raspbian
 * Download the Raspbian lite image [here](https://www.raspberrypi.org/downloads/raspbian/)
 * Download and install [Etcher](https://etcher.io/)
 * Flash the image on your sd card with Etcher
 * insert the sd card into your raspberry, attach monitor, keyboard and power supply, boot
 * login with pi/raspberry 
   * keyboard layout is probably us, therefore type raspberrz for the password (keep that in mind)

### Setting up Raspbian
 * raspi-config
   * type `sudo raspi-config`
   * choose `Boot Options` and activate console startup (maybe with autologin)
   * under `Localization Options` select your time zone and wifi country
   * activate SSH, SPI and I2C in `Interfacing Options`
 * change keyboard layout
   * `sudo nano /etc/default/keyboard`
   * change `us` to `de` (or your own layout)
   * Save: Ctrl + X, type Y, Enter
 

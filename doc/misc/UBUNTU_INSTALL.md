# Ubuntu Install

## Create a bootable USB 

- For Windows, use `etcher.io` or `Universal USB Installer`, ... many tools out there
- For MacOS, use `unetbootin.app`
- Follow the application instructions to create the bootable USB
- After downloading the desired ISO, simply boot into the BIOS 
  - Perhaps try `F1`, `F2`, `ESC`, `F10`, `F11`, `F12`, `del`
  - On MacBooks, with Unetbootin, it should automatically bring you to a selection menu to boot from the USB

## Install Ubuntu

- Click Install Ubuntu!
- There may be some hiccups here: 
  - The USB may be corrupt
  - The ISO was incorrect
  - Drivers missing...
  - ...

### Basics

- `sudo update-pciids` will update the names of the devices in the PCI slots
  - Check them out here: `lspci`

## Trouble Shooting

### Ubuntu 14.04 Network Drivers are Missing

- See https://askubuntu.com/questions/858546/wifi-not-working-intel-on-hp-spectre-x360-13
- You must install the required drivers: from a different computer download the following into a usb and install them.

Confirm a 64-bit installation:

```
arch
```

If the terminal returns `x86_64` then yours is a 64-bit install. Download the files to your desktop:

```
http://kernel.ubuntu.com/~kernel-ppa/mainline/v4.8.14/linux-headers-4.8.14-040814-generic_4.8.14-040814.201612101431_amd64.deb

http://kernel.ubuntu.com/~kernel-ppa/mainline/v4.8.14/linux-image-4.8.14-040814-generic_4.8.14-040814.201612101431_amd64.deb

http://kernel.ubuntu.com/~kernel-ppa/mainline/v4.8.14/linux-headers-4.8.14-040814_4.8.14-040814.201612101431_all.deb 

https://launchpad.net/ubuntu/+source/linux-firmware/1.162/+build/11279780/+files/linux-firmware_1.162_all.deb
```

Install them all from the terminal:

```
sudo dpkg -i *.deb
```

## Installing Ubuntu 16.04

This was much smoother of an experience and much fewer issues

- Installing CUDA was simple
  - `sudo dpkg -i <downloaded_cuda_repo.deb>`
  - `sudo apt-get update; sudo apt-get install cuda; sudo reboot`
- Issues which remained:
  - Soundcard fails to automatically switch outputs when headphones are plugged in
  - Mouse pad is too low of sensitivity even on maxed settings
  - Have yet to figure out how to safely encorporate SteelSeries keyboard-backlight

## Soundcard fails to switch between outputs automatically

Looking in Sound, there seems to be two output modes, "Digital" and "Speakers", where Digital handles output for headphones etc.

- Apparently this is a dumb known issue...
  - https://askubuntu.com/questions/769593/16-04-headphones-detected-but-not-switched-on-automatically-after-startup

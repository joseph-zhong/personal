# MacOS Reinstall

After I bought a mid-2014 MBP with a discrete NVIDIA GeForce 750m GPU, I ran into some issues
where the entire system would freeze, and then the computer would force a restart

I had seen this before when I upgraded to the first iteration of Sierra on my previous 
mid-2013 MBP with only the integrated Intel Iris Pro GPU. Back then, I concluded that watching 
videos in fullscreen mode was the culprit, but the problem went away with future iterations of 
Sierra updates.

However, on the NVIDIA enabled laptop, as I installed the CUDA toolkit and framework the 
crashes became increasingly frequent, as I disabled hardware acceleration in Chrome and 
switched my primary browser to Safari, but the crashes continued.

I then found several links where many users reported similar issues: seemingly random crashes 
related to the system dynamically switching between integrated and discrete mode.

Some users completely disabled the discrete GPU with limited success, while others took their 
laptop in to be replaced or repaired, believing that hardware was the issue. 

I tried installing `gfxCardStatus` to force my laptop to stay in Discrete mode, while also 
disabling dynamic graphics switching in the settings, but the issue seemed to persist.

I eventually found an official statement Apple released on this topic, instructing to 
re-install the OS and to upgrade the Adobe Flash Player drivers if necessary

0. Backup your files
1. Reboot the machine
2. Hold `CMD + R` while booting to enter recovery mode
3. Enter Disk Utility and Repair the Disk by clicking the drives, then `First-Aid`
4. Close Disk Utility and Re-Install OSX

At this point I haven't had any issues thus far in either discrete, integrated, or dynamically 
switching mode and I don't have Adobe Flash Player installed.

19 March 2017
-------------

- Crashed after opening VMFusion in Dynamic Switching Mode 
  - It seems to run smoothly in Discrete Mode, but if I switch out of VMWare Fusion and into VSCode it crashed once

23 March 2017
-------------

- I think I've figured it out: the key is to **ENABLE DYNAMIC SWITCHING in Energy Saver**
  - I never really understood why the CUDA installation tutorials to turn it off, must be an outdated workaround 
  - While watching videos in full screen or running WebGL applications crashing no longer occurs
    - While typing this it crashed again while running Spotify in fullscreen

3 April 2017
------------

- Disabled temporary files saving in Java Settings, seemed to improve things
- Set Battery delay to 20min, AC to 3hrs
- Upgrade CUDA to 8.0.71
- Boot in SafeMode by holding `Shift` while booting as sanity check -- no crashes

The above allowed me to run very intensive programs (VMWare, All my editors, two large textbooks in fullscreen...)

- https://discussions.apple.com/thread/4622383?start=0&tstart=0

```
Last login: Mon Apr  3 21:21:55 on console
~$ pmset -g | grep autopoweroff
 autopoweroffdelay    28800
 autopoweroff         1
~$ sudo pmset -a autopoweroff 0
Password:
~$ pmset -g | grep autopoweroff
 autopoweroffdelay    28800
 autopoweroff         0
```
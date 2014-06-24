AndroidWearBLE
==========

Android Wear and BLE - HRM example (with support for Google Glass)

for android wear support, a jar lib is required in libs see:
http://developer.android.com/wear/preview/start.html

You can change target to API Level 19 if Glass support not desired (remove glass sub package)

Design Considerations:

For Glass - a live heart rate display, with option of audio reading from the menu.

For Android Wear - notifications when you are above a limit “relax, slow down” and below a lower limit, “wake up”


TODO: 

1) For Google Glass HRM device address is currently hardcoded. Add leScan and persist selection by using
 CardScrollActivity to display device list.

2) For Android Wear the high and low limits are hard coded to 86-91 so add preference setting
   also device is hard coded - currently working on selecting device using stacked notifications 



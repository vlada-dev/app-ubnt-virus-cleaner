# app-ubnt-virus-cleaner

This app helps with removing MF virus attacking some Ubiquiti devices with obsolete firmware version.

**This is not official Ubiquiti app, its usage is at your own risk!**

## How to start?

    * Enter IP addresses of your Ubiquiti devices with username and password (form "ip,username,password", one ip per line)
    * You can specify additional pair of login credentials. The app will try alternative login pairs if original login credentials fail. You can leave it empty.
    * If you want to run app in passive mode (no action), uncheck both checkboxes.
    * If you want to just remove virus without firmware upgrade, you can check only "Remove virus" checkbox
    * If you want to just remove virus and run firmware upgrade, check both checkboxes


## How it works?


    * App prepares the list of IP addresses to connect
    * After successful login, app reads firmware version
    * App checks /etc/persistent directory for presence of the virus
    * App removes all virus files and reboot (if firmware upgrade is enabled, upgrade is started instead reboot)
    * App tried to reconnect upgraded devices to show you updated version of firmware. You can verify if upgrade was successful.




##  Tested devices


    * Airmax M
    * App will probably work on Airmax AC, Airgateway, Airfiber, ToughSwitch models but it hasn't been tested yet.


##  Compilation


    * Open project in Android Studio
    * Create APK package and install in your Android


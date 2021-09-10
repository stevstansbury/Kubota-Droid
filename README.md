# Connecting to local server stack

1. Replace BASE_URL with `http://aemp.kubota.lan:8080/` in the build.gradle file `buildConfigField("String", "BASE_URL", "http://aemp.kubota.lan:8080/")`


2. Configure emulator to have root access and change the hosts file to point to the ip the router assigned to your computer. https://www.thepolyglotdeveloper.com/2019/12/change-host-file-android-emulator/

sample hosts file
```
127.0.0.1       localhost
::1             ip6-localhost

# change IP
192.168.1.134   aemp.kubota.lan
```

example
```
~/Library/Android/sdk/emulator/emulator -writable-system -netdelay none -netspeed full -avd pixel_custom_hosts_file
adb root
adb remount

adb push ./hosts /etc/hosts && adb push ./hosts /etc/system/hosts && adb push ./hosts /system/etc/hosts
````

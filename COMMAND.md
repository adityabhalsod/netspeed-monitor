$ ANDROID_SERIAL="adb-RZCW61S07FX-FPqh1w._adb-tls-connect._tcp" ./gradlew installDebug 

$ adb -s "adb-RZCW61S07FX-FPqh1w._adb-tls-connect._tcp" shell am start -n com.netspeed.monitor.debug/com.netspeed.monitor.MainActivity 

$ adb -s "adb-RZCW61S07FX-FPqh1w._adb-tls-connect._tcp" shell pm grant com.netspeed.monitor.debug android.permission.POST_NOTIFICATIONS 

package com.arksine.easycam;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

public class NativeEasyCapture implements EasyCapture {

	
    private static String TAG = "NativeEasycam";
    private DeviceInfo currentDevice;
    boolean deviceConnected = false;


    private native boolean startDevice(String cacheDir, DeviceInfo dInfo);
	private native boolean startStreaming();
    private native void getNextFrame(Surface mySurface);
    private native boolean isDeviceAttached();
    private native void stopDevice();
    private static native String detectDevice(String deviceLocation);
    

    static {
        System.loadLibrary("easycapture");
    }

    public NativeEasyCapture(SharedPreferences sharedPrefs, Context context) {

	    boolean useToasts = sharedPrefs.getBoolean("pref_key_layout_toasts", true);

	    if (getDeviceSettings(sharedPrefs)) {

		    if (useToasts) {
			    // Show a toast telling the user what device has been set
			    CharSequence text = "Device set as " + currentDevice.getDriver()
					    + " at " + currentDevice.getLocation();
			    int duration = Toast.LENGTH_SHORT;
			    Toast toast = Toast.makeText(context, text, duration);
			    toast.show();
		    }

		    connect(context);
	    } else {

		    Log.e(TAG, "Unable to load device settings.");

		    if (useToasts) {
			    CharSequence text = "Unable to load device.";
			    int duration = Toast.LENGTH_SHORT;
			    Toast toast = Toast.makeText(context, text, duration);
			    toast.show();
		    }
	    }
    }


    private void connect(Context context) {
        boolean deviceReady = true;

        File deviceFile = new File(currentDevice.getLocation());
        if(deviceFile.exists()) {
            if(!deviceFile.canRead()) {
                Log.d(TAG, "Insufficient permissions on " + currentDevice.getLocation() +
                        " -- does the app have the CAMERA permission?");
                deviceReady = false;
            }
        }
        else {
            Log.w(TAG, currentDevice.getLocation() + " does not exist");
            deviceReady = false;
        }

        if(deviceReady) {
            Log.i(TAG, "Preparing camera with device name " + currentDevice.getLocation());
            deviceConnected = startDevice(context.getCacheDir().toString(), currentDevice);
        }
    }

    private boolean getDeviceSettings (SharedPreferences sharedPrefs) {

        String prefSelectDevice = sharedPrefs.getString("pref_key_select_device", "NO_DEVICE");
        if (prefSelectDevice.equals("NO_DEVICE")){
	        Log.e(TAG, "No device selected in preferences");
            // No device selected, exit.
            currentDevice = null;
            return false;
        }

        String prefTVStandard = sharedPrefs.getString("pref_key_select_standard", "NTSC");
        DeviceInfo.DeviceStandard std = DeviceInfo.DeviceStandard.valueOf(prefTVStandard);


        // Split the string into two parts.  The first part is the usb device name, the second is the driver name
        String[] devDesc = prefSelectDevice.split(":");

        if (devDesc.length < 2) {
            Log.e(TAG, "Error parsing Device settings");
            // Device was not in devices.json, exit
            return false;
        }

        currentDevice = JsonManager.getDevice(devDesc[1], std);

        if (currentDevice == null) {
	        Log.e(TAG, "Unable to find device " + devDesc[1] + " in devices.json");
            // Device was not in devices.json, exit
            return false;
        }

        // Set the TV Standard, device location, and deinterlace method as they are not device specific and thus
        // not stored in devices.json
        currentDevice.setDevStd(std);


        if (!setV4L2Location()) {
            Log.e(TAG, "Unable to V4L2 driver for " + devDesc[1] + " @ " + devDesc[0]);
            // Device was not in devices.json, exit
            return false;
        }

        String deintMethod = sharedPrefs.getString("pref_key_deinterlace_method", "NONE");
	    currentDevice.setDeinterlace(DeviceInfo.DeintMethod.valueOf(deintMethod));

        //Logging for debugging
        Log.i(TAG, "Currently set device name: " + currentDevice.getDriver());
        Log.i(TAG, "Currently set device location: " + currentDevice.getLocation());
        Log.i(TAG, "Currently set tv standard: " + currentDevice.getDevStd().toString());
        Log.i(TAG, "Currently set frame width: " + currentDevice.getFrameWidth());
        Log.i(TAG, "Currently set frame height: " + currentDevice.getFrameHeight());

	    return true;
    }


    public void getFrame(Surface mySurface) {

        getNextFrame(mySurface);
    }

    public void stop() {
        stopDevice();
    }

    public boolean isAttached() {
        return isDeviceAttached();
    }

    public boolean isDeviceConnected() {return deviceConnected;}

	public boolean streamOn() {
        return startStreaming();
	}

    static public String findDevice(String dName)
    {
    	return detectDevice(dName);
    }

    private boolean setV4L2Location () {

        String devLocation;
        String driver;      // The driver name returned from V4L2

			/*
			Iterate through the /dev/videoX devices located on the system
			to see if the driver for the current usb device has been loaded.
			If so, add it to the list that populates the preference fragment
			 */
        for (int i = 0; i < 99; i++) {

            devLocation = "/dev/video" + String.valueOf(i);
            File test = new File(devLocation);
            if (test.exists()) {

                // TODO: 3/24/2016
                // 		 right now the JNI function findDevice takes a location (file name) and returns
                //       the the driver name if the device is valid.  It would be better
                //		 for it to take bus info from the USB device and match it with
                //       what we have here.  Its possible that we have multiple V4l2 device
                //       that use the same driver, and the current implementation always
                //       selects the first one found.
                driver = detectDevice(devLocation);

                if (driver.equals(currentDevice.getDriver())) {

                    currentDevice.setLocation(devLocation);

                    Log.i(TAG, "V4L2 device " + currentDevice.getDriver() + " found at " +
                            devLocation);
                    return true;
                }
            }
        }

        return false;
    }

}

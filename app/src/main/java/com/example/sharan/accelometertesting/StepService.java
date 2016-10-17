/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.example.sharan.accelometertesting;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.sharan.accelometertesting.MainScreen.Activities.Pedometer;


public class StepService extends Service
{
    public static final String TAG = "name.bagi.levente.pedometer.StepService";
    public SharedPreferences mSettings;
    public PedometerSettings mPedometerSettings;
    public SharedPreferences mState;
    public SharedPreferences.Editor mStateEditor;
    public Utils mUtils;
    public SensorManager mSensorManager;

    public StepDetector mStepDetector;
    // public StepBuzzer mStepBuzzer; // used for debugging
    public StepDisplayer mStepDisplayer;
    public PaceNotifier mPaceNotifier;
    public DistanceNotifier mDistanceNotifier;
    public SpeedNotifier mSpeedNotifier;
    public CaloriesNotifier mCaloriesNotifier;
    public SpeakingTimer mSpeakingTimer;

    public PowerManager.WakeLock wakeLock;
    public NotificationManager mNM;

    public int mSteps;
    public int mPace;
    public float mDistance;
    public float mSpeed;
    public float mCalories;
    public String mSleepTime;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
     public class StepBinder extends Binder
    {
       public StepService getService()
        {
            return StepService.this;
        }
    }

    @Override
    public void onCreate()
    {
        Log.i(TAG, "[SERVICE] onCreate");
        super.onCreate();

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
       // showNotification();

        // Load settings
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mPedometerSettings = new PedometerSettings(mSettings);
        mState = getSharedPreferences("state", 0);

        mUtils = Utils.getInstance();
        mUtils.setService(this);
        mUtils.initTTS();

        acquireWakeLock();

        // Start detecting
        mStepDetector = new StepDetector();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        registerDetector();

        // Register our receiver for the ACTION_SCREEN_OFF action. This will make our receiver
        // code be called whenever the phone enters standby mode.
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);

        mStepDisplayer = new StepDisplayer(mPedometerSettings, mUtils);
        mStepDisplayer.setSteps(mSteps = mState.getInt("steps", 0));
        mStepDisplayer.addListener(mStepListener);
        mStepDetector.addStepListener(mStepDisplayer);

        mPaceNotifier = new PaceNotifier(mPedometerSettings, mUtils);
        mPaceNotifier.setPace(mPace = mState.getInt("pace", 0));
        mPaceNotifier.addListener(mPaceListener);
        mStepDetector.addStepListener(mPaceNotifier);

        mDistanceNotifier = new DistanceNotifier(mDistanceListener, mPedometerSettings, mUtils);
        mDistanceNotifier.setDistance(mDistance = mState.getFloat("distance", 0));
        mStepDetector.addStepListener(mDistanceNotifier);

        mSpeedNotifier = new SpeedNotifier(mSpeedListener, mPedometerSettings, mUtils);
        mSpeedNotifier.setSpeed(mSpeed = mState.getFloat("speed", 0));
        mPaceNotifier.addListener(mSpeedNotifier);

        mCaloriesNotifier = new CaloriesNotifier(mCaloriesListener, mPedometerSettings, mUtils);
        mCaloriesNotifier.setCalories(mCalories = mState.getFloat("calories", 0));
        mStepDetector.addStepListener(mCaloriesNotifier);

        mSpeakingTimer = new SpeakingTimer(mPedometerSettings, mUtils);
        mSpeakingTimer.addListener(mStepDisplayer);
        mSpeakingTimer.addListener(mPaceNotifier);
        mSpeakingTimer.addListener(mDistanceNotifier);
        mSpeakingTimer.addListener(mSpeedNotifier);
        mSpeakingTimer.addListener(mCaloriesNotifier);
        mStepDetector.addStepListener(mSpeakingTimer);

        // Used when debugging:
        // mStepBuzzer = new StepBuzzer(this);
        // mStepDetector.addStepListener(mStepBuzzer);

        // Start voice
        reloadSettings();

        // Tell the user we started.
        Toast.makeText(this, getText(R.string.started), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        Log.i(TAG, "[SERVICE] onStart");
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "[SERVICE] onDestroy");
        mUtils.shutdownTTS();

        // Unregister our receiver.
        unregisterReceiver(mReceiver);
        unregisterDetector();

        mStateEditor = mState.edit();
        mStateEditor.putInt("steps", mSteps);
        mStateEditor.putInt("pace", mPace);
        mStateEditor.putFloat("distance", mDistance);
        mStateEditor.putFloat("speed", mSpeed);
        mStateEditor.putFloat("calories", mCalories);
        mStateEditor.commit();

        mNM.cancel(R.string.app_name);

        wakeLock.release();

        super.onDestroy();

        // Stop detecting
        mSensorManager.unregisterListener(mStepDetector);

        // Tell the user we stopped.
        Toast.makeText(this, getText(R.string.stopped), Toast.LENGTH_SHORT).show();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void registerDetector()
    {
        Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER /*|Sensor.TYPE_STEP_COUNTER| Sensor.TYPE_MAGNETIC_FIELD | Sensor.TYPE_ORIENTATION*/);

        mSensorManager.registerListener(mStepDetector, mSensor,SensorManager.SENSOR_DELAY_FASTEST);



//        sharan
        if (mSensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER).size() != 0)
        {
            mSensorManager.registerListener(mStepDetector, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),SensorManager.SENSOR_DELAY_FASTEST, 5 * 60000);
        }
    }

    public void unregisterDetector()
    {
        mSensorManager.unregisterListener(mStepDetector);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.i(TAG, "[SERVICE] onBind");
        return mBinder;
    }

    /**
     * Receives messages from activity.
     */
    public final IBinder mBinder = new StepBinder();

    public interface ICallback
    {
        public void stepsChanged(int value);

        public void paceChanged(int value);

        public void distanceChanged(float value);

        public void speedChanged(float value);

        public void caloriesChanged(float value);

        /*public void sleepTimeChanged(String value);*/
    }

    public ICallback mCallback;

    public void registerCallback(ICallback cb)
    {
        mCallback = cb;
        //mStepDisplayer.passValue();
        //mPaceListener.passValue();
    }

    public int mDesiredPace;
    public float mDesiredSpeed;

    /**
     * Called by activity to pass the desired pace value,
     * whenever it is modified by the user.
     *
     * @param desiredPace
     */
    public void setDesiredPace(int desiredPace)
    {
        mDesiredPace = desiredPace;
        if (mPaceNotifier != null)
        {
            mPaceNotifier.setDesiredPace(mDesiredPace);
        }
    }

    /**
     * Called by activity to pass the desired speed value,
     * whenever it is modified by the user.
     *
     * @param desiredSpeed
     */
    public void setDesiredSpeed(float desiredSpeed)
    {
        mDesiredSpeed = desiredSpeed;
        if (mSpeedNotifier != null)
        {
            mSpeedNotifier.setDesiredSpeed(mDesiredSpeed);
        }
    }

    public void reloadSettings()
    {
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        if (mStepDetector != null)
        {
            mStepDetector.setSensitivity(Float.valueOf(mSettings.getString("sensitivity", "10")));
        }

        if (mStepDisplayer != null) mStepDisplayer.reloadSettings();
        if (mPaceNotifier != null) mPaceNotifier.reloadSettings();
        if (mDistanceNotifier != null) mDistanceNotifier.reloadSettings();
        if (mSpeedNotifier != null) mSpeedNotifier.reloadSettings();
        if (mCaloriesNotifier != null) mCaloriesNotifier.reloadSettings();
        if (mSpeakingTimer != null) mSpeakingTimer.reloadSettings();
    }

    public void resetValues()
    {
        mStepDisplayer.setSteps(0);
        mPaceNotifier.setPace(0);
        mDistanceNotifier.setDistance(0);
        mSpeedNotifier.setSpeed(0);
        mCaloriesNotifier.setCalories(0);
    }

    /**
     * Forwards pace values from PaceNotifier to the activity.
     */
    public StepDisplayer.Listener mStepListener = new StepDisplayer.Listener()
    {
        public void stepsChanged(int value)
        {
            mSteps = value;
            passValue();
        }

        public void passValue()
        {
            if (mCallback != null)
            {
                mCallback.stepsChanged(mSteps);
            }
        }

      /*  public void sleepTimeChanged(String sleepTime)
        {
            if (mCallback != null)
            {
                mCallback.sleepTimeChanged(sleepTime);
            }
        }*/


    };





    /**
     * Forwards pace values from PaceNotifier to the activity.
     */
    public PaceNotifier.Listener mPaceListener = new PaceNotifier.Listener()
    {
        public void paceChanged(int value)
        {
            mPace = value;
            passValue();
        }

        public void passValue()
        {
            if (mCallback != null)
            {
                mCallback.paceChanged(mPace);
            }
        }
    };
    /**
     * Forwards distance values from DistanceNotifier to the activity.
     */
    public DistanceNotifier.Listener mDistanceListener = new DistanceNotifier.Listener()
    {
        public void valueChanged(float value)
        {
            mDistance = value;
            passValue();
        }

        public void passValue()
        {
            if (mCallback != null)
            {
                mCallback.distanceChanged(mDistance);
            }
        }
    };
    /**
     * Forwards speed values from SpeedNotifier to the activity.
     */
    public SpeedNotifier.Listener mSpeedListener = new SpeedNotifier.Listener()
    {
        public void valueChanged(float value)
        {
            mSpeed = value;
            passValue();
        }

        public void passValue()
        {
            if (mCallback != null)
            {
                mCallback.speedChanged(mSpeed);
            }
        }
    };
    /**
     * Forwards calories values from CaloriesNotifier to the activity.
     */
    public CaloriesNotifier.Listener mCaloriesListener = new CaloriesNotifier.Listener()
    {
        public void valueChanged(float value)
        {
            mCalories = value;
            passValue();
        }

        public void passValue()
        {
            if (mCallback != null)
            {
                mCallback.caloriesChanged(mCalories);
            }
        }
    };





    /**
     * Show a notification while this service is running.
     */
    public void showNotification()
    {


        CharSequence text = getText(R.string.app_name);
        Notification notification = new Notification(R.drawable.ic_notification, null, System.currentTimeMillis());
        notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        Intent pedometerIntent = new Intent();
        pedometerIntent.setComponent(new ComponentName(this, Pedometer.class));
        pedometerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, pedometerIntent, 0);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

//        notification.setLatestEventInfo(this, text, getText(R.string.notification_subtitle), contentIntent);


        builder.setAutoCancel(false);
        builder.setTicker("this is ticker text");
        builder.setContentTitle("WhatsApp Notification");
        builder.setContentText("You have a new message");
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);
        builder.setSubText(text);   //API level 16
        builder.setNumber(100);
        builder.build();

        notification = builder.getNotification();


        mNM.notify(R.string.app_name, notification);
    }


    // BroadcastReceiver for handling ACTION_SCREEN_OFF.
    public BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // Check action just to be on the safe side.
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
            {
                // Unregisters the listener and registers it again.
                StepService.this.unregisterDetector();
                StepService.this.registerDetector();
                if (mPedometerSettings.wakeAggressively())
                {
                    wakeLock.release();
                    acquireWakeLock();
                }
            }
        }
    };

    public void acquireWakeLock()
    {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        int wakeFlags;
        if (mPedometerSettings.wakeAggressively())
        {
            wakeFlags = PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
        }
        else if (mPedometerSettings.keepScreenOn())
        {
            wakeFlags = PowerManager.SCREEN_DIM_WAKE_LOCK;
        }
        else
        {
            wakeFlags = PowerManager.PARTIAL_WAKE_LOCK;
        }
        wakeLock = pm.newWakeLock(wakeFlags, TAG);
        wakeLock.acquire();
    }

}


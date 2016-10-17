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

package com.example.sharan.accelometertesting.MainScreen.Activities;


import android.support.v4.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sharan.accelometertesting.PedometerSettings;
import com.example.sharan.accelometertesting.R;
import com.example.sharan.accelometertesting.StepService;
import com.example.sharan.accelometertesting.Utils;


public class Pedometer extends Fragment implements View.OnClickListener
{
    private static final String TAG = "Pedometer";
    private SharedPreferences mSettings;
    private PedometerSettings mPedometerSettings;
    private Utils mUtils;

    private TextView mStepValueView;
    private TextView mPaceValueView;
    private TextView mDistanceValueView;
    private TextView mSpeedValueView;
    private TextView mCaloriesValueView;
    static public  TextView txtv_sleepTime;




    //TextView mDesiredPaceView;
    private int mStepValue;
    private int mPaceValue;
    private float mDistanceValue;
    private float mSpeedValue;
    private int mCaloriesValue;
    private float mDesiredPaceOrSpeed;
    private int mMaintain;
    private boolean mIsMetric;
    private float mMaintainInc;
    private String sleepTime;
    private boolean mQuitting = false; // Set when user selected Quit from menu, can be used by onPause, onStop, onDestroy


    public static Context context;

  public static long stepLast = 0;

    /**
     * True, when service is running.
     */
    private boolean mIsRunning;


    public static boolean isStepCounterAvailable = false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "[ACTIVITY] onCreate");
        super.onCreate(savedInstanceState);

        mStepValue = 0;
        mPaceValue = 0;

//        context = this;
//
//        setContentView(R.layout.main_page);
//
//        mUtils = Utils.getInstance();
//
//        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


       /* List<Sensor> sensor= sensorManager.getSensorList(Sensor.TYPE_ALL);


        for(Sensor s: sensor)
        {
            Log.e("SensorName ",""+s.getName());
        }*/


//        if (sensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER).size() != 0)
//        {
//            isStepCounterAvailable = true;
//        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {

        context = getActivity();

        View view = inflater.inflate(R.layout.main_page, container, false);

        mUtils = Utils.getInstance();

        SensorManager sensorManager = (SensorManager)context. getSystemService(context.SENSOR_SERVICE);


        if (sensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER).size() != 0)
        {
            isStepCounterAvailable = true;
        }


        setUpIds(view);


        return view;
    }

    private void setUpIds(View view)
    {

        mStepValueView = (TextView)view. findViewById(R.id.step_value);
        mPaceValueView = (TextView)view. findViewById(R.id.pace_value);
        mDistanceValueView = (TextView)view. findViewById(R.id.distance_value);
        mSpeedValueView = (TextView)view. findViewById(R.id.speed_value);
        mCaloriesValueView = (TextView)view. findViewById(R.id.calories_value);
        //   mDesiredPaceView = (TextView) findViewById(R.id.desired_pace_value);
        txtv_sleepTime = (TextView)view. findViewById(R.id.txtv_sleepTime);


        //view.findViewById(R.id.imgv_refresh).setOnClickListener(this);


        ((TextView)view. findViewById(R.id.distance_units)).setText(getString(
                mIsMetric
                        ? R.string.kilometers
                        : R.string.miles
        ));
        ((TextView)view. findViewById(R.id.speed_units)).setText(getString(
                mIsMetric
                        ? R.string.kilometers_per_hour
                        : R.string.miles_per_hour
        ));


    }

    @Override
    public void onClick(View v)
    {
       /* switch (v.getId())
        {
            case R.id.imgv_refresh:

                resetValues(true);


                break;


        }*/
    }

    public static Toast toast;

    public static void show_Toast(String text)
    {
        if (toast != null)
        {
            toast.cancel();
        }
        toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);

        toast.show();
    }


    @Override
    public void onStart()
    {
        Log.i(TAG, "[ACTIVITY] onStart");
        super.onStart();
    }

    @Override
    public void onResume()
    {
        Log.i(TAG, "[ACTIVITY] onResume");
        super.onResume();

        mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        mPedometerSettings = new PedometerSettings(mSettings);

        mUtils.setSpeak(mSettings.getBoolean("speak", false));

        // Read from preferences if the service was running on the last onPause
        mIsRunning = mPedometerSettings.isServiceRunning();

        // Start the service if this is considered to be an application start (last onPause was long ago)
        if (!mIsRunning && mPedometerSettings.isNewStart())
        {
            startStepService();
            bindStepService();
        }
        else if (mIsRunning)
        {
            bindStepService();
        }

        mPedometerSettings.clearServiceRunning();

//        mStepValueView = (TextView) findViewById(R.id.step_value);
//        mPaceValueView = (TextView) findViewById(R.id.pace_value);
//        mDistanceValueView = (TextView) findViewById(R.id.distance_value);
//        mSpeedValueView = (TextView) findViewById(R.id.speed_value);
//        mCaloriesValueView = (TextView) findViewById(R.id.calories_value);
//     //   mDesiredPaceView = (TextView) findViewById(R.id.desired_pace_value);
//        txtv_sleepTime = (TextView) findViewById(R.id.txtv_sleepTime);
//
//
//        findViewById(R.id.imgv_refresh).setOnClickListener(this);

        mIsMetric = mPedometerSettings.isMetric();
       /* ((TextView) findViewById(R.id.distance_units)).setText(getString(
                mIsMetric
                        ? R.string.kilometers
                        : R.string.miles
        ));
        ((TextView) findViewById(R.id.speed_units)).setText(getString(
                mIsMetric
                        ? R.string.kilometers_per_hour
                        : R.string.miles_per_hour
        ));*/

        mMaintain = mPedometerSettings.getMaintainOption();
        /*(this.findViewById(R.id.desired_pace_control)).setVisibility(
                mMaintain != PedometerSettings.M_NONE
                        ? View.VISIBLE
                        : View.GONE
        );*/
        if (mMaintain == PedometerSettings.M_PACE)
        {
            mMaintainInc = 5f;
            mDesiredPaceOrSpeed = (float) mPedometerSettings.getDesiredPace();
        }
        else if (mMaintain == PedometerSettings.M_SPEED)
        {
            mDesiredPaceOrSpeed = mPedometerSettings.getDesiredSpeed();
            mMaintainInc = 0.1f;
        }
      /*  Button button1 = (Button) findViewById(R.id.button_desired_pace_lower);
        button1.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                mDesiredPaceOrSpeed -= mMaintainInc;
                mDesiredPaceOrSpeed = Math.round(mDesiredPaceOrSpeed * 10) / 10f;
                displayDesiredPaceOrSpeed();
                setDesiredPaceOrSpeed(mDesiredPaceOrSpeed);
            }
        });
        Button button2 = (Button) findViewById(R.id.button_desired_pace_raise);
        button2.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                mDesiredPaceOrSpeed += mMaintainInc;
                mDesiredPaceOrSpeed = Math.round(mDesiredPaceOrSpeed * 10) / 10f;
                displayDesiredPaceOrSpeed();
                setDesiredPaceOrSpeed(mDesiredPaceOrSpeed);
            }
        });
        if (mMaintain != PedometerSettings.M_NONE)
        {
            ((TextView) findViewById(R.id.desired_pace_label)).setText(
                    mMaintain == PedometerSettings.M_PACE
                            ? R.string.desired_pace
                            : R.string.desired_speed
            );
        }*/


       // displayDesiredPaceOrSpeed();
    }

  /*  private void displayDesiredPaceOrSpeed()
    {
        if (mMaintain == PedometerSettings.M_PACE)
        {
            mDesiredPaceView.setText("" + (int) mDesiredPaceOrSpeed);
        }
        else
        {
            mDesiredPaceView.setText("" + mDesiredPaceOrSpeed);
        }
    }*/




    @Override
    public void onPause()
    {
        Log.i(TAG, "[ACTIVITY] onPause");
        if (mIsRunning)
        {
            unbindStepService();
        }
        if (mQuitting)
        {
            mPedometerSettings.saveServiceRunningWithNullTimestamp(mIsRunning);
        }
        else
        {
            mPedometerSettings.saveServiceRunningWithTimestamp(mIsRunning);
        }

        super.onPause();
        savePaceSetting();
    }

    @Override
    public void onStop()
    {
        Log.i(TAG, "[ACTIVITY] onStop");
        super.onStop();
    }

    public void onDestroy()
    {
        Log.i(TAG, "[ACTIVITY] onDestroy");

        stopStepService();
        super.onDestroy();
    }



    private void setDesiredPaceOrSpeed(float desiredPaceOrSpeed)
    {
        if (mService != null)
        {
            if (mMaintain == PedometerSettings.M_PACE)
            {
                mService.setDesiredPace((int) desiredPaceOrSpeed);
            }
            else if (mMaintain == PedometerSettings.M_SPEED)
            {
                mService.setDesiredSpeed(desiredPaceOrSpeed);
            }
        }
    }

    private void savePaceSetting()
    {
        mPedometerSettings.savePaceOrSpeedSetting(mMaintain, mDesiredPaceOrSpeed);
    }

    private StepService mService;

    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            mService = ((StepService.StepBinder) service).getService();

            mService.registerCallback(mCallback);
            mService.reloadSettings();

        }

        public void onServiceDisconnected(ComponentName className)
        {
            mService = null;
        }
    };


    private void startStepService()
    {
        if (!mIsRunning)
        {
            Log.i(TAG, "[SERVICE] Start");
            mIsRunning = true;
            context.startService(new Intent(context, StepService.class));
        }
    }

    private void bindStepService()
    {
        Log.i(TAG, "[SERVICE] Bind");
        context.bindService(new Intent(context, StepService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

    private void unbindStepService()
    {
        Log.i(TAG, "[SERVICE] Unbind");
        context.unbindService(mConnection);
    }

    private void stopStepService()
    {
        Log.i(TAG, "[SERVICE] Stop");
        if (mService != null)
        {
            Log.i(TAG, "[SERVICE] stopService");
            context.stopService(new Intent(context,StepService.class));
        }
        mIsRunning = false;
    }

    public void resetValues(boolean updateDisplay)
    {
        if (mService != null && mIsRunning)
        {
            stepLast=0;
            txtv_sleepTime.setText("00:00:00");
            mService.resetValues();
        }
        else
        {
            txtv_sleepTime.setText("00:00:00");
            mStepValueView.setText("0");
            mPaceValueView.setText("0");
            mDistanceValueView.setText("0");
            mSpeedValueView.setText("0");
            mCaloriesValueView.setText("0");

            SharedPreferences state = context.getSharedPreferences("state", 0);
            SharedPreferences.Editor stateEditor = state.edit();
            if (updateDisplay)
            {
                stateEditor.putInt("steps", 0);
                stateEditor.putInt("pace", 0);
                stateEditor.putFloat("distance", 0);
                stateEditor.putFloat("speed", 0);
                stateEditor.putFloat("calories", 0);
                stateEditor.commit();
            }
        }
    }

   /* private static final int MENU_SETTINGS = 8;
    private static final int MENU_QUIT = 9;

    private static final int MENU_PAUSE = 1;
    private static final int MENU_RESUME = 2;
    private static final int MENU_RESET = 3;

    *//* Creates the menu items *//*
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.clear();
        if (mIsRunning)
        {
            menu.add(0, MENU_PAUSE, 0, R.string.pause)
                .setIcon(android.R.drawable.ic_media_pause)
                .setShortcut('1', 'p');
        }
        else
        {
            menu.add(0, MENU_RESUME, 0, R.string.resume)
                .setIcon(android.R.drawable.ic_media_play)
                .setShortcut('1', 'p');
        }
        menu.add(0, MENU_RESET, 0, R.string.reset)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
            .setShortcut('2', 'r');
        menu.add(0, MENU_SETTINGS, 0, R.string.settings)
            .setIcon(android.R.drawable.ic_menu_preferences)
            .setShortcut('8', 's')
            .setIntent(new Intent(this, Settings.class));
        menu.add(0, MENU_QUIT, 0, R.string.quit)
            .setIcon(android.R.drawable.ic_lock_power_off)
            .setShortcut('9', 'q');
        return true;
    }*/

    /* Handles item selections */
/*
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case MENU_PAUSE:
                unbindStepService();
                stopStepService();
                return true;
            case MENU_RESUME:
                startStepService();
                bindStepService();
                return true;
            case MENU_RESET:
                resetValues(true);
                return true;
            case MENU_QUIT:
                resetValues(false);
                unbindStepService();
                stopStepService();
                mQuitting = true;
                finish();
                return true;
        }
        return false;
    }
*/

    // TODO: unite all into 1 type of message
    private StepService.ICallback mCallback = new StepService.ICallback()
    {
        public void stepsChanged(int value)
        {
            mHandler.sendMessage(mHandler.obtainMessage(STEPS_MSG, value, 0));
        }

        public void paceChanged(int value)
        {
            mHandler.sendMessage(mHandler.obtainMessage(PACE_MSG, value, 0));
        }

        public void distanceChanged(float value)
        {
            mHandler.sendMessage(mHandler.obtainMessage(DISTANCE_MSG, (int) (value * 1000), 0));
        }

        public void speedChanged(float value)
        {
            mHandler.sendMessage(mHandler.obtainMessage(SPEED_MSG, (int) (value * 1000), 0));
        }

        public void caloriesChanged(float value)
        {
            mHandler.sendMessage(mHandler.obtainMessage(CALORIES_MSG, (int) (value), 0));
        }

        /*public void speepTimeChanged(String value)
        {
            mHandler.sendMessage(mHandler.obtainMessage(SPEEP_TIME,value));
        }*/

    };

    private static final int STEPS_MSG = 1;
    private static final int PACE_MSG = 2;
    private static final int DISTANCE_MSG = 3;
    private static final int SPEED_MSG = 4;
    private static final int CALORIES_MSG = 5;
    private static final int SPEEP_TIME = 6;

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case STEPS_MSG:
                    mStepValue = (int) msg.arg1;
                    mStepValueView.setText("" + mStepValue);
                    break;
                case PACE_MSG:
                    mPaceValue = msg.arg1;
                    if (mPaceValue <= 0)
                    {
                        mPaceValueView.setText("0");
                    }
                    else
                    {
                        mPaceValueView.setText("" + (int) mPaceValue);
                    }
                    break;
                case DISTANCE_MSG:
                    mDistanceValue = ((int) msg.arg1) / 1000f;
                    if (mDistanceValue <= 0)
                    {
                        mDistanceValueView.setText("0");
                    }
                    else
                    {
                        mDistanceValueView.setText(("" + (mDistanceValue + 0.000001f)).substring(0, 5));
                    }
                    break;
                case SPEED_MSG:
                    mSpeedValue = ((int) msg.arg1) / 1000f;
                    if (mSpeedValue <= 0)
                    {
                        mSpeedValueView.setText("0");
                    }
                    else
                    {
                        mSpeedValueView.setText(
                                ("" + (mSpeedValue + 0.000001f)).substring(0, 4)
                        );
                    }
                    break;
                case CALORIES_MSG:
                    mCaloriesValue = msg.arg1;
                    if (mCaloriesValue <= 0)
                    {
                        mCaloriesValueView.setText("0");
                    }
                    else
                    {
                        mCaloriesValueView.setText("" + (int) mCaloriesValue);
                    }
                    break;

               /* case SPEEP_TIME:

                    sleepTime = msg.arg1;

                    if (sleepTime.isEmpty())
                    {
                        txtv_sleepTime.setText("0");
                    }
                    else
                    {
                        txtv_sleepTime.setText(sleepTime);
                    }
                    break;*/

                default:
                    super.handleMessage(msg);
            }
        }

    };


}


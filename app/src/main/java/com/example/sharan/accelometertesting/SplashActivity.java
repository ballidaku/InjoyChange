package com.example.sharan.accelometertesting;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;

import com.example.sharan.accelometertesting.MainScreen.Activities.MainActivity;

public class SplashActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
      //  setContentView(R.layout.main_page);

       c.start();
    }


    CountDownTimer c = new CountDownTimer(2000, 1000)
    {

        public void onTick(long millisUntilFinished)
        {

        }

        public void onFinish()
        {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }
    };

}

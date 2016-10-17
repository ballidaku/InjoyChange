package com.example.sharan.accelometertesting.MainScreen.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.example.sharan.accelometertesting.Adapters.DrawerList_Adapter;
import com.example.sharan.accelometertesting.MainScreen.Fragments.FragmentDrawer;
import com.example.sharan.accelometertesting.R;

import java.util.ArrayList;

/*import Fragments.AboutUs;
import Fragments.Friends;
import Fragments.Home;
import Fragments.InviteFriends;
import Fragments.Messages;
import Fragments.Profile;
import Fragments.WishList;*/

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

    Toolbar mToolbar;
    DrawerLayout mDrawerLayout;
    FragmentDrawer drawerFragment;
    ListView listv_drawer;
    ArrayList<String> listDataHeader;

    DrawerList_Adapter drawer_adapter;
    Context con;

    FrameLayout frame_layout_profile;
    Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        con = this;
        //constant.ChangeStatusColor(this,R.color.Black);

        setUpIds();
        prepareListData();

    }

    private void setUpIds()
    {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mToolbar.findViewById(R.id.imgv_refresh).setOnClickListener(this);

        listv_drawer = (ListView) findViewById(R.id.listv_drawer);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        frame_layout_profile = (FrameLayout) findViewById(R.id.frame_layout_profile);
        frame_layout_profile.setOnClickListener(this);

        drawerFragment = (FragmentDrawer) getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
    }

    private void prepareListData()
    {
        listDataHeader = new ArrayList<>();

        listDataHeader.add("Home");
        listDataHeader.add("Settings");
        listDataHeader.add("About us");
        listDataHeader.add("Invite Friends");
        listDataHeader.add("Share App");


        drawer_adapter = new DrawerList_Adapter(con, listDataHeader, 0);
        listv_drawer.setAdapter(drawer_adapter);

        listv_drawer.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                displayView(position);

                drawer_adapter.changeSelectedBackground(position);
                drawer_adapter.notifyDataSetChanged();
            }
        });

        displayView(0);

    }

    public void displayView(int groupPosition)
    {


        switch (groupPosition)
        {

            case 0:
                fragment = new Pedometer();
                break;

            /*case 1:
                fragment = new WishList();
                break;

            case 2:
                fragment = new Friends();
                break;

            case 3:
                fragment = new Messages();
                break;

            case 4:
                fragment = new Settings();
                break;

            case 5:
                fragment = new AboutUs();
                break;

            case 6:
                fragment = new InviteFriends();

                break;

            case 7:
                mDrawerLayout.closeDrawers();
                callShare();

                //                fragment = new ShareApp();
                break;*/

            default:
                break;
        }

//        if (groupPosition != 7)
//        {
        changeFragment(fragment);
//        }

    }

    private void callShare()
    {
        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        // Add data to the intent, the receiving app will decide
        // what to do with it.
        share.putExtra(Intent.EXTRA_SUBJECT, "Title Of The Post");
        share.putExtra(Intent.EXTRA_TEXT, "http://www.codeofaninja.com");

        startActivity(Intent.createChooser(share, "Share link!"));
    }

    private void changeFragment(Fragment fragment)
    {
        if (fragment != null)
        {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.container_body, fragment);
            fragmentTransaction.commit();
            mDrawerLayout.closeDrawers();
        }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            /*case R.id.frame_layout_profile:

                txt_titleTV.setText("Profile");
                fragment = new Profile();
                changeFragment(fragment);

                break;*/

            case R.id.imgv_refresh:

               if( fragment instanceof Pedometer)
               {
                   ((Pedometer)fragment).resetValues(true);
               }
                break;

            default:
                break;
        }
    }

}
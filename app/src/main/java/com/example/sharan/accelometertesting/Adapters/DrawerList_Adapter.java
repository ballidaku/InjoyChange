package com.example.sharan.accelometertesting.Adapters;

/**
 * Created by brst-pc93 on 9/26/16.
 */

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.sharan.accelometertesting.R;

import java.util.ArrayList;

/**
 Created by sharan on 24/11/15. */
public class DrawerList_Adapter extends BaseAdapter
{

    Context           con;
    ArrayList<String> list;
    int selectedposition;

    public DrawerList_Adapter(Context con, ArrayList<String> list,int selectedposition)
    {
        this.con = con;
        this.list = list;
        this.selectedposition=selectedposition;
    }

    @Override
    public int getCount()
    {
        // TODO Auto-generated method stub
        return list.size();
    }

    @Override
    public Object getItem(int arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int arg0)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(final int position, View row, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        row = inflater.inflate(R.layout.custom_drawerlist_item, parent, false);

        TextView txtv_drawer_item = (TextView) row.findViewById(R.id.txtv_drawer_item);

        txtv_drawer_item.setText(list.get(position));


        if(selectedposition == position)
        {
            txtv_drawer_item.setTextColor(ContextCompat.getColor(con, R.color.White));
            row.setBackgroundColor(ContextCompat.getColor(con,R.color.YellowDark));
        }

        return row;
    }

    public void changeSelectedBackground(int psition)
    {
        selectedposition=psition;
    }

}
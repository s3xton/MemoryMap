package com.example.conor.project;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Zachary Diebold on 07/06/2015.
 */
public class PostInfo {
    public PostInfo() {};
    public int id;
    public String type;
    public String data;
    public String image;
    public int score;
    public float lat;
    public float lng;
    public String time;
    public Bitmap bitmap;
    public Circle circle;
    public Marker circleMarker;
    public boolean opened;
}
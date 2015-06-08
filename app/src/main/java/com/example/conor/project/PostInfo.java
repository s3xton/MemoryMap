package com.example.conor.project;

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
    public int score;
    public float lat;
    public float lng;
    public String time;
    public Circle circle;
    public Marker circleMarker;
    public boolean opened;
}
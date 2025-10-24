package com.aura.starter.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class GeoUtils {

    public static void openGoogleMaps(Context ctx, double lat, double lon) {
        String uri = "google.navigation:q=" + lat + "," + lon + "&mode=w";
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        i.setPackage("com.google.android.apps.maps");
        ctx.startActivity(i);
    }

    public static void openGenericMaps(Context ctx, double lat, double lon) {
        String uri = "geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(目的地)";
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        ctx.startActivity(i);
    }

    /** Haversine distance in meters */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}

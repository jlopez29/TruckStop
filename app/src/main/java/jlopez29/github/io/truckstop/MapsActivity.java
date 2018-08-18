package jlopez29.github.io.truckstop;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.view.View.GONE;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener {

    private GoogleMap mMap;
    String TAG = "Truck Stop";
    Context context;
    Marker currMarker;
    SharedPreferences prefs;
    boolean toggled = false;
    boolean toggleTracking = false;
    boolean startup = true;
    boolean viewReset = false;
    Marker currLocMarker;

    private ClusterManager<TruckStop> mClusterManager;

    boolean viewingDetails = false;

    LinearLayout search;

    LocationManager lm;

    RequestQueue reqQueue;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        context = this;

        startup = true;

        search = findViewById(R.id.searchBox);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

        if(prefs.getBoolean("track",false))
        {
            Log.e(TAG,"Track on startup");
            toggleTracking = true;
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED  && lm != null) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 500, ll);
            }
        }

        // Instantiate the RequestQueue.
        reqQueue = Volley.newRequestQueue(this);
        url ="http://webapp.transflodev.com/svc1.transflomobile.com/api/v3/stations/100";
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.e(TAG,"Map ready " + startup);
        mMap = googleMap;


        mClusterManager = new ClusterManager<>(this,mMap);

        LatLng sydney = new LatLng(-34, 151);

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            sydney = new LatLng(location.getLatitude(),location.getLongitude());
        }
//        // Add a marker in Sydney and move the camera
//        mMap.addMarker(new MarkerOptions().position(sydney).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)).title("Marker in Winter Park").snippet("Truck Stop"));

        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                Log.e(TAG,"Camera idle");
                if(!viewingDetails)
                {
                    Log.e(TAG,"Not viewing details");
                    if(toggled) {
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(!viewingDetails) {
                                    Log.e("RESET", "VIEW");
                                    toggleTracking(true);
                                    toggled = false;
                                    viewReset = true;
                                }
                                else
                                    Log.e(TAG,"Viewing details");
                            }
                        },5000);
                    }
                    else
                        Log.e(TAG,"Not toggled");

                }
                else
                    Log.e(TAG,"Viewing details");
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                currMarker = marker;
                viewingDetails = true;

                if(toggleTracking)
                    toggleTracking(false);


                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(toggleTracking) {
                            toggleTracking(true);
                            currMarker.hideInfoWindow();
                        }


                        viewingDetails = false;
                    }
                },15000);

                return false;
            }
        });
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                Log.e(TAG,"Cam move started " + toggleTracking);


                if(toggleTracking) {
                        toggled = true;
                        toggleTracking(false);

                }
            }
        });
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        else
        {
            ActivityCompat.requestPermissions((Activity)context,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100);
        }
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker arg0) {

                // Getting view from the layout file infowindowlayout.xml
                View v = getLayoutInflater().inflate(R.layout.infowindowlayout, null);

                LatLng latLng = arg0.getPosition();

                ImageView im = (ImageView) v.findViewById(R.id.imageView1);
                TextView tv1 = (TextView) v.findViewById(R.id.stopName);
                TextView tv2 = (TextView) v.findViewById(R.id.stopDistance);
                TextView tv3 = (TextView) v.findViewById(R.id.stopAddress);
                String title=arg0.getTitle();
                String informations=arg0.getSnippet();

                if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && lm != null) {
                    Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    double longitude = location.getLongitude();
                    double latitude = location.getLatitude();
                    LatLng myLoc = new LatLng(latitude,longitude);
                    LatLng markLoc = currMarker.getPosition();
                    double meters = SphericalUtil.computeDistanceBetween(myLoc,markLoc);

                    double inches = (39.370078 * meters);
                    int miles = (int) (inches / 63360);
                    tv2.setText(String.valueOf(miles) + " miles away");
                }

                tv1.setText(title);
                tv3.setText(informations);

                im.setImageResource(R.drawable.ic_drawable);


                return v;

            }
        });


        if(startup)
        {
            Log.e(TAG,"Startup location");

            if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG,"Move on startup");

                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 12.0f));
                startup = false;

                currLocMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_person)));

            }

            makeRequest(String.valueOf(sydney.latitude),String.valueOf(sydney.longitude));
        }

//        TruckStop truckStop = new TruckStop(sydney.latitude,sydney.longitude,"Winter Park","Truck Stop");
//        mClusterManager.addItem(truckStop);
//        mClusterManager.cluster();

    }

    LocationListener ll = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG,"Location Changed");
            currLocMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 12.0f));
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
//            logMsg("Status Changed");
        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    Toast.makeText(context, "Location is enabled", Toast.LENGTH_SHORT).show();
                    if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED )
                        mMap.setMyLocationEnabled(true);


                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(context, "Location is disabled", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void mapType(View v)
    {
        if(mMap.getMapType() == GoogleMap.MAP_TYPE_SATELLITE)
        {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        else
        {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
    }

    public void mapTrack(View v)
    {
        if(prefs.getBoolean("track",false))
        {
            prefs.edit().putBoolean("track",false).apply();
            toggleTracking = false;
            toggleTracking(false);

        }
        else
        {
            toggleTracking = true;
            toggleTracking(true);
            prefs.edit().putBoolean("track",true).apply();
        }
    }

    private void toggleTracking(boolean track)
    {
        if(track)
        {
            Log.e(TAG,"Track map");
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED  && lm != null) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 500, ll);

                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 12.0f));

            }
        }
        else {
            Log.e(TAG,"Untrack map");
            lm.removeUpdates(ll);

        }
    }

    public void mapSearch(View v)
    {
        search.setVisibility(View.VISIBLE);
    }

    public void mapZoom(View v)
    {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 8.0f));
            if(toggleTracking)
                toggled=true;
        }
    }
    @Override
    public void onBackPressed()
    {
        if(search.getVisibility() == View.VISIBLE)
            search.setVisibility(GONE);
        else
            super.onBackPressed();
    }

    @Override
    public void onDestroy()
    {
        if(toggleTracking)
            prefs.edit().putBoolean("track",true).apply();

        search.setVisibility(GONE);
        
        super.onDestroy();
    }

    public void makeRequest(final String lat, final String lng)
    {
        StringRequest sr = new StringRequest(Request.Method.POST,url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
//                Log.e(TAG,response);
                try {
                    JSONObject jsnobject = new JSONObject(response);
                    JSONArray jsonArray = jsnobject.getJSONArray("truckStops");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String name = obj.get("name").toString();
                        String city = obj.get("city").toString();
                        String state = obj.get("state").toString();
                        String country = obj.get("country").toString();
                        String zip = obj.get("zip").toString();
                        String lat = obj.get("lat").toString();
                        String lng = obj.get("lng").toString();
                        String raw1 = obj.get("rawLine1").toString();
                        String raw2 = obj.get("rawLine2").toString();
                        String raw3 = obj.get("rawLine3").toString();

                        String fullInfo = city + "," + state + " " + zip + " " + country + "\n" + raw1 + "\n" + raw2 + "\n" +raw3;

//                        TruckStop truckStop = new TruckStop(Double.valueOf(lat),Double.valueOf(lng),name,fullInfo);
//                        mClusterManager.addItem(truckStop);

                        mMap.addMarker(new MarkerOptions().position(new LatLng(Double.valueOf(lat),Double.valueOf(lng))).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)).title(name).snippet(fullInfo));
                    }

//                    mClusterManager.cluster();
//                    mClusterManager.
                }catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                params.put("lat",lat);
                params.put("lng",lng);

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                String auth = "Basic amNhdGFsYW5AdHJhbnNmbG8uY29tOnJMVGR6WmdVTVBYbytNaUp6RlIxTStjNmI1VUI4MnFYcEVKQzlhVnFWOEF5bUhaQzdIcjVZc3lUMitPTS9paU8= ";
                params.put("Authorization", auth);
                return params;
            }
        };

// Add the request to the RequestQueue.
        reqQueue.add(sr);
    }

    public void acceptSearch(View v)
    {
        TextView name = findViewById(R.id.pName);
        TextView city = findViewById(R.id.pCity);
        TextView zip = findViewById(R.id.pZip);
        Spinner state = findViewById(R.id.states_spinner);

        String searchName = name.getText().toString();
        String searchState = state.toString();
        String searchCity = city.getText().toString();
        String searchZip = zip.getText().toString();

        findLocation(searchName,searchState,searchCity,searchZip);

    }
    public void findLocation(String name,String state,String city,String zip)
    {
        String address = buildAddress(name,state,city,zip);

        Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
        try
        {
            List<Address> addresses = geoCoder.getFromLocationName(address, 5);
            if (addresses.size() > 0)
            {
                Double lat = (double) (addresses.get(0).getLatitude());
                Double lon = (double) (addresses.get(0).getLongitude());

                Log.e(TAG,"Location is lat: " + lat + " long: " + lon);
                final LatLng searchLoc = new LatLng(lat, lon);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(searchLoc, 8.0f));
                mMap.clear();

                makeRequest(String.valueOf(lat),String.valueOf(lon));


                search.setVisibility(GONE);

                if(toggleTracking) {
                    toggleTracking(false);
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(!viewingDetails) {
                                Log.e("RESET", "VIEW from search");
                                toggleTracking(true);
                                toggled = false;
                                viewReset = true;
                                mMap.clear();
                                if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 12.0f));
                                    makeRequest(String.valueOf(location.getLatitude()),String.valueOf(location.getLongitude()));
                                }

                            }
                            else
                                Log.e(TAG,"Viewing details");
                        }
                    },30000);
                }



            }
            else
                Log.e(TAG,"Address not found");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String buildAddress(String name,String state,String city,String zip)
    {
        String address = "";
        address = name + " " + city + "," + state + " " + zip;
        return address;
    }
}

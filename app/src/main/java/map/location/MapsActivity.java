package map.location;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerDragListener, GoogleMap.OnMapClickListener
{

    Switch sw;

    public static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;
    MarkerOptions markerOptionsLong, markerOptionsShort;
    Marker markerLong, markerShort;
    private double destinationLatitude, destinationLongitude;
    private CameraPosition mCameraPosition;

    // The entry points to the Places API
    private PlaceDetectionClient mPlaceDetect;
    private GeoDataClient mGeoDataClient;

    //The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mfFusedLoc;
    private Location mLastKnownLocation, markerLongLocation, markerShortLocation;

    double distance, p2pDistance;

    boolean gpsMode;

    //A default location (Oulu) and default zoom when location permission in
    //not granted
    private final LatLng mDefaultLocation = new LatLng(65, 25.5);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted = false;


    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null){
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }


        setContentView(R.layout.activity_maps);
        sw = (Switch) findViewById(R.id.switch1);


        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    gpsMode = true;
                    if(markerShort != null)
                        markerShort.setVisible(false);
                }
                else{
                    gpsMode = false;

                    if(markerShort != null)
                        markerShort.setVisible(true);
                }
            }
        });

        mPlaceDetect = Places.getPlaceDetectionClient(this, null);
        mGeoDataClient = Places.getGeoDataClient(this, null);
        mfFusedLoc = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    protected void onSaveInstanceState(Bundle outState){
        if(mMap != null){
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // set listeners for long click and marker drag
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnMapClickListener(this);

        getLocationPermission();
        updateLocationUI();

        getDeviceLocation();
    }

    // Save lat and lng for later use
    @Override
    public void onMapLongClick(LatLng point)
    {
        setDistanceLong(point);

        // If no new markerOption has been made, made one on click. After that only move the marker
        if (markerOptionsLong != null)
        {
            markerLong.setPosition(point);
            setDistanceLong(point);
        }
        else
        {
            markerOptionsLong = new MarkerOptions()
                    .position(point).draggable(true);
            markerLong = mMap.addMarker(markerOptionsLong);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {

        if(!gpsMode){

            // If no new markerOption has been made, made one on click. After that only move the marker
            if (markerOptionsShort != null)
            {
                markerShort.setPosition(latLng);
                setDistanceShort(latLng);
            }
            else
            {
                markerOptionsShort = new MarkerOptions()
                        .position(latLng).draggable(true).title("Custom marker")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                markerShort = mMap.addMarker(markerOptionsShort);
            }
        }
    }

//region Marker Drag

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker)
    {
        // On final destination show new position-
        if(marker.getId().equals(markerLong.getId()))
            setDistanceLong(marker.getPosition());
        else if( markerShort != null && marker.getId().equals(markerShort.getId()) )
            setDistanceShort(marker.getPosition());
    }
//endregion

    private void setDistanceShort(LatLng latLng){

        destinationLatitude = latLng.latitude;
        destinationLongitude = latLng.longitude;

        markerShortLocation = new Location("");
        markerShortLocation.setLatitude(destinationLatitude);
        markerShortLocation.setLongitude(destinationLongitude);

        // Distance from marker to marker
        if (markerLongLocation != null && markerShortLocation != null)
        {
            p2pDistance = markerLongLocation.distanceTo(markerShortLocation) / 1000;
            p2pDistance = (double) Math.round(p2pDistance * 100) / 100;
            Toast.makeText(MapsActivity.this, "Etäisyys linnuntietä p2p: " + p2pDistance + " km", Toast.LENGTH_LONG).show();
        }
    }

    private void setDistanceLong(LatLng point){

        destinationLatitude = point.latitude;
        destinationLongitude = point.longitude;
        Location destinationLocation = new Location("");
        destinationLocation.setLatitude(destinationLatitude);
        destinationLocation.setLongitude(destinationLongitude);
        markerLongLocation = new Location(destinationLocation);

        if(gpsMode && mLastKnownLocation != null){
            distance = mLastKnownLocation.distanceTo(destinationLocation) / 1000 ;
            distance = (double) Math.round(distance * 100) / 100;
            Toast.makeText(MapsActivity.this, "Etäisyys linnuntietä: " + distance +" km", Toast.LENGTH_LONG).show();
        } else {
            // Distance from marker to marker

            if (markerLongLocation != null && markerShortLocation != null)
            {
                p2pDistance = markerShortLocation.distanceTo(markerLongLocation) / 1000;
                p2pDistance = (double) Math.round(p2pDistance * 100) / 100;
                Toast.makeText(MapsActivity.this, "Etäisyys linnuntietä p2p: " + p2pDistance + " km", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getLocationPermission(){
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else
        {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void updateLocationUI(){
        if(mMap == null){
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                          @NonNull String permissions[],
                                          @NonNull int[] grantResults){
        mLocationPermissionGranted = false;
        switch (requestCode){
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void getDeviceLocation(){
        try{
            if(mLocationPermissionGranted){
                Task locationResult = mfFusedLoc.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if ( task.isSuccessful()){
                            mLastKnownLocation = (Location) task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        }
                        else {
                            Log.d(TAG, "Current location is null. Using defaults");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e){
            Log.e("Exception: %s", e.getMessage());
        }
    }




}

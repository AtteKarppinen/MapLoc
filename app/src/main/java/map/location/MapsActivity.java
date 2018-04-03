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

    //A default location (Oulu) and default zoom when location permission in
    //not granted
    private final LatLng mDefaultLocation = new LatLng(65, 25.5);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted = false;

    //used for selecting the current place
//    private String[] mLikelyPlaceNames;
//    private String[] mLikelyPlaceAddress;
//    private String[] mLikelyPlaceAttributions;
//    private LatLng[] mLikelyPlaceLatLngs;
//    private static final int M_MAX_ENTRIES = 5;

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
        destinationLatitude = point.latitude;
        destinationLongitude = point.longitude;

        Toast.makeText(MapsActivity.this, "Point latitude: " + point.latitude +
        " Point longitude: " + point.longitude, Toast.LENGTH_LONG).show();

        // If no new markerOption has been made, made one on click. After that only move the marker
        if (markerOptionsLong != null)
        {
            markerLong.setPosition(point);
            Location destinationLocation = new Location("");
            destinationLocation.setLatitude(destinationLatitude);
            destinationLocation.setLongitude(destinationLongitude);
            distance = mLastKnownLocation.distanceTo(destinationLocation) / 1000 ;
            distance = (double) Math.round(distance * 100) / 100;
            Log.wtf("TAG", "Distance: " + distance + "km");

            // Distance from marker to marker
            markerLongLocation = new Location(destinationLocation);
            if (markerLongLocation != null && markerShortLocation != null)
            {
                p2pDistance = markerShortLocation.distanceTo(markerLongLocation) / 1000;
                p2pDistance = (double) Math.round(p2pDistance * 100) / 100;
                Log.wtf("TAG", "Distance p2p: " + p2pDistance + "km");
            }

        }
        else
        {
            markerOptionsLong = new MarkerOptions()
                    .position(point).draggable(true).title("Custom marker");
            markerLong = mMap.addMarker(markerOptionsLong);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {

        destinationLatitude = latLng.latitude;
        destinationLongitude = latLng.longitude;

        Toast.makeText(MapsActivity.this, "Point latitude: " + latLng.latitude +
                " Point longitude: " + latLng.longitude, Toast.LENGTH_LONG).show();

        // If no new markerOption has been made, made one on click. After that only move the marker
        if (markerOptionsShort != null)
        {
            markerShort.setPosition(latLng);
            Location destinationLocation = new Location("");
            destinationLocation.setLatitude(destinationLatitude);
            destinationLocation.setLongitude(destinationLongitude);
            double distance = mLastKnownLocation.distanceTo(destinationLocation) / 1000 ;
            distance = (double) Math.round(distance * 100) / 100;
            Log.wtf("TAG", "Distance: " + distance + "km");

            // Distance from marker to marker
            markerShortLocation = new Location(destinationLocation);
            if (markerLongLocation != null && markerShortLocation != null)
            {
                p2pDistance = markerLongLocation.distanceTo(markerShortLocation) / 1000;
                p2pDistance = (double) Math.round(p2pDistance * 100) / 100;
                Log.wtf("TAG", "Distance p2p: " + p2pDistance + "km");
            }

        }
        else
        {
            markerOptionsShort = new MarkerOptions()
                    .position(latLng).draggable(true).title("Custom marker")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            markerShort = mMap.addMarker(markerOptionsShort);
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
        // On final destination show new position
        Toast.makeText(MapsActivity.this, "Point latitude: " + marker.getPosition().latitude +
                " Point longitude: " + marker.getPosition().longitude, Toast.LENGTH_LONG).show();
    }
//endregion


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

//    private void showCurrentPlace(){
//        if(mMap == null){
//            return;
//        }
//
//        if(mLocationPermissionGranted) {
//            @SuppressWarnings("MissingPermission")
//                    final Task<PlaceLikelihoodBufferResponse> placeResult =
//                    mPlaceDetect.getCurrentPlace(null);
//            placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
//                @Override
//                public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
//                    if(task.isSuccessful() && task.getResult() != null) {
//                        PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();
//
//                        int count;
//                        if(likelyPlaces.getCount() < M_MAX_ENTRIES) {
//                            count = likelyPlaces.getCount();
//                        }else
//                        {
//                            count = M_MAX_ENTRIES;
//                        }
//
//                        int i = 0;
//                        mLikelyPlaceNames = new String[count];
//                        mLikelyPlaceAddress = new String[count];
//                        mLikelyPlaceAttributions = new String[count];
//                        mLikelyPlaceLatLngs = new LatLng[count];
//
//                        for(PlaceLikelihood placeLikelihood : likelyPlaces){
//                            //build a list of likely places to show the user
//                            mLikelyPlaceNames[i] = (String) placeLikelihood.getPlace().getName();
//                            mLikelyPlaceAddress[i] = (String) placeLikelihood.getPlace()
//                                    .getAddress();
//                            mLikelyPlaceAttributions[i] = (String) placeLikelihood.getPlace()
//                                    .getAttributions();
//                            mLikelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();
//
//                            i++;
//                            if(i > (count - 1))
//                                break;
//                        }
//
//                        //release the place likehood buffer to avoid memory leaks
//                        likelyPlaces.release();
//                        openPlacesDialog();
//
//                } else {
//                        Log.e(TAG, "Excepition: %s", task.getException());
//                    }
//
//
//
//            }
//            });
//        } else {
//            Log.i(TAG, "The user did not granted permission");
//
//            mMap.addMarker(new MarkerOptions()
//            .title(getString(R.string.default_info_title))
//            .position(mDefaultLocation)
//            .snippet(getString(R.string.default_info_snippet)));
//
//            getLocationPermission();
//        }
//    }
//
//    private void openPlacesDialog(){
//        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                LatLng markerLatLng = mLikelyPlaceLatLngs[i];
//                String markerSnippet = mLikelyPlaceAddress[i];
//
//                if(mLikelyPlaceAttributions[i] != null){
//                    markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions[i];
//                }
//
//                mMap.addMarker(new MarkerOptions()
//                .title(mLikelyPlaceNames[i])
//                .position(markerLatLng)
//                .snippet(markerSnippet));
//
//                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng, DEFAULT_ZOOM));
//            }
//        };
//
//        AlertDialog dialog = new AlertDialog.Builder(this)
//                .setTitle(R.string.pick_place)
//                .setItems(mLikelyPlaceNames, listener)
//                .show();
//    }


}

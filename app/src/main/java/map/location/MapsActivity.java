package map.location;

import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerDragListener
{

    private GoogleMap mMap;
    MarkerOptions markerOptions;
    Marker marker;
    private double destinationLatitude, destinationLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
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
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        // set listeners for long click and marker drag
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerDragListener(this);
    }

    // Save lat and lng for later use
    @Override
    public void onMapLongClick(LatLng point)
    {
        destinationLatitude = point.latitude;
        destinationLongitude = point.longitude;

        Toast.makeText(MapsActivity.this, "Point latitude: " + point.latitude +
        " Point longitude: " + point.longitude, Toast.LENGTH_LONG).show();

        if (markerOptions != null)
        {
            marker.setPosition(point);
        }
        else
        {
            markerOptions = new MarkerOptions()
                    .position(point).draggable(true).title("Custom marker");
            marker = mMap.addMarker(markerOptions);
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
        Toast.makeText(MapsActivity.this, "Point latitude: " + marker.getPosition().latitude +
                " Point longitude: " + marker.getPosition().longitude, Toast.LENGTH_LONG).show();
    }
//endregion
}

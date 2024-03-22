package com.stringee.chat.ui.kit.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.stringee.chat.ui.kit.commons.utils.PermissionsUtils;
import com.stringee.exception.StringeeError;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Utils;


public class StringeeLocationActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {
    protected SupportMapFragment mapFragment;
    protected Location mCurrentLocation;
    protected LocationManager locationManager;
    protected static final long UPDATE_INTERVAL = 5;
    protected static final long FASTEST_INTERVAL = 1;
    protected Marker myLocationMarker;
    public static final int LOCATION_SERVICE_ENABLE = 1001;
    protected static final String TAG = "LocationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stringee_activity_location);

        Toolbar toolbar = findViewById(R.id.toolbar_map_screen);
        toolbar.setTitle(getResources().getString(R.string.send_location));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        processLocation();
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        try {
            if (mCurrentLocation != null) {
                LatLng position = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                googleMap.clear();
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.draggable(true);
                if (myLocationMarker == null) {
                    myLocationMarker = googleMap.addMarker(markerOptions.position(position).title(""));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 20));
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(17), 2000, null);
                } else {
                    googleMap.addMarker(markerOptions.position(myLocationMarker.getPosition()).title(""));
                }
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setZoomGesturesEnabled(true);
                googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                    @Override
                    public void onMarkerDragStart(@NonNull Marker marker) {

                    }

                    @Override
                    public void onMarkerDrag(@NonNull Marker marker) {

                    }

                    @Override
                    public void onMarkerDragEnd(@NonNull Marker marker) {
                        if (myLocationMarker != null) {
                            myLocationMarker.remove();
                        }
                        MarkerOptions newMarkerOptions = new MarkerOptions();
                        newMarkerOptions.draggable(true);
                        myLocationMarker = googleMap.addMarker(newMarkerOptions.position(marker.getPosition()).title(""));
                    }
                });
            }

            findViewById(R.id.v_send_location).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        } catch (Exception e) {
            Log.d(TAG, "Check if location permission are added");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_SERVICE_ENABLE) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                processingLocation();
            } else {
                Toast.makeText(StringeeLocationActivity.this, R.string.unable_to_fetch_location, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void processingLocation() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.location_services_disabled_title).setMessage(R.string.location_services_disabled_message).setCancelable(false).setPositiveButton(R.string.location_service_settings, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, LOCATION_SERVICE_ENABLE);
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    Toast.makeText(StringeeLocationActivity.this, R.string.location_sending_cancelled, Toast.LENGTH_LONG).show();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            try {
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                Toast.makeText(this, R.string.waiting_for_current_location, Toast.LENGTH_SHORT).show();
                LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY).setIntervalMillis(UPDATE_INTERVAL).setMinUpdateIntervalMillis(FASTEST_INTERVAL).setWaitForAccurateLocation(false).setMaxUpdateDelayMillis(UPDATE_INTERVAL).build();
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                        super.onLocationAvailability(locationAvailability);
                    }

                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        mCurrentLocation = locationResult.getLastLocation();
                        if (mCurrentLocation != null) {
                            mapFragment.getMapAsync(StringeeLocationActivity.this);
                        }
                    }
                }, Looper.getMainLooper());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void processLocation() {
        if (Utils.hasMarshmallow() && !PermissionsUtils.getInstance().checkSelfForLocationPermission(this)) {
            PermissionsUtils.getInstance().requestPermissions(this, PermissionsUtils.PERMISSIONS_LOCATION, PermissionsUtils.REQUEST_LOCATION);
        } else {
            processingLocation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionsUtils.REQUEST_LOCATION) {
            if (PermissionsUtils.verifyPermissions(grantResults)) {
                processingLocation();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}

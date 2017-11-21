package com.lstama.ppl.a86;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.location.*;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import android.Manifest;

public class ButtonActivity extends AppCompatActivity implements LocationListener {
    final String TAG = "GPS";
    private final static int ALL_PERMISSIONS_RESULT = 101;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;

    TextView tvLatitude, tvLongitude, tvSendStatus;
    LocationManager locationManager;
    Location loc;
    Button mSendLocationButton;
    ArrayList<String> permissions = new ArrayList<>();
    ArrayList<String> permissionsToRequest;
    ArrayList<String> permissionsRejected = new ArrayList<>();
    boolean isGPS = false;
    boolean isNetwork = false;
    boolean canGetLocation = true;

    SendLocationTask mSendLocationTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_button);

        tvLatitude = (TextView) findViewById(R.id.tvLatitude);
        tvLongitude = (TextView) findViewById(R.id.tvLongitude);
        tvSendStatus = (TextView) findViewById(R.id.tvSendStatus);

        locationManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
        isGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionsToRequest = findUnAskedPermissions(permissions);



        if (!isGPS && !isNetwork) {
            Log.d(TAG, "Connection off");
            showSettingsAlert();
            getLastLocation();
        } else {
            Log.d(TAG, "Connection on");
            // check permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (permissionsToRequest.size() > 0) {
                    requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                            ALL_PERMISSIONS_RESULT);
                    Log.d(TAG, "Permission requests");
                    canGetLocation = false;
                }
            }

            // get location
            getLocation();
        }

        mSendLocationButton = (Button) findViewById(R.id.sendLocationButton);
        mSendLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSendLocationButton.setEnabled(false);
                sendCurrentLocation();
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged");
        updateLocation(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {}

    @Override
    public void onProviderEnabled(String s) {
        getLocation();
    }

    @Override
    public void onProviderDisabled(String s) {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    private void getLocation() {
        try {
            if (canGetLocation) {
                Log.d(TAG, "Can get location");
                if (isGPS) {
                    // from GPS
                    Log.d(TAG, "GPS on");
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    if (locationManager != null) {
                        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null)
                            updateLocation(location);
                    }
                } else if (isNetwork) {
                    // from Network Provider
                    Log.d(TAG, "NETWORK_PROVIDER on");
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    if (locationManager != null) {
                        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null)
                            updateLocation(location);
                    }
                } else {
                    Location location = loc;
                    location.setLatitude(0);
                    location.setLongitude(0);
                    updateLocation(location);
                }
            } else {
                Log.d(TAG, "Can't get location");
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void getLastLocation() {
        try {
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, false);
            Location location = locationManager.getLastKnownLocation(provider);
            Log.d(TAG, provider);
            Log.d(TAG, location == null ? "NO LastLocation" : location.toString());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canAskPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canAskPermission() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case ALL_PERMISSIONS_RESULT:
                Log.d(TAG, "onRequestPermissionsResult");
                for (String perms : permissionsToRequest) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.toArray(
                                                        new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }
                } else {
                    Log.d(TAG, "No rejected permissions.");
                    canGetLocation = true;
                    getLocation();
                }
                break;
        }
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("GPS is not Enabled!");
        alertDialog.setMessage("Do you want to turn on GPS?");
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(ButtonActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void sendCurrentLocation(){
        if (mSendLocationTask != null) {
            return;
        }
        Intent intent = getIntent();
        String email = intent.getStringExtra("email");
        String hashedPassword = intent.getStringExtra("hashed_password");
        mSendLocationTask = new SendLocationTask(email, hashedPassword, loc);
        mSendLocationTask.execute((Void) null);
    }

    public class SendLocationTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mHashedPassword;
        private final Location mLocation;

        SendLocationTask(String email, String hashedPassword, Location location) {
            mEmail = email;
            mHashedPassword = hashedPassword;
            mLocation = location;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            String response;
            try {
                Map<String, String> data = new HashMap<>();
                data.put("email", mEmail);
                data.put("hashed_password", mHashedPassword);
                data.put("longitude", String.valueOf(mLocation.getLongitude()));
                data.put("latitude", String.valueOf(mLocation.getLatitude()));

                response = HttpRequest.post("http://7946c878.ngrok.io/sos_handler").form(data).body();
            } catch (Exception e) {
                return false;
            }

            return response.equals("1");
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSendLocationTask = null;
            if (success) {
                tvSendStatus.setText("Send Location Success!");
            } else {
                tvSendStatus.setText("Failed");
            }
            mSendLocationButton.setEnabled(true);
        }

        @Override
        protected void onCancelled() {
            mSendLocationTask = null;
            mSendLocationButton.setEnabled(true);
        }
    }

    private void updateLocation(Location location) {
        this.loc = location;
        updateLocationDetailsUI(location);
    }

    private void updateLocationDetailsUI(Location loc) {
        Log.d(TAG, "updateLocationDetailsUI");
        tvLatitude.setText(String.valueOf(loc.getLatitude()));
        tvLongitude.setText(String.valueOf(loc.getLongitude()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
}
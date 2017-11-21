package com.lstama.ppl.a86;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LocationListener{

    private TextView mTextMessage;
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
    public void onBackPressed() {
    }


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    switchToHomeFragment(getBundle("Home"));
                    return true;
                case R.id.navigation_dashboard:
                    switchToProfileFragment(getBundle("Profile"));
                    return true;
                case R.id.navigation_notifications:
                    switchToSettingFragment(getBundle("Setting"));
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        //getSupportActionBar().setHomeButtonEnabled(false);
        setContentView(R.layout.activity_main);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        switchToHomeFragment(getBundle("Home"));

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

//        mSendLocationButton = (Button) findViewById(R.id.button_send_location);
//        mSendLocationButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mSendLocationButton.setEnabled(false);
//                sendCurrentLocation();
//            }
//        });
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
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public void sendCurrentLocation(String email, String hashedPassword){
        if (mSendLocationTask != null) {
            return;
        }
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

                response = HttpRequest.post("http://d58625b4.ngrok.io/sos_handler").form(data).body();
            } catch (Exception e) {
                return false;
            }

            return response.equals("1");
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSendLocationTask = null;
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_SHORT;


            if (success) {
                CharSequence text = "SOS Success!";
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            } else {
                CharSequence text = "SOS failed!";
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
//            mSendLocationButton.setEnabled(true);
        }

        @Override
        protected void onCancelled() {
            mSendLocationTask = null;
//            mSendLocationButton.setEnabled(true);
        }
    }

    private void updateLocation(Location location) {
        this.loc = location;
        updateLocationDetailsUI(location);
    }

    private void updateLocationDetailsUI(Location loc) {
        Log.d(TAG, "updateLocationDetailsUI");
//        tvLatitude.setText(String.valueOf(loc.getLatitude()));
//        tvLongitude.setText(String.valueOf(loc.getLongitude()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    public void switchToHomeFragment(Bundle bundle) {
        Fragment fragment = new HomeFragment();
        switchToFragment(fragment, bundle);
    }

    public void switchToProfileFragment(Bundle bundle) {
        Fragment fragment = new ProfileFragment();
        switchToFragment(fragment, bundle);
    }

    public void switchToSettingFragment(Bundle bundle) {
        Fragment fragment = new SettingFragment();
        switchToFragment(fragment, bundle);
    }

    public void switchToFragment(Fragment fragment, Bundle bundle) {
        FragmentManager manager = getSupportFragmentManager();
        fragment.setArguments(bundle);
        manager.beginTransaction().replace(R.id.content, fragment).commit();
    }

    public Bundle getBundle(String loc) {
        Bundle bundle =  new Bundle();
        Intent intent = getIntent();
        String email = intent.getStringExtra("email");
        String hashedPassword = intent.getStringExtra("hashed_password");
        String fullname = intent.getStringExtra("fullname");
        bundle.putString("email", email);
        bundle.putString("hashed_password", hashedPassword);
        bundle.putString("fullname", fullname);
        return bundle;
    }

}

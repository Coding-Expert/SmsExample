package com.example.smsexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.MessageFormat;
import java.util.Observable;
import java.util.Observer;


public class MainActivity extends AppCompatActivity implements
        Observer {

    private static final String DEVICE_DEFAULT_SMS_PACKAGE_KEY = "com.example.smsexample.deviceDefaultSmsPackage";
    private static final String INVALID_PACKAGE = "invalid_package";

    private TextView msg_textview;
    SharedPreferences permissionStatus;
    private boolean sentToSettings = false;
    private static final int SMS_PERMISSION_CONSTANT = 100;
    private static final int REQUEST_PERMISSION_SETTING = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpViews();
        saveDeviceDefaultSmsPackage();

        msg_textview = (TextView) findViewById(R.id.message_textview);
        permissionStatus = getSharedPreferences("permissionStatus", MODE_PRIVATE);

        ObservableObject.getInstance().addObserver(this);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.SEND_SMS)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Need SMS Permission");
                builder.setMessage("This app needs SMS permission to send Messages.");
                builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CONSTANT);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            } else if (permissionStatus.getBoolean(Manifest.permission.SEND_SMS, false)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Need SMS Permission");
                builder.setMessage("This app needs SMS permission to send Messages.");
                builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        sentToSettings = true;
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                        Toast.makeText(getBaseContext(),
                                "Go to Permissions to Grant SMS permissions", Toast.LENGTH_LONG).show();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS}
                        , SMS_PERMISSION_CONSTANT);
            }

            SharedPreferences.Editor editor = permissionStatus.edit();
            editor.putBoolean(Manifest.permission.SEND_SMS, true);
            editor.commit();

        }
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECEIVE_SMS}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();

                    }
                    else{
                        Toast.makeText(this, "No permission granted", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
            }
        }
    }

    private void saveDeviceDefaultSmsPackage() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        if (hasNoPreviousSmsDefaultPackage(preferences)) {
            String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this);
            preferences.edit().putString(DEVICE_DEFAULT_SMS_PACKAGE_KEY, defaultSmsPackage).apply();
        }
    }

    private boolean hasNoPreviousSmsDefaultPackage(SharedPreferences preferences) {
        return !preferences.contains(DEVICE_DEFAULT_SMS_PACKAGE_KEY);
    }

    private void setUpViews() {
        findViewById(R.id.set_as_default).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDeviceDefaultSmsPackage(getPackageName());
            }
        });

        findViewById(R.id.restore_default).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDeviceDefaultSmsPackage(getPreviousSmsDefaultPackage());
            }
        });
    }

    private void setDeviceDefaultSmsPackage(String packageName) {
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
        startActivity(intent);
    }

    private String getPreviousSmsDefaultPackage() {
        return getPreferences(MODE_PRIVATE).getString(DEVICE_DEFAULT_SMS_PACKAGE_KEY, INVALID_PACKAGE);
    }

    @Override
    public void update(Observable observable, Object arg) {
        Intent intent = (Intent) arg;
        String phoneNo = intent.getStringExtra("phone_number");
        String msg = msg_textview.getText().toString();
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNo, null, msg, null, null);
        Toast.makeText(getApplicationContext(), msg, msg.length()).show();

    }


}

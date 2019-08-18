package com.gmail.pinlock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;
    private KeyguardManager mKeyguardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (!mKeyguardManager.isKeyguardSecure()) {
            new AlertDialog.Builder(this)
            .setTitle("Lock Screen")
                    .setMessage("Secure Lock Screen hasn't set up. To continue working with this app, please go to 'Setting ->Security ->Screenlock'and set a Lock Screen")
                    .setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setCancelable(BuildConfig.DEBUG)
                    .show();
        }
        else {
            showAuthenticationScreen();
        }
    }

    private void showAuthenticationScreen() {
        Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        }
    }}

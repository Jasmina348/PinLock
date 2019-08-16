package com.gmail.pinlock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends Activity {
    private final int version = Build.VERSION.SDK_INT;
    private static final String KEY_NAME = "my_key";
    private static final byte[] SECRET_BYTE_ARRAY = new byte[]{1, 2, 3, 4, 5, 6};
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;
    private static final int AUTHENTICATION_DURATION_SECONDS = 10;
    private KeyguardManager mKeyguardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (!mKeyguardManager.isKeyguardSecure()) {
            Toast.makeText(this,
                    "Secure lock screen hasn't set up.\n"
                            + "Go to 'Settings -> Security -> Screenlock' to set up a lock screen",
                    Toast.LENGTH_LONG).show();
            return;
        }
        createKey();

    }
    private boolean tryEncrypt() {
        if (version >= 23) {
            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_NAME, null);
                Cipher cipher = Cipher.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/"
                                + KeyProperties.ENCRYPTION_PADDING_PKCS7);

                // Try encrypting something, it will only work if the user authenticated within
                // the last AUTHENTICATION_DURATION_SECONDS seconds.
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                cipher.doFinal(SECRET_BYTE_ARRAY);

                // If the user has recently authenticated, you will reach here.
               // onStart();
                return true;
            } catch (UserNotAuthenticatedException e) {
                // User is not authenticated, let's authenticate with device credentials.
                showAuthenticationScreen();
                return false;
            } catch (KeyPermanentlyInvalidatedException e) {
                // This happens if the lock screen has been disabled or reset after the key was
                // generated after the key was generated.
                Toast.makeText(this, "Keys are invalidated after created. Retry the purchase\n"
                                + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                return false;
            } catch (BadPaddingException | IllegalBlockSizeException | KeyStoreException |
                    CertificateException | UnrecoverableKeyException | IOException
                    | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }
        return true;

    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with device credentials within the last X seconds.
     */
    private void createKey() {
        // Generate a key to decrypt payment credentials, tokens, etc.
        // This will most likely be a registration step for the user when they are setting up your app.
        if (version >= 23)
            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

                // Set the alias of the entry in Android KeyStore where the key will appear
                // and the constrains (purposes) in the constructor of the Builder
                keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setUserAuthenticationRequired(true)
                        // Require that the user has unlocked in the last 30 seconds
                        .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .build());
                keyGenerator.generateKey();
            } catch (NoSuchAlgorithmException | NoSuchProviderException
                    | InvalidAlgorithmParameterException | KeyStoreException
                    | CertificateException | IOException e) {
                throw new RuntimeException("Failed to create a symmetric key", e);
            }
        else {

        }

    }

    private void showAuthenticationScreen() {
        // Create the Confirm Credentials screen. You can customize the title and description. Or
        // we will provide a generic one for you if you leave it null
        Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode == RESULT_OK) {
                if (tryEncrypt()) {
                    onStart();
                }
            } else {
                Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        tryEncrypt();
    }
}
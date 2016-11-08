package com.aptasystems.vernamcipher;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.aptasystems.vernamcipher.database.MessageDatabase;
import com.aptasystems.vernamcipher.database.SecretKeyDatabase;
import com.aptasystems.vernamcipher.model.SecretKey;
import com.aptasystems.vernamcipher.util.Crypto;
import com.aptasystems.vernamcipher.util.FileManager;
import com.aptasystems.vernamcipher.util.WarningDialogUtil;

import org.spongycastle.crypto.CryptoException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class DecryptMessageActivity extends AppCompatActivity {

    private static final String STATE_SECRET_KEY = "secretKey";

    private CoordinatorLayout _coordinatorLayout;
    private Spinner _keySpinner;
    private SecretKeyListAdapter _keySpinnerAdapter;
    private EditText _keyPasswordEditText;

    // Tracks whether we've just rotated the screen.  Gets set and unset in the beginning and end of onCreate().
    private boolean _justRotated = false;

    private int _selectedColour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            _justRotated = true;
        }

        setContentView(R.layout.activity_decrypt_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Grab widgets to use later.
        _coordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_coordinator);
        _keySpinner = (Spinner) findViewById(R.id.key_spinner);
        _keyPasswordEditText = (EditText) findViewById(R.id.key_password_edit_text);

        _keySpinnerAdapter = new SecretKeyListAdapter(this);
        _keySpinner.setAdapter(_keySpinnerAdapter);

        int selectedSecretKey = 0;
        if (savedInstanceState != null) {
            selectedSecretKey = savedInstanceState.getInt(STATE_SECRET_KEY, selectedSecretKey);
        }
        _keySpinner.setSelection(selectedSecretKey);

        _justRotated = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_decrypt_message, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SECRET_KEY, _keySpinner.getSelectedItemPosition());
    }

    public void decryptMessage(MenuItem menuItem) {

        // Fetch the secret key.
        SecretKey selectedSecretKey = (SecretKey) _keySpinner.getSelectedItem();
        final SecretKey secretKey = SecretKeyDatabase.getInstance(this).fetch(selectedSecretKey.getId(), true);

        // Attempt to decrypt the key if a password is provided.  If that fails, we can't go further.
        byte[] decryptedKey = null;
        String password = _keyPasswordEditText.getText().toString();
        if (password.length() > 0) {
            try {
                String salt = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                javax.crypto.SecretKey
                        javaSecretKey = Crypto.getSecretKey(password, salt);
                decryptedKey = Crypto.decryptToByteArray(javaSecretKey, secretKey.getKey());
            } catch (CryptoException e) {

                // Decryption failed.  Show a snackbar and put a validation error in the field.
                Snackbar snackbar = Snackbar
                        .make(_coordinatorLayout, R.string.incorrect_password, Snackbar.LENGTH_SHORT);
                snackbar.show();

                _keyPasswordEditText.setError(getString(R.string.incorrect_password));
                return;
            }
        }

        // If we got here, either the decryption of the key worked fine or there was no password provided (and, thus, no decrypt attempt).
        byte[] key = null;
        if (decryptedKey != null) {
            key = decryptedKey;
        } else {
            key = secretKey.getKey();
        }


        // Get the message content to decrypt.
        byte[] cipherText = null;
        Intent intent = getIntent();
        Uri uri = intent.getData();
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            // TODO - Smrt.
            e.printStackTrace();
        }
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();

        byte oneByte = -1;
        try {
            while ((oneByte = (byte) dis.read()) != -1) {
                byteOutStream.write(oneByte);
            }
            cipherText = byteOutStream.toByteArray();
            Log.v(getClass().getName(), "Cipher length: " + cipherText.length);
            dis.close();
        } catch (IOException e) {
            // TODO - Smrt.
            e.printStackTrace();
        }

        // Show a warning dialog.  Maybe.
        final byte[] finalKey = key;
        final byte[] finalCipherText = cipherText;
        WarningDialogUtil.showDialog(this, R.string.decrypt_message_warning_title, R.string.decrypt_message_warning_text, "decryptMessageWarning", new WarningDialogUtil.WarningDialogCallback() {
            @Override
            public void onProceed() {
                decryptMessage(secretKey, finalKey, finalCipherText);
            }

            @Override
            public void onCancel() {

            }
        });
    }

    private void decryptMessage(SecretKey secretKey, byte[] key, byte[] cipherText) {

        // Decrypt the message.
        byte[] clearText = Crypto.vernamCipher(key, cipherText);

        // Convert to string.
        String clearTextString = null;
        try {
            clearTextString = new String(clearText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO - Smrt.
            e.printStackTrace();
        }

        // Store the message in the database.
        long messageId = MessageDatabase.getInstance(this).insert(clearTextString);

        // Cut off the part of the key that we used and update the secret key.
        byte[] newKey = new byte[key.length - clearText.length];
        for (int ii = clearText.length; ii < key.length; ii++) {
            newKey[ii - clearText.length] = key[ii];
        }
        SecretKeyDatabase.getInstance(this).updateKey(secretKey.getId(), newKey);

        // Start the read message activity and finish this one.
        Intent readIntent = new Intent(this, ReadMessageActivity.class);
        readIntent.putExtra("messageId", messageId);
        startActivity(readIntent);
        finish();
    }
}

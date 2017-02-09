package com.aptasystems.vernamcipher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;

import com.aptasystems.vernamcipher.database.SecretKeyDatabase;
import com.aptasystems.vernamcipher.model.Message;
import com.aptasystems.vernamcipher.model.SecretKey;
import com.aptasystems.vernamcipher.util.Crypto;
import com.aptasystems.vernamcipher.util.DialogUtil;

import org.spongycastle.crypto.CryptoException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class DecryptMessageActivity extends AppCompatActivity {

    private static final String STATE_SECRET_KEY = "secretKey";

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private static final String DECRYPT_MESSAGE_WARNING = "decryptMessageWarning";

    private static final int BYTE_BUFFER_SIZE = 16384;

    private CoordinatorLayout _coordinatorLayout;
    private Spinner _keySpinner;
    private SecretKeyListAdapter _keySpinnerAdapter;
    private EditText _keyPasswordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_decrypt_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Grab widgets to use later.
        _coordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_coordinator);
        _keySpinner = (Spinner) findViewById(R.id.key_spinner);
        _keyPasswordEditText = (EditText) findViewById(R.id.key_password_edit_text);

        // Set up the key spinner and select the appropriate entry.
        _keySpinnerAdapter = new SecretKeyListAdapter(this);
        _keySpinner.setAdapter(_keySpinnerAdapter);
        int selectedSecretKey = 0;
        if (savedInstanceState != null) {
            selectedSecretKey = savedInstanceState.getInt(STATE_SECRET_KEY, selectedSecretKey);
        }
        _keySpinner.setSelection(selectedSecretKey);
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

        String action = getIntent().getAction();

        // This list is populated differently depending on the intent action.
        List<Uri> uris = new ArrayList<>();

        switch (action) {
            case Intent.ACTION_VIEW:
            case Intent.ACTION_EDIT:
                uris.add(getIntent().getData());
                break;
            case Intent.ACTION_SEND:
                // Only streams.
                uris.add(getIntent().<Uri>getParcelableExtra(Intent.EXTRA_STREAM));
                break;
            case Intent.ACTION_SEND_MULTIPLE:
                // Only streams.
                uris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                break;
        }

        for (Uri uri : uris) {
            decryptMessage(uri);
        }
    }

    private void decryptMessage(Uri uri)
    {
        // Fetch the secret key.
        SecretKey selectedSecretKey = (SecretKey) _keySpinner.getSelectedItem();
        final SecretKey secretKey = SecretKeyDatabase.getInstance(this).fetch(selectedSecretKey.getId(), true);

        // Attempt to decrypt the key if a password is provided.  If that fails, we can't go further.
        byte[] decryptedKey = null;
        String password = _keyPasswordEditText.getText().toString();
        if (password.length() > 0) {
            try {
                String salt = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                decryptedKey = Crypto.decryptToByteArray(password, salt, secretKey.getKey());
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

        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            InputStream inStream = getContentResolver().openInputStream(uri);
            int nRead;
            byte[] data = new byte[BYTE_BUFFER_SIZE];
            while ((nRead = inStream.read(data, 0, data.length)) != -1) {
                outStream.write(data, 0, nRead);
            }
            outStream.flush();
            cipherText = outStream.toByteArray();
        } catch (IOException e) {
            Snackbar snackbar = Snackbar
                    .make(_coordinatorLayout, R.string.error_reading_cipher_text, Snackbar.LENGTH_LONG);
            snackbar.show();
        }

        // Show a warning dialog.  Maybe.
        final byte[] finalKey = key;
        final byte[] finalCipherText = cipherText;
        DialogUtil.showWarningDialog(this,
                R.string.decrypt_message_warning_title,
                R.string.decrypt_message_warning_text,
                DECRYPT_MESSAGE_WARNING,
                new DialogUtil.WarningDialogCallback() {
            @Override
            public void onProceed() {
                decryptMessage(secretKey, finalKey, finalCipherText);
            }

            @Override
            public void onCancel() {
                // Noop.
            }
        });
    }

    private void decryptMessage(SecretKey secretKey, byte[] key, byte[] ciphertext) {

        // Decrypt the message.
        byte[] cleartext = Crypto.vernamCipher(key, ciphertext);

        // Convert to string.
        String cleartextString = new String(cleartext, UTF8_CHARSET);

        // Cut off the part of the key that we used and update the secret key.
        byte[] newKey = new byte[key.length - cleartext.length];
        for (int ii = cleartext.length; ii < key.length; ii++) {
            newKey[ii - cleartext.length] = key[ii];
        }

        // If there was a password for this key, encrypt the new key before updating it in the database.
        byte[] keyFinal = null;
        if (_keyPasswordEditText.getText().toString().length() != 0 ) {
            String password = _keyPasswordEditText.getText().toString();
            String salt = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            try {
                keyFinal = Crypto.encryptToByteArray(password, salt, newKey);
            } catch (CryptoException e) {
                Snackbar.make(_coordinatorLayout, R.string.error_secret_key_encryption_failed, Snackbar.LENGTH_SHORT).show();
                return;
            }
        } else {
            // Not password-protected.
            keyFinal = newKey;
        }

        SecretKeyDatabase.getInstance(this).updateKey(secretKey.getId(), keyFinal, newKey.length);

        // Build the message for the read activity.
        Message message = new Message(null, true, cleartextString);

        // Start the read message activity and finish this one.
        Intent readIntent = new Intent(this, ReadMessageActivity.class);
        readIntent.putExtra(ReadMessageActivity.EXTRA_KEY_MESSAGE, message);
        startActivity(readIntent);
        finish();
    }
}

package com.aptasystems.vernamcipher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aptasystems.vernamcipher.database.SecretKeyDatabase;
import com.aptasystems.vernamcipher.model.Message;
import com.aptasystems.vernamcipher.model.SecretKey;
import com.aptasystems.vernamcipher.util.Crypto;
import com.aptasystems.vernamcipher.util.DialogUtil;
import com.aptasystems.vernamcipher.util.HashUtil;

import org.spongycastle.crypto.CryptoException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private EditText _keyPasswordEditText;
    private TextView _cipherTextTextView;

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
        _cipherTextTextView = (TextView) findViewById(R.id.cipher_text_text_view);

        // Set up the key spinner and select the appropriate entry.
        SecretKeyListAdapter keySpinnerAdapter = new SecretKeyListAdapter(this);
        _keySpinner.setAdapter(keySpinnerAdapter);
        int selectedSecretKey = 0;
        if (savedInstanceState != null) {
            selectedSecretKey = savedInstanceState.getInt(STATE_SECRET_KEY, selectedSecretKey);
        }
        _keySpinner.setSelection(selectedSecretKey);

        // Populate the cipher text text view.
        byte[] cipherText = getCipherTextFromIntent();
        if (cipherText != null) {
            // Base64 encode so we have "pretty" text instead of unprintable characters.
            String cipherStringBase64 = new String(Base64.encode(cipherText, Base64.DEFAULT), UTF8_CHARSET);
            _cipherTextTextView.setText(cipherStringBase64);
        }

        // If there are no secret keys in the database, show a message and finish the activity.
        List<SecretKey> secretKeys = SecretKeyDatabase.getInstance(this).list(false);
        if (secretKeys.isEmpty()) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.no_secret_keys_title)
                    .cancelable(false)
                    .customView(R.layout.dialog_message, true)
                    .positiveText(android.R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                        }
                    })
                    .build();
            ((TextView) dialog.findViewById(R.id.message_text)).setText(R.string.no_secret_keys_message);
            dialog.show();
        }
    }

    private byte[] getCipherTextFromIntent() {
        String action = getIntent().getAction();

        // Depending on how the share happened, we may either have a single URI, a list of URIs,
        // or some text.  Either the URIs list will contain at least one URI or the text will
        // contain base64-encoded cipher text.
        List<Uri> uris = new ArrayList<>();
        String base64CipherText = null;

        switch (action) {
            case Intent.ACTION_VIEW:
            case Intent.ACTION_EDIT:
                // Single stream.
                uris.add(getIntent().getData());
                break;
            case Intent.ACTION_SEND:

                // Check to see if text was shared.
                base64CipherText = getIntent().getStringExtra(Intent.EXTRA_TEXT);

                // If there was no text, we have a stream.
                if (base64CipherText == null) {
                    uris.add(getIntent().<Uri>getParcelableExtra(Intent.EXTRA_STREAM));
                }

                break;
            case Intent.ACTION_SEND_MULTIPLE:
                // Multiple streams.
                uris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                break;
        }

        // Either the list of URIs will be populated or the base64CipherText will be populated.
        // Get the cipher text from either the URI list or the base64CipherText.
        // The list of URIs should only ever have a single entry.
        byte[] cipherText = null;
        if (uris.size() == 1) {
            // Get the message content to decrypt.
            try {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                InputStream inStream = getContentResolver().openInputStream(uris.get(0));
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
        } else if (base64CipherText != null) {
            try {
                cipherText = Base64.decode(base64CipherText, Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                // Not Base64 encoded text.
                Snackbar snackbar = Snackbar
                        .make(_coordinatorLayout, R.string.error_invalid_cipher_text, Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        }

        return cipherText;
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
        byte[] cipherText = getCipherTextFromIntent();
        if (cipherText != null) {

            // Ensure that the selected secret key has enough data to decrypt the cipher text.
            SecretKey selectedSecretKey = (SecretKey) _keySpinner.getSelectedItem();
            if (selectedSecretKey.getBytesRemaining() < cipherText.length) {
                Snackbar snackbar = Snackbar
                        .make(_coordinatorLayout, R.string.error_key_length, Snackbar.LENGTH_LONG);
                snackbar.show();
            } else {
                decryptMessage(cipherText);
            }
        }
    }

    private byte[] decryptKeyIfNecessary(final SecretKey secretKey) {

        // Attempt to decrypt the key if a password is provided.  If that fails, we can't go further.
        String password = _keyPasswordEditText.getText().toString();
        if (password.length() > 0) {
            try {
                // Hash the password and use it as the salt.
                String salt = HashUtil.hashPassword(password);
                return Crypto.decryptToByteArray(password, salt, secretKey.getKey());
            } catch (CryptoException e) {

                // Decryption failed.  Show a snackbar and put a validation error in the field.
                Snackbar snackbar = Snackbar
                        .make(_coordinatorLayout, R.string.incorrect_password, Snackbar.LENGTH_SHORT);
                snackbar.show();

                _keyPasswordEditText.setError(getString(R.string.incorrect_password));
                return null;
            }
        } else {
            return secretKey.getKey();
        }
    }

    private void decryptMessage(byte[] cipherText) {

        SecretKey selectedSecretKey = (SecretKey) _keySpinner.getSelectedItem();
        final SecretKey secretKey = SecretKeyDatabase.getInstance(this).fetch(selectedSecretKey.getId(), true);

        // Decrypt the key if necessary.
        byte[] key = decryptKeyIfNecessary(secretKey);
        if (key == null) {
            return;
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
        if (_keyPasswordEditText.getText().toString().length() != 0) {
            String password = _keyPasswordEditText.getText().toString();
            // Hash the password and use it as the salt.
            String salt = HashUtil.hashPassword(password);
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

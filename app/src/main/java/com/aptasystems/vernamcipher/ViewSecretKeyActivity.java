package com.aptasystems.vernamcipher;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.aptasystems.vernamcipher.model.SecretKey;
import com.aptasystems.vernamcipher.util.Crypto;

import org.spongycastle.crypto.CryptoException;

import java.nio.charset.Charset;
import java.text.NumberFormat;

public class ViewSecretKeyActivity extends AppCompatActivity {

    public static final String EXTRA_KEY_SECRET_KEY = "secretKey";

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private CoordinatorLayout _coordinatorLayout;
    private EditText _keyPasswordEditText;
    private TextView _secretKeyContentTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_secret_key);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SecretKey secretKey = (SecretKey) getIntent().getSerializableExtra(EXTRA_KEY_SECRET_KEY);

        _coordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_coordinator);
        TextView nameTextView = (TextView) findViewById(R.id.text_view_filename);
        TextView secretKeyDescriptionTextView = (TextView) findViewById(R.id.text_view_description);
        TextView bytesRemainingTextView = (TextView) findViewById(R.id.text_view_bytes_remaining);
        _keyPasswordEditText = (EditText) findViewById(R.id.key_password_edit_text);
        _secretKeyContentTextView = (TextView) findViewById(R.id.secret_key_content_text_view);

        // Populate the key name.
        nameTextView.setText(secretKey.getName());
        nameTextView.setTextColor(secretKey.getColour());

        // Populate the key description
        if (TextUtils.getTrimmedLength(secretKey.getDescription()) > 0) {
            secretKeyDescriptionTextView.setVisibility(View.VISIBLE);
            secretKeyDescriptionTextView.setText(secretKey.getDescription());
        } else {
            secretKeyDescriptionTextView.setVisibility(View.GONE);
        }

        // Populate the byes remaining.
        int secretKeyLength = secretKey.getBytesRemaining();
        String bytesRemainingString = String.format(getResources().getString(R.string.bytes_remaining), NumberFormat.getIntegerInstance().format(secretKey.getBytesRemaining()));
        bytesRemainingTextView.setText(bytesRemainingString);

        // Put in some instructions in the key content text view.
        _secretKeyContentTextView.setText(R.string.view_secret_key_instructions);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view_secret_key, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showKeyHex(MenuItem menuItem) {
        PrepareViewableSecretKeyTask task = new PrepareViewableSecretKeyTask(this) {
            @Override
            protected void onPostExecute(String encodedKey) {
                if (encodedKey != null) {
                    _secretKeyContentTextView.setText(encodedKey);
                }
                super.onPostExecute(encodedKey);
            }
        };
        byte[] keyData = getKeyData();
        task.execute(keyData, PrepareViewableSecretKeyTask.OutputEncoding.HEXADECIMAL);
    }

    public void showKeyBase64(MenuItem menuItem) {
        PrepareViewableSecretKeyTask task = new PrepareViewableSecretKeyTask(this) {
            @Override
            protected void onPostExecute(String encodedKey) {
                if (encodedKey != null) {
                    _secretKeyContentTextView.setText(encodedKey);
                }
                super.onPostExecute(encodedKey);
            }
        };
        byte[] keyData = getKeyData();
        task.execute(keyData, PrepareViewableSecretKeyTask.OutputEncoding.BASE64);
    }

    private byte[] getKeyData() {
        SecretKey secretKey = (SecretKey) getIntent().getSerializableExtra(EXTRA_KEY_SECRET_KEY);

        // Attempt to decrypt the key if a password is provided.  If that fails, we can't go further.
        byte[] decryptedData = null;
        String password = _keyPasswordEditText.getText().toString();
        if (password.length() > 0) {
            try {
                String salt = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                decryptedData = Crypto.decryptToByteArray(password, salt, secretKey.getKey());
            } catch (CryptoException e) {

                // Decryption failed.  Show a snackbar and put a validation error in the field.
                Snackbar snackbar = Snackbar
                        .make(_coordinatorLayout, R.string.incorrect_password, Snackbar.LENGTH_SHORT);
                snackbar.show();

                _keyPasswordEditText.setError(getString(R.string.incorrect_password));
                return null;
            }
        }

        // If we got here, either the decryption of the key worked fine or there was no password provided (and, thus, no decrypt attempt).
        byte[] key = null;
        if (decryptedData != null) {
            key = decryptedData;
        } else {
            key = secretKey.getKey();
        }

        return key;
    }

    public void showHelpActivity(MenuItem menuItem) {
        Intent intent = new Intent(this, HelpActivity.class);
        intent.putExtra(HelpActivity.EXTRA_KEY_SOURCE, this.getClass().getName());
        startActivity(intent);
    }
}

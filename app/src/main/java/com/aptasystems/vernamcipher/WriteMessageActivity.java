package com.aptasystems.vernamcipher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.aptasystems.vernamcipher.database.SecretKeyDatabase;
import com.aptasystems.vernamcipher.model.SecretKey;
import com.aptasystems.vernamcipher.util.Crypto;
import com.aptasystems.vernamcipher.util.FileManager;
import com.aptasystems.vernamcipher.util.WarningDialogUtil;

import org.spongycastle.crypto.CryptoException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.ArrayList;

public class WriteMessageActivity extends AppCompatActivity {

    private static final String STRING_ENCODING = "UTF-8";

    private CoordinatorLayout _coordinatorLayout;
    private TextView _nameTextView;
    private TextView _secretKeyDescriptionTextView;
    private TextView _bytesRemainingTextView;
    private EditText _keyPasswordEditText;
    private EditText _contentEditText;

    private int _secretKeyLength;
    private int originalBytesRemainingColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // TODO - Hardcoded string.
        SecretKey secretKey = (SecretKey) getIntent().getSerializableExtra("secretKey");

        _coordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_coordinator);
        _nameTextView = (TextView) findViewById(R.id.text_view_filename);
        _secretKeyDescriptionTextView = (TextView) findViewById(R.id.text_view_description);
        _bytesRemainingTextView = (TextView) findViewById(R.id.text_view_bytes_remaining);
        _keyPasswordEditText = (EditText) findViewById(R.id.key_password_edit_text);
        _contentEditText = (EditText) findViewById(R.id.message_edit_text);

        _nameTextView.setText(secretKey.getName());
        _nameTextView.setTextColor(secretKey.getColour());

        if (TextUtils.getTrimmedLength(secretKey.getDescription()) > 0) {
            _secretKeyDescriptionTextView.setVisibility(View.VISIBLE);
            _secretKeyDescriptionTextView.setText(secretKey.getDescription());
        } else {
            _secretKeyDescriptionTextView.setVisibility(View.GONE);
        }

        _secretKeyLength = secretKey.getBytesRemaining();
        originalBytesRemainingColor = _bytesRemainingTextView.getCurrentTextColor();
        String bytesRemainingString = String.format(getResources().getString(R.string.bytes_remaining), NumberFormat.getIntegerInstance().format(secretKey.getBytesRemaining()));
        _bytesRemainingTextView.setText(bytesRemainingString);

        _contentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Noop.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Noop.
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Convert the message to bytes.
                String messageContent = s.toString();
                byte[] clearText = null;
                try {
                    clearText = messageContent.getBytes(STRING_ENCODING);
                } catch (UnsupportedEncodingException e) {
                    // TODO - Do something smrt.
                    e.printStackTrace();
                }

                int bytesRemaining = _secretKeyLength - clearText.length;
                if (bytesRemaining < 0) {
                    _bytesRemainingTextView.setText(getResources().getString(R.string.message_too_long));
                    _bytesRemainingTextView.setTextColor(getResources().getColor(R.color.error_label));
                } else {

                    String bytesRemainingString = String.format(getResources().getString(R.string.bytes_remaining), NumberFormat.getIntegerInstance().format(bytesRemaining));
                    _bytesRemainingTextView.setText(bytesRemainingString);

                    if (bytesRemaining == 0) {
                        _bytesRemainingTextView.setTextColor(getResources().getColor(R.color.warning_label));
                    } else {
                        _bytesRemainingTextView.setTextColor(originalBytesRemainingColor);
                    }
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_write_message, menu);
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


    public void sendMessage(MenuItem menuItem) {

        // First ensure that there was content entered.
        String message = _contentEditText.getText().toString();
        if (message.length() == 0) {
            Snackbar.make(_coordinatorLayout, R.string.empty_message, Snackbar.LENGTH_SHORT).show();
            _contentEditText.setError(getString(R.string.empty_message));
            return;
        }

        // Convert the message to bytes using UTF-8 encoding.
        String messageContent = _contentEditText.getText().toString();
        byte[] clearText = null;
        try {
            // TODO - hardcoded.
            clearText = messageContent.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO - Do something smrt.
            e.printStackTrace();
        }
        int bytesRemaining = _secretKeyLength - clearText.length;

        // Ensure our message isn't too long.
        if (bytesRemaining < 0) {
            Snackbar.make(_coordinatorLayout, R.string.message_too_long, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.snackbar_explain_button, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // TODO - Write me.
                        }
                    }).show();
            return;
        }

        // TODO - Hardcoded string.
        final SecretKey secretKey = (SecretKey) getIntent().getSerializableExtra("secretKey");

        // Attempt to decrypt the key if a password is provided.  If that fails, we can't go further.
        byte[] decryptedData = null;
        String password = _keyPasswordEditText.getText().toString();
        if (password.length() > 0) {
            try {
                String salt = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                javax.crypto.SecretKey
                        javaSecretKey = Crypto.getSecretKey(password, salt);
                decryptedData = Crypto.decryptToByteArray(javaSecretKey, secretKey.getKey());
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
        if (decryptedData != null) {
            key = decryptedData;
        } else {
            key = secretKey.getKey();
        }

        // Show a warning dialog.  Maybe.
        final byte[] finalKey = key;
        final byte[] finalClearText = clearText;
        WarningDialogUtil.showDialog(this, R.string.send_message_warning_title, R.string.send_message_warning_text, "sendMessageWarning", new WarningDialogUtil.WarningDialogCallback() {
            @Override
            public void onProceed() {
                sendMessage(secretKey, finalKey, finalClearText);
            }

            @Override
            public void onCancel() {

            }
        });
    }

    private void sendMessage(SecretKey secretKey, byte[] key, byte[] clearText) {

        // Encrypt the message.
        byte[] cipherText = Crypto.vernamCipher(key, clearText);

        // Write the cipher text to disk
        File messageFile = FileManager.getInstance(this).newTempFile();
        try {
            FileOutputStream fos = new FileOutputStream(messageFile);
            fos.write(cipherText);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.v(getClass().getName(), "File length: " + messageFile.length());

        // Cut off the part of the key that we used and update the secret key.
        byte[] newKey = new byte[key.length - clearText.length];
        for (int ii = clearText.length; ii < key.length; ii++) {
            newKey[ii - clearText.length] = key[ii];
        }
        SecretKeyDatabase.getInstance(this).updateKey(secretKey.getId(), newKey);

        // Share.
        Intent shareIntent = buildShareIntent(messageFile);
//            startActivityForResult(shareIntent, ACTIVITY_SHARE);
        startActivity(shareIntent);

        finish();

    }

    private Intent buildShareIntent(File file) {

        // Set up our share intent with the currently selected files.
        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        ArrayList<Uri> uris = new ArrayList<Uri>();
        uris.add(Uri.fromFile(file));
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.setType("application/octet-stream");
        return shareIntent;
    }

}

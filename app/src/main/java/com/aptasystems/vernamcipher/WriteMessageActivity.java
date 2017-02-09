package com.aptasystems.vernamcipher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.aptasystems.vernamcipher.database.MessageDatabase;
import com.aptasystems.vernamcipher.database.SecretKeyDatabase;
import com.aptasystems.vernamcipher.model.SecretKey;
import com.aptasystems.vernamcipher.util.Crypto;
import com.aptasystems.vernamcipher.util.FileManager;
import com.aptasystems.vernamcipher.util.DialogUtil;
import com.aptasystems.vernamcipher.util.ShareUtil;

import org.spongycastle.crypto.CryptoException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WriteMessageActivity extends AppCompatActivity {

    public static final String EXTRA_KEY_SECRET_KEY = "secretKey";

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private CoordinatorLayout _coordinatorLayout;
    private TextView _nameTextView;
    private TextView _secretKeyDescriptionTextView;
    private TextView _bytesRemainingTextView;
    private EditText _keyPasswordEditText;
    private EditText _contentEditText;
    private CheckBox _saveCopyCheckBox;

    private int _secretKeyLength;
    private int originalBytesRemainingColor;

    private int _currentRequestCode;
    private Map<Integer, List<File>> _fileMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        _currentRequestCode = 1;
        _fileMap = new HashMap<>();

        SecretKey secretKey = (SecretKey) getIntent().getSerializableExtra(EXTRA_KEY_SECRET_KEY);

        _coordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_coordinator);
        _nameTextView = (TextView) findViewById(R.id.text_view_filename);
        _secretKeyDescriptionTextView = (TextView) findViewById(R.id.text_view_description);
        _bytesRemainingTextView = (TextView) findViewById(R.id.text_view_bytes_remaining);
        _keyPasswordEditText = (EditText) findViewById(R.id.key_password_edit_text);
        _contentEditText = (EditText) findViewById(R.id.message_edit_text);
        _saveCopyCheckBox = (CheckBox) findViewById(R.id.check_box_save_message_copy);

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
                clearText = messageContent.getBytes(UTF8_CHARSET);

                int bytesRemaining = _secretKeyLength - clearText.length;
                if (bytesRemaining < 0) {
                    _bytesRemainingTextView.setText(getResources().getString(R.string.message_too_long));
                    _bytesRemainingTextView.setTextColor(ContextCompat.getColor(WriteMessageActivity.this, R.color.error_label));
                } else {

                    String bytesRemainingString = String.format(getResources().getString(R.string.bytes_remaining), NumberFormat.getIntegerInstance().format(bytesRemaining));
                    _bytesRemainingTextView.setText(bytesRemainingString);

                    if (bytesRemaining == 0) {
                        _bytesRemainingTextView.setTextColor(ContextCompat.getColor(WriteMessageActivity.this, R.color.warning_label));
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

    public void toggleSaveMessage(View view) {
        boolean checked = _saveCopyCheckBox.isChecked();

        if (checked) {
            DialogUtil.showWarningDialog(this, R.string.save_message_copy_warning_title, R.string.save_message_copy_warning_text, "saveCopyWarning", new DialogUtil.WarningDialogCallback() {
                @Override
                public void onProceed() {
                    // Noop.
                }

                @Override
                public void onCancel() {
                    // Uncheck the checkbox.
                    _saveCopyCheckBox.setChecked(false);
                }
            });
        }
    }

    public void showHelpActivity(MenuItem menuItem) {
        Intent intent = new Intent(this, HelpActivity.class);
        intent.putExtra(HelpActivity.EXTRA_KEY_SOURCE, this.getClass().getName());
        startActivity(intent);
    }

    public void sendMessage(MenuItem menuItem) {

        // We need to ensure that external storage is available and writable before we continue.
        FileManager.StorageState storageState = FileManager.getInstance(this).getExternalStorageState();
        if (storageState == FileManager.StorageState.READ_ONLY) {
            Snackbar.make(_coordinatorLayout, R.string.toast_external_storage_read_only_send_message, Snackbar.LENGTH_LONG).show();
            return;
        } else if (storageState == FileManager.StorageState.NOT_AVAILABLE) {
            Snackbar.make(_coordinatorLayout, R.string.toast_external_storage_not_available_send_message, Snackbar.LENGTH_LONG).show();
            return;
        }

        // First ensure that there was content entered.
        String message = _contentEditText.getText().toString();
        if (message.length() == 0) {
            Snackbar.make(_coordinatorLayout, R.string.empty_message, Snackbar.LENGTH_SHORT).show();
            _contentEditText.setError(getString(R.string.empty_message));
            return;
        }

        // Convert the message to bytes.
        String messageContent = _contentEditText.getText().toString();
        byte[] clearText = messageContent.getBytes(UTF8_CHARSET);
        int bytesRemaining = _secretKeyLength - clearText.length;

        // Ensure our message isn't too long.
        if (bytesRemaining < 0) {
            // TODO - Add an action?
            Snackbar.make(_coordinatorLayout, R.string.message_too_long, Snackbar.LENGTH_SHORT)
//                    .setAction(R.string.snackbar_explain_button, new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                        }
//                    })
                    .show();
            return;
        }

        final SecretKey secretKey = (SecretKey) getIntent().getSerializableExtra(EXTRA_KEY_SECRET_KEY);

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
        DialogUtil.showWarningDialog(this, R.string.send_message_warning_title, R.string.send_message_warning_text, "sendMessageWarning", new DialogUtil.WarningDialogCallback() {
            @Override
            public void onProceed() {
                sendMessage(secretKey, finalKey, finalClearText);
            }

            @Override
            public void onCancel() {
                // Noop.
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

        // Put the file in the file map so we can delete it afterwards.
        List<File> files = new ArrayList<>();
        files.add(messageFile);
        _fileMap.put(_currentRequestCode, files);

        // Cut off the part of the key that we used and update the secret key.
        byte[] newKey = new byte[key.length - clearText.length];
        for (int ii = clearText.length; ii < key.length; ii++) {
            newKey[ii - clearText.length] = key[ii];
        }

        // If there was a password for this key, encrypt the new key before updating it in the database.
        byte[] keyFinal = null;
        if (_keyPasswordEditText.getText().toString().length() != 0) {
            String password = _keyPasswordEditText.getText().toString();
            String salt = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            try {
                keyFinal = Crypto.encryptToByteArray(password, salt, newKey);
            } catch (CryptoException e) {
                // TODO: Handle exception.
                e.printStackTrace();
            }
        } else {
            // Not password-protected.
            keyFinal = newKey;
        }

        SecretKeyDatabase.getInstance(this).updateKey(secretKey.getId(), keyFinal, newKey.length);

        // Optionally save a copy of the message.
        if (_saveCopyCheckBox.isChecked()) {
            MessageDatabase.getInstance(this).insert(false, _contentEditText.getText().toString());
        }

        // Share.
        Intent shareIntent = ShareUtil.buildShareIntent(this, messageFile);
        startActivityForResult(shareIntent, _currentRequestCode++);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Delete the files that were shared.
        if (_fileMap.containsKey(requestCode)) {
            List<File> files = _fileMap.remove(requestCode);
            for (File file : files) {
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        finish();
    }

}

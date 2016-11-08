package com.aptasystems.vernamcipher;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.aptasystems.vernamcipher.database.SecretKeyDatabase;
import com.aptasystems.vernamcipher.util.Crypto;

import org.spongycastle.crypto.CryptoException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.UUID;

import javax.crypto.SecretKey;

public class ImportSecretKeyActivity extends AppCompatActivity {

    private static final String STATE_DESCRIPTION = "description";
    private static final String STATE_COLOUR = "colour";

    private static final String PREF_COLOUR = "defaultColour";

    private CoordinatorLayout _coordinatorLayout;
    private EditText _descriptionEditText;
    private RadioButton _colourBlackRadioButton;
    private RadioButton _colourRedRadioButton;
    private RadioButton _colourGreenRadioButton;
    private RadioButton _colourBlueRadioButton;
    private RadioButton _colourCyanRadioButton;
    private RadioButton _colourOrangeRadioButton;
    private RadioButton _colourPurpleRadioButton;
    private RadioButton _colourGreyRadioButton;

    // Tracks whether we've just rotated the screen.  Gets set and unset in the beginning and end of onCreate().
    private boolean _justRotated = false;

    private int _selectedColour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            _justRotated = true;
        }

        setContentView(R.layout.activity_import_secret_key);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Grab widgets to use later.
        _coordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_coordinator);
        _colourBlackRadioButton = (RadioButton) findViewById(R.id.colour_black);
        _colourRedRadioButton = (RadioButton) findViewById(R.id.colour_red);
        _colourGreenRadioButton = (RadioButton) findViewById(R.id.colour_green);
        _colourBlueRadioButton = (RadioButton) findViewById(R.id.colour_blue);
        _colourCyanRadioButton = (RadioButton) findViewById(R.id.colour_cyan);
        _colourOrangeRadioButton = (RadioButton) findViewById(R.id.colour_orange);
        _colourPurpleRadioButton = (RadioButton) findViewById(R.id.colour_purple);
        _colourGreyRadioButton = (RadioButton) findViewById(R.id.colour_grey);
        _descriptionEditText = (EditText) findViewById(R.id.text_view_description);

        // Set our colours in the colour radio button's tags.
        _colourBlackRadioButton.setTag(getResources().getColor(R.color.key_black));
        _colourRedRadioButton.setTag(getResources().getColor(R.color.key_red));
        _colourGreenRadioButton.setTag(getResources().getColor(R.color.key_green));
        _colourBlueRadioButton.setTag(getResources().getColor(R.color.key_blue));
        _colourCyanRadioButton.setTag(getResources().getColor(R.color.key_cyan));
        _colourOrangeRadioButton.setTag(getResources().getColor(R.color.key_orange));
        _colourPurpleRadioButton.setTag(getResources().getColor(R.color.key_purple));
        _colourGreyRadioButton.setTag(getResources().getColor(R.color.key_grey));

        int colourRadioButtonChecked = R.id.colour_black;
        if (savedInstanceState != null) {
            colourRadioButtonChecked = savedInstanceState.getInt(STATE_COLOUR, colourRadioButtonChecked);
        } else {
            colourRadioButtonChecked = getPreferences(MODE_PRIVATE).getInt(PREF_COLOUR, colourRadioButtonChecked);
        }

        if (colourRadioButtonChecked != R.id.colour_black &&
                colourRadioButtonChecked != R.id.colour_red &&
                colourRadioButtonChecked != R.id.colour_green &&
                colourRadioButtonChecked != R.id.colour_blue &&
                colourRadioButtonChecked != R.id.colour_cyan &&
                colourRadioButtonChecked != R.id.colour_orange &&
                colourRadioButtonChecked != R.id.colour_purple &&
                colourRadioButtonChecked != R.id.colour_grey) {
            colourRadioButtonChecked = R.id.colour_black;
        }

        // Check the appropriate radio button.
        ((RadioButton) findViewById(colourRadioButtonChecked)).setChecked(true);
        selectColour(colourRadioButtonChecked);

        if (savedInstanceState != null) {
            // Restore description.
            String description = savedInstanceState.getString(STATE_DESCRIPTION);
            _descriptionEditText.setText(description);
        }

        _justRotated = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_import_secret_key, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int colourRadioButtonChecked = 0;
        for (RadioButton radioButton : new RadioButton[]{_colourBlackRadioButton, _colourRedRadioButton, _colourGreenRadioButton, _colourBlueRadioButton, _colourCyanRadioButton, _colourOrangeRadioButton, _colourPurpleRadioButton, _colourGreyRadioButton}) {
            if (radioButton.isChecked()) {
                colourRadioButtonChecked = radioButton.getId();
                break;
            }
        }
        outState.putInt(STATE_COLOUR, colourRadioButtonChecked);

        outState.putString(STATE_DESCRIPTION, _descriptionEditText.getText().toString());
    }

    public void selectColour(View view) {
        selectColour(view.getId());
    }

    private void selectColour(int viewId) {
        if (!_justRotated) {
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_COLOUR, viewId).commit();
        }

        _selectedColour = viewId;

        // Deselect the other ones.
        for (RadioButton radioButton : new RadioButton[]{_colourBlackRadioButton, _colourRedRadioButton, _colourGreenRadioButton, _colourBlueRadioButton, _colourCyanRadioButton, _colourOrangeRadioButton, _colourPurpleRadioButton, _colourGreyRadioButton}) {
            if (radioButton.getId() != viewId) {
                radioButton.setChecked(false);
            }
        }
    }

    // TODO - Hardcoded strings.
    public void importKey(MenuItem menuItem) {

        // Get the key from the intent.
        byte[] keyData = null;
        Intent inputIntent = getIntent();
        Uri uri = inputIntent.getData();
        BufferedInputStream inStream = null;
        // DataInputStream dis = null;
        try {
            inStream = new BufferedInputStream(getContentResolver().openInputStream(uri));
            // dis = new DataInputStream(getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            // TODO - Smrt.
            e.printStackTrace();
        }
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();

        int oneByte = -1;
        try {
            while ((oneByte = inStream.read()) != -1) {
                byteOutStream.write((byte) oneByte);
            }
            keyData = byteOutStream.toByteArray();
            Log.v(getClass().getName(), "Key length: " + keyData.length);
            inStream.close();
        } catch (IOException e) {
            // TODO - Smrt.
            e.printStackTrace();
        }

        // Get the filename.
        String filename = null;
        Log.v(getClass().getName(), uri.getLastPathSegment());
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            String[] proj = {MediaStore.MediaColumns.DISPLAY_NAME};
            Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
            if (cursor != null && cursor.getCount() != 0) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                filename = cursor.getString(columnIndex);
            }
            if (cursor != null) {
                cursor.close();
            }
        } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            filename = uri.getLastPathSegment();

        }

        // Get the selected colour.
        int colour = (int) findViewById(_selectedColour).getTag();

        // Insert the key into the database.
        SecretKeyDatabase.getInstance(this).insert(filename, colour, _descriptionEditText.getText().toString(), keyData);

        // Go to the main activity.
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
        finish();
    }

}

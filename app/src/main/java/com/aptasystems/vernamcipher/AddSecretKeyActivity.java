package com.aptasystems.vernamcipher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
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

import java.security.SecureRandom;
import java.util.UUID;

import javax.crypto.SecretKey;

public class AddSecretKeyActivity extends AppCompatActivity {

    private static final String DICEWARE_PASSGEN_PACKAGE = "com.aptasystems.dicewarepasswordgenerator";
    private static final int ACTIVITY_DICEWARE_PASSWORD = 1;

    private static final String EXTRA_DICEWARE_PASSWORD_KEY = "password";

    private static final int DEFAULT_SECRET_KEY_LENGTH = 50;

    private static final String STATE_PASSWORD_MECHANISM = "passwordMechanism";
    private static final String STATE_SECRET_KEY_LENGTH = "secretKeyLength";
    private static final String STATE_PASSWORD = "password";
    private static final String STATE_REPEAT_PASSWORD = "repeatPassword";
    private static final String STATE_SHOW_PASSWORD = "showPassword";
    private static final String STATE_DESCRIPTION = "description";
    private static final String STATE_COLOUR = "colour";

    private static final String PREF_DEFAULT_PASSWORD_MECHANISM = "defaultPasswordMechanism";
    private static final String PREF_DEFAULT_SECRET_KEY_LENGTH = "defaultSecretKeyLength";
    private static final String PREF_SHOW_PASSWORD = "defaultShowPassword";
    private static final String PREF_COLOUR = "defaultColour";

    private CoordinatorLayout _coordinatorLayout;
    private SeekBar _secretKeyLengthSeekBar;
    private EditText _passwordEditText;
    private EditText _repeatPasswordEditText;
    private CheckBox _showPasswordCheckBox;
    private LinearLayout _dicewareNotInstalledLayout;
    private LinearLayout _dicewareInstalledLayout;
    private LinearLayout _enterPasswordLayout;
    private RadioButton _passwordMechanismDiceware;
    private RadioButton _passwordMechanismEnter;
    private RadioButton _passwordMechanismNone;
    private TextView _secretKeyLengthInfo;
    private EditText _descriptionEditText;
    private RadioButton _colourBlackRadioButton;
    private RadioButton _colourRedRadioButton;
    private RadioButton _colourGreenRadioButton;
    private RadioButton _colourBlueRadioButton;
    private RadioButton _colourCyanRadioButton;
    private RadioButton _colourOrangeRadioButton;
    private RadioButton _colourPurpleRadioButton;
    private RadioButton _colourGreyRadioButton;

    private boolean _dicewareAppInstalled = false;

    // Tracks whether we've just rotated the screen.  Gets set and unset in the beginning and end of onCreate().
    private boolean _justRotated = false;

    private int _selectedColour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            _justRotated = true;
        }

        setContentView(R.layout.activity_add_secret_key);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Grab widgets to use later.
        _coordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_coordinator);
        _secretKeyLengthSeekBar = (SeekBar) findViewById(R.id.seek_bar_secret_key_length);
        _passwordEditText = (EditText) findViewById(R.id.edit_text_password);
        _repeatPasswordEditText = (EditText) findViewById(R.id.edit_text_repeat_password);
        _showPasswordCheckBox = (CheckBox) findViewById(R.id.check_box_show_password);
        _dicewareNotInstalledLayout = (LinearLayout) findViewById(R.id.layout_diceware_app_not_installed);
        _dicewareInstalledLayout = (LinearLayout) findViewById(R.id.layout_diceware_app_installed);
        _enterPasswordLayout = (LinearLayout) findViewById(R.id.layout_enter_password);
        _passwordMechanismDiceware = (RadioButton) findViewById(R.id.password_mechanism_diceware);
        _passwordMechanismEnter = (RadioButton) findViewById(R.id.password_mechanism_enter);
        _passwordMechanismNone = (RadioButton) findViewById(R.id.password_mechanism_none);
        _secretKeyLengthInfo = (TextView) findViewById(R.id.text_view_secret_key_length_info);
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

        // Check to see if the Diceware Password Generator application is installed.
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(DICEWARE_PASSGEN_PACKAGE, 0);
            _dicewareAppInstalled = packageInfo != null;
        } catch (PackageManager.NameNotFoundException e) {
            // Ignore.
        }

        // Set up our seekbar listener.
        _secretKeyLengthSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                // If this flag is true, we're recreating the activity after a rotation.  In this case, don't do anything.
                if (_justRotated) {
                    return;
                }

                // Update the text that shows the secret key length info.
                updateSecretKeyText();

                // Update the password length preference.
                getPreferences(MODE_PRIVATE).edit().putInt(PREF_DEFAULT_SECRET_KEY_LENGTH, progress + 1).commit();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Noop.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Noop.
            }
        });

        _passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                validatePasswordFields();
            }
        });

        _repeatPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                validatePasswordFields();
            }
        });

        int mechanismRadioButtonChecked = R.id.password_mechanism_diceware;
        int secretKeyLength = DEFAULT_SECRET_KEY_LENGTH;
        boolean showPassword = false;
        int colourRadioButtonChecked = R.id.colour_black;
        if (savedInstanceState != null) {

            // Handle restoration from a saved instance state: which radio button is checked, secret key length, and show password.
            mechanismRadioButtonChecked = savedInstanceState.getInt(STATE_PASSWORD_MECHANISM, mechanismRadioButtonChecked);
            secretKeyLength = savedInstanceState.getInt(STATE_SECRET_KEY_LENGTH, secretKeyLength);
            showPassword = savedInstanceState.getBoolean(STATE_SHOW_PASSWORD, showPassword);
            colourRadioButtonChecked = savedInstanceState.getInt(STATE_COLOUR, colourRadioButtonChecked);

        } else {

            // If there was no saved instance state, attempt to get the default radio button
            // selection and secret key length from the preferences.
            mechanismRadioButtonChecked = getPreferences(MODE_PRIVATE).getInt(PREF_DEFAULT_PASSWORD_MECHANISM, mechanismRadioButtonChecked);
            secretKeyLength = getPreferences(MODE_PRIVATE).getInt(PREF_DEFAULT_SECRET_KEY_LENGTH, secretKeyLength);
            showPassword = getPreferences(MODE_PRIVATE).getBoolean(PREF_SHOW_PASSWORD, showPassword);
            colourRadioButtonChecked = getPreferences(MODE_PRIVATE).getInt(PREF_COLOUR, colourRadioButtonChecked);

        }

        // It is possible for the saved button ids to not match the buttons (due to app code changes).
        if (mechanismRadioButtonChecked != R.id.password_mechanism_diceware && mechanismRadioButtonChecked != R.id.password_mechanism_enter && mechanismRadioButtonChecked != R.id.password_mechanism_none) {
            mechanismRadioButtonChecked = R.id.password_mechanism_diceware;
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

        // Check the appropriate radio buttons and set the secret key length seek bar.

        _secretKeyLengthSeekBar.setProgress(secretKeyLength - 1);
        ((RadioButton) findViewById(mechanismRadioButtonChecked)).setChecked(true);
        selectPasswordMechanism(mechanismRadioButtonChecked);

        ((RadioButton) findViewById(colourRadioButtonChecked)).setChecked(true);
        selectColour(colourRadioButtonChecked);

        _showPasswordCheckBox.setChecked(showPassword);
        toggleShowPassword(_showPasswordCheckBox);

        if (savedInstanceState != null) {

            // Restore the password if applicable.
            String password = savedInstanceState.getString(STATE_PASSWORD);
            String repeatPassword = savedInstanceState.getString(STATE_REPEAT_PASSWORD);
            _passwordEditText.setText(password);
            _repeatPasswordEditText.setText(repeatPassword);

            // Restore description.
            String description = savedInstanceState.getString(STATE_DESCRIPTION);
            _descriptionEditText.setText(description);

        }

        validatePasswordFields();

        _justRotated = false;
    }

    @Override
    protected void onResume() {

        // Update the text that shows the secret key length info.
        updateSecretKeyText();

        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_secret_key, menu);
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

    /**
     * Update the text that shows the information about how long the password will take to brute
     * force.
     */
    private void updateSecretKeyText() {

        int secretkeyLength = _secretKeyLengthSeekBar.getProgress() + 1;
        Resources res = getResources();
        _secretKeyLengthInfo.setText(String.format(res.getString(R.string.key_length_content_2), secretkeyLength));
        _secretKeyLengthInfo.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int mechanismRadioButtonChecked = 0;
        for (RadioButton radioButton : new RadioButton[]{_passwordMechanismDiceware, _passwordMechanismEnter, _passwordMechanismNone}) {
            if (radioButton.isChecked()) {
                mechanismRadioButtonChecked = radioButton.getId();
                break;
            }
        }
        outState.putInt(STATE_PASSWORD_MECHANISM, mechanismRadioButtonChecked);

        int colourRadioButtonChecked = 0;
        for (RadioButton radioButton : new RadioButton[]{_colourBlackRadioButton, _colourRedRadioButton, _colourGreenRadioButton, _colourBlueRadioButton, _colourCyanRadioButton, _colourOrangeRadioButton, _colourPurpleRadioButton, _colourGreyRadioButton}) {
            if (radioButton.isChecked()) {
                colourRadioButtonChecked = radioButton.getId();
                break;
            }
        }
        outState.putInt(STATE_COLOUR, colourRadioButtonChecked);

        outState.putInt(STATE_SECRET_KEY_LENGTH, _secretKeyLengthSeekBar.getProgress());
        outState.putString(STATE_PASSWORD, _passwordEditText.getText().toString());
        outState.putString(STATE_REPEAT_PASSWORD, _repeatPasswordEditText.getText().toString());
        outState.putBoolean(STATE_SHOW_PASSWORD, _showPasswordCheckBox.isChecked());
        outState.putString(STATE_DESCRIPTION, _descriptionEditText.getText().toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ACTIVITY_DICEWARE_PASSWORD) {
            if (resultCode == Activity.RESULT_OK) {
                String password = data.getStringExtra(EXTRA_DICEWARE_PASSWORD_KEY);

                if (password != null) {
                    _passwordEditText.setText(password);
                    _repeatPasswordEditText.setText(password);

//                    _showPasswordCheckBox.setChecked(true);
//                    toggleShowPassword(_showPasswordCheckBox);
                }
            } else {
                // TODO - Handle this.
            }
        }
    }

    public void selectPasswordMechanism(View view) {
        selectPasswordMechanism(view.getId());
    }

    private void selectPasswordMechanism(int viewId) {

        if (!_justRotated) {
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_DEFAULT_PASSWORD_MECHANISM, viewId).commit();
        }

        switch (viewId) {
            case R.id.password_mechanism_diceware:
                _dicewareInstalledLayout.setVisibility(_dicewareAppInstalled ? View.VISIBLE : View.GONE);
                _dicewareNotInstalledLayout.setVisibility(_dicewareAppInstalled ? View.GONE : View.VISIBLE);
                _enterPasswordLayout.setVisibility(View.VISIBLE);
                break;
            case R.id.password_mechanism_enter:
                _dicewareInstalledLayout.setVisibility(View.GONE);
                _dicewareNotInstalledLayout.setVisibility(View.GONE);
                _enterPasswordLayout.setVisibility(View.VISIBLE);
                break;
            case R.id.password_mechanism_none:
                _dicewareInstalledLayout.setVisibility(View.GONE);
                _dicewareNotInstalledLayout.setVisibility(View.GONE);
                _enterPasswordLayout.setVisibility(View.GONE);
                break;
        }
    }

    public void installDiceware(View view) {
        final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + DICEWARE_PASSGEN_PACKAGE)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + DICEWARE_PASSGEN_PACKAGE)));
        }
    }

    public void generateDicewarePassword(View view) {
        PackageManager packageManager = getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(DICEWARE_PASSGEN_PACKAGE);

        // For some reason, the new task flag is being set, even though it's not in the
        // manifest.  Until we can figure out why and fix it, we manually unset it here.
        intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivityForResult(intent, ACTIVITY_DICEWARE_PASSWORD);
    }

    public void toggleShowPassword(View view) {
        CheckBox checkBox = (CheckBox) view;
        if (checkBox.isChecked()) {
            _passwordEditText.setTransformationMethod(null);
            _repeatPasswordEditText.setTransformationMethod(null);
        } else {
            _passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            _repeatPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        getPreferences(MODE_PRIVATE).edit().putBoolean(PREF_SHOW_PASSWORD, checkBox.isChecked()).commit();
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
    public void createKey(MenuItem menuItem) {

        boolean passwordProtected = _passwordMechanismDiceware.isChecked() || _passwordMechanismEnter.isChecked();

        // Ensure that we have passwords and that they match if we are password protecting the key.
        if( passwordProtected) {
            String password = _passwordEditText.getText().toString();
            String repeatPassword = _repeatPasswordEditText.getText().toString();
            if (password.length() < 1) {
                Snackbar.make(_coordinatorLayout, "Please enter a password", Snackbar.LENGTH_SHORT).show();
                return;
            }

            if (password.compareTo(repeatPassword) != 0) {
                Snackbar.make(_coordinatorLayout, "Passwords do not match", Snackbar.LENGTH_SHORT).show();
                return;
            }
        }

        // TODO - Hardcoded number.
        int secretKeyLength = (_secretKeyLengthSeekBar.getProgress() + 1) * 10;

        // TODO - This is not cryptographically secure.
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[secretKeyLength];
        secureRandom.nextBytes(key);

        byte[] keyFinal = null;

        // Encrypt the key if password-protected.

        if (passwordProtected) {
            String password = _passwordEditText.getText().toString();
            try {
                String salt = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                SecretKey secretKey = Crypto.getSecretKey(password, salt);
                keyFinal = Crypto.encryptToByteArray(secretKey, key);
            } catch (CryptoException e) {
                // TODO - Something smart.
                e.printStackTrace();
            }
        } else {
            keyFinal = key;
            // TODO - Clean up.
            Log.v(getClass().getName(), "Key string length: " + keyFinal.length);
        }

        // The key is in keyFinal.
        int colour = (int) findViewById(_selectedColour).getTag();

        SecretKeyDatabase.getInstance(this).insert(UUID.randomUUID().toString(), colour, _descriptionEditText.getText().toString(), keyFinal);

//        Intent resultIntent = new Intent();
//        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    // TODO - Hardcoded strings.
    private void validatePasswordFields() {
        String password = _passwordEditText.getText().toString();
        if (password.length() < 1) {
            // Validation failure.
            _passwordEditText.setError("Please enter a password");
        } else {
            _passwordEditText.setError(null);
        }

        String repeatPassword = _repeatPasswordEditText.getText().toString();
        if (password.compareTo(repeatPassword) != 0) {
            // Validation failure.
            _repeatPasswordEditText.setError("Passwords do not match");
        } else {
            _repeatPasswordEditText.setError(null);
        }
    }

}

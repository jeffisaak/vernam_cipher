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
import android.support.v4.content.ContextCompat;
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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.aptasystems.vernamcipher.database.SecretKeyDatabase;
import com.aptasystems.vernamcipher.util.Crypto;

import org.spongycastle.crypto.CryptoException;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

public class AddSecretKeyActivity extends AppCompatActivity {

    private static final String DICEWARE_PASSGEN_PACKAGE = "com.aptasystems.dicewarepasswordgenerator";
    private static final int ACTIVITY_DICEWARE_PASSWORD = 1;

    private static final String EXTRA_DICEWARE_PASSWORD_KEY = "password";

    private static final int DEFAULT_SECRET_KEY_LENGTH = 50;
    private static final int AVERAGE_EMAIL_LENGTH_BYTES = 300;
    private static final int KEY_LENGTH_THRESHOLD_BYTES_KB = 5000;
    private static final float BYTES_PER_KB = 1024f;

    private static final String STATE_DATA_SOURCE = "dataSource";
    private static final String STATE_PASSWORD_MECHANISM = "passwordMechanism";
    private static final String STATE_SECRET_KEY_LENGTH = "secretKeyLength";
    private static final String STATE_PASSWORD = "password";
    private static final String STATE_REPEAT_PASSWORD = "repeatPassword";
    private static final String STATE_SHOW_PASSWORD = "showPassword";
    private static final String STATE_DESCRIPTION = "description";
    private static final String STATE_COLOUR = "colour";

    private static final String PREF_DEFAULT_DATA_SOURCE = "defaultDataSource";
    private static final String PREF_DEFAULT_PASSWORD_MECHANISM = "defaultPasswordMechanism";
    private static final String PREF_DEFAULT_SECRET_KEY_LENGTH = "defaultSecretKeyLength";
    private static final String PREF_SHOW_PASSWORD = "defaultShowPassword";
    private static final String PREF_COLOUR = "defaultColour";

    // Widgets.
    private CoordinatorLayout _coordinatorLayout;
    private SeekBar _secretKeyLengthSeekBar;
    private RadioButton _androidPrngRadioButton;
    private RadioButton _randomOrgRadioButton;
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

    private GenerateRandomDataTask _generateRandomDataTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            _justRotated = true;
            if (_generateRandomDataTask != null && !_generateRandomDataTask.isCancelled()) {
                _generateRandomDataTask.cancel(true);
            }
        }

        setContentView(R.layout.activity_add_secret_key);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Grab widgets to use later.
        _coordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_coordinator);
        _secretKeyLengthSeekBar = (SeekBar) findViewById(R.id.seek_bar_secret_key_length);
        _androidPrngRadioButton = (RadioButton) findViewById(R.id.radio_android_prng);
        _randomOrgRadioButton = (RadioButton) findViewById(R.id.radio_random_org);
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
        _colourBlackRadioButton.setTag(ContextCompat.getColor(this, R.color.key_black));
        _colourRedRadioButton.setTag(ContextCompat.getColor(this, R.color.key_red));
        _colourGreenRadioButton.setTag(ContextCompat.getColor(this, R.color.key_green));
        _colourBlueRadioButton.setTag(ContextCompat.getColor(this, R.color.key_blue));
        _colourCyanRadioButton.setTag(ContextCompat.getColor(this, R.color.key_cyan));
        _colourOrangeRadioButton.setTag(ContextCompat.getColor(this, R.color.key_orange));
        _colourPurpleRadioButton.setTag(ContextCompat.getColor(this, R.color.key_purple));
        _colourGreyRadioButton.setTag(ContextCompat.getColor(this, R.color.key_grey));

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

        // Password validator.
        TextWatcher validatingTextWatcher = new TextWatcher() {
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
                validatePasswordFields();
            }
        };
        _passwordEditText.addTextChangedListener(validatingTextWatcher);
        _repeatPasswordEditText.addTextChangedListener(validatingTextWatcher);

        int dataSourceRadioButtonChecked = R.id.radio_android_prng;
        int mechanismRadioButtonChecked = R.id.password_mechanism_diceware;
        int secretKeyLength = DEFAULT_SECRET_KEY_LENGTH;
        boolean showPassword = false;
        int colourRadioButtonChecked = R.id.colour_black;
        if (savedInstanceState != null) {

            // Handle restoration from a saved instance state: which radio button is checked, secret key length, and show password.
            dataSourceRadioButtonChecked = savedInstanceState.getInt(STATE_DATA_SOURCE, dataSourceRadioButtonChecked);
            mechanismRadioButtonChecked = savedInstanceState.getInt(STATE_PASSWORD_MECHANISM, mechanismRadioButtonChecked);
            secretKeyLength = savedInstanceState.getInt(STATE_SECRET_KEY_LENGTH, secretKeyLength);
            showPassword = savedInstanceState.getBoolean(STATE_SHOW_PASSWORD, showPassword);
            colourRadioButtonChecked = savedInstanceState.getInt(STATE_COLOUR, colourRadioButtonChecked);

        } else {

            // If there was no saved instance state, attempt to get the default radio button
            // selection and secret key length from the preferences.
            dataSourceRadioButtonChecked = getPreferences(MODE_PRIVATE).getInt(PREF_DEFAULT_DATA_SOURCE, dataSourceRadioButtonChecked);
            mechanismRadioButtonChecked = getPreferences(MODE_PRIVATE).getInt(PREF_DEFAULT_PASSWORD_MECHANISM, mechanismRadioButtonChecked);
            secretKeyLength = getPreferences(MODE_PRIVATE).getInt(PREF_DEFAULT_SECRET_KEY_LENGTH, secretKeyLength);
            showPassword = getPreferences(MODE_PRIVATE).getBoolean(PREF_SHOW_PASSWORD, showPassword);
            colourRadioButtonChecked = getPreferences(MODE_PRIVATE).getInt(PREF_COLOUR, colourRadioButtonChecked);

        }

        // It is possible for the saved button ids to not match the buttons (due to app code changes).
        if (dataSourceRadioButtonChecked != R.id.radio_android_prng &&
                dataSourceRadioButtonChecked != R.id.radio_random_org) {
            dataSourceRadioButtonChecked = R.id.radio_android_prng;
        }
        if (mechanismRadioButtonChecked != R.id.password_mechanism_diceware &&
                mechanismRadioButtonChecked != R.id.password_mechanism_enter &&
                mechanismRadioButtonChecked != R.id.password_mechanism_none) {
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

        ((RadioButton) findViewById(dataSourceRadioButtonChecked)).setChecked(true);
        selectDataSource(dataSourceRadioButtonChecked);
        ((RadioButton) findViewById(mechanismRadioButtonChecked)).setChecked(true);
        selectPasswordMechanism(mechanismRadioButtonChecked);
        ((RadioButton) findViewById(colourRadioButtonChecked)).setChecked(true);
        selectColour(colourRadioButtonChecked);

        // Set the show password text box.
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

        // Perform an initial validate of the passwords.
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

    public void showHelpActivity(MenuItem menuItem) {
        Intent intent = new Intent(this, HelpActivity.class);
        intent.putExtra(HelpActivity.EXTRA_KEY_SOURCE, this.getClass().getName());
        startActivity(intent);
    }

    /**
     * Update the text that shows how many average-length emails a key is good for.
     */
    private void updateSecretKeyText() {

        int secretKeyLength = _secretKeyLengthSeekBar.getProgress() + 1;
        Resources res = getResources();
        int keyLengthBytes = secretKeyLength * AVERAGE_EMAIL_LENGTH_BYTES;
        String keyLengthString = null;
        if (keyLengthBytes < KEY_LENGTH_THRESHOLD_BYTES_KB) {
            keyLengthString = String.format(res.getString(R.string.key_length_content_2),
                    secretKeyLength,
                    NumberFormat.getIntegerInstance().format(keyLengthBytes),
                    res.getString(R.string.key_length_bytes));
        } else {
            keyLengthString = String.format(res.getString(R.string.key_length_content_2),
                    secretKeyLength,
                    NumberFormat.getIntegerInstance().format(keyLengthBytes / BYTES_PER_KB),
                    res.getString(R.string.key_length_kilobytes));
        }
        _secretKeyLengthInfo.setText(keyLengthString);
        _secretKeyLengthInfo.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the data source selection.
        int dataSourceRadioButtonChecked = 0;
        for (RadioButton radioButton : new RadioButton[]{_androidPrngRadioButton, _randomOrgRadioButton}) {
            if (radioButton.isChecked()) {
                dataSourceRadioButtonChecked = radioButton.getId();
                break;
            }
        }
        outState.putInt(STATE_DATA_SOURCE, dataSourceRadioButtonChecked);

        // Save the password mechanism selection.
        int mechanismRadioButtonChecked = 0;
        for (RadioButton radioButton : new RadioButton[]{_passwordMechanismDiceware, _passwordMechanismEnter, _passwordMechanismNone}) {
            if (radioButton.isChecked()) {
                mechanismRadioButtonChecked = radioButton.getId();
                break;
            }
        }
        outState.putInt(STATE_PASSWORD_MECHANISM, mechanismRadioButtonChecked);

        // Save the colour selection.
        int colourRadioButtonChecked = 0;
        for (RadioButton radioButton : new RadioButton[]{_colourBlackRadioButton, _colourRedRadioButton, _colourGreenRadioButton, _colourBlueRadioButton, _colourCyanRadioButton, _colourOrangeRadioButton, _colourPurpleRadioButton, _colourGreyRadioButton}) {
            if (radioButton.isChecked()) {
                colourRadioButtonChecked = radioButton.getId();
                break;
            }
        }
        outState.putInt(STATE_COLOUR, colourRadioButtonChecked);

        // Save the other stuff - key length, password, etc.
        outState.putInt(STATE_SECRET_KEY_LENGTH, _secretKeyLengthSeekBar.getProgress());
        outState.putString(STATE_PASSWORD, _passwordEditText.getText().toString());
        outState.putString(STATE_REPEAT_PASSWORD, _repeatPasswordEditText.getText().toString());
        outState.putBoolean(STATE_SHOW_PASSWORD, _showPasswordCheckBox.isChecked());
        outState.putString(STATE_DESCRIPTION, _descriptionEditText.getText().toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // If we've just come back from the diceware app, set the password text from the result.
        if (requestCode == ACTIVITY_DICEWARE_PASSWORD) {
            if (resultCode == Activity.RESULT_OK) {
                String password = data.getStringExtra(EXTRA_DICEWARE_PASSWORD_KEY);

                if (password != null) {
                    _passwordEditText.setText(password);
                    _repeatPasswordEditText.setText(password);
                }
            } else {
                // Noop.
            }
        }
    }

    public void selectDataSource(View view) {
        selectDataSource(view.getId());
    }

    private void selectDataSource(int viewId) {
        if (!_justRotated) {
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_DEFAULT_DATA_SOURCE, viewId).commit();
        }
    }

    public void selectPasswordMechanism(View view) {
        selectPasswordMechanism(view.getId());
    }

    private void selectPasswordMechanism(int viewId) {

        if (!_justRotated) {
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_DEFAULT_PASSWORD_MECHANISM, viewId).commit();
        }

        // Change the visibility of UI components based on the new selection.
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

    /**
     * Open the play store listing for the diceware password generator app.
     *
     * @param view
     */
    public void installDiceware(View view) {
        // Not really sure under which circumstances the market:// URL will fail, but there you go.
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + DICEWARE_PASSGEN_PACKAGE)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + DICEWARE_PASSGEN_PACKAGE)));
        }
    }

    /**
     * Launch the dieware password generation app to generate a password and return.
     *
     * @param view
     */
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
        RadioButton[] otherRadioButtons = new RadioButton[]{_colourBlackRadioButton,
                _colourRedRadioButton,
                _colourGreenRadioButton,
                _colourBlueRadioButton,
                _colourCyanRadioButton,
                _colourOrangeRadioButton,
                _colourPurpleRadioButton,
                _colourGreyRadioButton};
        for (RadioButton radioButton : otherRadioButtons) {
            if (radioButton.getId() != viewId) {
                radioButton.setChecked(false);
            }
        }

    }

    /**
     * Create the secret key using the selected options.
     *
     * @param menuItem
     */
    public void createKey(MenuItem menuItem) {

        boolean passwordProtected = _passwordMechanismDiceware.isChecked() || _passwordMechanismEnter.isChecked();

        // Ensure that we have passwords and that they match if we are password protecting the key.
        if (passwordProtected) {
            String password = _passwordEditText.getText().toString();
            String repeatPassword = _repeatPasswordEditText.getText().toString();
            if (password.length() < 1) {
                Snackbar.make(_coordinatorLayout, R.string.toast_please_enter_password, Snackbar.LENGTH_SHORT).show();
                return;
            }

            if (password.compareTo(repeatPassword) != 0) {
                Snackbar.make(_coordinatorLayout, R.string.toast_passwords_do_not_match, Snackbar.LENGTH_SHORT).show();
                return;
            }
        }

        int secretKeyLength = (_secretKeyLengthSeekBar.getProgress() + 1) * AVERAGE_EMAIL_LENGTH_BYTES;

        // If the Android PRNG is selected:
        if (_androidPrngRadioButton.isChecked()) {
            generateAndroidRandomData(secretKeyLength);
        } else if (_randomOrgRadioButton.isChecked()) {
            checkQuotaAndGenerateRandomOrgData(secretKeyLength);
        }
    }

    /**
     * Generate the random data for the secret key using the Android PRNG.
     *
     * @param length
     */
    private void generateAndroidRandomData(final int length) {

        if (_generateRandomDataTask != null && !_generateRandomDataTask.isCancelled()) {
            _generateRandomDataTask.cancel(true);
        }

        _generateRandomDataTask =
                new GenerateRandomDataTask(this) {
                    @Override
                    public void generateRandomNumbers(int count) {

                        SecureRandom secureRandom = new SecureRandom();
                        byte[] key = new byte[count];
                        secureRandom.nextBytes(key);

                        for (byte keyEntry : key) {
                            String hexValue = String.format("%02X", keyEntry);
                            _hexStringQueue.offer(hexValue);
                        }
                    }

                    @Override
                    protected void onPostExecute(byte[] byteArray) {
                        if (!isCancelled() && byteArray != null) {
                            finishCreateKey(byteArray);
                        } else if (byteArray == null) {
                            Snackbar.make(_coordinatorLayout, R.string.toast_secret_key_generation_failed, Snackbar.LENGTH_LONG).show();
                        }
                        super.onPostExecute(byteArray);
                    }
                };
        _generateRandomDataTask.execute(length);
    }

    /**
     * Check the quota at and use random.org to generate the random data for the secret key.
     *
     * @param length
     */
    private void checkQuotaAndGenerateRandomOrgData(final int length) {

        CheckRandomOrgQuotaTask task = new CheckRandomOrgQuotaTask(this) {
            @Override
            protected void onPostExecute(Integer quotaRemaining) {

                if (!isCancelled() &&
                        _success &&
                        _quotaRemaining != null &&
                        quotaRemaining >= length) {

                    // If the task wasn't cancelled and successfully got the remaining quota, and the remaining quota
                    // is greater than the amount of data we need, go get it.
                    generateRandomOrgRandomData(length);

                } else if (!_success) {
                    // If the task was unsuccessful, we couldn't discover the quota.
                    Snackbar.make(_coordinatorLayout, R.string.toast_get_quota_failed, Snackbar.LENGTH_LONG).show();
                } else if (_quotaRemaining < length) {
                    // If we got the quota, but we've used it all up, show an error message and let the user
                    // open the random.org quota page.
                    Snackbar.make(_coordinatorLayout, R.string.toast_quota_used, Snackbar.LENGTH_LONG)
                            .setAction(R.string.random_org_quota_details, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_random_org_quota)));
                                    startActivity(browserIntent);
                                }
                            }).show();
                }

                super.onPostExecute(quotaRemaining);
            }
        };
        task.execute(length);
    }

    private void generateRandomOrgRandomData(final int length) {

        GenerateRandomDataTask task =
                new GenerateRandomDataTask(this) {
                    @Override
                    public void generateRandomNumbers(int count) {
                        Resources res = AddSecretKeyActivity.this.getResources();

                        // random.org only allows us to fetch up to 10,000 values at a time.  Let's play it safe and only get 5,000.
                        // Because we're asynchronous, we just queue up all the requests necessary to get all the random data we want.
                        int fetchedCount = 0;
                        int valuesInFetch = 5000;
                        while (fetchedCount < count) {
                            int countToFetch = Math.min(valuesInFetch, count - fetchedCount);
                            String url = String.format(res.getString(R.string.random_org_url), countToFetch);
                            fetchedCount += countToFetch;

                            RequestQueue queue = Volley.newRequestQueue(AddSecretKeyActivity.this);

                            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            String[] tokens = response.split("\n");
                                            for (String token : tokens) {
                                                _hexStringQueue.offer(token);
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    // This gets set if any (or all) of the requests fail. If this happens,
                                    // we treat the whole batch of fetches as having failed.
                                    _success = false;
                                    _errorMessage = new String(error.networkResponse.data);
                                }
                            }) {
                                @Override
                                public Map<String, String> getHeaders() throws AuthFailureError {
                                    // random.org has been responding with 503s if there is no user agent set here.
                                    Map<String, String> headers = new HashMap<>();
                                    headers.put("User-Agent", "volley/0");
                                    return headers;
                                }
                            };
                            queue.add(stringRequest);
                        }
                    }

                    @Override
                    protected void onPostExecute(byte[] byteArray) {
                        if (!isCancelled() && byteArray != null && _success) {
                            finishCreateKey(byteArray);
                        } else if (!_success) {
                            Resources res = AddSecretKeyActivity.this.getResources();
                            String errorMessage = String.format(res.getString(R.string.random_org_error), _errorMessage);
                            Snackbar.make(_coordinatorLayout, errorMessage, Snackbar.LENGTH_INDEFINITE).show();
                        }
                        super.onPostExecute(byteArray);
                    }
                };
        task.execute(length);
    }

    private void finishCreateKey(byte[] key) {
        byte[] keyFinal = null;

        // Encrypt the key if password-protected.
        boolean passwordProtected = _passwordMechanismDiceware.isChecked() || _passwordMechanismEnter.isChecked();
        if (passwordProtected) {
            String password = _passwordEditText.getText().toString();
            String salt = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            try {
                keyFinal = Crypto.encryptToByteArray(password, salt, key);
            } catch (CryptoException e) {
                // This really shouldn't happen...
                Snackbar.make(_coordinatorLayout, R.string.error_secret_key_encryption_failed, Snackbar.LENGTH_SHORT).show();
                return;
            }
        } else {
            // Not password-protected.
            keyFinal = key;
        }

        int colour = (int) findViewById(_selectedColour).getTag();

        SecretKeyDatabase.getInstance(this).insert(UUID.randomUUID().toString(), colour, _descriptionEditText.getText().toString(), keyFinal);

        finish();
    }

    private void validatePasswordFields() {
        String password = _passwordEditText.getText().toString();
        if (password.length() < 1) {
            _passwordEditText.setError(getString(R.string.error_enter_password));
        } else {
            _passwordEditText.setError(null);
        }

        String repeatPassword = _repeatPasswordEditText.getText().toString();
        if (password.compareTo(repeatPassword) != 0) {
            _repeatPasswordEditText.setError(getString(R.string.error_password_mismatch));
        } else {
            _repeatPasswordEditText.setError(null);
        }
    }

}

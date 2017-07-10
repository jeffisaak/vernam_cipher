package com.aptasystems.vernamcipher;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;

import java.nio.charset.Charset;

/**
 * Task to check our quota at random.org.
 */
public abstract class PrepareViewableSecretKeyTask extends AsyncTask<Object, Void, String> {

    private Context _context;
    private ProgressDialog _progressDialog;

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    public PrepareViewableSecretKeyTask(Context context) {
        _context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        _progressDialog = new ProgressDialog(_context);
        _progressDialog.setMessage(_context.getString(R.string.progress_dialog_formatting_secret_key));
        _progressDialog.setCancelable(false);
        _progressDialog.show();
    }

    @Override
    protected String doInBackground(Object... params) {

        if (isCancelled()) {
            return null;
        }

        // Params expected:
        // 0 - Key data
        // 1 - Desired output encoding (OutputEncoding)
        byte[] keyData = (byte[]) params[0];
        OutputEncoding outputEncoding = (OutputEncoding) params[1];

        switch (outputEncoding) {
            case BASE64:
                String base64EncodedString = null;
                if (keyData != null) {
                    base64EncodedString = new String(Base64.encode(keyData, Base64.DEFAULT), UTF8_CHARSET);
                }
                return base64EncodedString;
            case HEXADECIMAL:
                StringBuilder hexStringBuilder = new StringBuilder();
                if (keyData != null) {
                    for (byte byteInKeyData : keyData) {
                        String hexValue = String.format("%02X", byteInKeyData);
                        hexStringBuilder.append(hexValue).append(" ");
                    }
                }
                return hexStringBuilder.toString();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String encodedKey) {
        if (_progressDialog != null && _progressDialog.isShowing()) {
            _progressDialog.dismiss();
        }
        super.onPostExecute(encodedKey);
    }

    public enum OutputEncoding {BASE64, HEXADECIMAL}
}

package com.aptasystems.vernamcipher;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * Task to check our quota at random.org.
 */
public abstract class CheckRandomOrgQuotaTask extends AsyncTask<Integer, Void, Integer> {

    private static final int LOOP_SLEEP_TIME_MS = 50;

    private Context _context;
    protected boolean _success = true;
    private boolean _complete = false;
    private ProgressDialog _progressDialog;
    protected Integer _quotaRemaining;

    public CheckRandomOrgQuotaTask(Context context) {
        _context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        _progressDialog = new ProgressDialog(_context);
        _progressDialog.setMessage(_context.getString(R.string.progress_dialog_checking_quota));
        _progressDialog.setCancelable(false);
        _progressDialog.show();
    }

    @Override
    protected Integer doInBackground(Integer... params) {

        if (isCancelled()) {
            return null;
        }

        // Fire off the volley request and wait for it to complete.
        Resources res = _context.getResources();
        String url = res.getString(R.string.random_org_quota_url);
        RequestQueue queue = Volley.newRequestQueue(_context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        _quotaRemaining = Integer.parseInt(response.trim());
                        _complete = true;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                _success = false;
                _complete = true;
            }
        });
        queue.add(stringRequest);

        // Wait for the volley request to complete.
        while (!_complete) {
            try {
                Thread.sleep(LOOP_SLEEP_TIME_MS);

                if (isCancelled() || !_success) {
                    return null;
                }

            } catch (InterruptedException e) {
                // This is probably because we tried to cancel the task.
                return null;
            }
        }

        if (isCancelled()) {
            return null;
        }

        return _quotaRemaining;
    }

    @Override
    protected void onPostExecute(Integer quotaRemaining) {
        if (_progressDialog != null && _progressDialog.isShowing()) {
            _progressDialog.dismiss();
        }
        super.onPostExecute(quotaRemaining);
    }
}

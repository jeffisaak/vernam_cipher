package com.aptasystems.vernamcipher;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Task to generate random data.  The generateRandomData() abstract method must be implemented in
 * subclasses.  This method must feed random data to the task using the hex string queue queue using
 * _hexStringQueue.offer().
 */
public abstract class GenerateRandomDataTask extends AsyncTask<Integer, Void, byte[]> {

    private static final int LOOP_SLEEP_TIME_MS = 50;

    private Context _context;
    protected Queue<String> _hexStringQueue;
    protected boolean _success = true;
    protected String _errorMessage = null;
    private ProgressDialog _progressDialog;

    public GenerateRandomDataTask(Context context) {
        _context = context;
        _hexStringQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        _progressDialog = new ProgressDialog(_context);
        _progressDialog.setMessage(_context.getString(R.string.progress_generating_secret_key));
        _progressDialog.setCancelable(false);
        _progressDialog.show();
    }

    @Override
    protected byte[] doInBackground(Integer... params) {

        int dataCount = params[0];
        generateRandomNumbers(dataCount);

        if (isCancelled()) {
            return null;
        }

        // Wait for the queue to fill.
        while (_hexStringQueue.size() < dataCount) {
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

        // Generate the random data.
        ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
        for (String hexString : _hexStringQueue) {
            byte byteValue = (byte) (Integer.parseInt(hexString, 16) & 0xff);
            resultStream.write(byteValue);
        }
        return resultStream.toByteArray();
    }

    @Override
    protected void onPostExecute(byte[] bytes) {
        if( _progressDialog != null && _progressDialog.isShowing()) {
            _progressDialog.dismiss();
        }
        super.onPostExecute(bytes);
    }

    /**
     * Generate random numbers.  This method must offer random numbers to _numberQueue.  Once the
     * queue is full the doInBackground() method will pull them out of the queue and generate the
     * password.  This mechanism allows for asynchronous random number generation (for example,
     * fetching random data from random.org).
     *
     * @param count
     */
    public abstract void generateRandomNumbers(int count);
}

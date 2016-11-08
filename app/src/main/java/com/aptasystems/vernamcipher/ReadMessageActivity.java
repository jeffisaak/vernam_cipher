package com.aptasystems.vernamcipher;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.aptasystems.vernamcipher.database.MessageDatabase;
import com.aptasystems.vernamcipher.model.Message;

public class ReadMessageActivity extends AppCompatActivity {

    private TextView _contentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // TODO - set home up enabled but only if we're not coming from the decrypt...

        _contentView = (TextView) findViewById(R.id.message_content);

        long messageId = getIntent().getLongExtra("messageId", 0L);
        Message message = MessageDatabase.getInstance(this).fetch(messageId);
        _contentView.setText(message.getContent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_read_message, menu);
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

    public void shareMessage(MenuItem menuItem) {
        Intent shareIntent = buildShareIntent();
        startActivity(shareIntent);
    }

    private Intent buildShareIntent() {
        Intent result = new Intent();
        result.setAction(Intent.ACTION_SEND);
        result.putExtra(Intent.EXTRA_TEXT, _contentView.getText().toString());
        result.setType("text/plain");
        return result;
    }

    public void showHelp(MenuItem menuItem) {
    }

}

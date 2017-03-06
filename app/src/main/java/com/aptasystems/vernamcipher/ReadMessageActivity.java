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
import com.aptasystems.vernamcipher.util.ShareUtil;

public class ReadMessageActivity extends AppCompatActivity {

    public static final String EXTRA_KEY_MESSAGE_ID = "messageId";
    public static final String EXTRA_KEY_MESSAGE = "message";

    private TextView _contentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        _contentView = (TextView) findViewById(R.id.message_content);

        long messageId = getIntent().getLongExtra(EXTRA_KEY_MESSAGE_ID, 0L);
        Message message = null;
        if (messageId == 0L) {
            message = (Message) getIntent().getSerializableExtra(EXTRA_KEY_MESSAGE);
        } else {
            message = MessageDatabase.getInstance(this).fetch(messageId);
        }
        _contentView.setText(message.getContent());
        if( message.isIncoming())
        {
            setTitle(R.string.title_activity_read_received_message);
        } else {
            setTitle(R.string.title_activity_read_sent_message);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_read_message, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        long messageId = getIntent().getLongExtra(EXTRA_KEY_MESSAGE_ID, 0L);
        menu.findItem(R.id.action_save_message).setVisible(messageId == 0L);
        return super.onPrepareOptionsMenu(menu);
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
        Intent shareIntent = ShareUtil.buildShareIntent( _contentView.getText().toString());
        Intent chooserIntent = Intent.createChooser(shareIntent, "Share decrypted message");
        startActivity(chooserIntent);
    }

    public void showHelpActivity(MenuItem menuItem) {
        Intent intent = new Intent(this, HelpActivity.class);
        intent.putExtra(HelpActivity.EXTRA_KEY_SOURCE, this.getClass().getName());
        startActivity(intent);
    }

    public void saveMessage(MenuItem menuItem) {
        // Store the message in the database, then put the message id in the extra.
        Message message = (Message) getIntent().getSerializableExtra(EXTRA_KEY_MESSAGE);
        long messageId = MessageDatabase.getInstance(this).insert(true, message.getContent());
        getIntent().putExtra(EXTRA_KEY_MESSAGE_ID, messageId);
        invalidateOptionsMenu();
    }

}

package com.aptasystems.vernamcipher;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

public class HelpActivity extends AppCompatActivity {

    public static final String EXTRA_KEY_SOURCE = "source";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String source = getIntent().getStringExtra(EXTRA_KEY_SOURCE);
        if( source.compareTo(MainActivity.class.getName()) == 0 ) {
            setContentView(R.layout.activity_help_main);
        } else if( source.compareTo(WriteMessageActivity.class.getName()) == 0 )
        {
            setContentView(R.layout.activity_help_write_message);
        } else if( source.compareTo(ReadMessageActivity.class.getName()) == 0 )
        {
            setContentView(R.layout.activity_help_read_message);
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
}

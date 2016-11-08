package com.aptasystems.vernamcipher;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.aptasystems.vernamcipher.database.SecretKeyDatabase;
import com.aptasystems.vernamcipher.model.SecretKey;
import com.aptasystems.vernamcipher.util.PRNGFixes;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.Serializable;
import java.security.Security;

public class MainActivity extends AppCompatActivity {

    private ListView _listView;
    private SecretKeyListAdapter _listAdapter;

    static {
        PRNGFixes.apply();
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        _listView = (ListView) findViewById(R.id.list_view_secret_keys);
        _listAdapter = new SecretKeyListAdapter(this);
        _listView.setAdapter(_listAdapter);
        _listView.setMultiChoiceModeListener(new SecretKeyListChoiceListener(_listView));
        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SecretKey selectedKey = (SecretKey) _listView.getItemAtPosition(position);
                SecretKey keyWithData = SecretKeyDatabase.getInstance(MainActivity.this).fetch(selectedKey.getId(), true);

                Intent intent = new Intent(MainActivity.this, WriteMessageActivity.class);
                // TODO - Hardcoded.
                intent.putExtra("secretKey", (Serializable) keyWithData);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {

        // Ensure the list view has the most recent data.
        _listAdapter.refresh();

        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void showInfoActivity(MenuItem menuItem) {
        Intent intent = new Intent(this, InfoActivity.class);
        startActivity(intent);
    }

    public void showHelpActivity(MenuItem menuItem) {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    public void addSecretKey(View view) {
        Intent intent = new Intent(this, AddSecretKeyActivity.class);
        startActivity(intent);
    }


}

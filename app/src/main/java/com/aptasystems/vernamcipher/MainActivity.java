package com.aptasystems.vernamcipher;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.aptasystems.vernamcipher.util.FileManager;
import com.aptasystems.vernamcipher.util.PRNGFixes;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public class MainActivity extends AppCompatActivity {

    private CoordinatorLayout _coordinatorLayout;
    private FloatingActionsMenu _floatingActionsMenu;
    private ViewPager _viewPager;
    private SectionsPagerAdapter _pagerAdapter;

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

        _coordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_coordinator);
        _floatingActionsMenu = (FloatingActionsMenu) findViewById(R.id.floating_menu);

        // Create the adapter that will return fragments.
        _pagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        _viewPager = (ViewPager) findViewById(R.id.view_pager);
        _viewPager.setAdapter(_pagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(_viewPager);

        FileManager.StorageState storageState = FileManager.getInstance(this).getExternalStorageState();
        if (storageState != FileManager.StorageState.WRITABLE) {
            MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(this)
                    .title(R.string.external_storage_alert_title);
            if (storageState == FileManager.StorageState.READ_ONLY) {
                dialogBuilder.content(R.string.external_storage_alert_read_only_text);
            } else if (storageState == FileManager.StorageState.NOT_AVAILABLE) {
                dialogBuilder.content(R.string.external_storage_alert_not_available_text);
            }
            MaterialDialog dialog = dialogBuilder
                    .positiveText(android.R.string.ok)
                    .build();
            dialog.show();
        }
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
        intent.putExtra(HelpActivity.EXTRA_KEY_SOURCE, this.getClass().getName());
        startActivity(intent);
    }

    public void addSecretKey(View view) {
        _floatingActionsMenu.collapse();
        Intent intent = new Intent(this, AddSecretKeyActivity.class);
        startActivity(intent);
    }

    public void writeMessage(View view) {
        _floatingActionsMenu.collapse();
        _viewPager.setCurrentItem(SectionsPagerAdapter.SECRET_KEY_LIST_PAGE);
        Snackbar.make(_coordinatorLayout, R.string.snack_new_message, Snackbar.LENGTH_LONG).show();
    }

    public void decryptClipboard(View view)
    {
        _floatingActionsMenu.collapse();

        // Ensure that there is something on the clipboard that we can decrypt.
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = clipboardManager.getPrimaryClip();
        String clipText = null;
        for( int ii=0; ii<clipData.getItemCount(); ii++)
        {
            ClipData.Item item = clipData.getItemAt(ii);
            if( item.getText() != null )
            {
                clipText = item.getText().toString();
                break;
            }
        }

        if( clipText == null )
        {
            // TODO - Show a snack and return or something.
        }

        Intent intent = new Intent(Intent.ACTION_SEND, null, this, DecryptMessageActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT, clipText);
        startActivity(intent);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the
     * sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public static final int SECRET_KEY_LIST_PAGE = 0;
        private static final int MESSAGE_LIST_PAGE = 1;
        private static final int PAGE_COUNT = 2;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case SECRET_KEY_LIST_PAGE:
                    return SecretKeyListFragment.newInstance();
                case MESSAGE_LIST_PAGE:
                    return MessagesFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case SECRET_KEY_LIST_PAGE:
                    return getResources().getString(R.string.page_title_secret_keys);
                case MESSAGE_LIST_PAGE:
                    return getResources().getString(R.string.page_title_messages);
            }
            return null;
        }
    }

}

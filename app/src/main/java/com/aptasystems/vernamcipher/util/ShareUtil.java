package com.aptasystems.vernamcipher.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.aptasystems.vernamcipher.BuildConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jisaak on 2016-11-07.
 */

public class ShareUtil {

    private static final String SHARE_BINARY_MIME_TYPE = "application/octet-stream";
    private static final String SHARE_TEXT_MIME_TYPE = "text/plain";

    private ShareUtil() {
        // Prevents instantiation.
    }

    public static Intent buildShareIntent(Context context, File file) {
        List<File> files = new ArrayList<>();
        files.add(file);

        return buildShareIntent(context, files);
    }

    public static Intent buildShareIntent(Context context, List<File> files) {

        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        ArrayList<Uri> uris = new ArrayList<Uri>();
        for (File file : files) {
            Uri uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
            uris.add(uri);
        }
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.setType(SHARE_BINARY_MIME_TYPE);
        return shareIntent;
    }

    public static Intent buildShareIntent(String message) {
        Intent result = new Intent();
        result.setAction(Intent.ACTION_SEND);
        result.putExtra(Intent.EXTRA_TEXT, message);
        result.setType(SHARE_TEXT_MIME_TYPE);
        return result;
    }


}

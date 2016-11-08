package com.aptasystems.vernamcipher.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.UUID;

/**
 * Created by jisaak on 2015-11-24.
 */
public class FileManager {

    private static FileManager _instance;

    public static FileManager getInstance(Context context) {
        if (_instance == null) {
            _instance = new FileManager(context);
        }
        return _instance;
    }

    private File _tempFolder;

    private FileManager(Context context) {
        // TODO - Constants.
        _tempFolder = new File(context.getExternalFilesDir(null), "temp");
    }

    public File newTempFile(String name) {
        if (!_tempFolder.exists()) {
            _tempFolder.mkdirs();
        }
        File result = new File(_tempFolder, name);
        Log.v(getClass().getSimpleName(), "" + result.exists());
        return result;
    }

    public File newTempFile() {
        return newTempFile(UUID.randomUUID().toString());
    }

}

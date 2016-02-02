/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.fenneclite;

import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoThread;
import org.mozilla.gecko.util.ThreadUtils;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Browser extends AppCompatActivity implements Handler.Callback {

    private static final String APPOMNI_JAR = "appomni.jar";
    private static final String PREF_APPOMNI_VERSION = "appomni_version";

    private static final int MSG_SETUP_GECKOVIEW = 1;

    /* package */ final Handler handler = new Handler(this);

    /* package */ void unpackOmniJar() {
        final AssetManager am = getAssets();
        InputStream jarin = null;
        FileOutputStream jarout = null;
        try {
            try {
                int bytesRead;
                final byte[] buffer = new byte[0x1000];
                jarin = am.open(APPOMNI_JAR);
                jarout = openFileOutput(APPOMNI_JAR, MODE_PRIVATE);
                while ((bytesRead = jarin.read(buffer)) >= 0) {
                    jarout.write(buffer, 0, bytesRead);
                }
            } finally {
                if (jarin != null) {
                    jarin.close();
                }
                if (jarout != null) {
                    jarout.close();
                }
            }
        } catch (IOException e) {
            throw new UnsupportedOperationException("cannot unpack omnijar", e);
        }
    }

    private void setupGeckoView() {
        // Launch Gecko and inflate GeckoView.
        GeckoAppShell.setApplicationContext(getApplicationContext());

        final String appomni_path = getFileStreamPath(APPOMNI_JAR).getAbsolutePath();
        final String args = "-appomni " + appomni_path;
        if (GeckoThread.ensureInit(args, null)) {
            GeckoThread.launch();
        }

        final ViewGroup root = (ViewGroup) findViewById(R.id.contentRoot);
        getLayoutInflater().inflate(
                R.layout.content_browser, root, /* attachToRoot */ true);
    }

    @Override
    public boolean handleMessage(final Message msg) {
        switch (msg.what) {
            case MSG_SETUP_GECKOVIEW:
                setupGeckoView();
                return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // If we don't have an unpacked omnijar, we have to unpack it on a background thread first
        // before passing its path to Gecko.
        final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        if (!BuildConfig.DEBUG && prefs.getInt(PREF_APPOMNI_VERSION, -1) != BuildConfig.VERSION_CODE) {
            setupGeckoView();
        } else {
            ThreadUtils.postToBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    unpackOmniJar();
                    prefs.edit().putInt(PREF_APPOMNI_VERSION, BuildConfig.VERSION_CODE).apply();
                    handler.sendEmptyMessage(MSG_SETUP_GECKOVIEW);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_browser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

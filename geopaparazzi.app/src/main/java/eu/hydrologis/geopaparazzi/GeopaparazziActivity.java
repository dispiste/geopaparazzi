// MainActivity.java
// Hosts the GeopaparazziActivityFragment on a phone and both the
// GeopaparazziActivityFragment and SettingsActivityFragment on a tablet
package eu.hydrologis.geopaparazzi;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.SimplePosition;
import eu.geopaparazzi.library.util.UrlUtilities;
import eu.geopaparazzi.mapsforge.BaseMapSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.mapview.MapviewActivity;
import eu.hydrologis.geopaparazzi.utilities.IApplicationChangeListener;
import eu.hydrologis.geopaparazzi.ui.fragments.GeopaparazziActivityFragment;
import eu.geopaparazzi.library.permissions.IChainedPermissionHelper;
import eu.geopaparazzi.library.permissions.PermissionWriteStorage;

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_DATABASE_TO_LOAD;

/**
 * Main activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopaparazziActivity extends AppCompatActivity implements IApplicationChangeListener {
    private IChainedPermissionHelper permissionHelper = new PermissionWriteStorage();
    private GeopaparazziActivityFragment geopaparazziActivityFragment;

    // CMI
    public static final String ACTION_PICK_PLUGIN = "androidsrc.intent.action.PICK_PLUGIN";
    static final String KEY_PKG = "pkg";
    static final String KEY_SERVICENAME = "servicename";
    static final String KEY_ACTIONS = "actions";
    static final String KEY_CATEGORIES = "categories";
    static final String BUNDLE_EXTRAS_CATEGORY = "category";

    static final String LOG_TAG = "PluginApp";

    // configure the GeopaparazziActivity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_geopaparazzi);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkIncomingProject();

        if (Build.VERSION.SDK_INT >= 23) {
            // PERMISSIONS START
            if (permissionHelper.hasPermission(this) && permissionHelper.getNextWithoutPermission(this) == null) {
                init();

                checkIncomingUrl();
                checkAvailableProfiles();
            } else {
                if (permissionHelper.hasPermission(this)) {
                    permissionHelper = permissionHelper.getNextWithoutPermission(this);
                }
                permissionHelper.requestPermission(this);
            }
            // PERMISSIONS STOP
        } else {
            init();

            checkIncomingUrl();
            checkAvailableProfiles();
        }
        initPlugins();
    }

    private void initPlugins() {
        ArrayList<HashMap<String, String>> services = new ArrayList<HashMap<String, String>>();
        ArrayList<String> categories = new ArrayList<String>();

        PackageManager packageManager = getPackageManager();
        //String category = "androidsrc.intent.category.ADD_PLUGIN";
        Intent baseIntent = new Intent("androidsrc.intent.action.PICK_PLUGIN");
        baseIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
        List<ResolveInfo> list = packageManager.queryIntentServices(baseIntent,
                PackageManager.GET_RESOLVED_FILTER);
        for (int i = 0; i < list.size(); ++i) {
            ResolveInfo info = list.get(i);
            ServiceInfo sinfo = info.serviceInfo;
            IntentFilter filter = info.filter;
            Log.d(LOG_TAG, "fillPluginList: i: " + i + "; sinfo: " + sinfo + ";filter: " + filter);
            if (sinfo != null) {
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(KEY_PKG, sinfo.packageName);
                item.put(KEY_SERVICENAME, sinfo.name);
                String firstCategory = null;
                if (filter != null) {
                    StringBuilder actions = new StringBuilder();
                    for (Iterator<String> actionIterator = filter.actionsIterator(); actionIterator.hasNext(); ) {
                        String action = actionIterator.next();
                        if (actions.length() > 0)
                            actions.append(",");
                        actions.append(action);
                    }
                    StringBuilder categoryList = new StringBuilder();
                    for (Iterator<String> categoryIterator = filter.categoriesIterator();
                         categoryIterator.hasNext(); ) {
                        String category = categoryIterator.next();
                        if (firstCategory == null)
                            firstCategory = category;
                        if (categoryList.length() > 0)
                            categoryList.append(",");
                        categoryList.append(category);
                    }
                    item.put(KEY_ACTIONS, new String(actions));
                    item.put(KEY_CATEGORIES, new String(categoryList));
                } else {
                    item.put(KEY_ACTIONS, "<null>");
                    item.put(KEY_CATEGORIES, "<null>");
                }
                if (firstCategory == null)
                    firstCategory = "";
                categories.add(firstCategory);
                services.add(item);
            }
        }
        Log.d(LOG_TAG, "services: " + services);
        Log.d(LOG_TAG, "categories: " + categories);
    }

    private void init() {

        // set default values in the app's SharedPreferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        geopaparazziActivityFragment = new GeopaparazziActivityFragment();
        FragmentTransaction transaction =
                getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, geopaparazziActivityFragment);
        transaction.commitAllowingStateLoss();
    }

    private void checkIncomingProject() {
        Uri data = getIntent().getData();
        if (data != null) {
            String path = data.getEncodedPath();
            if (path.endsWith(LibraryConstants.GEOPAPARAZZI_DB_EXTENSION)) {
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(PREFS_KEY_DATABASE_TO_LOAD, path);
                editor.apply();
            }
        }
    }

    private void checkAvailableProfiles() {
        try {
            ProfilesHandler.INSTANCE.checkActiveProfile(getContentResolver());

            BaseMapSourcesManager.INSTANCE.forceBasemapsreRead();
            SpatialiteSourcesManager.INSTANCE.forceSpatialitemapsreRead();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void checkIncomingUrl() {
        Uri data = getIntent().getData();
        if (data != null) {
            final String path = data.toString();
            // try osm
            final SimplePosition simplePosition = UrlUtilities.getLatLonTextFromOsmUrl(path);
            if (simplePosition.latitude != null) {
                GPDialogs.yesNoMessageDialog(this, getString(R.string.import_bookmark_prompt), new Runnable() {
                    @Override
                    public void run() {
                        GeopaparazziActivity activity = GeopaparazziActivity.this;
                        try {
                            DaoBookmarks.addBookmark(simplePosition.longitude, simplePosition.latitude, simplePosition.text, simplePosition.zoomLevel, -1, -1, -1, -1);
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                            PositionUtilities.putMapCenterInPreferences(preferences, simplePosition.longitude, simplePosition.latitude, 16);
                            Intent mapIntent = new Intent(activity, MapviewActivity.class);
                            startActivity(mapIntent);
                        } catch (IOException e) {
                            GPLog.error(this, "Error parsing URI: " + path, e); //$NON-NLS-1$
                            GPDialogs
                                    .warningDialog(
                                            activity,
                                            "Unable to parse the url: " + path,
                                            null);
                        }
                    }
                }, null);

            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (permissionHelper.hasGainedPermission(requestCode, grantResults)) {
            IChainedPermissionHelper nextWithoutPermission = permissionHelper.getNextWithoutPermission(this);
            permissionHelper = nextWithoutPermission;
            if (permissionHelper == null) {
                init();
            } else {
                permissionHelper.requestPermission(this);
            }

        } else {
            GPDialogs.infoDialog(this, getString(R.string.premissions_cant_start) + permissionHelper.getDescription(), new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }
    }

    // called after onCreate completes execution
    @Override
    protected void onStart() {
        super.onStart();
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // force to exit through the exit button
        // System.out.println(keyCode + "/" + KeyEvent.KEYCODE_BACK);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onApplicationNeedsRestart() {

        if (geopaparazziActivityFragment != null && geopaparazziActivityFragment.getGpsServiceBroadcastReceiver() != null) {
            GpsServiceUtilities.stopDatabaseLogging(this);
            GpsServiceUtilities.stopGpsService(this);
            GpsServiceUtilities.unregisterFromBroadcasts(this, geopaparazziActivityFragment.getGpsServiceBroadcastReceiver());
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 11) {
                    recreate();
                } else {
                    Intent intent = getIntent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    finish();
                    overridePendingTransition(0, 0);

                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
            }
        }, 10);
    }

    @Override
    public void onAppIsShuttingDown() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putFloat(MapviewActivity.MAPSCALE_X, 1);
        edit.putFloat(MapviewActivity.MAPSCALE_Y, 1);
        edit.apply();


        GpsServiceUtilities.stopDatabaseLogging(this);
        GpsServiceUtilities.stopGpsService(this);

        TagsManager.reset();
        GeopaparazziApplication.reset();
    }

}

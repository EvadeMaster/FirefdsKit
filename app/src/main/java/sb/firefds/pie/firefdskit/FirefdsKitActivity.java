/*
 * Copyright (C) 2016 Mohamed Karami for XTouchWiz Project (Wanam@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sb.firefds.pie.firefdskit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.topjohnwu.superuser.Shell;

import androidx.preference.SwitchPreference;
import de.robv.android.xposed.XposedBridge;
import sb.firefds.pie.firefdskit.adapters.BackupAdapter;
import sb.firefds.pie.firefdskit.notifications.RebootNotification;
import sb.firefds.pie.firefdskit.utils.Constants;
import sb.firefds.pie.firefdskit.utils.Utils;

import static sb.firefds.pie.firefdskit.utils.Constants.PREFS;
import static sb.firefds.pie.firefdskit.utils.Preferences.*;

public class FirefdsKitActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static RelativeLayout mLayout;
    private static TextView progressBarText;
    private AlertDialog dialog;
    private File dir;
    private File[] backups;
    private ListView listView;

    static {
        Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR);
        Shell.Config.verboseLogging(BuildConfig.DEBUG);
    }

    private static final String[] defaultSettings = MainApplication
            .getAppContext()
            .getResources()
            .getStringArray(R.array.default_settings);

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Utils.setOmcEncryptedFlag();
        fixAppPermissions(this);
        verifyStoragePermissions(this);

        setContentView(R.layout.firefds_main);
        mLayout = findViewById(R.id.mainLayout);
        progressBarText = findViewById(R.id.progressBarText);

        try {
            MainApplication.setWindowsSize(new Point());
            getWindowManager().getDefaultDisplay().getSize(MainApplication.getWindowsSize());

            if (savedInstanceState == null)
                getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
                        new SettingsFragment()).commit();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (!(getVisibleFragment() instanceof SettingsFragment)) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setHomeButtonEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle(R.string.app_name);
            }
            super.onBackPressed();
        } else {
            new QuitTask().execute(this);
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment(),
                args);
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(pref.getTitle());
        }
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_credits:
                showCreditsDialog();
                break;
            case R.id.recommended_settings:
                showRecommendedSettingsDiaglog();
                break;
            case R.id.action_save:
                showSaveDialog();
                break;
            case R.id.action_restore:
                showRestoreDialog();
                break;
            case android.R.id.home:
                onBackPressed();
            default:
                break;
        }
        return super.onOptionsItemSelected(item);

    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }

        if (!Settings.System.canWrite(activity)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        }
    }

    @SuppressLint("SetWorldReadable")
    public static void fixPermissions(Context context) {
        File sharedPrefsFolder =
                new File(context.getFilesDir().getParentFile(), "shared_prefs");
        if (sharedPrefsFolder.exists()) {
            sharedPrefsFolder.setExecutable(true, false);
            sharedPrefsFolder.setReadable(true, false);
            File f = new File(String.format("%s/%s_preferences.xml",
                    sharedPrefsFolder.getAbsolutePath(),
                    BuildConfig.APPLICATION_ID));
            if (f.exists()) {
                f.setReadable(true, false);
                f.setExecutable(true, false);
            }
        }
    }

    @SuppressLint("SetWorldReadable")
    public static void fixAppPermissions(Context context) {
        File appFolder = context.getFilesDir().getParentFile();
        appFolder.setExecutable(true, false);
        appFolder.setReadable(true, false);
    }

    private void showCreditsDialog() {
        PackageInfo pInfo;
        String pkgVersion = "";
        try {
            pInfo = this
                    .getPackageManager()
                    .getPackageInfo(this.getPackageName(), 0);
            pkgVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            XposedBridge.log(e);
        }
        TextView tv = new TextView(this);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(R.string.credit_details);
        tv.setPadding(16, 16, 16, 16);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name) + " " + pkgVersion)
                .setView(tv)
                .setNeutralButton(R.string.ok_btn, (dialog, id) -> dialog.dismiss())
                .show();
    }

    private void showRecommendedSettingsDiaglog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true)
                .setTitle(R.string.app_name)
                .setMessage(R.string.set_recommended_settings)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.apply, (dialog, which) -> restoreRecommendedSettings())
                .create()
                .show();
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        editText.setHint(R.string.backup_name);
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count == 0) {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        dialog = builder.setCancelable(true).setTitle(R.string.save).setView(editText)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    if (savePreferencesToSdCard(editText.getText().toString())) {
                        Toast.makeText(this, R.string.save_successful, Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        Toast.makeText(this, R.string.save_unsuccessful, Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        dialog.show();
        dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private boolean savePreferencesToSdCard(String string) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                + Constants.BACKUP_DIR;
        File dir = new File(path);
        dir.mkdirs();

        File file = new File(dir, string + ".xt");

        boolean res = false;
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(file));
            output.writeObject(MainApplication.getSharedPreferences().getAll());

            res = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }

    private void showRestoreDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String path = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + File.separator + Constants.BACKUP_DIR;
        LinearLayout linearLayout = new LinearLayout(this);
        listView = new ListView(this);
        linearLayout.addView(listView);
        TextView emptyView = new TextView(this, null, android.R.layout.simple_list_item_1);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (Resources.getSystem().getDisplayMetrics().density * 48)));
        emptyView.setGravity(Gravity.CENTER);
        linearLayout.addView(emptyView);
        emptyView.setText(R.string.no_backups);
        listView.setEmptyView(emptyView);
        dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        setupData();

        listView.setOnItemClickListener((arg0, arg1, arg2, arg3) -> {
            onRestoreBackup(backups[arg2]);
            dialog.dismiss();
        });
        listView.setOnItemLongClickListener((arg0, arg1, arg2, arg3) -> {
            PopupMenu menu = new PopupMenu(this, arg1);
            menu.inflate(R.menu.backup_item);
            menu.setOnMenuItemClickListener(item1 -> {
                if (item1.getItemId() == R.id.action_delete) {
                    backups[arg2].delete();
                    setupData();
                }
                return true;
            });
            menu.show();
            return true;
        });
        dialog = builder.setCancelable(true).setTitle(R.string.restore).setView(linearLayout)
                .setPositiveButton(R.string.defaults, (dialog, which) -> onRestoreDefaults())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        dialog.show();
    }

    private void setupData() {
        backups = dir.listFiles();
        if (backups == null || backups.length == 0) {
            return;
        }
        BackupAdapter adapter = new BackupAdapter(this, backups);
        listView.setAdapter(adapter);
    }

    private void restoreRecommendedSettings() {

        MainApplication.getSharedPreferences().edit().clear().apply();
        PreferenceManager.setDefaultValues(this, R.xml.firefds_settings, false);

        Editor editor = MainApplication.getSharedPreferences().edit();

        for (String defaultSetting : defaultSettings) {
            editor.putBoolean(defaultSetting, true).apply();
        }

        editor.putInt(PREF_NOTIFICATION_SIZE, MainApplication.getWindowsSize().x).apply();

        fixPermissions(getApplicationContext());

        Toast.makeText(this, R.string.recommended_restored, Toast.LENGTH_SHORT).show();

        if (!Utils.isOmcEncryptedFlag()) {
            XCscFeaturesManager.applyCscFeatures(MainApplication.getSharedPreferences());
        }
        RebootNotification.notify(this, 999, false);

        recreate();

    }

    private void onRestoreDefaults() {

        MainApplication.getSharedPreferences().edit().clear().apply();
        PreferenceManager.setDefaultValues(this, R.xml.firefds_settings, false);

        Toast.makeText(this, R.string.defaults_restored, Toast.LENGTH_SHORT).show();

        MainApplication.getSharedPreferences()
                .edit()
                .putInt(PREF_NOTIFICATION_SIZE, MainApplication.getWindowsSize().x)
                .apply();

        fixPermissions(getApplicationContext());

        if (!Utils.isOmcEncryptedFlag()) {
            XCscFeaturesManager.applyCscFeatures(MainApplication.getSharedPreferences());
        }

        recreate();

        RebootNotification.notify(this, 999, false);
    }

    private void onRestoreBackup(final File backup) {
        new RestoreBackupTask(backup).execute();
    }

    private Fragment getVisibleFragment() {
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isVisible())
                return fragment;
        }
        return null;
    }

    private static class QuitTask extends AsyncTask<Activity, Void, Void> {
        private Activity mActivity = null;

        protected Void doInBackground(Activity... params) {

            try {
                mActivity = params[0];
                if (!Utils.isOmcEncryptedFlag()) {
                    XCscFeaturesManager.applyCscFeatures(MainApplication.getSharedPreferences());
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                Utils.resetPermissions(mActivity);
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                if (mActivity != null) {
                    mActivity.finish();
                }
            }
            super.onPostExecute(result);
        }

    }

    class RestoreBackupTask extends AsyncTask<Void, Void, Void> {

        private File backup;

        RestoreBackupTask(File backup) {
            this.backup = backup;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBarText.setText(getString(R.string.restoring_backup));
            mLayout.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            ObjectInputStream input = null;
            try {
                input = new ObjectInputStream(new FileInputStream(backup));
                Editor prefEdit = MainApplication.getSharedPreferences().edit();
                prefEdit.clear();
                @SuppressWarnings("unchecked")
                Map<String, ?> entries = (Map<String, ?>) input.readObject();
                for (Entry<String, ?> entry : entries.entrySet()) {
                    Object v = entry.getValue();
                    String key = entry.getKey();

                    if (v instanceof Boolean)
                        prefEdit.putBoolean(key, (Boolean) v);
                    else if (v instanceof Float)
                        prefEdit.putFloat(key, (Float) v);
                    else if (v instanceof Integer)
                        prefEdit.putInt(key, (Integer) v);
                    else if (v instanceof Long)
                        prefEdit.putLong(key, (Long) v);
                    else if (v instanceof String)
                        prefEdit.putString(key, ((String) v));
                }
                prefEdit.apply();
                fixPermissions(getApplicationContext());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            SystemClock.sleep(1500);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mLayout.setVisibility(View.INVISIBLE);
            Toast.makeText(FirefdsKitActivity.this, R.string.backup_restored, Toast.LENGTH_SHORT).show();
            RebootNotification.notify(FirefdsKitActivity.this, 999, false);
        }
    }

    public static class NotificationSettingsFragment extends FirefdsPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.notification_settings, rootKey);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            ListPreference clock_date_preference = findPreference(PREF_CLOCK_DATE_PREFERENCE);
            SwitchPreference clockDateOnRight = findPreference(PREF_CLOCK_DATE_ON_RIGHT);
            if (clockDateOnRight != null) {
                if (clock_date_preference != null) {
                    clockDateOnRight.setEnabled(!clock_date_preference.getValue().equals("disabled"));
                }
            }
            if (clock_date_preference != null) {
                clock_date_preference.setOnPreferenceChangeListener((preference, o) -> {
                    if (!o.toString().equals("disabled")) {
                        if (clockDateOnRight != null) {
                            clockDateOnRight.setEnabled(true);
                        }
                    } else {
                        if (clockDateOnRight != null) {
                            clockDateOnRight.setEnabled(false);
                        }
                    }
                    return true;
                });
            }
        }
    }

    public static class LockscreenSettingsFragment extends FirefdsPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.lockscreen_settings, rootKey);
        }
    }

    public static class MessagingSettingsFragment extends FirefdsPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.messaging_settings, rootKey);
        }
    }

    public static class PhoneSettingsFragment extends FirefdsPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.phone_settings, rootKey);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (Utils.isOmcEncryptedFlag()) {
                PreferenceCategory phoneScreen = findPreference(PREF_PHONE_KEY_CAT);
                SwitchPreference disableNumberFormattingPreference =
                        findPreference(PREF_DISABLE_NUMBER_FORMATTING);

                if (phoneScreen != null) {
                    if (disableNumberFormattingPreference != null) {
                        phoneScreen.removePreference(disableNumberFormattingPreference);
                    }
                }
            }
        }
    }

    public static class SecuritySettingsFragment extends FirefdsPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.security_settings, rootKey);
        }
    }

    public static class SoundSettingsFragment extends FirefdsPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.sound_settings, rootKey);
        }
    }

    public static class SystemSettingsFragment extends FirefdsPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.system_settings, rootKey);
        }
    }

    public static class ScreenTimeoutSettingsFragment extends FirefdsPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.screen_timeout_settings, rootKey);
        }
    }

    public static class SettingsFragment extends FirefdsPreferenceFragment {

        private static Resources res;
        private AlertDialog alertDialog;

        private Runnable delayedRoot = new Runnable() {

            @Override
            public void run() {
                try {
                    mLayout.setVisibility(View.INVISIBLE);
                    Toast.makeText(getmContext(), R.string.root_info, Toast.LENGTH_LONG).show();

                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            try {
                res = getResources();

                SharedPreferences sharedPreferences
                        = getmContext().getSharedPreferences(PREFS, 0);
                MainApplication.setSharedPreferences(sharedPreferences);

                if (Utils.isOmcEncryptedFlag()) {
                    PreferenceScreen rootScreen = findPreference(PREF_ROOT);
                    PreferenceScreen messagingScreen = findPreference(PREF_MESSAGING_KEY);

                    if (rootScreen != null) {
                        if (messagingScreen != null) {
                            rootScreen.removePreference(messagingScreen);
                        }
                    }
                }

                showProgressBar();

                MainApplication.getSharedPreferences().edit()
                        .putInt(PREF_NOTIFICATION_SIZE, MainApplication.getWindowsSize().x).apply();
                fixPermissions(getmContext());


                if (!Utils.isSamsungRom()) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogBuilder.setTitle(res.getString(R.string.samsung_rom_warning));

                    alertDialogBuilder.setMessage(res.getString(R.string.samsung_rom_warning_msg))
                            .setCancelable(false)
                            .setPositiveButton(res.getString(R.string.ok_btn), null);

                    alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }

                //TextViewPreference textViewInformationHeader;
                PreferenceScreen ps = findPreference(PREF_ROOT);
                Preference textViewInformationHeader = findPreference(PREF__FAKE_HEADER);
                if (textViewInformationHeader != null) {
                    textViewInformationHeader.setTitle("");
                }

                if (!XposedChecker.isActive()) {
                    if (textViewInformationHeader != null) {
                        textViewInformationHeader.setTitle(R.string.firefds_kit_is_not_active);
                    }
                    //textViewInformationHeader.getTextView().setTextColor(Color.RED);
                    if (textViewInformationHeader != null) {
                        textViewInformationHeader.setEnabled(false);
                    }
                } else {
                    if (ps != null) {
                        if (textViewInformationHeader != null) {
                            ps.removePreference(textViewInformationHeader);
                        }
                    }
                }

                new CheckRootTask().execute();

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.firefds_settings, rootKey);
        }

        private void showRootDisclaimer() {
            if (getmContext() != null) {
                try {

                    mLayout.setVisibility(View.INVISIBLE);

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

                    alertDialogBuilder.setTitle(R.string.app_name);

                    alertDialogBuilder.setMessage(R.string.root_info)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                            .setCancelable(true);

                    alertDialog = alertDialogBuilder.create();
                    alertDialog.show();

                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        private void showProgressBar() {
            progressBarText.setText(res.getString(R.string.checking_root_access));
            mLayout.setVisibility(View.VISIBLE);
            showDelayedRootMsg();
        }

        private void showDelayedRootMsg() {

            MainApplication.getHandler().postDelayed(delayedRoot, 20000);

        }

        @Override
        public void onDestroy() {
            try {
                if (mLayout.getVisibility() == View.VISIBLE) {
                    mLayout.setVisibility(View.INVISIBLE);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            super.onDestroy();
        }

        private class CheckRootTask extends AsyncTask<Void, Void, Void> {
            private boolean suAvailable = false;

            protected Void doInBackground(Void... params) {
                try {
                    suAvailable = Shell.rootAccess();

                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return null;
            }

            protected void onPostExecute(Void p) {

                try {
                    MainApplication.getHandler().removeCallbacks(delayedRoot);
                    mLayout.setVisibility(View.INVISIBLE);
                    // Check for root access
                    if (!suAvailable) {
                        showRootDisclaimer();
                    } else {
                        progressBarText.setText(res.getString(R.string.loading_application_preferences_));
                        if (mLayout.getVisibility() == View.INVISIBLE) {
                            mLayout.setVisibility(View.VISIBLE);
                        }
                        new CopyCSCTask().execute(getmContext());

                        if (!MainApplication.getSharedPreferences()
                                .getBoolean(PREF_IS_FIREFDS_KIT_FIRST_LAUNCH, false)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setCancelable(true)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.firefds_xposed_disclaimer)
                                    .setPositiveButton(R.string.ok_btn, (dialog, which) -> dialog.dismiss())
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .create()
                                    .show();
                            MainApplication.getSharedPreferences()
                                    .edit()
                                    .putBoolean(PREF_IS_FIREFDS_KIT_FIRST_LAUNCH, true)
                                    .apply();
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        private class CopyCSCTask extends AsyncTask<Context, Void, Void> {

            protected Void doInBackground(Context... params) {
                try {
                    if (!Utils.isOmcEncryptedFlag()) {
                        XCscFeaturesManager.getDefaultCSCFeaturesFromFiles();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                try {
                    if (!Utils.isOmcEncryptedFlag()) {
                        Utils.createCSCFiles(getmContext());
                    }
                    if (mLayout.getVisibility() == View.VISIBLE) {
                        mLayout.setVisibility(View.INVISIBLE);
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                }
                super.onPostExecute(result);
            }
        }
    }
}

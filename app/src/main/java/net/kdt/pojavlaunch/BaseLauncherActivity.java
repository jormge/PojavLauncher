package net.kdt.pojavlaunch;

import android.app.*;
import android.content.*;
import android.support.v7.app.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.kdt.filerapi.*;
import java.io.*;
import net.kdt.pojavlaunch.fragments.*;
import net.kdt.pojavlaunch.prefs.*;
import net.kdt.pojavlaunch.tasks.*;

import android.support.v7.app.AlertDialog;

public abstract class BaseLauncherActivity extends BaseActivity {
	public Button mPlayButton;
	public ConsoleFragment mConsoleView;
    public CrashFragment mCrashView;
    public ProgressBar mLaunchProgress;
	public Spinner mVersionSelector;
	public TextView mLaunchTextStatus, mTextVersion;
    
    public JMinecraftVersionList mVersionList;
	public MinecraftDownloaderTask mTask;
	public MCProfile.Builder mProfile;
	public String[] mAvailableVersions;
    
	public boolean mIsAssetsProcessing = false;
    protected boolean canBack = false;
    
    public abstract void statusIsLaunching(boolean isLaunching);

    public void mcaccSwitchUser(View view) {
        showProfileInfo();
    }

    public void mcaccLogout(View view) {
        //PojavProfile.reset();
        finish();
    }

    private void showProfileInfo() {
        /*
         new AlertDialog.Builder(this)
         .setTitle("Info player")
         .setMessage(
         "AccessToken=" + profile.getAccessToken() + "\n" +
         "ClientID=" + profile.getClientID() + "\n" +
         "ProfileID=" + profile.getProfileID() + "\n" +
         "Username=" + profile.getUsername() + "\n" +
         "Version=" + profile.getVersion()
         ).show();
         */
    }
    
    public void launcherMenu(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.mcl_options);
        builder.setItems(R.array.mcl_options, new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface p1, int p2)
                {
                    switch (p2) {
                        case 0: // Mod installer
                            installMod(false);
                            break;
                        case 1: // Mod installer with java args 
                            installMod(true);
                            break;
                        case 2: // Custom controls
                            if (Tools.enableDevFeatures) {
                                startActivity(new Intent(BaseLauncherActivity.this, CustomControlsActivity.class));
                            }
                            break;
                        case 3: // Settings
                            startActivity(new Intent(BaseLauncherActivity.this, LauncherPreferenceActivity.class));
                            break;
                        case 4: { // About
                                final AlertDialog.Builder aboutB = new AlertDialog.Builder(BaseLauncherActivity.this);
                                aboutB.setTitle(R.string.mcl_option_about);
                                try
                                {
                                    aboutB.setMessage(Html.fromHtml(String.format(Tools.read(getAssets().open("about_en.txt")),
                                                                                  Tools.APP_NAME,
                                                                                  Tools.usingVerName,
                                                                                  "3.2.3")
                                                                    ));
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                aboutB.setPositiveButton(android.R.string.ok, null);
                                aboutB.show();
                            } break;
                    }
                }
            });
        builder.show();
    }

    private void installMod(boolean customJavaArgs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.alerttitle_installmod);
        builder.setNegativeButton(android.R.string.cancel, null);

        final AlertDialog dialog;
        if (customJavaArgs) {
            final EditText edit = new EditText(this);
            edit.setSingleLine();
            edit.setHint("-jar/-cp /path/to/file.jar ...");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface di, int i) {
                        Intent intent = new Intent(BaseLauncherActivity.this, JavaGUILauncherActivity.class);
                        intent.putExtra("skipDetectMod", true);
                        intent.putExtra("javaArgs", edit.getText().toString());
                        startActivity(intent);
                    }
                });
            dialog = builder.create();
            dialog.setView(edit);
        } else {
            dialog = builder.create();
            FileListView flv = new FileListView(dialog);
            flv.setFileSelectedListener(new FileSelectedListener(){
                    @Override
                    public void onFileSelected(File file, String path) {
                        if (file.getName().endsWith(".jar")) {
                            Intent intent = new Intent(BaseLauncherActivity.this, JavaGUILauncherActivity.class);
                            intent.putExtra("modFile", file);
                            startActivity(intent);
                            dialog.dismiss();
                        }
                    }
                });
            dialog.setView(flv);
        }
        dialog.show();
    }

    public void launchGame(View v) {
        if (!canBack && mIsAssetsProcessing) {
            mIsAssetsProcessing = false;
            statusIsLaunching(false);
        } else if (canBack) {
            v.setEnabled(false);
            mTask = new MinecraftDownloaderTask(this);
            mTask.execute(mProfile.getVersion());
            mCrashView.resetCrashLog = true;
        }
    }
    
    @Override
    public void onBackPressed() {
        if (canBack) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Tools.updateWindowSize(this);
        
    }
    
    @Override
    protected void onResume(){
        super.onResume();
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        new RefreshVersionListTask(this).execute();

        try{
            final ProgressDialog barrier = new ProgressDialog(this);
            barrier.setMessage("Waiting");
            barrier.setProgressStyle(barrier.STYLE_SPINNER);
            barrier.setCancelable(false);
            barrier.show();

            new Thread(new Runnable(){

                    @Override
                    public void run()
                    {
                        while (mConsoleView == null) {
                            try {
                                Thread.sleep(20);
                            } catch (Throwable th) {}
                        }

                        try {
                            Thread.sleep(100);
                        } catch (Throwable th) {}

                        runOnUiThread(new Runnable() {
                                @Override
                                public void run()
                                {
                                    try {
                                        mConsoleView.putLog("");
                                        barrier.dismiss();
                                    } catch (Throwable th) {
                                        startActivity(getIntent());
                                        finish();
                                    }
                                }
                            });
                    }
                }).start();

            File lastCrashFile = Tools.lastFileModified(Tools.crashPath);
            if(CrashFragment.isNewCrash(lastCrashFile) || !mCrashView.getLastCrash().isEmpty()){
                mCrashView.resetCrashLog = false;
                selectTabPage(2);
            } else throw new Exception();
        } catch(Throwable e) {
            e.printStackTrace();
            // selectTabPage(tabLayout.getSelectedTabPosition());
        }
    }
    
    // Catching touch exception
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    protected abstract void selectTabPage(int pageIndex);
    protected abstract float updateWidthHeight();
}

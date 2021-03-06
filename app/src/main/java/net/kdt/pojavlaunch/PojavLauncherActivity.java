package net.kdt.pojavlaunch;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.support.design.widget.*;
import android.support.design.widget.VerticalTabLayout.*;
import android.support.v4.view.*;
import android.support.v7.app.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;
import com.google.gson.*;
import com.kdt.filerapi.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import net.kdt.pojavlaunch.*;
import net.kdt.pojavlaunch.fragments.*;
import net.kdt.pojavlaunch.prefs.*;
import net.kdt.pojavlaunch.utils.*;
import net.kdt.pojavlaunch.value.*;
import org.apache.commons.io.*;
import org.lwjgl.glfw.*;

import android.support.v7.app.AlertDialog;
import net.kdt.pojavlaunch.tasks.*;
//import android.support.v7.view.menu.*;
//import net.zhuoweizhang.boardwalk.downloader.*;

public class PojavLauncherActivity extends BaseLauncherActivity
{
    //private FragmentTabHost mTabHost;
    private LinearLayout fullTab, leftTab;
    /*
     private PojavLauncherViewPager viewPager;
     private VerticalTabLayout tabLayout;
     */

    private ViewPager viewPager;
    private VerticalTabLayout tabLayout;

    private TextView tvUsernameView;
    private Spinner accountSelector;
    private String profilePath = null;
    private ViewPagerAdapter viewPageAdapter;

    private Button switchUsrBtn, logoutBtn; // MineButtons
    private ViewGroup leftView, rightView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            Toast.makeText(this, "Launcher process id: " + android.os.Process.myPid(), Toast.LENGTH_LONG).show();
        }
        
        setContentView(R.layout.launcher_main_v3);
        // setContentView(R.layout.launcher_main);

        leftTab = findViewById(R.id.launchermain_layout_leftmenu);
        leftTab.setLayoutParams(new LinearLayout.LayoutParams(
            CallbackBridge.windowWidth / 4,
            LinearLayout.LayoutParams.MATCH_PARENT));
        
        fullTab = findViewById(R.id.launchermain_layout_viewpager);
        tabLayout = findViewById(R.id.launchermainTabLayout);
        viewPager = findViewById(R.id.launchermainTabPager);

        mConsoleView = new ConsoleFragment();
        mCrashView = new CrashFragment();

        viewPageAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPageAdapter.addFragment(new LauncherFragment(), R.drawable.ic_menu_news, getString(R.string.mcl_tab_news));
        viewPageAdapter.addFragment(mConsoleView, R.drawable.ic_menu_java, getString(R.string.mcl_tab_console));
        viewPageAdapter.addFragment(mCrashView, 0, getString(R.string.mcl_tab_crash));
        viewPageAdapter.addFragment(new LauncherPreferenceFragment(), R.drawable.ic_menu_settings, getString(R.string.mcl_option_settings));
        
        viewPager.setAdapter(viewPageAdapter);
        // tabLayout.setTabMode(VerticalTabLayout.MODE_SCROLLABLE);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setLastTabAsBottom();

        tvUsernameView = (TextView) findViewById(R.id.launcherMainUsernameView);
        mTextVersion = (TextView) findViewById(R.id.launcherMainVersionView);

        try {
            profilePath = PojavProfile.getCurrentProfilePath(this);
            mProfile = PojavProfile.getCurrentProfileContent(this);

            tvUsernameView.setText(mProfile.getUsername());
        } catch(Exception e) {
            //Tools.throwError(this, e);
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.toast_login_error, e.getMessage()), Toast.LENGTH_LONG).show();
            finish();
        }

        File logFile = new File(Tools.MAIN_PATH, "latestlog.txt");
        if (logFile.exists() && logFile.length() < 20480) {
            String errMsg = "Error occurred during initialization of ";
            try {
                String logContent = Tools.read(logFile.getAbsolutePath());
                if (logContent.contains(errMsg + "VM") && 
                    logContent.contains("Could not reserve enough space for")) {
                    OutOfMemoryError ex = new OutOfMemoryError("Java error: " + logContent);
                    ex.setStackTrace(null);
                    Tools.showError(PojavLauncherActivity.this, ex);

                    // Do it so dialog will not shown for second time
                    Tools.write(logFile.getAbsolutePath(), logContent.replace(errMsg + "VM", errMsg + "JVM"));
                }
            } catch (Throwable th) {
                System.err.println("Could not detect java crash");
                th.printStackTrace();
            }
        }

        //showProfileInfo();

        final String[] accountList = new File(Tools.mpProfiles).list();
        
        ArrayAdapter<String> adapterAcc = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, accountList);
        adapterAcc.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        accountSelector = (Spinner) findViewById(R.id.launchermain_spinner_account);
        accountSelector.setAdapter(adapterAcc);
        accountSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> p1, View p2, int position, long p4) {
                PojavProfile.setCurrentProfile(PojavLauncherActivity.this, accountList[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> p1) {
                // TODO: Implement this method
            }
        });
        
        List<String> versions = new ArrayList<String>();
        final File fVers = new File(Tools.versnDir);

        try {
            if (fVers.listFiles().length < 1) {
                throw new Exception(getString(R.string.error_no_version));
            }

            for (File fVer : fVers.listFiles()) {
                if (fVer.isDirectory())
                    versions.add(fVer.getName());
            }
        } catch (Exception e) {
            versions.add(getString(R.string.global_error) + ":");
            versions.add(e.getMessage());

        } finally {
            mAvailableVersions = versions.toArray(new String[0]);
        }

        //mAvailableVersions;

        ArrayAdapter<String> adapterVer = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mAvailableVersions);
        adapterVer.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        mVersionSelector = (Spinner) findViewById(R.id.launchermain_spinner_version);
        mVersionSelector.setAdapter(adapterVer);

        mLaunchProgress = (ProgressBar) findViewById(R.id.progressDownloadBar);
        mLaunchTextStatus = (TextView) findViewById(R.id.progressDownloadText);
        LinearLayout exitLayout = (LinearLayout) findViewById(R.id.launcherMainExitbtns);
        switchUsrBtn = (Button) exitLayout.getChildAt(0);
        logoutBtn = (Button) exitLayout.getChildAt(1);

        leftView = (LinearLayout) findViewById(R.id.launcherMainLeftLayout);
        mPlayButton = (Button) findViewById(R.id.launcherMainPlayButton);
        rightView = (ViewGroup) findViewById(R.id.launcherMainRightLayout);

        statusIsLaunching(false);
    }

    @Override
    protected float updateWidthHeight() {
        float leftRightWidth = (float) CallbackBridge.windowWidth / 100f * 32f;
        float mPlayButtonWidth = CallbackBridge.windowWidth - leftRightWidth * 2f;
        LinearLayout.LayoutParams leftRightParams = new LinearLayout.LayoutParams((int) leftRightWidth, (int) Tools.dpToPx(CallbackBridge.windowHeight / 9));
        LinearLayout.LayoutParams mPlayButtonParams = new LinearLayout.LayoutParams((int) mPlayButtonWidth, (int) Tools.dpToPx(CallbackBridge.windowHeight / 9));
        leftView.setLayoutParams(leftRightParams);
        rightView.setLayoutParams(leftRightParams);
        mPlayButton.setLayoutParams(mPlayButtonParams);

        return leftRightWidth;
    }

    @Override
    protected void selectTabPage(int pageIndex){
        if (tabLayout.getSelectedTabPosition() != pageIndex) {
            tabLayout.setScrollPosition(pageIndex,0f,true);
            viewPager.setCurrentItem(pageIndex);
        }
    }

    public void statusIsLaunching(boolean isLaunching) {
        // As preference fragment put to tab, changes without notice, so need re-load pref
        if (isLaunching) LauncherPreferences.loadPreferences();
        
        LinearLayout.LayoutParams reparam = new LinearLayout.LayoutParams((int) updateWidthHeight(), LinearLayout.LayoutParams.WRAP_CONTENT);
        ViewGroup.MarginLayoutParams lmainTabParam = (ViewGroup.MarginLayoutParams) fullTab.getLayoutParams();
        int launchVisibility = isLaunching ? View.VISIBLE : View.GONE;
        mLaunchProgress.setVisibility(launchVisibility);
        mLaunchTextStatus.setVisibility(launchVisibility);
        lmainTabParam.bottomMargin = reparam.height;
        leftView.setLayoutParams(reparam);

        switchUsrBtn.setEnabled(!isLaunching);
        logoutBtn.setEnabled(!isLaunching);
        mVersionSelector.setEnabled(!isLaunching);
        canBack = !isLaunching;
    }
}


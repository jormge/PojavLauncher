package net.kdt.pojavlaunch.prefs;

import net.kdt.pojavlaunch.*;
import android.os.*;
import android.support.v7.app.*;
import android.widget.*;
import android.content.*;
import com.pojavdx.dex.*;
import com.kdt.mcgui.app.*;

public class PojavPreferenceActivity extends MineActivity
{
	public static boolean PREF_FREEFORM = false;
	
	private SeekBar viewSeekDxRef;
	private TextView viewSeekProgress;
	private Switch viewSwitchFreeform;
	
	private SharedPreferences mainPreference;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		PojavPreferenceActivity.loadPreferences(this);
		
		mainPreference = getSharedPreferences("pojav_preferences", MODE_PRIVATE);
		final SharedPreferences.Editor mainPrefEdit = mainPreference.edit();
		
		viewSeekDxRef = (SeekBar) findViewById(R.id.settings_seekbar_setmaxdxref);
		viewSeekProgress = (TextView) findViewById(R.id.setting_seektext_progress);
		viewSwitchFreeform = (Switch) findViewById(R.id.settings_switch_enablefreeform);
		
		viewSeekDxRef.setMax(0xFFFF - 0xFFF);
		viewSeekDxRef.setProgress(DexFormat.MAX_MEMBER_IDX - 0xFFF);
		viewSeekDxRef.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
			private int currProgress = 0;
				@Override
				public void onProgressChanged(SeekBar bar, int progress, boolean p3) {
					currProgress = 0xFFF + progress;
					viewSeekProgress.setText(currProgress + "/" + 0xFFFF);
				}

				@Override
				public void onStartTrackingTouch(SeekBar bar) {
					// Unused
				}

				@Override
				public void onStopTrackingTouch(SeekBar bar) {
					mainPrefEdit.putInt("maxDxRefs", currProgress);
					mainPrefEdit.commit();
				}
			});
		viewSeekProgress.setText((viewSeekDxRef.getProgress() + 0xFFF) + "/" + 0xFFFF);
		
		viewSwitchFreeform.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener(){

				@Override
				public void onCheckedChanged(CompoundButton b, boolean z)
				{
					mainPrefEdit.putBoolean("freeform", z);
					mainPrefEdit.commit();
				}
			});
		viewSwitchFreeform.setChecked(PREF_FREEFORM);
		viewSwitchFreeform.setEnabled(Build.VERSION.SDK_INT >= 24);
	}

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		PojavPreferenceActivity.loadPreferences(this);
	}
	
	public static void loadPreferences(Context ctx) {
		SharedPreferences mainPreference = ctx.getSharedPreferences("pojav_preferences", MODE_PRIVATE);
		int maxDxPref = mainPreference.getInt("maxDxRefs", 0xFFF);
		DexFormat.MAX_MEMBER_IDX = maxDxPref;
		DexFormat.MAX_TYPE_IDX = maxDxPref;
		
		PREF_FREEFORM = mainPreference.getBoolean("freeform", false);
	}
}
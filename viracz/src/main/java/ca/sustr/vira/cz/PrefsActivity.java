package ca.sustr.vira.cz;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import androidx.core.app.JobIntentService;

public class PrefsActivity extends PreferenceActivity
{
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public void onBackPressed()
	{
		JobIntentService.enqueueWork(this, WidgetService.class, R.integer.jobId, new Intent(this.getString(R.string.ONBACKPRESSED_PREFS_ACTION)));

	    // Continue with the default handler
	    super.onBackPressed();
	}
}

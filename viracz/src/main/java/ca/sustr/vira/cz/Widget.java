package ca.sustr.vira.cz;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.app.JobIntentService;

public class Widget extends AppWidgetProvider
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(context.getString(R.string.ONCLICK_QUOTE_ACTION)))
		{
			// Clicked on "Quote of the Day", forward the intent
			JobIntentService.enqueueWork(context, WidgetService.class, R.integer.jobId,
					new Intent(context.getString(R.string.ONCLICK_QUOTE_ACTION)));
		}
		else if (intent.getAction().equals(context.getString(R.string.ONCLICK_TOPIC_ACTION)))
		{
			// Clicked on "Topic of the Week", forward the intent
			JobIntentService.enqueueWork(context, WidgetService.class, R.integer.jobId,
					new Intent(context.getString(R.string.ONCLICK_TOPIC_ACTION)));
		}
		else if (intent.getAction().equals(context.getString(R.string.ONBACKPRESSED_PREFS_ACTION)))
		{
			// Pressed Back in Preferences, forward the intent
			JobIntentService.enqueueWork(context, WidgetService.class, R.integer.jobId,
					new Intent(context.getString(R.string.ONBACKPRESSED_PREFS_ACTION)));
		}
		else if (intent.getAction().equals(context.getString(R.string.ONWIDGET_MANUALUPDATE_ACTION)))
		{
			// Clicked on the Refresh button, forward the intent
			JobIntentService.enqueueWork(context, WidgetService.class, R.integer.jobId,
					new Intent(context.getString(R.string.ONWIDGET_MANUALUPDATE_ACTION)));
		}
		else
		{
			super.onReceive(context, intent);
		}
	}

	// Called upon receiving "android.appwidget.action.APPWIDGET_UPDATE" through
	// onReceive() 
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds)
	{
		Log.d("onUpdate", "Automatic update widget entry point");
		JobIntentService.enqueueWork(context, WidgetService.class, R.integer.jobId, new Intent(context.getString(R.string.ONWIDGET_AUTOUPDATE_ACTION)));
	}
}

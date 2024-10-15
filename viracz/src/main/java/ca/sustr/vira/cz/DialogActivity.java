package ca.sustr.vira.cz;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import androidx.core.app.JobIntentService;

public class DialogActivity extends Activity implements OnSharedPreferenceChangeListener
{
	// Member variables
	private View					m_dialogFrame;
	private String					m_dialogText;
	private boolean 				m_IAmQuote;
	private TextView				m_textView;
	private ImageButton				m_buttonSettings;
	private ImageButton				m_buttonRefresh;
	private View					m_toolBar;
	private SharedPreferences		m_prefs;
	private Context					m_context;

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key)
	{
		if (key.equals("dialog_font_size"))
		{
			String string = sharedPreferences.getString(key, null);
			if (string != null)
			{
				float floatValue = Float.parseFloat(string);
				m_textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, floatValue);
			}
		}
		else if (key.equals("dialog_font_colour"))
		{
			Integer value = sharedPreferences.getInt(key, Color.LTGRAY);
			m_textView.setTextColor(value);
		}
		else if (key.equals("dialog_background_colour"))
		{
			Integer value = sharedPreferences.getInt(key, Color.BLACK);
			// If you want to set transparency (e.g. 0x44...) you can do for
			// example this:
			//
			// String hexColor = String.format("#44%06X", (0xFFFFFF & value));
			// m_textView.setBackgroundColor(Color.parseColor(hexColor));
			m_dialogFrame.setBackgroundColor(value);
			View title = getWindow().findViewById(android.R.id.title);
			View titleBar = (View) title.getParent();
			titleBar.setBackgroundColor(value);		
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Determine caller's context
		getCaller();
	
		// Set window title
		setWindowTitle();
		
		// Open Dialog
		setContentView(R.layout.dialog_activity);

		// Initialize member variables
		initMemberVars();
		
		// Set GUI properties for the window
		setGUIAttributes();

		// Display the window contents
	    setWindowContents();

		// Register click handlers
		setClickHandlers();
	}
	
	private void getCaller()
	{
		// Determine the caller's context
		Intent intent = getIntent();
		if (intent.getAction().equals(this.getString(R.string.ONCLICK_QUOTE_ACTION)))
		{
			m_IAmQuote = true;
		}
		
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
		    m_dialogText = extras.getString(getString(R.string.BUNDLE_KEY));
		}
		
	}

	private void setWindowTitle()
	{
        if (m_IAmQuote)
		{
    		setTitle(R.string.quoteHeader);
		}
        else
        {
    		setTitle(R.string.topicHeader);
        }
	}

	private void initMemberVars()
	{
		m_prefs = PreferenceManager.getDefaultSharedPreferences(this);
		m_context = this;

		// Store pointers to GUI elements
		m_dialogFrame = findViewById(R.id.dialogFrame);
		m_textView = (TextView) findViewById(R.id.dialogTextView);
		m_buttonSettings = (ImageButton) findViewById(R.id.buttonSettings);
		m_buttonRefresh = (ImageButton) findViewById(R.id.buttonRefresh);
		m_toolBar = findViewById(R.id.toolbar);
	}

	private void setGUIAttributes()
	{
		// Set font size
		String string = m_prefs.getString("dialog_font_size", null);
		if (string != null)
		{
			float floatValue = Float.parseFloat(string);
			m_textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, floatValue);
		}

		// Set font colour
		Integer value = m_prefs.getInt("dialog_font_colour", Color.LTGRAY);
		m_textView.setTextColor(value);

		// Set background colour
		value = m_prefs.getInt("dialog_background_colour", Color.BLACK);
		m_dialogFrame.setBackgroundColor(value);
		View title = getWindow().findViewById(android.R.id.title);
		View titleBar = (View) title.getParent();
		titleBar.setBackgroundColor(value);		
		
		// Make the text view scrollable
		// m_textView.setMovementMethod(new ScrollingMovementMethod());

		// Make URL links clickable
	    m_textView.setMovementMethod(LinkMovementMethod.getInstance());
	}
	
	private void setClickHandlers()
	{
		// Listener for the TextView, the only purpose is to hide the Preferences
		// button
		m_textView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Hide the image button if visible
				if (m_toolBar.getVisibility() == View.VISIBLE)
				{
					Animation a = new AlphaAnimation(1.00f, 0.00f);
					a.setDuration(500);
					a.setAnimationListener(new AnimationListener()
					{
					    public void onAnimationStart(Animation animation)
					    {
					    }
	
					    public void onAnimationRepeat(Animation animation)
					    {
					    }
	
					    public void onAnimationEnd(Animation animation)
					    {
					        m_toolBar.setVisibility(View.GONE);
					    }
					});
					m_toolBar.startAnimation(a);
				}
			}
		}); 		

		// Listener for the Preferences button which starts the Preferences
		// dialog
		m_buttonSettings.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Open the Preferences dialog
                startActivity(new Intent(getApplicationContext(), PrefsActivity.class));

                // This line MUST be called AFTER opening the Preferences dialog
                m_prefs.registerOnSharedPreferenceChangeListener((OnSharedPreferenceChangeListener) m_context);
			}
		}); 		

		// Listener for the Refresh button which refreshes the widget
		m_buttonRefresh.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				JobIntentService.enqueueWork(m_context, WidgetService.class, R.integer.jobId,
						new Intent(m_context.getString(R.string.ONWIDGET_MANUALUPDATE_ACTION)));
			}
		}); 		
		
	}

	private void setWindowContents()
	{
		if (m_dialogText != null)
		{
			// Set the text, keep much of the formatting
			m_textView.setText(Html.fromHtml(m_dialogText));
		}
		else
		{
	        if (m_IAmQuote)
			{
	        	m_textView.setText(R.string.quoteDetail);
			}
	        else
	        {
	        	m_textView.setText(R.string.topicDetail);
	        }
		}
	}
}

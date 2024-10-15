package ca.sustr.vira.cz;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;


import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import androidx.annotation.NonNull;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WidgetService extends AppWidgetProvider
{
	// Member variables
	private String						m_quoteShort;
	private String						m_topicShort;
	private String						m_quote;
	private String						m_chapter;
	private String						m_topic;
	private boolean						m_alert;

	private Hashtable<String, String>	m_bibleBooks;

	public WidgetService()
	{
        // Set up book hash table
        setBibleBooks();
	}

	@Override
	// Called when the widget is first enabled, including during a phone reboot
    public void onEnabled(Context context)
	{
		// Initialize context variables
		m_alert		= false;

        // TODO: Debug logs are compiled in but stripped at runtime
        Log.d("onEnabled", "Enter");

        // Load saved preferences
		loadState(context);
    }

    @Override
    // This routine is called during the first update when the widget is
    // created, and then during all subsequent updates and user actions
    public void onReceive(Context context, Intent intent)
    {
        Log.d("onReceive", intent.getAction());

        if (intent.getAction().equals(context.getResources().getString(R.string.ONWIDGET_AUTOUPDATE_ACTION)))
        {
            // TODO: Debug logs are compiled in but stripped at runtime
            Log.d("onReceive", "Automatic update");

            // Download new data. The widget will be updated by these routines.
            downloadQuote(context);
            downloadTopic(context);
            return;
        }

        if (intent.getAction().equals(context.getResources().getString(R.string.ONBACKPRESSED_PREFS_ACTION)))
        {
            // TODO: Debug logs are compiled in but stripped at runtime
            Log.d("onReceive", "Pressed Back in Preferences");

            // Pressed Back in Preferences. Pass the event on to the parent, which
            // will result in calling onUpdate().
            super.onReceive(context, intent);
            return;
        }

        if (intent.getAction().equals(context.getResources().getString(R.string.ONWIDGET_MANUALUPDATE_ACTION)))
        {
            // TODO: Debug logs are compiled in but stripped at runtime
            Log.d("onReceive", "Manual update");

            // Pressed Refresh. Download new data and alert the user when done.
            m_alert = true;
            downloadQuote(context);
            downloadTopic(context);
            return;
        }

        // By default, pass the event on to the parent, which will allow
        // calling onUpdate() for example
        super.onReceive(context, intent);
    }

    @Override
    // Called during the first update when the widget is created, and then
    // during all all subsequent updates upon receiving
    // "android.appwidget.action.APPWIDGET_UPDATE" through onReceive().
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        // TODO: Debug logs are compiled in but stripped at runtime
        Log.d("onUpdate", "Enter");

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds)
        {
            // TODO: Debug logs are compiled in but stripped at runtime
            Log.d("onUpdate", "Updating widget Id: " + appWidgetId);

            // Update widget
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    // Called when the background download of web contents has finished
	public void onReceiveDownload(Context context, int resourceID, String result)
	{
        // TODO: Debug logs are compiled in but stripped at runtime
        Log.d("onReceiveDownload", result);

        switch (resourceID)
		{
			case R.id.quoteOfTheDay:
			{
				// Update the widget and member variable
				parseQuote(context, result);

                // Save the new value. This call should be unconditional.
				saveState(context);

				// Update the widget
                // TODO: review
                updateWidget(context, AppWidgetManager.getInstance(context), 0);

				// Download the book/chapter/verse. This call should be
				// unconditional to handle the cases where the quote was
				// downloaded successfully but the chapter/book detail download
				// failed. We should always try to get the latest quote detail. 
				downloadQuoteDetail(context);

				// Upload if necessary. Just like the previous case, this call
				// should be unconditional to retry possibly failed uploads. 
				uploadQuote(context);

				break;
			}
			
			case R.id.dialogTextView:
			{
				// Update the member variable
				parseChapter(context, result);

				// Save the new value. This call should be unconditional.
				saveState(context);

				// Update the widget
                // TODO: review
                updateWidget(context, AppWidgetManager.getInstance(context), 0);
				
				break;
			}
				
			case R.id.topicOfTheWeek:
			{
				// Update the widget and member variable
				parseTopic(context, result);

				// Save the new value. This call should be unconditional.
				saveState(context);

				// Update the widget
                // TODO: Review
                updateWidget(context, AppWidgetManager.getInstance(context), 0);

				// Upload if necessary. Just like the previous case, this call
				// should be unconditional to retry possibly failed uploads. 
				uploadTopic(context);

				break;
			}
		}
	}

	public String getQuote()
	{
		return m_quote;
	}

	public String getChapter()
	{
		return m_chapter;
	}

	public String getTopic()
	{
		return m_topic;
	}
	
	// Maps Bible book abbreviations or full names to a short name used by
	// biblenet.cz. Each pair consists of a key (to be found at the quotation
	// line) and a corresponding string pointing to the book at biblenet.cz.
	// Each book can have multiple mappings.
	private void setBibleBooks()
	{
        m_bibleBooks = new Hashtable<String, String>();

        m_bibleBooks.put("Genesis", "Gen");
        m_bibleBooks.put("1. kniha Mojžíšova", "Gen");
        m_bibleBooks.put("Gn", "Gen");
        m_bibleBooks.put("Gen", "Gen");
        m_bibleBooks.put("1M", "Gen");
        m_bibleBooks.put("1 M", "Gen");
        m_bibleBooks.put("I M", "Gen");
        m_bibleBooks.put("1Moj", "Gen");
        m_bibleBooks.put("1 Moj", "Gen");
        m_bibleBooks.put("IMoj", "Gen");
        m_bibleBooks.put("I Moj", "Gen");
        m_bibleBooks.put("1Mojzisova", "Gen");
        m_bibleBooks.put("1 Mojzisova", "Gen");
        m_bibleBooks.put("1Mojz", "Gen");
        m_bibleBooks.put("1 Mojz", "Gen");
        m_bibleBooks.put("IMojz", "Gen");
        m_bibleBooks.put("I Mojz", "Gen");
        m_bibleBooks.put("IMojzisova", "Gen");
        m_bibleBooks.put("I Mojzisova", "Gen");

        m_bibleBooks.put("Exodus", "Exod");
        m_bibleBooks.put("2. kniha Mojžíšova", "Exod");
        m_bibleBooks.put("Ex", "Exod");
        m_bibleBooks.put("Exo", "Exod");
        m_bibleBooks.put("2M", "Exod");
        m_bibleBooks.put("2 M", "Exod");
        m_bibleBooks.put("IIM", "Exod");
        m_bibleBooks.put("II M", "Exod");
        m_bibleBooks.put("2Moj", "Exod");
        m_bibleBooks.put("2 Moj", "Exod");
        m_bibleBooks.put("IIMoj", "Exod");
        m_bibleBooks.put("II Moj", "Exod");
        m_bibleBooks.put("Exod", "Exod");
        m_bibleBooks.put("2Mojzisova", "Exod");
        m_bibleBooks.put("2 Mojzisova", "Exod");
        m_bibleBooks.put("2Mojz", "Exod");
        m_bibleBooks.put("2 Mojz", "Exod");
        m_bibleBooks.put("IIMojzisova", "Exod");
        m_bibleBooks.put("II Mojzisova", "Exod");
        m_bibleBooks.put("IIMojz", "Exod");
        m_bibleBooks.put("II Mojz", "Exod");

        m_bibleBooks.put("Leviticus", "Lev");
        m_bibleBooks.put("3. kniha Mojžíšova", "Lev");
        m_bibleBooks.put("Lv", "Lev");
        m_bibleBooks.put("Lev", "Lev");
        m_bibleBooks.put("3M", "Lev");
        m_bibleBooks.put("3 M", "Lev");
        m_bibleBooks.put("IIIM", "Lev");
        m_bibleBooks.put("III M", "Lev");
        m_bibleBooks.put("3Moj", "Lev");
        m_bibleBooks.put("3 Moj", "Lev");
        m_bibleBooks.put("IIIMoj", "Lev");
        m_bibleBooks.put("III Moj", "Lev");
        m_bibleBooks.put("3Mojzisova", "Lev");
        m_bibleBooks.put("3 Mojzisova", "Lev");
        m_bibleBooks.put("3Mojz", "Lev");
        m_bibleBooks.put("3 Mojz", "Lev");
        m_bibleBooks.put("IIIMojzisova", "Lev");
        m_bibleBooks.put("III Mojzisova", "Lev");
        m_bibleBooks.put("IIIMojz", "Lev");
        m_bibleBooks.put("III Mojz", "Lev");

        m_bibleBooks.put("Numeri", "Num");
        m_bibleBooks.put("4. kniha Mojžíšova", "Num");
        m_bibleBooks.put("Nu", "Num");
        m_bibleBooks.put("Nm", "Num");
        m_bibleBooks.put("Num", "Num");
        m_bibleBooks.put("4M", "Num");
        m_bibleBooks.put("4 M", "Num");
        m_bibleBooks.put("IVM", "Num");
        m_bibleBooks.put("IV M", "Num");
        m_bibleBooks.put("4Moj", "Num");
        m_bibleBooks.put("4 Moj", "Num");
        m_bibleBooks.put("IVMoj", "Num");
        m_bibleBooks.put("IV Moj", "Num");
        m_bibleBooks.put("Numer", "Num");
        m_bibleBooks.put("4Mojzisova", "Num");
        m_bibleBooks.put("4 Mojzisova", "Num");
        m_bibleBooks.put("4Mojz", "Num");
        m_bibleBooks.put("4 Mojz", "Num");
        m_bibleBooks.put("IVMojzisova", "Num");
        m_bibleBooks.put("IV Mojzisova", "Num");
        m_bibleBooks.put("IVMojz", "Num");
        m_bibleBooks.put("IV Mojz", "Num");

        m_bibleBooks.put("Deuteronomium", "Deut");
        m_bibleBooks.put("5. kniha Mojžíšova", "Deut");
        m_bibleBooks.put("Dt", "Deut");
        m_bibleBooks.put("Dtn", "Deut");
        m_bibleBooks.put("Deut", "Deut");
        m_bibleBooks.put("5M", "Deut");
        m_bibleBooks.put("5 M", "Deut");
        m_bibleBooks.put("VM", "Deut");
        m_bibleBooks.put("V M", "Deut");
        m_bibleBooks.put("5Moj", "Deut");
        m_bibleBooks.put("5 Moj", "Deut");
        m_bibleBooks.put("VMoj", "Deut");
        m_bibleBooks.put("V Moj", "Deut");
        m_bibleBooks.put("Deuter", "Deut");
        m_bibleBooks.put("Deuteron", "Deut");
        m_bibleBooks.put("5Mojzisova", "Deut");
        m_bibleBooks.put("5 Mojzisova", "Deut");
        m_bibleBooks.put("5Mojz", "Deut");
        m_bibleBooks.put("5 Mojz", "Deut");
        m_bibleBooks.put("VMojzisova", "Deut");
        m_bibleBooks.put("V Mojzisova", "Deut");
        m_bibleBooks.put("VMojz", "Deut");
        m_bibleBooks.put("V Mojz", "Deut");

        m_bibleBooks.put("Kniha Jozue", "Josh");
        m_bibleBooks.put("Joz", "Josh");
        m_bibleBooks.put("Jz", "Josh");

        m_bibleBooks.put("Kniha Soudců", "Judg");
        m_bibleBooks.put("Sd", "Judg");
        m_bibleBooks.put("Sdc", "Judg");
        m_bibleBooks.put("Soud", "Judg");
        m_bibleBooks.put("Soudc", "Judg");
        m_bibleBooks.put("Soudcu", "Judg");

        m_bibleBooks.put("Kniha Rút", "Ruth");
        m_bibleBooks.put("Rt", "Ruth");
        m_bibleBooks.put("Rut", "Ruth");
        m_bibleBooks.put("Rút", "Ruth");

        m_bibleBooks.put("1. Samuelova", "Sam1");
        m_bibleBooks.put("1S", "Sam1");
        m_bibleBooks.put("1 S", "Sam1");
        m_bibleBooks.put("I S", "Sam1");
        m_bibleBooks.put("1Sam", "Sam1");
        m_bibleBooks.put("1 Sam", "Sam1");
        m_bibleBooks.put("I Sam", "Sam1");
        m_bibleBooks.put("1Samuelova", "Sam1");
        m_bibleBooks.put("1 Samuelova", "Sam1");
        m_bibleBooks.put("ISamuelova", "Sam1");
        m_bibleBooks.put("I Samuelova", "Sam1");
        m_bibleBooks.put("1Sm", "Sam1");
        m_bibleBooks.put("1 Sm", "Sam1");
        m_bibleBooks.put("ISm", "Sam1");
        m_bibleBooks.put("I Sm", "Sam1");
        m_bibleBooks.put("1Samuel", "Sam1");
        m_bibleBooks.put("1 Samuel", "Sam1");
        m_bibleBooks.put("ISamuel", "Sam1");
        m_bibleBooks.put("I Samuel", "Sam1");

        m_bibleBooks.put("2. Samuelova", "Sam2");
        m_bibleBooks.put("2S", "Sam2");
        m_bibleBooks.put("2 S", "Sam2");
        m_bibleBooks.put("IIS", "Sam2");
        m_bibleBooks.put("II S", "Sam2");
        m_bibleBooks.put("2Sam", "Sam2");
        m_bibleBooks.put("2 Sam", "Sam2");
        m_bibleBooks.put("IISam", "Sam2");
        m_bibleBooks.put("II Sam", "Sam2");
        m_bibleBooks.put("IISamuelova", "Sam2");
        m_bibleBooks.put("II Samuelova", "Sam2");
        m_bibleBooks.put("2Samuelova", "Sam2");
        m_bibleBooks.put("2 Samuelova", "Sam2");
        m_bibleBooks.put("2Sm", "Sam2");
        m_bibleBooks.put("2 Sm", "Sam2");
        m_bibleBooks.put("IISm", "Sam2");
        m_bibleBooks.put("II Sm", "Sam2");
        m_bibleBooks.put("2Samuel", "Sam2");
        m_bibleBooks.put("2 Samuel", "Sam2");
        m_bibleBooks.put("IISamuel", "Sam2");
        m_bibleBooks.put("II Samuel", "Sam2");

        m_bibleBooks.put("1. Královská", "Kgs1");
        m_bibleBooks.put("1Kr", "Kgs1");
        m_bibleBooks.put("1 Kr", "Kgs1");
        m_bibleBooks.put("I Kr", "Kgs1");
        m_bibleBooks.put("1Kral", "Kgs1");
        m_bibleBooks.put("1 Kral", "Kgs1");
        m_bibleBooks.put("IKral", "Kgs1");
        m_bibleBooks.put("I Kral", "Kgs1");
        m_bibleBooks.put("1Král", "Kgs1");
        m_bibleBooks.put("1 Král", "Kgs1");
        m_bibleBooks.put("IKrál", "Kgs1");
        m_bibleBooks.put("I Král", "Kgs1");
        m_bibleBooks.put("1Kralovska", "Kgs1");
        m_bibleBooks.put("1 Kralovska", "Kgs1");
        m_bibleBooks.put("IKralovska", "Kgs1");
        m_bibleBooks.put("I Kralovska", "Kgs1");

        m_bibleBooks.put("2. Královská", "Kgs2");
        m_bibleBooks.put("2Kr", "Kgs2");
        m_bibleBooks.put("2 Kr", "Kgs2");
        m_bibleBooks.put("IIKr", "Kgs2");
        m_bibleBooks.put("II Kr", "Kgs2");
        m_bibleBooks.put("2Kral", "Kgs2");
        m_bibleBooks.put("2 Kral", "Kgs2");
        m_bibleBooks.put("IIKral", "Kgs2");
        m_bibleBooks.put("II Kral", "Kgs2");
        m_bibleBooks.put("2Král", "Kgs2");
        m_bibleBooks.put("2 Král", "Kgs2");
        m_bibleBooks.put("IIKrál", "Kgs2");
        m_bibleBooks.put("II Král", "Kgs2");
        m_bibleBooks.put("2Kralovska", "Kgs2");
        m_bibleBooks.put("2 Kralovska", "Kgs2");
        m_bibleBooks.put("IIKralovska", "Kgs2");
        m_bibleBooks.put("II Kralovska", "Kgs2");

        m_bibleBooks.put("1. Paralipomenon", "Chr1");
        m_bibleBooks.put("1. kniha Kronik", "Chr1");
        m_bibleBooks.put("1Pa", "Chr1");
        m_bibleBooks.put("1 Pa", "Chr1");
        m_bibleBooks.put("IPa", "Chr1");
        m_bibleBooks.put("I Pa", "Chr1");
        m_bibleBooks.put("1Par", "Chr1");
        m_bibleBooks.put("1 Par", "Chr1");
        m_bibleBooks.put("IPar", "Chr1");
        m_bibleBooks.put("I Par", "Chr1");
        m_bibleBooks.put("1Paralipomenon", "Chr1");
        m_bibleBooks.put("1 Paralipomenon", "Chr1");
        m_bibleBooks.put("IParalipomenon", "Chr1");
        m_bibleBooks.put("I Paralipomenon", "Chr1");
        m_bibleBooks.put("1Paral", "Chr1");
        m_bibleBooks.put("1 Paral", "Chr1");
        m_bibleBooks.put("IParal", "Chr1");
        m_bibleBooks.put("I Paral", "Chr1");
        m_bibleBooks.put("1Kron", "Chr1");
        m_bibleBooks.put("1 Kron", "Chr1");

        m_bibleBooks.put("2. Paralipomenon", "Chr2");
        m_bibleBooks.put("2. kniha Kronik", "Chr2");
        m_bibleBooks.put("2Pa", "Chr2");
        m_bibleBooks.put("2 Pa", "Chr2");
        m_bibleBooks.put("IIPa", "Chr2");
        m_bibleBooks.put("II Pa", "Chr2");
        m_bibleBooks.put("2Par", "Chr2");
        m_bibleBooks.put("2 Par", "Chr2");
        m_bibleBooks.put("IIPar", "Chr2");
        m_bibleBooks.put("II Par", "Chr2");
        m_bibleBooks.put("2Paralipomenon", "Chr2");
        m_bibleBooks.put("2 Paralipomenon", "Chr2");
        m_bibleBooks.put("IIParalipomenon", "Chr2");
        m_bibleBooks.put("II Paralipomenon", "Chr2");
        m_bibleBooks.put("2Paral", "Chr2");
        m_bibleBooks.put("2 Paral", "Chr2");
        m_bibleBooks.put("IIParal", "Chr2");
        m_bibleBooks.put("II Paral", "Chr2");
        m_bibleBooks.put("2Kron", "Chr2");
        m_bibleBooks.put("2 Kron", "Chr2");

        m_bibleBooks.put("Kniha Ezdrášova", "Ezra");
        m_bibleBooks.put("Ezd", "Ezra");
        m_bibleBooks.put("Ezdr", "Ezra");
        m_bibleBooks.put("Ezdras", "Ezra");

        m_bibleBooks.put("Kniha Nehemjášova", "Neh");
        m_bibleBooks.put("Neh", "Neh");
        m_bibleBooks.put("Ne", "Neh");
        m_bibleBooks.put("Nehemjas", "Neh");
        m_bibleBooks.put("Nehemias", "Neh");
        m_bibleBooks.put("Nehemijas", "Neh");

        m_bibleBooks.put("Kniha Ester", "Esth");
        m_bibleBooks.put("Est", "Esth");
        m_bibleBooks.put("Es", "Esth");

        m_bibleBooks.put("Kniha Jób", "Job");
        m_bibleBooks.put("Jb", "Job");
        m_bibleBooks.put("Job", "Job");
        m_bibleBooks.put("Jób", "Job");

        m_bibleBooks.put("Kniha Žalmů", "Ps");
        m_bibleBooks.put("Ž", "Ps");
        m_bibleBooks.put("Z", "Ps");
        m_bibleBooks.put("Žalm", "Ps");
        m_bibleBooks.put("Zalm", "Ps");
        m_bibleBooks.put("Žalmy", "Ps");
        m_bibleBooks.put("Zalmy", "Ps");
        m_bibleBooks.put("Zl", "Ps");
        m_bibleBooks.put("Zlm", "Ps");
        m_bibleBooks.put("Žl", "Ps");

        m_bibleBooks.put("Kniha Přísloví", "Prov");
        m_bibleBooks.put("Př", "Prov");
        m_bibleBooks.put("Pr", "Prov");
        m_bibleBooks.put("Přísl", "Prov");
        m_bibleBooks.put("Prisl", "Prov");
        m_bibleBooks.put("Pri", "Prov");
        m_bibleBooks.put("Prislov", "Prov");
        m_bibleBooks.put("Prislovi", "Prov");

        m_bibleBooks.put("Kniha Kazatel", "Eccl");
        m_bibleBooks.put("Kaz", "Eccl");
        m_bibleBooks.put("Ka", "Eccl");
        m_bibleBooks.put("Kz", "Eccl");
        m_bibleBooks.put("Kazat", "Eccl");

        m_bibleBooks.put("Píseň písní", "Song");
        m_bibleBooks.put("Pís", "Song");
        m_bibleBooks.put("Pis", "Song");
        m_bibleBooks.put("Pisen", "Song");
        m_bibleBooks.put("pisen pisni", "Song");

        m_bibleBooks.put("Izajáš", "Isa");
        m_bibleBooks.put("Iz", "Isa");
        m_bibleBooks.put("Iza", "Isa");
        m_bibleBooks.put("Izaj", "Isa");
        m_bibleBooks.put("Izaias", "Isa");
        m_bibleBooks.put("Izajas", "Isa");
        m_bibleBooks.put("Izaijas", "Isa");

        m_bibleBooks.put("Jeremjáš", "Jer");
        m_bibleBooks.put("Jr", "Jer");
        m_bibleBooks.put("Jer", "Jer");
        m_bibleBooks.put("Jerem", "Jer");
        m_bibleBooks.put("Jeremias", "Jer");
        m_bibleBooks.put("Jeremjas", "Jer");
        m_bibleBooks.put("Jeremijas", "Jer");

        m_bibleBooks.put("Pláč", "Lam");
        m_bibleBooks.put("Pl", "Lam");
        m_bibleBooks.put("Plac", "Lam");

        m_bibleBooks.put("Ezechiel", "Ezek");
        m_bibleBooks.put("Ez", "Ezek");
        m_bibleBooks.put("Ezech", "Ezek");

        m_bibleBooks.put("Daniel", "Dan");
        m_bibleBooks.put("Dn", "Dan");
        m_bibleBooks.put("Da", "Dan");
        m_bibleBooks.put("Dan", "Dan");

        m_bibleBooks.put("Ozeáš", "Hos");
        m_bibleBooks.put("Oz", "Hos");
        m_bibleBooks.put("Oze", "Hos");
        m_bibleBooks.put("Ozeas", "Hos");

        m_bibleBooks.put("Jóel", "Joel");
        m_bibleBooks.put("Jl", "Joel");
        m_bibleBooks.put("Jo", "Joel");
        m_bibleBooks.put("Jóel", "Joel");
        m_bibleBooks.put("Joel", "Joel");

        m_bibleBooks.put("Ámos", "Amos");
        m_bibleBooks.put("Am", "Amos");
        m_bibleBooks.put("Ámos", "Amos");
        m_bibleBooks.put("Amos", "Amos");

        m_bibleBooks.put("Abdijáš", "Obad");
        m_bibleBooks.put("Abd", "Obad");
        m_bibleBooks.put("Abdijas", "Obad");
        m_bibleBooks.put("Abdias", "Obad");

        m_bibleBooks.put("Jonáš", "Jonah");
        m_bibleBooks.put("Jon", "Jonah");
        m_bibleBooks.put("Jonáš", "Jonah");
        m_bibleBooks.put("Jonas", "Jonah");

        m_bibleBooks.put("Micheáš", "Mic");
        m_bibleBooks.put("Mi", "Mic");
        m_bibleBooks.put("Mich", "Mic");
        m_bibleBooks.put("Micheas", "Mic");

        m_bibleBooks.put("Nahum", "Nah");
        m_bibleBooks.put("Na", "Nah");
        m_bibleBooks.put("Nah", "Nah");
        m_bibleBooks.put("Nahum", "Nah");

        m_bibleBooks.put("Habakuk", "Hab");
        m_bibleBooks.put("Abakuk", "Hab");
        m_bibleBooks.put("Ab", "Hab");
        m_bibleBooks.put("Abk", "Hab");
        m_bibleBooks.put("Abak", "Hab");
        m_bibleBooks.put("Hab", "Hab");

        m_bibleBooks.put("Sofonjáš", "Zeph");
        m_bibleBooks.put("Sf", "Zeph");
        m_bibleBooks.put("Sof", "Zeph");
        m_bibleBooks.put("Sofon", "Zeph");
        m_bibleBooks.put("Sofonjas", "Zeph");
        m_bibleBooks.put("Sofonias", "Zeph");
        m_bibleBooks.put("Sofonijas", "Zeph");

        m_bibleBooks.put("Ageus", "Hag");
        m_bibleBooks.put("Ag", "Hag");
        m_bibleBooks.put("Ageus", "Hag");
        m_bibleBooks.put("Hag", "Hag");
        m_bibleBooks.put("Hageus", "Hag");
        m_bibleBooks.put("Hg", "Hag");

        m_bibleBooks.put("Zacharjáš", "Zech");
        m_bibleBooks.put("Za", "Zech");
        m_bibleBooks.put("Zach", "Zech");
        m_bibleBooks.put("Zachar", "Zech");
        m_bibleBooks.put("Zacharjas", "Zech");
        m_bibleBooks.put("Zacharias", "Zech");
        m_bibleBooks.put("Zacharijas", "Zech");
        m_bibleBooks.put("Zch", "Zech");

        m_bibleBooks.put("Malachjáš", "Mal");
        m_bibleBooks.put("Mal", "Mal");
        m_bibleBooks.put("Ma", "Mal");
        m_bibleBooks.put("Malach", "Mal");
        m_bibleBooks.put("Malachjas", "Mal");
        m_bibleBooks.put("Malachias", "Mal");
        m_bibleBooks.put("Malachijas", "Mal");

        m_bibleBooks.put("Tóbit", "Tob");
        m_bibleBooks.put("Tobiáš", "Tob");
        m_bibleBooks.put("Tób", "Tob");
        m_bibleBooks.put("Tob", "Tob");
        m_bibleBooks.put("Tóbit", "Tob");
        m_bibleBooks.put("Tobit", "Tob");
        m_bibleBooks.put("Tb", "Tob");

        m_bibleBooks.put("Júdit", "Jdt");
        m_bibleBooks.put("Júd", "Jdt");
        m_bibleBooks.put("Jud", "Jdt");
        m_bibleBooks.put("Júdit", "Jdt");
        m_bibleBooks.put("Judit", "Jdt");
        m_bibleBooks.put("Jdt", "Jdt");

        m_bibleBooks.put("Přídavky k Ester", "AddEsth");
        m_bibleBooks.put("Est", "AddEsth");

        m_bibleBooks.put("Kniha Moudrosti", "Wis");
        m_bibleBooks.put("Sap", "Wis");
        m_bibleBooks.put("Mdr", "Wis");
        m_bibleBooks.put("Moud", "Wis");
        m_bibleBooks.put("Moudr", "Wis");
        m_bibleBooks.put("Moudrost", "Wis");

        m_bibleBooks.put("Sírach", "Sir");
        m_bibleBooks.put("Sír", "Sir");
        m_bibleBooks.put("Sir", "Sir");
        m_bibleBooks.put("Eccl", "Sir");
        m_bibleBooks.put("Ecc", "Sir");
        m_bibleBooks.put("Sirach", "Sir");
        m_bibleBooks.put("Sirachovec", "Sir");

        m_bibleBooks.put("Báruk", "Bar");
        m_bibleBooks.put("Bár", "Bar");
        m_bibleBooks.put("Bar", "Bar");
        m_bibleBooks.put("Báruk", "Bar");
        m_bibleBooks.put("Baruk", "Bar");
        m_bibleBooks.put("Baruch", "Bar");

        m_bibleBooks.put("Přídavky k Danielovi", "PrAzar");
        m_bibleBooks.put("Da", "PrAzar");

        m_bibleBooks.put("1. kniha Makabejská", "Macc1");
        m_bibleBooks.put("1Mak", "Macc1");
        m_bibleBooks.put("1 Mak", "Macc1");
        m_bibleBooks.put("IMak", "Macc1");
        m_bibleBooks.put("I Mak", "Macc1");
        m_bibleBooks.put("1Makab", "Macc1");
        m_bibleBooks.put("1 Makab", "Macc1");
        m_bibleBooks.put("IMakab", "Macc1");
        m_bibleBooks.put("I Makab", "Macc1");
        m_bibleBooks.put("1Makabejska", "Macc1");
        m_bibleBooks.put("1 Makabejska", "Macc1");
        m_bibleBooks.put("IMakabejska", "Macc1");
        m_bibleBooks.put("I Makabejska", "Macc1");
        m_bibleBooks.put("1Makabejskych", "Macc1");
        m_bibleBooks.put("1 Makabejskych", "Macc1");
        m_bibleBooks.put("IMakabejskych", "Macc1");
        m_bibleBooks.put("I Makabejskych", "Macc1");
        m_bibleBooks.put("1Mk", "Macc1");
        m_bibleBooks.put("1 Mk", "Macc1");

        m_bibleBooks.put("2. kniha Makabejská", "Macc2");
        m_bibleBooks.put("2Mak", "Macc2");
        m_bibleBooks.put("2 Mak", "Macc2");
        m_bibleBooks.put("IIMak", "Macc2");
        m_bibleBooks.put("II Mak", "Macc2");
        m_bibleBooks.put("2Makab", "Macc2");
        m_bibleBooks.put("2 Makab", "Macc2");
        m_bibleBooks.put("IIMakab", "Macc2");
        m_bibleBooks.put("II Makab", "Macc2");
        m_bibleBooks.put("2Makabejska", "Macc2");
        m_bibleBooks.put("2 Makabejska", "Macc2");
        m_bibleBooks.put("IIMakabejska", "Macc2");
        m_bibleBooks.put("II Makabejska", "Macc2");
        m_bibleBooks.put("2Makabejskych", "Macc2");
        m_bibleBooks.put("2 Makabejskych", "Macc2");
        m_bibleBooks.put("IIMakabejskych", "Macc2");
        m_bibleBooks.put("II Makabejskych", "Macc2");
        m_bibleBooks.put("2Mk", "Macc2");
        m_bibleBooks.put("2 Mk", "Macc2");

        // === NEW TESTAMENT ===

        m_bibleBooks.put("Matouš", "Matt");
        m_bibleBooks.put("Mt", "Matt");
        m_bibleBooks.put("Mat", "Matt");
        m_bibleBooks.put("Matous", "Matt");

        m_bibleBooks.put("Marek", "Mark");
        m_bibleBooks.put("Mk", "Mark");
        m_bibleBooks.put("Marek", "Mark");
        m_bibleBooks.put("Mar", "Mark");

        m_bibleBooks.put("Lukáš", "Luke");
        m_bibleBooks.put("L", "Luke");
        m_bibleBooks.put("Lk", "Luke");
        m_bibleBooks.put("Luk", "Luke");
        m_bibleBooks.put("Lukas", "Luke");

        m_bibleBooks.put("Jan", "John");
        m_bibleBooks.put("J", "John");
        m_bibleBooks.put("Jan", "John");

        m_bibleBooks.put("Skutky apoštolů", "Acts");
        m_bibleBooks.put("Sk", "Acts");
        m_bibleBooks.put("Skut", "Acts");
        m_bibleBooks.put("Skutk", "Acts");
        m_bibleBooks.put("Akt", "Acts");

        m_bibleBooks.put("Římanům", "Rom");
        m_bibleBooks.put("Ř", "Rom");
        m_bibleBooks.put("R", "Rom");
        m_bibleBooks.put("Řím", "Rom");
        m_bibleBooks.put("Rim", "Rom");
        m_bibleBooks.put("Riman", "Rom");
        m_bibleBooks.put("Rimanum", "Rom");

        m_bibleBooks.put("1. Korintským", "Cor1");
        m_bibleBooks.put("1K", "Cor1");
        m_bibleBooks.put("1 K", "Cor1");
        m_bibleBooks.put("IK", "Cor1");
        m_bibleBooks.put("I K", "Cor1");
        m_bibleBooks.put("1Kor", "Cor1");
        m_bibleBooks.put("1 Kor", "Cor1");
        m_bibleBooks.put("IKor", "Cor1");
        m_bibleBooks.put("I Kor", "Cor1");
        m_bibleBooks.put("1Korint", "Cor1");
        m_bibleBooks.put("1 Korint", "Cor1");
        m_bibleBooks.put("IKorint", "Cor1");
        m_bibleBooks.put("I Korint", "Cor1");
        m_bibleBooks.put("1Korintskym", "Cor1");
        m_bibleBooks.put("1 Korintskym", "Cor1");
        m_bibleBooks.put("IKorintskym", "Cor1");
        m_bibleBooks.put("I Korintskym", "Cor1");

        m_bibleBooks.put("2. Korintským", "Cor2");
        m_bibleBooks.put("2K", "Cor2");
        m_bibleBooks.put("2 K", "Cor2");
        m_bibleBooks.put("IIK", "Cor2");
        m_bibleBooks.put("II K", "Cor2");
        m_bibleBooks.put("2Kor", "Cor2");
        m_bibleBooks.put("2 Kor", "Cor2");
        m_bibleBooks.put("IIKor", "Cor2");
        m_bibleBooks.put("II Kor", "Cor2");
        m_bibleBooks.put("2Korint", "Cor2");
        m_bibleBooks.put("2 Korint", "Cor2");
        m_bibleBooks.put("IIKorint", "Cor2");
        m_bibleBooks.put("II Korint", "Cor2");
        m_bibleBooks.put("2Korintskym", "Cor2");
        m_bibleBooks.put("2 Korintskym", "Cor2");
        m_bibleBooks.put("IIKorintskym", "Cor2");
        m_bibleBooks.put("II Korintskym", "Cor2");

        m_bibleBooks.put("Galatským", "Gal");
        m_bibleBooks.put("Ga", "Gal");
        m_bibleBooks.put("Gal", "Gal");
        m_bibleBooks.put("Galat", "Gal");
        m_bibleBooks.put("Galatskym", "Gal");
        m_bibleBooks.put("Gl", "Gal");

        m_bibleBooks.put("Efezským", "Eph");
        m_bibleBooks.put("Ef", "Eph");
        m_bibleBooks.put("Efes", "Eph");
        m_bibleBooks.put("Efez", "Eph");
        m_bibleBooks.put("Efezskym", "Eph");
        m_bibleBooks.put("Efezkym", "Eph");
        m_bibleBooks.put("Efeskym", "Eph");

        m_bibleBooks.put("Filipským", "Phil");
        m_bibleBooks.put("Fp", "Phil");
        m_bibleBooks.put("F", "Phil");
        m_bibleBooks.put("Filip", "Phil");
        m_bibleBooks.put("Filipskym", "Phil");
        m_bibleBooks.put("Flp", "Phil");

        m_bibleBooks.put("Koloským", "Col");
        m_bibleBooks.put("Ko", "Col");
        m_bibleBooks.put("Kol", "Col");
        m_bibleBooks.put("Kolos", "Col");
        m_bibleBooks.put("Koloskym", "Col");
        m_bibleBooks.put("Kolosskym", "Col");
        m_bibleBooks.put("Kl", "Col");

        m_bibleBooks.put("1. Tesalonickým", "Thess1");
        m_bibleBooks.put("1. Soluňanům", "Thess1");
        m_bibleBooks.put("1Te", "Thess1");
        m_bibleBooks.put("1 Te", "Thess1");
        m_bibleBooks.put("ITe", "Thess1");
        m_bibleBooks.put("I Te", "Thess1");
        m_bibleBooks.put("1T", "Thess1");
        m_bibleBooks.put("1 T", "Thess1");
        m_bibleBooks.put("IT", "Thess1");
        m_bibleBooks.put("I T", "Thess1");
        m_bibleBooks.put("1Tes", "Thess1");
        m_bibleBooks.put("1 Tes", "Thess1");
        m_bibleBooks.put("ITes", "Thess1");
        m_bibleBooks.put("I Tes", "Thess1");
        m_bibleBooks.put("1Tesal", "Thess1");
        m_bibleBooks.put("1 Tesal", "Thess1");
        m_bibleBooks.put("ITesal", "Thess1");
        m_bibleBooks.put("I Tesal", "Thess1");
        m_bibleBooks.put("1Tesalon", "Thess1");
        m_bibleBooks.put("1 Tesalon", "Thess1");
        m_bibleBooks.put("ITesalon", "Thess1");
        m_bibleBooks.put("I Tesalon", "Thess1");
        m_bibleBooks.put("1Tesalonickym", "Thess1");
        m_bibleBooks.put("1 Tesalonickym", "Thess1");
        m_bibleBooks.put("ITesalonickym", "Thess1");
        m_bibleBooks.put("I Tesalonickym", "Thess1");
        m_bibleBooks.put("1Sol", "Thess1");
        m_bibleBooks.put("1 Sol", "Thess1");

        m_bibleBooks.put("2. Tesalonickým", "Thess2");
        m_bibleBooks.put("1. Soluňanům", "Thess2");
        m_bibleBooks.put("2Te", "Thess2");
        m_bibleBooks.put("2 Te", "Thess2");
        m_bibleBooks.put("IITe", "Thess2");
        m_bibleBooks.put("II Te", "Thess2");
        m_bibleBooks.put("2T", "Thess2");
        m_bibleBooks.put("2 T", "Thess2");
        m_bibleBooks.put("IIT", "Thess2");
        m_bibleBooks.put("II T", "Thess2");
        m_bibleBooks.put("2Tes", "Thess2");
        m_bibleBooks.put("2 Tes", "Thess2");
        m_bibleBooks.put("IITes", "Thess2");
        m_bibleBooks.put("II Tes", "Thess2");
        m_bibleBooks.put("2Tesal", "Thess2");
        m_bibleBooks.put("2 Tesal", "Thess2");
        m_bibleBooks.put("IITesal", "Thess2");
        m_bibleBooks.put("II Tesal", "Thess2");
        m_bibleBooks.put("2Tesalon", "Thess2");
        m_bibleBooks.put("2 Tesalon", "Thess2");
        m_bibleBooks.put("IITesalon", "Thess2");
        m_bibleBooks.put("II Tesalon", "Thess2");
        m_bibleBooks.put("2Tesalonickym", "Thess2");
        m_bibleBooks.put("2 Tesalonickym", "Thess2");
        m_bibleBooks.put("IITesalonickym", "Thess2");
        m_bibleBooks.put("II Tesalonickym", "Thess2");
        m_bibleBooks.put("2Sol", "Thess2");
        m_bibleBooks.put("2 Sol", "Thess2");

        m_bibleBooks.put("1. Timoteovi", "Tim1");
        m_bibleBooks.put("1Tm", "Tim1");
        m_bibleBooks.put("1 Tm", "Tim1");
        m_bibleBooks.put("ITm", "Tim1");
        m_bibleBooks.put("I Tm", "Tim1");
        m_bibleBooks.put("1Tim", "Tim1");
        m_bibleBooks.put("1 Tim", "Tim1");
        m_bibleBooks.put("ITim", "Tim1");
        m_bibleBooks.put("I Tim", "Tim1");
        m_bibleBooks.put("1Timot", "Tim1");
        m_bibleBooks.put("1 Timot", "Tim1");
        m_bibleBooks.put("ITimot", "Tim1");
        m_bibleBooks.put("I Timot", "Tim1");
        m_bibleBooks.put("1Timoteovi", "Tim1");
        m_bibleBooks.put("1 Timoteovi", "Tim1");
        m_bibleBooks.put("ITimoteovi", "Tim1");
        m_bibleBooks.put("I Timoteovi", "Tim1");
        m_bibleBooks.put("1Timoteova", "Tim1");
        m_bibleBooks.put("1 Timoteova", "Tim1");
        m_bibleBooks.put("ITimoteova", "Tim1");
        m_bibleBooks.put("I Timoteova", "Tim1");

        m_bibleBooks.put("2. Timoteovi", "Tim2");
        m_bibleBooks.put("2Tm", "Tim2");
        m_bibleBooks.put("2 Tm", "Tim2");
        m_bibleBooks.put("IITm", "Tim2");
        m_bibleBooks.put("II Tm", "Tim2");
        m_bibleBooks.put("2Tim", "Tim2");
        m_bibleBooks.put("2 Tim", "Tim2");
        m_bibleBooks.put("IITim", "Tim2");
        m_bibleBooks.put("II Tim", "Tim2");
        m_bibleBooks.put("2Timot", "Tim2");
        m_bibleBooks.put("2 Timot", "Tim2");
        m_bibleBooks.put("IITimot", "Tim2");
        m_bibleBooks.put("II Timot", "Tim2");
        m_bibleBooks.put("2Timoteovi", "Tim2");
        m_bibleBooks.put("2 Timoteovi", "Tim2");
        m_bibleBooks.put("IITimoteovi", "Tim2");
        m_bibleBooks.put("II Timoteovi", "Tim2");
        m_bibleBooks.put("2Timoteova", "Tim2");
        m_bibleBooks.put("2 Timoteova", "Tim2");
        m_bibleBooks.put("IITimoteova", "Tim2");
        m_bibleBooks.put("II Timoteova", "Tim2");

        m_bibleBooks.put("Titovi", "Titus");
        m_bibleBooks.put("Tt", "Titus");
        m_bibleBooks.put("Tit", "Titus");
        m_bibleBooks.put("Titova", "Titus");

        m_bibleBooks.put("Filemonovi", "Phlm");
        m_bibleBooks.put("Fm", "Phlm");
        m_bibleBooks.put("Filem", "Phlm");
        m_bibleBooks.put("Filemon", "Phlm");
        m_bibleBooks.put("Filemonova", "Phlm");

        m_bibleBooks.put("Židům", "Heb");
        m_bibleBooks.put("Žd", "Heb");
        m_bibleBooks.put("Zd", "Heb");
        m_bibleBooks.put("Zid", "Heb");
        m_bibleBooks.put("Zidum", "Heb");
        m_bibleBooks.put("Heb", "Heb");
        m_bibleBooks.put("Žid", "Heb");

        m_bibleBooks.put("Jakub", "Jas");
        m_bibleBooks.put("Jk", "Jas");
        m_bibleBooks.put("Jak", "Jas");
        m_bibleBooks.put("Jakub", "Jas");
        m_bibleBooks.put("Jakubova", "Jas");
        m_bibleBooks.put("Jkb", "Jas");

        m_bibleBooks.put("1. list Petrův", "Pet1");
        m_bibleBooks.put("1Pt", "Pet1");
        m_bibleBooks.put("1 Pt", "Pet1");
        m_bibleBooks.put("IPt", "Pet1");
        m_bibleBooks.put("I Pt", "Pet1");
        m_bibleBooks.put("1P", "Pet1");
        m_bibleBooks.put("1 P", "Pet1");
        m_bibleBooks.put("IP", "Pet1");
        m_bibleBooks.put("I P", "Pet1");
        m_bibleBooks.put("1Petr", "Pet1");
        m_bibleBooks.put("1 Petr", "Pet1");
        m_bibleBooks.put("IPetr", "Pet1");
        m_bibleBooks.put("I Petr", "Pet1");
        m_bibleBooks.put("1Petrova", "Pet1");
        m_bibleBooks.put("1 Petrova", "Pet1");
        m_bibleBooks.put("IPetrova", "Pet1");
        m_bibleBooks.put("I Petrova", "Pet1");

        m_bibleBooks.put("2. list Petrův", "Pet2");
        m_bibleBooks.put("2Pt", "Pet2");
        m_bibleBooks.put("2 Pt", "Pet2");
        m_bibleBooks.put("IIPt", "Pet2");
        m_bibleBooks.put("II Pt", "Pet2");
        m_bibleBooks.put("2P", "Pet2");
        m_bibleBooks.put("2 P", "Pet2");
        m_bibleBooks.put("IIP", "Pet2");
        m_bibleBooks.put("II P", "Pet2");
        m_bibleBooks.put("2Petr", "Pet2");
        m_bibleBooks.put("2 Petr", "Pet2");
        m_bibleBooks.put("IIPetr", "Pet2");
        m_bibleBooks.put("II Petr", "Pet2");
        m_bibleBooks.put("2Petrova", "Pet2");
        m_bibleBooks.put("2 Petrova", "Pet2");
        m_bibleBooks.put("IIPetrova", "Pet2");
        m_bibleBooks.put("II Petrova", "Pet2");

        m_bibleBooks.put("1. list Janův", "John1");
        m_bibleBooks.put("1J", "John1");
        m_bibleBooks.put("1 J", "John1");
        m_bibleBooks.put("IJ", "John1");
        m_bibleBooks.put("I J", "John1");
        m_bibleBooks.put("1Jan", "John1");
        m_bibleBooks.put("1 Jan", "John1");
        m_bibleBooks.put("IJan", "John1");
        m_bibleBooks.put("I Jan", "John1");
        m_bibleBooks.put("1Janova", "John1");
        m_bibleBooks.put("1 Janova", "John1");
        m_bibleBooks.put("IJanova", "John1");
        m_bibleBooks.put("I Janova", "John1");

        m_bibleBooks.put("2. list Janův", "John2");
        m_bibleBooks.put("2J", "John2");
        m_bibleBooks.put("2 J", "John2");
        m_bibleBooks.put("IIJ", "John2");
        m_bibleBooks.put("II J", "John2");
        m_bibleBooks.put("2Jan", "John2");
        m_bibleBooks.put("2 Jan", "John2");
        m_bibleBooks.put("IIJan", "John2");
        m_bibleBooks.put("II Jan", "John2");
        m_bibleBooks.put("2Janova", "John2");
        m_bibleBooks.put("2 Janova", "John2");
        m_bibleBooks.put("IIJanova", "John2");
        m_bibleBooks.put("II Janova", "John2");

        m_bibleBooks.put("3. list Janův", "John3");
        m_bibleBooks.put("3J", "John3");
        m_bibleBooks.put("3 J", "John3");
        m_bibleBooks.put("IIIJ", "John3");
        m_bibleBooks.put("III J", "John3");
        m_bibleBooks.put("3Jan", "John3");
        m_bibleBooks.put("3 Jan", "John3");
        m_bibleBooks.put("IIIJan", "John3");
        m_bibleBooks.put("III Jan", "John3");
        m_bibleBooks.put("3Janova", "John3");
        m_bibleBooks.put("3 Janova", "John3");
        m_bibleBooks.put("IIIJanova", "John3");
        m_bibleBooks.put("III Janova", "John3");

        m_bibleBooks.put("List Judův", "Jude");
        m_bibleBooks.put("Ju", "Jude");
        m_bibleBooks.put("Juda", "Jude");
        m_bibleBooks.put("Jd", "Jude");

        m_bibleBooks.put("Zjevení Janovo", "Rev");
        m_bibleBooks.put("Zj", "Rev");
        m_bibleBooks.put("Zjev", "Rev");
        m_bibleBooks.put("Apok", "Rev");
        m_bibleBooks.put("Zjv", "Rev");
        m_bibleBooks.put("Zjeveni", "Rev");
	}

	// RemoteViews cannot be updated partially (at least in older API releases).
	// A widget update must fully reconstruct all the properties of RemoteViews.
	private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId)
	{
        // TODO: Debug logs are compiled in but stripped at runtime
        Log.d("updateWidget ", "Updating widget Id: " + appWidgetId);

		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		// 1. SETUP INTENTS
		// =====================================================================
        // Clicking on Quote displays a dialog
        Intent a1 = new Intent(context, DialogActivity.class);
		a1.setAction(context.getResources().getString(R.string.ONCLICK_QUOTE_ACTION));
		a1.putExtra(context.getResources().getString(R.string.BUNDLE_KEY), getChapter());

		// FLAG_CANCEL_CURRENT is necessary in order for the extras to be sent
		PendingIntent pi1 = PendingIntent.getActivity(context, 0, a1,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        updateViews.setOnClickPendingIntent(R.id.quoteHeader, pi1);
        updateViews.setOnClickPendingIntent(R.id.quoteOfTheDay, pi1);
        
        // Clicking on Topic displays a dialog
        Intent a2 = new Intent(context, DialogActivity.class);
		a2.putExtra(context.getResources().getString(R.string.BUNDLE_KEY), getTopic());
		a2.setAction(context.getString(R.string.ONCLICK_TOPIC_ACTION));

		// FLAG_CANCEL_CURRENT is necessary in order for the extras to be sent
		PendingIntent pi2 = PendingIntent.getActivity(context, 0, a2,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        updateViews.setOnClickPendingIntent(R.id.topicHeader, pi2);
        updateViews.setOnClickPendingIntent(R.id.topicOfTheWeek, pi2);

		// 2. SET GUI ATTRIBUTES
		// =====================================================================
		// Set font size
		String string = prefs.getString("widget_font_size", null);
		if (string != null)
		{
			float floatValue = Float.parseFloat(string);
			updateViews.setFloat(R.id.quoteOfTheDay, "setTextSize", floatValue);
			updateViews.setFloat(R.id.quoteHeader, "setTextSize", floatValue);
			updateViews.setFloat(R.id.topicOfTheWeek, "setTextSize", floatValue);
			updateViews.setFloat(R.id.topicHeader, "setTextSize", floatValue);
		}

		// Set font colour
		Integer value = prefs.getInt("widget_font_colour", Color.LTGRAY);
		if (value != null)
		{
			updateViews.setTextColor(R.id.quoteOfTheDay, value);
			updateViews.setTextColor(R.id.quoteHeader, value);
			updateViews.setTextColor(R.id.topicOfTheWeek, value);
			updateViews.setTextColor(R.id.topicHeader, value);
		}

        // 3. SET TEXT
		// =====================================================================
		if (m_quoteShort != null)
		{
			updateViews.setTextViewText(R.id.quoteOfTheDay, m_quoteShort);
		}
		if (m_topicShort != null)
		{
			updateViews.setTextViewText(R.id.topicOfTheWeek, m_topicShort);
		}
        
		// PERFORM THE UPDATE
		// =====================================================================
        appWidgetManager.updateAppWidget(appWidgetId, updateViews);
	}

	private void downloadQuote(Context context)
	{
		WebDownloadContent httpDownload = new WebDownloadContent(context, this,
                R.id.quoteOfTheDay);
		httpDownload.execute(context.getString(R.string.quoteUrl) +
                             context.getString(R.string.quoteFileName));
	}

	private void downloadQuoteDetail(Context context)
	{
		String  book 	= "";
		String	bookRef = "";
		String  chapter = "";
		String  url 	= "";

		if ((m_quote == "") || (m_quote == null))
		{
			return;
		}
				
		// Parse the downloaded quote of the day
		Document 	doc	= Jsoup.parse(m_quote);

		// Find the book/chapter/verse
		Element 	el 	= doc.getElementById(context.getString(R.string.quoteRefElemId));
		
		if (el == null)
		{
			return;
		}

		// Translate element's text into an URL used to get the chapter. 
		// Example of the text: "(Lk  10,21)"
		// Example of the URL: "http://www.biblenet.cz/b/Matt/9"
		// .   ... any character
		// \\s ... whitespace
		// \\S ... non-whitespace character
		// \\d ... digit
		// X*? ... X, zero or more times (RELUCTANT mode) 
		//
		// Book   : group(1)
		// Chapter: group(2)
		// Verse  : group(5) - currently not used
		// Pattern p = Pattern.compile("[(](.+)(\\s+)(\\d+)(\\s*)[,](\\s*)(\\d+)(\\s*)[)]");
		// Pattern p = Pattern.compile("[(](.*?)(\\d+)(\\s*)[,](\\s*)(\\d+)(\\s*)[)]");
        // Pattern p = Pattern.compile("[(](.*?)(\\d+)(.*?)[)]");
        Pattern p = Pattern.compile("[(](.*?)(\\d+)(\\s*)[,|:|-](.*?)[)]");
        Matcher m = p.matcher(el.text().trim());

		if (m.matches())
		{
			book 	= m.group(1).trim();
			chapter = m.group(2).trim();
		}
		else
		{
			Toast.makeText(context,
                    context.getResources().getString(R.string.error_quote_title) + el.text().trim() +
                            context.getResources().getString(R.string.error_quote_syntax),
					Toast.LENGTH_LONG).show();
			return;
		}

		// Translate the book into the form that biblenet.cz understands
		bookRef = m_bibleBooks.get(book);
		if (bookRef == null)
		{
			Toast.makeText(context,
                    context.getResources().getString(R.string.error_book_title) + book +
                            context.getResources().getString(R.string.error_book_unknown),
					Toast.LENGTH_LONG).show();
			return;
		}

		// Construct the URL and start the download
		url = context.getString(R.string.quoteDetailUrl) + bookRef + "/" + chapter;
		WebDownloadContent httpDownload = new WebDownloadContent(context, this,
                R.id.dialogTextView);
		httpDownload.execute(url);
	}	

	private void downloadTopic(Context context)
	{
		WebDownloadContent httpDownload = new WebDownloadContent(context, this,
                R.id.topicOfTheWeek);
		httpDownload.execute(context.getString(R.string.topicUrl) +
                             context.getString(R.string.topicFileName));
	}

	private void uploadQuote(Context context)
	{
		WebUploadContent ftpUpload = new WebUploadContent(context, this,
                R.id.quoteOfTheDay);
		ftpUpload.execute();
	}

	private void uploadTopic(Context context)
	{
		WebUploadContent ftpUpload = new WebUploadContent(context, this,
                R.id.topicOfTheWeek);
		ftpUpload.execute();
	}
	
	// Sets "Quote of the Day"
	private void parseQuote(Context context, String htmlString)
	{
		try
        {
            // Ignore empty data
            if ((htmlString == "") || (htmlString == null))
            {
                throw new Exception();
            }

            Document doc = Jsoup.parse(htmlString);
            Element el1  = doc.getElementById(context.getString(R.string.quoteTextElemId));
            Element el2  = doc.getElementById(context.getString(R.string.quoteRefElemId));

            // Ignore empty data
            if ((el1 != null) && (el2 != null))
            {
                // Update the member variable
                m_quoteShort = el1.text() + " " + el2.text();

                // Store the full HTML code
                m_quote = htmlString;

                if (m_alert)
                {
                    // Alert the user but keep the alert flag set (will be unset later)
                    Toast.makeText(context, context.getString(R.string.quote_updated),
                            Toast.LENGTH_SHORT).show();
                }
            }
            else
            {
                throw new Exception();
            }
        }
		catch (Exception e)
        {
            // Alert the user but keep the alert flag set (will be unset later)
            Toast.makeText(context, context.getResources().getString(R.string.quote_empty),
                    Toast.LENGTH_SHORT).show();
        }
	}

	// Sets biblical chapter detail
	private void parseChapter(Context context, String htmlString)
	{
		// Ignore empty data
		if ((htmlString == "") || (htmlString == null))
		{
			return;
		}

		Document doc	= Jsoup.parse(htmlString);
        doc.setBaseUri("http://www.biblenet.cz");

        // Replace relative links with absolute ones
        Elements urls = doc.select("a[href]");
        for( Element urlElement : urls )
        {
            urlElement.attr("href", urlElement.absUrl("href"));
        }

        // Cleanup: Remove all instances of class="chapterControls" (used to
		// create navigation links) and class="mainForm" (used to implement the
		// book selector)
		doc.select(context.getResources().getString(R.string.quoteDetailJunk1)).remove();
		doc.select(context.getResources().getString(R.string.quoteDetailJunk2)).remove();

        // Ignore empty and identical data
        Element el 	= doc.getElementById(context.getString(R.string.quoteDetailElemId));
        if ((el != null) && (el.html() != m_chapter))
		{
			// Store the full HTML code
			m_chapter = el.html();
		}
	}

	// Sets "Topic of the Week"
	private void parseTopic(Context context, String htmlString)
	{
		try
        {
            // Ignore empty data
            if ((htmlString == "") || (htmlString == null))
            {
                throw new Exception();
            }

            Document doc = Jsoup.parse(htmlString);
            Element el1  = doc.getElementById(context.getString(R.string.topicTextElemId));
            Element el2  = doc.getElementById(context.getString(R.string.topicDateElemId));

            // Ignore empty data
            if ((el1 != null) && (el2 != null))
            {
                // Update the member variable
                m_topicShort = el1.text() + " " + el2.text();

                // Store the full HTML code
                m_topic = htmlString;

                if (m_alert)
                {
                    // Alert the user and unset the alert flag
                    Toast.makeText(context, context.getString(R.string.topic_updated), Toast.LENGTH_SHORT).show();
                    m_alert = false;
                }
            }
            else
            {
                throw new Exception();
            }
        }
		catch (Exception e)
        {
            // Alert the user and unset the flag
            Toast.makeText(context, context.getString(R.string.topic_empty), Toast.LENGTH_SHORT).show();
            m_alert = false;
        }
	}

	private void loadState(Context context)
	{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		m_quoteShort = prefs.getString("m_quoteShort", m_quoteShort);
		m_topicShort = prefs.getString("m_topicShort", m_topicShort);
		m_quote = prefs.getString("m_quote", m_quote);
		m_chapter = prefs.getString("m_chapter", m_chapter);
		m_topic = prefs.getString("m_topic", m_topic);
	}

	private void saveState(Context context)
	{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();

		if (m_quoteShort != null)
		{
			editor.putString("m_quoteShort", m_quoteShort);
		}
		if (m_topicShort != null)
		{
			editor.putString("m_topicShort", m_topicShort);
		}
		if (m_quote != null)
		{
			editor.putString("m_quote", m_quote);
		}
		if (m_chapter != null)
		{
			editor.putString("m_chapter", m_chapter);
		}
		if (m_topic != null)
		{
			editor.putString("m_topic", m_topic);
		}
		editor.commit();
	}
}
package ca.sustr.vira.cz;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;

public class WebDownloadContent extends AsyncTask<String, Void, String>
{
	private Context				m_context;
	private WidgetService		m_widgetService;
	private int 				m_viewID;
	
	public WebDownloadContent(Context context, WidgetService widgetService, int viewID)
	{
		m_context		= context;
		m_widgetService = widgetService;
		m_viewID 		= viewID;
	}
	
	@Override
	protected String doInBackground(String... arg0)
	{
		String httpResponse = "";
		for (String url : arg0)
		{
			try
			{
				URL urlObj = new URL(url);
				HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();
				InputStream content = urlConnection.getInputStream();
		    	BufferedReader buffer = new BufferedReader(new InputStreamReader(content, WebEncoding.getEncoding(m_viewID)));
		    	String s = "";
		        while ((s = buffer.readLine()) != null)
		        {
		        	httpResponse += s;
		        }
			}
			catch (Exception e)
			{
		    	e.printStackTrace();
			}
		}
        
		return httpResponse;
	}

	@Override
	protected void onPostExecute(String result)
	{
		m_widgetService.onReceiveDownload(m_context, m_viewID, result);
	}
}
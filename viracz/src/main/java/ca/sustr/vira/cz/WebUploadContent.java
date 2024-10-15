package ca.sustr.vira.cz;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class WebUploadContent extends AsyncTask<Void, Void, Void>
{
	private WidgetService		m_widgetService;
	private int 				m_viewID;
	private SharedPreferences	m_prefs;
	private boolean				m_uploadAllowed;
	private	String				m_ftpServer;
	private	String				m_ftpDirectory;
	private	String				m_ftpUser;
	private	String				m_ftpPassword;
	
	public WebUploadContent( WidgetService widgetService, int viewID )
	{
		m_widgetService = widgetService;
		m_viewID 		= viewID;

		// The arguments will be read from the shared preferences 
		m_prefs 		= PreferenceManager.getDefaultSharedPreferences(m_widgetService);
		m_uploadAllowed = m_prefs.getBoolean("upload_allowed", false);
		m_ftpServer 	= m_prefs.getString("ftp_server", null);
		m_ftpDirectory 	= m_prefs.getString("ftp_directory", null);
		m_ftpUser	 	= m_prefs.getString("ftp_user", null);
		m_ftpPassword 	= m_prefs.getString("ftp_password", null);
	}
	
	@Override
	protected Void doInBackground(Void... params)
	{
		// If any of the arguments is null or upload is not allowed, just exit
		if ((m_uploadAllowed == false) ||
			(m_ftpServer == null) || (m_ftpDirectory == null) ||
			(m_ftpUser == null) || (m_ftpPassword == null))
		{
			return null;
		}
				
		try
		{
			String string = "";
			String outFileName = "";
			if (m_viewID == R.id.quoteOfTheDay)
			{
				string = m_widgetService.getQuote();
				outFileName = m_widgetService.getString(R.string.quoteFileName);
			}
			else
			{
				string = m_widgetService.getTopic();
				outFileName = m_widgetService.getString(R.string.topicFileName);
			}

			// Exit if there is nothing to upload
			if ((string == "") || (string == null))
			{
				return null;
			}

			FTPClient ftpClient = new FTPClient();
			ftpClient.connect(m_ftpServer);
			if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode()))
			{
				throw new Exception(ftpClient.getReplyString());
			}

			ftpClient.login(m_ftpUser, m_ftpPassword);
			if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode()))
			{
				throw new Exception(ftpClient.getReplyString());
			}

			ftpClient.changeWorkingDirectory(m_ftpDirectory);
			if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode()))
			{
				throw new Exception(ftpClient.getReplyString());
			}

			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			InputStream stream = new ByteArrayInputStream(string.getBytes(WebEncoding.getEncoding(m_viewID)));
			ftpClient.enterLocalPassiveMode();
			ftpClient.storeFile(outFileName, stream);
			if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode()))
			{
				throw new Exception(ftpClient.getReplyString());
			}

			stream.close();
			ftpClient.logout();
			ftpClient.disconnect();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}
}
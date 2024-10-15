package ca.sustr.vira.cz;

public class WebEncoding
{
	// Not pretty, but serves its purpose. Set the encoding depending on what
	// data we are reading. For example, vira.cz is using windows-1250,
	// biblenet.cz is at utf8.
	public static String getEncoding(int resourceID)
	{
		String encoding = "";

    	switch (resourceID)
    	{
    		case R.id.quoteOfTheDay:
    		{
    			encoding = "windows-1250";
    			break;
    		}

    		case R.id.topicOfTheWeek:
    		{
    			encoding = "windows-1250";
    			break;
    		}
    		
    		case R.id.dialogTextView:
    		{
    			encoding = "utf-8";
    			break;
    		}
    		
    		default:
    		{
    			encoding = "windows-1250";
    			break;
    		}
    	}
    	
    	return encoding;
	}
}

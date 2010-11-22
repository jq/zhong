package music.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test
{
	private static final Pattern PATTERN = Pattern.compile(
			"<td\\sclass=\"Title.*?\"><a.*?\">(.*?)</a></td>.*?" +	""// Title
			//"(\\(.*?\\))</a></td>.*?" +						// Artist
			//"《(.*?)》.*?"+									// Album
			//"(http://g.top.*?).{2}26resnum" 				// url
			,Pattern.DOTALL);
	
	public static void run(String str)
	{
		Matcher matcher = PATTERN.matcher(str);
		while(matcher.find())
		{
			System.out.println(matcher.group(1));
		}
	}
	
	public static void main(String[] args)
	{
		try
		{
			StringBuffer buffer = new StringBuffer();
			File file = new File("test");
			BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line;
			while((line=reader.readLine()) != null)
				buffer.append(line);
			run(buffer.toString());
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

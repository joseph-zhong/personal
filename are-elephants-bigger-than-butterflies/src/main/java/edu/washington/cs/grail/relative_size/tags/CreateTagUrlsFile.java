package edu.washington.cs.grail.relative_size.tags;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import edu.washington.cs.grail.relative_size.tags.models.TagUrl;

public class CreateTagUrlsFile {
	private static String targetFile = "data/datasets/Flickr100tags/tags.txt";
	private static String sourceFilePrefix = "/Users/hessam/UW/Dataset/F100m/yfcc100m_dataset-";
	
	public static void main(String[] args) throws FileNotFoundException {
		PrintStream ps = new PrintStream(targetFile);
		F100mFilesTagUrl tagsParser = new F100mFilesNeatTagUrls(sourceFilePrefix);
		
		int cnt = 0;
		while(true){
			if ((++cnt)%1000 == 0){
				System.out.println(cnt + " tags group written into file.");
			}
			
			TagUrl now = tagsParser.nextTagUrl();
			if (now == null)
				break;
			
			ps.println(now);
		}
		ps.flush();
		ps.close();
	}
}

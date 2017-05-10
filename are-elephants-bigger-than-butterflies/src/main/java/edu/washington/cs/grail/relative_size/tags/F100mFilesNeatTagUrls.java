package edu.washington.cs.grail.relative_size.tags;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import edu.washington.cs.grail.relative_size.tags.models.TagUrl;

public class F100mFilesNeatTagUrls extends F100mFilesTagUrl {
	private static final int HASHING_MAXIMUM_SIZE = 1000000;
	
	private Set<Long> hashs = new HashSet<Long>();

	public F100mFilesNeatTagUrls(String pathPrefix) throws FileNotFoundException {
		super(pathPrefix);
	}
	
	private int hashOut = 0;
	
	@Override
	public TagUrl nextTagUrl() {
		if (hashs.size() > HASHING_MAXIMUM_SIZE){
			System.out.println("HASH OUT " + hashOut);
			hashs.clear();
		}
		
		TagUrl cur;
		boolean goodTagsGroup = false;
		long curHash = 0;
		
		do {
			cur = super.nextTagUrl();
			if (cur == null)
				return null;
			String[] tags = cur.getTags();
			if (tags.length < 2)
				continue;
			
			curHash = hashCode(tags);
			if (hashs.contains(curHash)){
				hashOut++;
				continue;
			}
			
			goodTagsGroup = true;
		} while (!goodTagsGroup);

		hashs.add(curHash);
		
		cur.makeTagsNeat();
		
		return cur;
	}
	
	private long hashCode(String[] arr){
		long res = 0;
		for(String str: arr){
			res *= 1000000007;
			res += str.hashCode();
		}
		return res;
	}
}

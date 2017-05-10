package edu.washington.cs.grail.relative_size.dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Logger;

import edu.washington.cs.grail.relative_size.utils.Config;



public class SizePairsDataset implements Iterable<ObjectPair>{
	private static final String DATASET_PATH = Config.getValue("size-dataset-path", "data/datasets/ObjectSize/sizePairsFull.txt");
	private static final Logger LOG = Logger.getLogger(SizePairsDataset.class.getName());
	
	private static ArrayList<ObjectPair> allPairs;
	
	static{
		allPairs = new ArrayList<ObjectPair>();
		allPairs.iterator();
		try {
			Scanner fileScanner = new Scanner(new File(DATASET_PATH));
			while(fileScanner.hasNext()){
				String bigger = fileScanner.next();
				String smaller = fileScanner.next();
				allPairs.add(new ObjectPair(bigger, smaller));
			}
			fileScanner.close();
		} catch (FileNotFoundException e) {
			LOG.warning("Size pairs files not found at " + DATASET_PATH);
		}
	}
	
	private List<ObjectPair> pairList;
	
	public SizePairsDataset(){
		this(SizePairsDataset.allPairs);
	}
	
	public SizePairsDataset(List<ObjectPair> allPairs){
		this.pairList = allPairs;
	}
	
	public List<ObjectPair> getAll() {
		return this.pairList;
	}
	
	public Map<String,Integer> getObjectRate(){
		TreeMap<String, Integer> resMap = new TreeMap<String, Integer>();

		for (ObjectPair pair: getAll()){
			Integer curBigger = resMap.get(pair.getBiggerObject());
			Integer curSmaller = resMap.get(pair.getSmallerObject());
			
			if (curBigger == null)
				curBigger = 0;
			if (curSmaller == null)
				curSmaller = 0;
			
			resMap.put(pair.getBiggerObject(), curBigger+1);
			resMap.put(pair.getSmallerObject(), curSmaller+1);
		}
		return resMap;
	}
	
	public Map<String,Integer> getBiggerObjectRate(){
		TreeMap<String, Integer> resMap = new TreeMap<String, Integer>();

		for (ObjectPair pair: getAll()){
			Integer curBigger = resMap.get(pair.getBiggerObject());
			if (curBigger == null)
				curBigger = 0;
			resMap.put(pair.getBiggerObject(), curBigger+1);
		}
		return resMap;
	}

	public Map<String,Integer> getSmallerObjectRate(){
		TreeMap<String, Integer> resMap = new TreeMap<String, Integer>();

		for (ObjectPair pair: getAll()){
			Integer curSmaller = resMap.get(pair.getSmallerObject());
			
			if (curSmaller == null)
				curSmaller = 0;
			
			resMap.put(pair.getSmallerObject(), curSmaller+1);
		}
		return resMap;
	}
	
	
	public int getTotalSize(){
		return pairList.size();
	}
	
	public Iterator<ObjectPair> iterator() {
		return pairList.iterator();
	}
	
}

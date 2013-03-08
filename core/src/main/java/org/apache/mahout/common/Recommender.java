package org.apache.mahout.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.SequenceFile.*;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.IntWritable;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterator;
import org.apache.commons.lang.ArrayUtils;

public class Recommender extends AbstractJob{
	
	private int docId;
	private Configuration conf;

	
	public static void main(String []args) throws Exception{
		Recommender recom = new Recommender();
		recom.run(args);
	}
	

	@Override
	public int run(String[] arg0) throws Exception {
		addInputOption();
		addOutputOption();
		String doc_id = "document_id";
		String num_docs = "num_docs";
		String num_tops = "num_tops";
		String seq_out = "seq_out";
		String window = "win";
		addOption(doc_id, "id", "The id of the document to gain recommendations for.", true);
		addOption(num_tops, "nt", "Specify the number of most best fitting topics used to gain recommendations.",true);
		addOption(num_docs, "nd", "Specify the number of closest documents to a topics to use in the recommender.", true);
		addOption(window, "win", "Specify a window around targ document to retrieve similar docs.", false);
		addOption(seq_out, "sq", "Specify a location to have a sequence file of recommended vectors to be output", false);
		try {
			if (parseArguments(arg0,true,true) == null){
				return - 1;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		conf = new Configuration();
		
		docId = Integer.valueOf(getOption(doc_id));
		
		int[] topicIds = getTopics(Integer.valueOf(getOption(num_tops)));
		
		HashMap<Integer, List<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>>> documents;
		boolean win = (getOption(window) == null) ? false : true;
		documents = getTopTopicDocuments(Integer.valueOf(getOption(num_docs)), topicIds, win);
		
//		if(getOption(seq_out) != null){
//			Writer writer = new SequenceFile.Writer(FileSystem.get(conf), conf, new Path(getOption(seq_out)), IntWritable.class, VectorWritable.class);
//		}
		
		for(Map.Entry<Integer, List<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>>> entry : documents.entrySet()){
			String outputPath = getOutputPath().toString() + "/" + docId;
			File dir = new File(outputPath);
			dir.mkdir();
			PrintStream out = new PrintStream(new FileOutputStream(outputPath + "/" + entry.getKey().intValue()));
			System.setOut(out);
			Writer writer = null;
			if(getOption(seq_out) != null){
				writer = new SequenceFile.Writer(FileSystem.get(conf), conf, new Path((getOption(seq_out) + "/" + getOption(doc_id)  + "/" + entry.getKey().intValue())), IntWritable.class, VectorWritable.class);
			}
			for(Pair<Pair<IntWritable,VectorWritable>, DoubleWritable> value : entry.getValue()){
				//System.out.println(value.getFirst().getFirst().get() + " " + value.getSecond());
				//System.err.println(value.getFirst().getFirst().get());
				System.out.println(value.getFirst().getFirst());
				if(writer != null){
					writer.append(value.getFirst().getFirst(), value.getFirst().getSecond());
				}
			}
			if(writer != null){
				writer.close();
			}
		}
		return 0;
	}

	
	public Recommender(){}
	
	public int[] getTopics(int numTopics) throws Exception{
		int[] topicIds = new int[numTopics];
		SequenceFileIterator<IntWritable, VectorWritable>  it = new SequenceFileIterator<IntWritable,VectorWritable>(getInputPath(), true, conf);
		boolean found = false;
		while(it.hasNext() && !found){
			Pair<IntWritable,VectorWritable> next = it.next();
			if (next.getFirst().get() == docId){
				found = !found;
				it.close();
				Vector doc = next.getSecond().get();
				Pair[] pairs = new Pair[doc.size()];
				Pair<IntWritable,DoubleWritable>[] elems = pairs;
				
				for(int i = 0; i < doc.size(); i++){
					elems[i] = new Pair<IntWritable, DoubleWritable>(new IntWritable(i), new DoubleWritable(doc.get(i)));
				}
				
				Arrays.sort(elems, new TopicElementComparator());
				
				for(int i = 0; i < numTopics; i++){
					topicIds[i] = elems[i].getFirst().get();
				}
			}
		}
		return topicIds;
	}
	
//	public HashMap<Integer, List<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>>> getTopicDocumentWindow(int numDocuments, int[] topicIds) throws IOException{
//		HashMap<Integer, List<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>>> topDocIds = new HashMap<Integer, List<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>>>();
//		for(int i = 0; i < topicIds.length; i++){
//			SequenceFileIterator<IntWritable, VectorWritable>  it = new SequenceFileIterator<IntWritable,VectorWritable>(getInputPath(), true, conf);
//			ArrayList<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>> pairs = new ArrayList<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>>();
//			while(it.hasNext()){
//				Pair<IntWritable,VectorWritable> next = it.next();
//				Vector doc = next.getSecond().get();
//				Pair<Pair<IntWritable,VectorWritable>, DoubleWritable> pair = new Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>(next, new DoubleWritable(doc.get(topicIds[i])));
//				pairs.add(pair);
//			}
//			it.close();
//			Collections.sort(pairs, new ElementComparator());
//			List<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>> window = getDocumentWindow(pairs, numDocuments);
//			topDocIds.put(new Integer(topicIds[i]), window);
//		}
//		return topDocIds;
//	}
	

	public List<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>> getDocumentWindow(ArrayList<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>> docs, int winSize){
		ArrayList<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>> docWindow = new ArrayList<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>>();
		
		boolean found = false;
		int j = 0;
		while(!found && j < docs.size()){
			if(docs.get(j).getFirst().getFirst().get() == docId){
				found = true;
			}
			else{
				j++;
			}
		}
		int i = 0;
		while((j-i) > 0 && i < (Math.ceil(winSize/2))){
			docWindow.add(docs.get((j-(i+1))));
			i++;
		}
		
		int k = 0;
		while((j+k) < docs.size() && k < (Math.ceil(winSize/2))){
			docWindow.add(docs.get((j+k+1)));
			k++;
		}
		
		return docWindow;
	}
	
	public HashMap<Integer, List<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>>> getTopTopicDocuments(int numDocuments, int[] topicIds, boolean window) throws Exception{
		HashMap<Integer, List<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>>> topDocs = new HashMap<Integer, List<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>>>();
		for(int i = 0; i < topicIds.length; i++){
			SequenceFileIterator<IntWritable, VectorWritable>  it = new SequenceFileIterator<IntWritable,VectorWritable>(getInputPath(), true, conf);
			ArrayList<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>> pairs = new ArrayList<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>>();
			while(it.hasNext()){
				Pair<IntWritable,VectorWritable> next = it.next();
				Vector doc = next.getSecond().get();
				Pair<Pair<IntWritable,VectorWritable>, DoubleWritable> pair = new Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>(clonePair(next), new DoubleWritable(doc.get(topicIds[i])));
				pairs.add(pair);
			}
			it.close();
			Collections.sort(pairs, new ElementComparator());
			if(window){
				List<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>> win = getDocumentWindow(pairs, numDocuments);
				topDocs.put(new Integer(topicIds[i]), win);
				System.err.println(win.size());
			}
			else{
				List<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>> out = pairs.subList(0, numDocuments);
				topDocs.put(new Integer(topicIds[i]), out);
				
			}
		}
		return topDocs;
	}
	
	public Pair<IntWritable,VectorWritable> clonePair(Pair<IntWritable,VectorWritable> pair){
		Pair<IntWritable,VectorWritable> newPair = new Pair<IntWritable,VectorWritable>(new IntWritable(pair.getFirst().get()), new VectorWritable(pair.getSecond().get().clone()));
		return newPair;
	}
	
//	public int[][] getTopTopicDocuments(int numDocuments, int[] topicIds) throws Exception{
//		int[][] topDocIds = new int[topicIds.length][numDocuments];
//		for(int i = 0; i < topicIds.length; i++){
//			SequenceFileIterator<IntWritable, VectorWritable>  it = new SequenceFileIterator<IntWritable,VectorWritable>(getInputPath(), true, conf);
//			ArrayList<Pair<IntWritable, DoubleWritable>> pairs = new ArrayList<Pair<IntWritable, DoubleWritable>>();
//			while(it.hasNext()){
//				int l = pairs.size();
//				Pair<IntWritable,VectorWritable> next = it.next();
//				Vector doc = next.getSecond().get();
//				Pair<IntWritable, DoubleWritable> pair = new Pair<IntWritable, DoubleWritable>(new IntWritable(l), new DoubleWritable(doc.get(topicIds[i])));
//				pairs.add(pair);
//			}
//			
//			it.close();
//			Collections.sort(pairs, new ElementComparator());
//			
//			for(int k = 0; k < numDocuments; k++){
//					topDocIds[i][k] = pairs.get(k).getFirst().get();
//			}
//		}
//		return topDocIds;
//	}
	
	public class ElementComparator implements Comparator<Pair<Pair<IntWritable,VectorWritable>, DoubleWritable>>{
		
		@Override
		public int compare(Pair<Pair<IntWritable,VectorWritable>, DoubleWritable> elem1,
				Pair<Pair<IntWritable,VectorWritable>, DoubleWritable> elem2) {
			
			int result = 0;
			
			if(elem1.getSecond().get() == elem2.getSecond().get()){
				return result;
			}
			else{
				result = (elem1.getSecond().get() > elem2.getSecond().get())? -1 : 1;
			}
			
			return result;
		}
		
	}
	
	public class TopicElementComparator implements Comparator<Pair<IntWritable, DoubleWritable>>{
		
		@Override
		public int compare(Pair<IntWritable, DoubleWritable> elem1,
				Pair<IntWritable, DoubleWritable> elem2) {
			
			int result = 0;
			
			if(elem1.getSecond().get() == elem2.getSecond().get()){
				return result;
			}
			else{
				result = (elem1.getSecond().get() > elem2.getSecond().get())? -1 : 1;
			}
			
			return result;
		}
		
	}
	
	
}

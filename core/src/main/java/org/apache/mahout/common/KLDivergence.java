package org.apache.mahout.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterator;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

public class KLDivergence extends AbstractJob{
	
	private static Configuration conf = new Configuration();
	
	public KLDivergence(){}
	
	public static void main(String []args) throws Exception{
		KLDivergence kl = new KLDivergence();
		kl.run(args);
	}
	
	@Override
	public int run(String[] arg0) throws Exception {
		addInputOption();
		addOutputOption();
		String targDoc = "targDoc";
		addOption(targDoc, "t", "Specify the ID of the target Document", true);
		String corpus = "corpus";
		addOption(corpus, "c", "Location of the full topic|document sequence file.", true);
		String nComp = "nDocs";
		addOption(nComp, "n", "Specify top n highest scoring document vectors to output", true);
		String writeIds = "writeIds";
		addOption(writeIds, "w", "Specify whether the output needs to be both raw ids and sequence file or just sequence file", false);
		
		try {
			if (parseArguments(arg0,true,true) == null){
				return - 1;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		conf = new Configuration();

		//SequenceFileIterator<IntWritable,VectorWritable> sfi = new SequenceFileIterator<IntWritable,VectorWritable>(new Path(getOption(corpus)), true, conf);
		Pair<IntWritable,VectorWritable> td = getTargetVector(Integer.valueOf(getOption(targDoc)), new SequenceFileIterator<IntWritable,VectorWritable>(new Path(getOption(corpus)), true, conf));
		if(td == null){
			throw new Exception("Target value not in corpus");
		}
		
		File directory = new File(getInputPath().toString());
		File[] files = null;
		if(directory.isDirectory() && directory.exists()){
			files = directory.listFiles();
		}
		else{
			throw new Exception("Input path must be a directory of sequence files");
		}
		
		ArrayList<Integer> topics = new ArrayList<Integer>();
		for(File f : files){
			if(!f.isHidden()){
				topics.add(Integer.valueOf(f.getName()));
			}
		}
		int[] tops = new int[topics.size()];
		for(int i = 0; i < topics.size(); i++){
			tops[i] = topics.get(i).intValue(); 
		}
		
		for(File f : files){
			if(!f.isHidden()){
				SequenceFileIterator<IntWritable,VectorWritable> it = new SequenceFileIterator<IntWritable,VectorWritable>(new Path(f.getAbsolutePath()), true, conf);
				ArrayList<Pair<Double, Pair<IntWritable,VectorWritable>>> scores = new ArrayList<Pair<Double, Pair<IntWritable,VectorWritable>>>();
				while(it.hasNext()){
					final Pair<IntWritable,VectorWritable> next = it.next();
					scores.add(KLDivergencePair(td, next, tops));
				}
				Collections.sort(scores, new ScorePairComparator());
				for(Pair<Double, Pair<IntWritable,VectorWritable>> pair : scores){
					System.err.println(pair.getFirst().doubleValue() + " " + pair.getSecond().getFirst());
				}
				boolean writeRaw = (getOption(writeIds) == null) ? false : true;
				int nDocs = (getOption(nComp) == null) ? scores.size() : Integer.parseInt(getOption(nComp));
				writeOutput(writeRaw, td.getFirst().get(), nDocs, f.getName(), scores);
			}
		}
		return 0;
	}
	
	public void writeOutput(boolean raw, int targDocId, int nDocs, String topicName, ArrayList<Pair<Double,Pair<IntWritable,VectorWritable>>> output){
		try {
			Writer writer = new SequenceFile.Writer(FileSystem.get(conf), conf, new Path(getOutputPath().toString() + "/" + targDocId + "/" + topicName), IntWritable.class, VectorWritable.class);
			if(raw){
				PrintStream out;
				try {
					String outputPath = getOutputPath().toString() + "/" + targDocId;
					File dir = new File(outputPath);
					dir.mkdir();
					out = new PrintStream(new FileOutputStream(outputPath + "/" + topicName + ".txt"));
					System.setOut(out);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			for(int i = 0; i < nDocs; i++){
				writer.append(output.get(i).getSecond().getFirst(), output.get(i).getSecond().getSecond());
				if(raw){
					System.out.println(output.get(i).getSecond().getFirst());
				}
				
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	public ArrayList<Pair<Double,Pair<IntWritable,VectorWritable>>> compareVectors(Pair<IntWritable,VectorWritable> targ, Vector cIds, SequenceFileIterator<IntWritable,VectorWritable> corpus){
//		
//		ArrayList<Pair<Double,Pair<IntWritable,VectorWritable>>> scores = new ArrayList<Pair<Double,Pair<IntWritable,VectorWritable>>>();
//		
//		int[] comps = new int[cIds.size()];
//		for(int i = 0; i < cIds.size(); i++){
//			comps[i] = (int) cIds.get(i);
//		}
//		Arrays.sort(comps);
//		
//		int it = 0;
//		while(corpus.hasNext()){
//			Pair<IntWritable,VectorWritable> next = corpus.next();
//			if(next.getFirst().get() == comps[it]){
//				try {
//					scores.add(KLDivergencePair(targ,next));
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				it++;
//			}
//		}
//		corpus.close();
//		
//		Collections.sort(scores, new ScorePairComparator()); // Sorts in descending order
//		return scores;
//	}
	
	public Pair<IntWritable,VectorWritable> getTargetVector(int targ, SequenceFileIterator<IntWritable,VectorWritable> corpus){
		while(corpus.hasNext()){
			Pair<IntWritable,VectorWritable> next = corpus.next();
			if(next.getFirst().get() == targ){
				return next;
			}
		}
		corpus.close();
		return null;
	}
	
	public Pair<Double,Pair<IntWritable, VectorWritable>> KLDivergencePair(Pair<IntWritable,VectorWritable> targ, Pair<IntWritable,VectorWritable> comp, int[] topics) throws Exception{
		if(targ.getSecond().get().size() != comp.getSecond().get().size()){
			throw new Exception("Vectors to be compared must be of the same length. Target size = " + targ.getSecond().get().size() + " and comparator size = " + comp.getSecond().get().size());
		}
		
		double score = 0.0;
//		for(int i = 0; i < topics.length; i++){
//			
//		}
//		for(int i = 0; i < topics.length; i++){
//			score += (targ.getSecond().get().get(topics[i]) - comp.getSecond().get().get(topics[i])) * Math.log((targ.getSecond().get().get(i)/comp.getSecond().get().get(i)));
//		}
		for(int i = 0; i < targ.getSecond().get().size(); i++){
			score += (targ.getSecond().get().get(i) - comp.getSecond().get().get(i)) * Math.log((targ.getSecond().get().get(i)/comp.getSecond().get().get(i)));
		}
		return new Pair<Double,Pair<IntWritable, VectorWritable>>(new Double(score), clonePair(comp));
	}
	
	public Pair<IntWritable,VectorWritable> clonePair(Pair<IntWritable,VectorWritable> pair){
		Pair<IntWritable,VectorWritable> newPair = new Pair<IntWritable,VectorWritable>(new IntWritable(pair.getFirst().get()), new VectorWritable(pair.getSecond().get().clone()));
		return newPair;
	}
	
	public class ScorePairComparator implements Comparator<Pair<Double,Pair<IntWritable,VectorWritable>>>{

		@Override
		public int compare(Pair<Double,Pair<IntWritable,VectorWritable>> arg0, Pair<Double,Pair<IntWritable,VectorWritable>> arg1) {
			int result = 0;
			if(arg0.getFirst().doubleValue() == arg1.getFirst().doubleValue()){
				return result;
			}
			else{
				result = (arg0.getFirst().doubleValue() > arg1.getFirst().doubleValue()) ? 1 : -1;
			}
			return result;
		}
		
	}

}

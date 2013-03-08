package org.apache.mahout.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.mahout.common.Recommender.ElementComparator;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterator;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

public class TopicDocumentRetriever extends AbstractJob{
	
	private Configuration conf;
	
	public static void main(String[] arg0) throws Exception{
		TopicDocumentRetriever topDocs = new TopicDocumentRetriever();
		topDocs.run(arg0);
	}

	@Override
	public int run(String[] arg0) throws Exception {
		addInputOption();
		addOutputOption();
		String num_docs = "num_docs";
		addOption(num_docs, "nd", "Specify the number of closest documents to a topics to use in the retriever.", true);
		try {
			if (parseArguments(arg0,true,true) == null){
				return - 1;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		conf = new Configuration();
		
		PrintStream out = new PrintStream(new FileOutputStream(getOutputPath().toString()));
		System.setOut(out);
		
		getTopDocuments(Integer.valueOf(getOption(num_docs)));
		
		return 0;
	}
	
	public void getTopDocuments(int nDocs) throws IOException{
		SequenceFileIterator<IntWritable, VectorWritable>  it = new SequenceFileIterator<IntWritable,VectorWritable>(getInputPath(), true, conf);
		ArrayList<ArrayList<Pair<IntWritable,DoubleWritable>>> matrix = new ArrayList<ArrayList<Pair<IntWritable,DoubleWritable>>>();
		while(it.hasNext()){
			Pair<IntWritable, VectorWritable> next= it.next();
			Vector vec = next.getSecond().get();
			for(int j = 0; j < vec.size(); j++){
				try{
					IntWritable doc = new IntWritable(next.getFirst().get());
					matrix.get(j).add(new Pair(doc, new DoubleWritable(vec.get(j))));
				}
				catch(Exception e){
					ArrayList<Pair<IntWritable,DoubleWritable>> newVec = new ArrayList<Pair<IntWritable,DoubleWritable>>();
					IntWritable doc = new IntWritable(next.getFirst().get());
					newVec.add(new Pair(doc, new DoubleWritable(vec.get(j))));
					matrix.add(newVec);
				}
			}
		}
		for(ArrayList<Pair<IntWritable,DoubleWritable>> vec : matrix){
			//double[] dVec = ArrayUtils.toPrimitive(vec.toArray(new Double[vec.size()]));
			int lenVec = (vec.size() > nDocs) ?  nDocs : vec.size();
			Collections.sort(vec, new ElementComparator());
			for(int i = 0; i < lenVec; i++){
				System.out.print(vec.get(i).getFirst() + " ");
				//System.err.print(vec.get(i).getSecond() + " ");
			}
			//System.err.println();
			System.out.println();
		}
		
		for(int j = 0; j <  matrix.get(0).size(); j++){
			System.err.println(matrix.get(0).get(j).getSecond().get());
		}
	}
	
	public class ElementComparator implements Comparator<Pair<IntWritable,DoubleWritable>>{
		
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

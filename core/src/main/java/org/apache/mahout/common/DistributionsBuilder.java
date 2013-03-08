package org.apache.mahout.common;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterator;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

public class DistributionsBuilder extends AbstractJob{
	
	
	private Configuration conf;

	public static void main(String []args) throws Exception{
		DistributionsBuilder builder = new DistributionsBuilder();
		builder.run(args);
	} 
	
	@Override
	public int run(String[] arg0) throws Exception {
		addInputOption();
		addOutputOption();
		try {
			if (parseArguments(arg0,true,true) == null){
				return - 1;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		conf = new Configuration();
		
		getTopicDistributions();
		getDocumentDistributions();
		return 0;
	}

	public void getTopicDistributions() throws IOException{
		SequenceFileIterator<IntWritable, VectorWritable>  it = new SequenceFileIterator<IntWritable,VectorWritable>(getInputPath(), true, conf);
		ArrayList<ArrayList<Double>> topDists = new ArrayList<ArrayList<Double>>();
		while(it.hasNext()){
			Pair<IntWritable, VectorWritable> pair = it.next();
			Vector vec = pair.getSecond().get();
			for(int i = 0; i < vec.size(); i++){
				try{
					ArrayList<Double> dist = topDists.get(i);
					dist.add(vec.get(i));
				}
				catch(Exception e){
					ArrayList<Double> newDist = new ArrayList<Double>();
					newDist.add(vec.get(i));
					topDists.add(i, newDist);
				}
			}
		}
		it.close();
		for(int j = 0; j < topDists.size(); j++){
			PrintStream out = new PrintStream(new FileOutputStream(getOutputPath().toString() + "/" + "topic." + j + ".txt"));
			System.setOut(out);
			System.out.println(topDists.get(j).toString() + " ");
		}
	}
	
	public void getDocumentDistributions() throws IOException{
		SequenceFileIterator<IntWritable, VectorWritable>  it = new SequenceFileIterator<IntWritable,VectorWritable>(getInputPath(), true, conf);
		while(it.hasNext()){
			Pair<IntWritable, VectorWritable> pair = it.next();
			PrintStream out = new PrintStream(new FileOutputStream(getOutputPath().toString() + "/" + "doc." + pair.getFirst().get() + ".txt"));
			System.setOut(out);
			Vector vec = pair.getSecond().get();
			for(int i = 0; i < vec.size(); i++){
				System.out.print(vec.get(i) + " ");
			}
		}
		it.close();
	}

}

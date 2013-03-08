package org.apache.mahout.common.longtoint;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.mahout.common.AbstractJob;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterator;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.VectorWritable;

public class LongSeq2IntSeq extends AbstractJob{
	
	public static void main(String[] args) throws Exception {
		LongSeq2IntSeq lontoint = new LongSeq2IntSeq();
		lontoint.run(args);
	}
	
	@Override
	public int run(String[] arg0) throws Exception{
		addInputOption();
		addOutputOption();
		try {
			if (parseArguments(arg0,true,true) == null){
				return - 1;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		convertLongWritableToIntWritable();
		
		return 0;
	}
	
	protected void convertLongWritableToIntWritable() {	
		Path inPath = getInputPath();
		Path outPath = getOutputPath();
		
		Configuration conf = new Configuration();
		FileSystem fs = null;
		try {
			fs = FileSystem.get(conf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SequenceFile.Reader sfr = null;
		try {
			sfr = new SequenceFile.Reader(fs, inPath, conf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		if(sfr.getKeyClass() == LongWritable.class && sfr.getValueClass() == VectorWritable.class) {
			SequenceFile.Writer seqWriter = null;
			try {
				seqWriter = SequenceFile.createWriter(
					fs, conf, outPath, IntWritable.class, VectorWritable.class);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				SequenceFileIterator<LongWritable, VectorWritable>  it = new SequenceFileIterator<LongWritable,VectorWritable>(getInputPath(), true, conf);
				while(it.hasNext()) {
					Pair<LongWritable,VectorWritable> next = it.next();
					int docNum = (int) next.getFirst().get();
					IntWritable newInt = new IntWritable();
					newInt.set(docNum);
					if (next.getFirst().get() < Integer.MIN_VALUE || next.getFirst().get() > Integer.MAX_VALUE) {
						throw new IllegalArgumentException(next.getFirst().get() + " cannot be cast to int without changing its value.");
					}
					seqWriter.append(newInt, next.getSecond());
				}
				it.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				seqWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			sfr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

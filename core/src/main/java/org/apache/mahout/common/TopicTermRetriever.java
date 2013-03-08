package org.apache.mahout.common;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.hadoop.fs.Path;

public class TopicTermRetriever {
	
	private static final String VECT_START_DELIM = "{";
	private static final String VECT_END_DELIM = "}";
	private static final String TERM_DELIM = ",";
	private static final String PROB_DELIM = ":";
	
	public TopicTermRetriever(){
	}
	
	public String[][] processTermFile(int nTerms, final String termLoc){
		
		ArrayList<String[]> topicTerms = new ArrayList<String[]>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(termLoc));
			String line;
			while((line = reader.readLine()) != null){
				topicTerms.add(getTopNTopicTerms(line));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return topicTerms.toArray(new String[topicTerms.size()][]);
	}
	
	public String[] getTopNTopicTerms(String line){
		line = line.replace(VECT_START_DELIM, "");
		line = line.replace(VECT_END_DELIM,"");
		String[] termProbs = line.split(TERM_DELIM);
		for(int i = 0; i < termProbs.length; i++){
			termProbs[i] = termProbs[i].split(PROB_DELIM)[0];
		}
		return termProbs;
	}

}

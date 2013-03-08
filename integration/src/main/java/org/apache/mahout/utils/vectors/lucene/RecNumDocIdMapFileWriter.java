package org.apache.mahout.utils.vectors.lucene;

import java.io.IOException;

import java.io.Writer;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;
import com.google.common.io.Closeables;

public class RecNumDocIdMapFileWriter{
	  private final Writer writer;
	  private long recNum = 0;
	  private final String delimeter;
	  public RecNumDocIdMapFileWriter(final Writer writer, final String delimeter) {
	    this.writer = writer;
	    this.delimeter = delimeter;
	  }
	  
	  public long write(Iterable<Vector> iterable, long maxDocs) throws IOException {

	    for (Vector point : iterable) {
	      if (recNum >= maxDocs) {
	        break;
	      }
	      if (point != null) {
	    	  writer.write(Long.toString(recNum++));
	    	  writer.write(delimeter);
	    	  writer.write(((NamedVector)point).getName());
	    	  writer.write('\n');
	      }
	    }
	    return recNum;
	  }
	  
	  public void close() throws IOException {
	    Closeables.closeQuietly(writer);
	  }
	  
	  public Writer getWriter() {
	    return writer;
	  }
}

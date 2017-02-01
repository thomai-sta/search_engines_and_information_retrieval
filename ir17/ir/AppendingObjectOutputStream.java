/**
 *  Subclass to enable appending objects to files
 */

package ir;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;


public class AppendingObjectOutputStream extends ObjectOutputStream
{

  public AppendingObjectOutputStream(OutputStream out) throws IOException
  {
    super(out);
  }

  @Override
  protected void writeStreamHeader() throws IOException
  {
    // do not write a header, but reset:
    // this line added after another question
    // showed a problem with the original
    reset();
  }

}
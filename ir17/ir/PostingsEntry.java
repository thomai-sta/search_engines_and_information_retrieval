/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */

package ir;

import java.util.LinkedList;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable
{

  /// Make offset a list, so that one document gets the same
  /// postingsEntry for several occurrences
  public LinkedList<Integer> offsets = new LinkedList<Integer>();
  public double score = 0.0;
  public int docID;

  /**
   * Contructor:
   * Creates a postings entry for the current token.
   * Contains the docID of the document that contains the token and
   * the offset, which indicates the position of the token within the
   * document.
   */
  public PostingsEntry(int docID, int offset)
  {
    this.docID = docID;
    offsets.add(offset);
  }

  /**
   *  PostingsEntries are compared by their score (only relevant
   *  in ranked retrieval).
   *
   *  The comparison is defined so that entries will be put in
   *  descending order.
   */
  public int compareTo(PostingsEntry other)
  {
    return Double.compare(other.score, score);
  }

  //
  //  YOUR CODE HERE
  //

}



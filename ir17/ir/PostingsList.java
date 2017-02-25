/*
*   This file is part of the computer assignment for the
*   Information Retrieval course at KTH.
*
*   First version:  Johan Boye, 2010
*   Second version: Johan Boye, 2012
*/

package ir;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.Serializable;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
*   A list of postings for a given word.
*/
public class PostingsList implements Serializable
{

  /**
  *  The postings list as a HashMap.
  */
  private Map<Integer, PostingsEntry> list =
      new HashMap<Integer, PostingsEntry>();


  /**  Number of postings in this list  */
  public int size()
  {
    return list.size();
  }

  /**  Returns the docID posting */
  public PostingsEntry get(int docID)
  {
    return list.get(docID);
  }

  /** Removes the docID from list */
  public void remove(int docID)
  {
    list.remove(docID);
  }

  /** Checks if list contains the docID */
  public boolean containsKey(int docID)
  {
    return list.containsKey(docID);
  }

  /** Return a set of all the docIDs of the list */
  public Set<Integer> keySet()
  {
    return list.keySet();
  }

  /** Appends an element at the list */
  public void put(Integer docID, PostingsEntry entry)
  {
    list.put(docID, entry);
  }

  /** Checks if list is empty */
  public boolean isEmpty()
  {
    return list.isEmpty();
  }

  public void sort()
  {
    List<Integer> mapKeys = new ArrayList<>(list.keySet());
    List<PostingsEntry> mapValues = new ArrayList<>(list.values());
    Collections.sort(mapValues);
    Collections.sort(mapKeys);

    LinkedHashMap<Integer, PostingsEntry> sortedMap =
    new LinkedHashMap<>();

    Iterator<PostingsEntry> valueIt = mapValues.iterator();

    while (valueIt.hasNext())
    {
      PostingsEntry val = valueIt.next();
      Iterator<Integer> keyIt = mapKeys.iterator();

      while (keyIt.hasNext())
      {
        Integer key = keyIt.next();
        PostingsEntry comp1 = list.get(key);
        PostingsEntry comp2 = val;

        if (comp1.equals(comp2))
        {
          keyIt.remove();
          sortedMap.put(key, val);
          break;
        }
      }
    }
    list = sortedMap;
  }
}

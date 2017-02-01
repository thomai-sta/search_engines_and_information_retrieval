/*
*   This file is part of the computer assignment for the
*   Information Retrieval course at KTH.
*
*   First version:  Johan Boye, 2010
*   Second version: Johan Boye, 2012
*/

package ir;

import java.util.LinkedList;
import java.util.HashMap;
import java.io.Serializable;
import java.util.Set;


/**
*   A list of postings for a given word.
*/
public class PostingsList implements Serializable
{

    /**
     *  The postings list as a HashMap.
     */
    private HashMap<Integer, PostingsEntry> list =
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
}

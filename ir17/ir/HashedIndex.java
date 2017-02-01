/*
*   This file is part of the computer assignment for the
*   Information Retrieval course at KTH.
*
*   First version:  Johan Boye, 2010
*   Second version: Johan Boye, 2012
*   Additions: Hedvig Kjellström, 2012-14
*/


package ir;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;



/**
*   Implements an inverted index as a Hashtable from words to PostingsLists.
*/
public class HashedIndex implements Index
{
  /** We save the index into files while indexing. */

  /** The index as a hashtable. */
  private HashMap<String, PostingsList> index =
   new HashMap<String, PostingsList>();
  private LinkedList<String> unindexed_directories = new LinkedList<String>();

  private int index_limit = 5000; /** max number of tokens to keep in memory */
  private int file_limit = 10000;
  private HashMap<String, String> mappings = new HashMap<String, String>();
  private String FOLDER = "index/";
  private int file_counter = 0; /// No. of the last not-full file
  private int file_token_counter = 0; /// No. of tokens in the last not-full file


  public HashedIndex(LinkedList<String> directories)
  {
    /// READ IN MAPPINGS OF FILES
    /// READ FILE OF INDEXED DIRECTORIES
    /// KEEP TRACK OF THE UNINDEXED ONES, TO INDEX THEM NOW!
    unindexed_directories = directories;
  }


  /**
  *  Inserts this token in the index.
  */
  public void insert(String token, int docID, int offset)
  {
    if (!index.containsKey(token))
    {
      /// New token, create postings list
      PostingsEntry new_entry = new PostingsEntry(docID, offset);
      PostingsList new_list = new PostingsList();
      new_list.put(docID, new_entry);
      index.put(token, new_list);
    }
    else
    {
      /// Token exists, update
      PostingsList list = getPostings(token);
      if (list.containsKey(docID))
      {
        /// Document already there, update offsets
        PostingsEntry new_entry = list.get(docID);
        new_entry.offsets.add(offset);
        list.put(docID, new_entry);
      }
      else
      {
        PostingsEntry new_entry = new PostingsEntry(docID, offset);
        list.put(docID, new_entry);
      }
      index.put(token, list);
    }

    if (index.size() == index_limit)
    {
      updateBinaryFiles();
    }
  }


  /**
  *  Returns all the words in the index.
  */
  public Iterator<String> getDictionary()
  {
    return index.keySet().iterator();
  }


  /**
  *  Returns the postings for a specific term, or null
  *  if the term is not in the index.
  */
  public PostingsList getPostings(String token)
  {
    return index.get(token);
  }


  /**
  *  Searches the index for postings matching the query.
  */
  public PostingsList search(Query query, int queryType, int rankingType,
                             int structureType)
  {
    PostingsList resDocIDPostings = new PostingsList();
    /// First check that the tokens are contained in the dataset
    Iterator<String> queryIteratorCheck = query.terms.listIterator(0);

    while (queryIteratorCheck.hasNext())
    {
      if (!index.containsKey(queryIteratorCheck.next()))
      {
        System.err.println("ONE OR MORE TERMS DON'T EXIST IN THE DICTIONARY!!!\nRETYPE AND SEARCH AGAIN!");
        return resDocIDPostings;
      }
    }

    if (queryType == Index.INTERSECTION_QUERY)
    {
      System.out.println("INTERSECTION_QUERY");
      /// ======================= INTERSECTION QUERY ===========================
      /// Return all documents in which ALL of the tokens exist
      Iterator<String> queryIterator = query.terms.listIterator(0);
      resDocIDPostings = getPostings(queryIterator.next());

      while(queryIterator.hasNext())
      {
        PostingsList new_list = getPostings(queryIterator.next());

        resDocIDPostings = instersect_lists(resDocIDPostings, new_list);
      }
      /// ======================================================================
    }
    else if (queryType == Index.PHRASE_QUERY)
    {
      System.out.println("PHRASE_QUERY");
      /// ======================= PHRASE QUERY =================================
      /// Return all documents in which ALL of the tokens exist
      Iterator<String> queryIterator = query.terms.listIterator(0);
      resDocIDPostings = getPostings(queryIterator.next());

      while(queryIterator.hasNext())
      {
        PostingsList new_list = getPostings(queryIterator.next());

        resDocIDPostings = instersect_phrase_lists(resDocIDPostings, new_list);
      }
      /// ======================================================================
    }
    return resDocIDPostings;
  }

  public PostingsList instersect_lists(PostingsList list_a, PostingsList list_b)
  {
    /// Convert PostingsLists to linkedLists for fast iteration
    Set<Integer> list_a_set = list_a.keySet();
    LinkedList<Integer> list_a_linked = new LinkedList<Integer>();
    list_a_linked.addAll(list_a_set);
    java.util.Collections.sort(list_a_linked);

    Set<Integer> list_b_set = list_b.keySet();
    LinkedList<Integer> list_b_linked = new LinkedList<Integer>();
    list_b_linked.addAll(list_b_set);
    java.util.Collections.sort(list_b_linked);


    PostingsList list = new PostingsList();

    while(!list_a_linked.isEmpty() && !list_b_linked.isEmpty())
    {
      if (list_a_linked.getFirst().equals(list_b_linked.getFirst()))
      {
        list.put(list_a_linked.getFirst(),
                 list_a.get(list_a_linked.getFirst()));
        list_a_linked.removeFirst();
        list_b_linked.removeFirst();
      }
      else if (list_a_linked.getFirst() > list_b_linked.getFirst())
      {
        list_b_linked.removeFirst();
      }
      else
      {
        list_a_linked.removeFirst();
      }
    }
    return list;
  }


  public PostingsList instersect_phrase_lists(PostingsList list_a,
                                              PostingsList list_b)
  {
    /// Convert PostingsLists to linkedLists for fast iteration
    Set<Integer> list_a_set = list_a.keySet();
    LinkedList<Integer> list_a_linked = new LinkedList<Integer>();
    list_a_linked.addAll(list_a_set);
    java.util.Collections.sort(list_a_linked);

    Set<Integer> list_b_set = list_b.keySet();
    LinkedList<Integer> list_b_linked = new LinkedList<Integer>();
    list_b_linked.addAll(list_b_set);
    java.util.Collections.sort(list_b_linked);


    PostingsList list = new PostingsList();

    while(!list_a_linked.isEmpty() && !list_b_linked.isEmpty())
    {
      if (list_a_linked.getFirst().equals(list_b_linked.getFirst()))
      {
        /// Common doc. Check offsets
        System.out.println("COMMON DOC!");
        LinkedList<Integer> offsets_a =
         list_a.get(list_a_linked.getFirst()).offsets;
        LinkedList<Integer> offsets_b =
         list_b.get(list_b_linked.getFirst()).offsets;

        while(!offsets_a.isEmpty() && !offsets_b.isEmpty())
        {
          System.out.println("Comparing offset " + offsets_a.getFirst() + " and " + offsets_b.getFirst());
          if (offsets_b.getFirst().equals(offsets_a.getFirst() + 1))
          {
            list.put(list_b_linked.getFirst(),
                     list_b.get(list_b_linked.getFirst()));
            break;
          }
          else if (offsets_b.getFirst() > offsets_a.getFirst() + 1)
          {
            offsets_a.removeFirst();
          }
          else
          {
            offsets_b.removeFirst();
          }
        }

        list_a_linked.removeFirst();
        list_b_linked.removeFirst();
      }
      else if (list_a_linked.getFirst() > list_b_linked.getFirst())
      {
        list_b_linked.removeFirst();
      }
      else
      {
        list_a_linked.removeFirst();
      }
    }
    return list;
  }

  /** Returns a list of unindexed directories */
  public LinkedList<String> getUnindexedDirectories()
  {
    return unindexed_directories;
  }

  /**
  *  No need for cleanup in a HashedIndex.
  */
  public void cleanup()
  {
  }

  /**
   *  When the index's token surpass the limit, the index is distributed into
   *  binary files and cleared
   */
  public void updateBinaryFiles()
  {
    Set<String> current_tokens = index.keySet();
    for (String token : current_tokens)
    {
      if (mappings.containsKey(token))
      {
        /// token has already been indexed, we need to update the corresponding files
      }
      else
      {
        if (file_token_counter == file_limit)
        {
          /// Create new file
          file_token_counter = 1;
          file_counter++;
          String filename;
          filename = String.format("%sindex_%d", FOLDER, file_counter);

          FileOutputStream fout = new FileOutputStream(filename);
          ObjectOutputStream oos = new ObjectOutputStream(fout);
          oos.writeObject(index);
        }
        /// Create new entry to last file, or make new one


      }
    }

    index.clear();
  }

  /** Returns size of index */
  public int getSize()
  {
    return index.size();
  }

}
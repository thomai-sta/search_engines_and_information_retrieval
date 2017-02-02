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
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;



/**
*   Implements an inverted index as a Hashtable from words to PostingsLists.
*/
public class HashedIndex implements Index
{
  /** We save the index into files while indexing. */

  /** The index as a hashtable. */
  private HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();

  private int index_limit = 2000; /** max number of tokens to keep in memory */
  private int file_limit = 500;
  private HashMap<String, String> mappings = new HashMap<String, String>();
  private String FOLDER = "index/";
  private int file_counter = -1; /// No. of the last not-full file
  private int file_token_counter = file_limit; /// No. of tokens in the last not-full file


  public HashedIndex()
  {
    File f = new File("index/mappings");
    if(f.exists() && !f.isDirectory())
    {
      String filename = "index/mappings";
      /// READ IN MAPPINGS OF FILES
      HashMap<String, String> list = new HashMap<String, String>();
      try
      {
        FileInputStream fin = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(fin);
        mappings = (HashMap<String, String>) ois.readObject();
        ois.close();
        fin.close();
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
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
        LinkedList<Integer> offsets_a =
          list_a.get(list_a_linked.getFirst()).offsets;
        java.util.Collections.sort(offsets_a);
        LinkedList<Integer> offsets_b =
          list_b.get(list_b_linked.getFirst()).offsets;
        java.util.Collections.sort(offsets_b);

        while(!offsets_a.isEmpty() && !offsets_b.isEmpty())
        {
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

  /**
  *  No need for cleanup in a HashedIndex.
  */
  public void cleanup()
  {
    /// Delete all files of the index (in case of "QUIT")
    File file = new File(FOLDER);
    String[] myFiles;

    if(file.isDirectory())
    {
      myFiles = file.list();
      for (int i = 0; i < myFiles.length; i++)
      {
        File myFile = new File(file, myFiles[i]);
        myFile.delete();
      }
    }
  }

  /** Save mappings file (in case of "SAVE & QUIT") */
  public void saveAndQuit()
  {
    String filename = "mappings";
    try
    {
      FileOutputStream fout = new FileOutputStream(filename);
      ObjectOutputStream oos = new ObjectOutputStream(fout);
      oos.writeObject(mappings);
      oos.close();
      fout.close();
    }
    catch (FileNotFoundException e)
    {
      System.err.println("File " + filename + " not found");
    }
    catch (IOException e)
    {
      System.err.println("Error initializing stream");
    }
  }

  /**
   *  When the index's token surpass the limit, the index is distributed into
   *  binary files and cleared
   */
  public void updateBinaryFiles()
  {
    Set<String> tokens = index.keySet();
    for (String token : tokens)
    {
      if (mappings.containsKey(token))
      {
        /// Token is saved in one of the files. Read and update it's index
        HashMap<String, PostingsList> saved_index =
                new HashMap<String, PostingsList>();
        /// Make filename
        String filename;
        filename = String.format(FOLDER + mappings.get(token));
        /// Read saved index
        saved_index = readFromFile(filename);


        /// We need to go over all the docIDs of the current postings list
        /// and the saved one.
        PostingsList new_list = getPostings(token);
        PostingsList saved_list = saved_index.get(token);

        for (int docID : new_list.keySet())
        {
          if (saved_list.containsKey(docID))
          {
            /// Document already there, update offsets
            PostingsEntry new_entry = saved_list.get(docID);
            for (int offset : new_list.get(docID).offsets)
            {
              new_entry.offsets.add(offset);
            }
            saved_list.put(docID, new_entry);
          }
          else
          {
            saved_list.put(docID, new_list.get(docID));
          }
        }

        /// Add token to index
        saved_index.put(token, saved_list);

        writeToFile(saved_index, filename);

      }
      else
      {
        System.out.println("File token counter: " + file_token_counter + " file counter: " + file_counter);
        /// New token. Write it in file
        /// Make hashmap to write to file
        HashMap<String, PostingsList> index_to_write =
        new HashMap<String, PostingsList>();
        String filename;
        if (file_token_counter == file_limit)
        {
          /// File is full. Make new one
          file_counter++;
          file_token_counter = 0;
          /// Make filename
          filename = String.format(FOLDER + "index_" + file_counter);
        }
        else
        {
          /// Make filename
          filename = String.format(FOLDER + "index_" + file_counter);
          /// Read saved index
          index_to_write = readFromFile(filename);
        }
        /// Add token to index
        index_to_write.put(token, getPostings(token));

        writeToFile(index_to_write, filename);
        /// Update mappings
        mappings.put(token, filename);
      }
      file_token_counter++;
    }
    index.clear();
  }


  /** Reads object from given file */
  public HashMap<String, PostingsList> readFromFile(String filename)
  {
    HashMap<String, PostingsList> list = new HashMap<String, PostingsList>();
    try
    {
      FileInputStream fin = new FileInputStream(filename);
      ObjectInputStream ois = new ObjectInputStream(fin);
      list = (HashMap<String, PostingsList>) ois.readObject();
      ois.close();
      fin.close();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    return list;
  }

  /** Writes given list to the given file */
  public void writeToFile(HashMap<String, PostingsList> list, String filename)
  {
    try
    {
      FileOutputStream fout = new FileOutputStream(filename);
      ObjectOutputStream oos = new ObjectOutputStream(fout);
      oos.writeObject(list);
      oos.close();
      fout.close();
    }
    catch (FileNotFoundException e)
    {
      System.err.println("File " + filename + " not found");
    }
    catch (IOException e)
    {
      System.err.println("Error initializing stream");
    }
  }


  /** Returns size of index */
  public int getSize()
  {
    return index.size();
  }

}

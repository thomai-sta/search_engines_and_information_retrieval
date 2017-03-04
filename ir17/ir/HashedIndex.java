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
import java.util.Scanner;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.*;



/**
*   Implements an inverted index as a Hashtable from words to PostingsLists.
*/
public class HashedIndex implements Index
{
  /** The index as a hashtable. */
  private HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();

  private int index_limit = 30000; /** max number of tokens to keep in memory */
  private int file_limit = 150;
  private HashMap<String, String> mappings = new HashMap<String, String>();
  private String FOLDER = "index/";
  private int file_counter = -1; /// No. of the last not-full file
  private int file_token_counter = file_limit; /// No. of tokens in the last not-full file

  private double TF_IDF_WEIGHT = 0.5;
  private double PAGERANK_WEIGHT = 1.0 - TF_IDF_WEIGHT;


  public HashedIndex()
  {
    /// Read mappings
    File f = new File(FOLDER + "/mappings");
    if(f.exists() && !f.isDirectory())
    {
      String filename = FOLDER + "/mappings";
      /// READ IN MAPPINGS OF FILES
      try
      {
        FileInputStream fin = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(fin);
        mappings = (HashMap<String, String>) ois.readObject();
        ois.close();
        fin.close();
        System.out.println(mappings.size() + " tokens indexed");
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    /// Read docIDs
    f = new File(FOLDER + "/docIDs");
    if(f.exists() && !f.isDirectory())
    {
      String filename = FOLDER + "/docIDs";
      /// READ IN docIDs OF FILES
      try
      {
        FileInputStream fin = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(fin);

        HashMap<String, String> temp =
         (HashMap<String, String>) ois.readObject();

        for (String docID : temp.keySet())
        {
          docIDs.put(docID, temp.get(docID));
        }
        ois.close();
        fin.close();
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    /// Read docLengths
    f = new File(FOLDER + "/docLengths");
    if(f.exists() && !f.isDirectory())
    {
      String filename = FOLDER + "/docLengths";
      /// READ IN docLengths OF FILES
      try
      {
        FileInputStream fin = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(fin);

        HashMap<String, Integer> temp =
         (HashMap<String, Integer>) ois.readObject();

        for (String docID : temp.keySet())
        {
          docLengths.put(docID, temp.get(docID));
        }
        ois.close();
        fin.close();
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }

    /// Read pageranks
    f = new File("pagerank/pageranks_names_hashmap");
    if(f.exists() && !f.isDirectory())
    {
      String filename = "pagerank/pageranks_names_hashmap";
      /// READ IN pageranks OF documents
      try
      {
        FileInputStream fin = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(fin);

        HashMap<String, Double> temp =
         (HashMap<String, Double>) ois.readObject();

        for (String name : temp.keySet())
        {
          pageranks.put(name, temp.get(name));
        }
        ois.close();
        fin.close();
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
  }

  /// **************************************************************************
  /// **                              SEARCHING                               **
  /// **************************************************************************

  /**
  *  Searches the index for postings matching the query.
  */
  public PostingsList search(Query query, int queryType, int rankingType,
   int structureType)
  {
    PostingsList resDocIDPostings = new PostingsList();
    /// First check that the tokens are contained in the dataset
    Iterator<String> queryIteratorCheck = query.terms.listIterator(0);

    index = new HashMap<String, PostingsList>();

    // if (index.size() >= index_limit)
    // {
      // index = new HashMap<String, PostingsList>();
    // }

    while (queryIteratorCheck.hasNext())
    {
      String tok = queryIteratorCheck.next();
      if (!mappings.containsKey(tok))
      {
        System.err.println("ONE OR MORE TERMS DON'T EXIST IN THE DICTIONARY!!!\nRETYPE AND SEARCH AGAIN!");
        return resDocIDPostings;
      }
      else
      {
        if (!index.containsKey(tok))
        {
          readFromFile(mappings.get(tok));
        }
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
      String token = queryIterator.next();
      resDocIDPostings = getPostings(token);

      int k = 1;
      while(queryIterator.hasNext())
      {
        token = queryIterator.next();
        PostingsList new_list = getPostings(token);

        resDocIDPostings = positionalIntersect(resDocIDPostings, new_list, k);
        k += 1;
      }
      /// ======================================================================
    }
    else if (queryType == Index.RANKED_QUERY)
    {
      HashMap<Integer, Double> scores = new HashMap<Integer, Double>();
      /// ======================= RANKED QUERY =================================
      if (rankingType == COMBINATION)
      {
        /// Return all documents in which at least one token exists
        Iterator<String> queryIterator = query.terms.listIterator(0);
        String token = queryIterator.next();

        HashMap<Integer, Double> scores_TF_IDF = new HashMap<Integer, Double>();
        HashMap<Integer, Double> scores_PR = new HashMap<Integer, Double>();

        PostingsList postings_TF_IDF = getPostings(token);
        scores_TF_IDF = calculateTF_IDF(postings_TF_IDF);

        PostingsList postings_PR = getPostings(token);
        scores_PR = calculatePagerank(postings_PR);

        while(queryIterator.hasNext())
        {
          token = queryIterator.next();
          PostingsList new_list_TF_IDF = getPostings(token);
          HashMap<Integer, Double> new_scores_TF_IDF =
                                   calculateTF_IDF(new_list_TF_IDF);

          scores_TF_IDF = mergeLists(scores_TF_IDF, new_scores_TF_IDF);

          PostingsList new_list_PR = getPostings(token);
          HashMap<Integer, Double> new_scores_PR =
                                   calculatePagerank(new_list_PR);

          scores_PR = mergeLists(scores_PR, new_scores_PR);

        }
        scores = mergeCombination(scores_TF_IDF, scores_PR);

      }
      else
      {
        /// Return all documents in which at least one token exists
        Iterator<String> queryIterator = query.terms.listIterator(0);
        String token = queryIterator.next();
        PostingsList postings = getPostings(token);
        scores = getScores(postings, rankingType);

        while(queryIterator.hasNext())
        {
          token = queryIterator.next();
          PostingsList new_list = getPostings(token);
          HashMap<Integer, Double> new_scores = getScores(new_list, rankingType);

          scores = mergeLists(scores, new_scores);
        }

      }
      /// ======================================================================
      /// Make postingsList for scores
      for (int doc : scores.keySet())
      {
        PostingsEntry entry = new PostingsEntry(doc, 0);
        entry.score = scores.get(doc);
        resDocIDPostings.put(doc, entry);
      }
      resDocIDPostings.sort();
    }
    return resDocIDPostings;
  }


  public HashMap<Integer, Double> mergeLists(HashMap<Integer, Double> list_a,
                                             HashMap<Integer, Double> list_b)
  {
    HashMap<Integer, Double> new_list = new HashMap<Integer, Double>();

    for (int doc : list_a.keySet())
    {
      if (list_b.containsKey(doc))
      {
        /// Merge, add to new list and remove.
        new_list.put(doc, list_b.get(doc) + list_a.get(doc));
        list_b.remove(doc);
      }
      else
      {
        /// Add list_a to new list
        new_list.put(doc, list_a.get(doc));
      }
    }
    /// The remaining entries of list_b (if any), are added to the new list
    for (int doc : list_b.keySet())
    {
      /// Add list_b to new list
      new_list.put(doc, list_b.get(doc));
    }

    return new_list;
  }

  public HashMap<Integer, Double> mergeCombination(HashMap<Integer, Double> list_TF_IDF,
                                                   HashMap<Integer, Double> list_PR)
  {
    Double tf_sum = 0.0;
    for (int doc : list_TF_IDF.keySet())
    {
      tf_sum = tf_sum + list_TF_IDF.get(doc);
    }

    Double pr_sum = 0.0;
    for (int doc : list_PR.keySet())
    {
      pr_sum = pr_sum + list_PR.get(doc);
    }


    HashMap<Integer, Double> new_list = new HashMap<Integer, Double>();
    for (int doc : list_TF_IDF.keySet())
    {
      // System.out.println(entry_tf.score + " " + entry_pr.score);


      Double score = (double) TF_IDF_WEIGHT * list_TF_IDF.get(doc) / tf_sum +
                     (double) PAGERANK_WEIGHT * list_PR.get(doc) / pr_sum;
      new_list.put(doc, score);
    }

    return new_list;
  }

  public HashMap<Integer, Double> getScores(PostingsList list, int rankingType)
  {
    if (rankingType == TF_IDF)
    {
      return calculateTF_IDF(list);
    }
    else if (rankingType == PAGERANK)
    {
      return calculatePagerank(list);
    }
    HashMap<Integer, Double> new_list = new HashMap<Integer, Double>();
    return new_list;
  }

  public HashMap<Integer, Double> calculateTF_IDF(PostingsList list)
  {
    Double n = (double) docIDs.size(); // Number of docs in corpus
    Double df = (double) list.size();
    double idf = Math.log(n / df);

    HashMap<Integer, Double> scores = new HashMap<Integer, Double>();

    for (Integer doc : list.keySet())
    {
      PostingsEntry entry = list.get(doc);
      Double tf =
       (double) entry.offsets.size() / (double) docLengths.get("" + doc);
      scores.put(doc, tf * idf);
    }
    return scores;
  }

  public HashMap<Integer, Double> calculatePagerank(PostingsList list)
  {
    HashMap<Integer, Double> scores = new HashMap<Integer, Double>();

    for (Integer doc : list.keySet())
    {
      PostingsEntry entry = list.get(doc);
      String name = docIDs.get("" + doc);
      if (name.endsWith(".f"))
      {
        name = name.substring(0, name.length() - 2);
      }

      if (pageranks.containsKey(name))
      {
        scores.put(doc, pageranks.get(name));
      }
      else
      {
        scores.put(doc, 0.0);
      }
    }
    return scores;
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

  public PostingsList positionalIntersect(PostingsList p1, PostingsList p2,
                                          int k)
  {
    PostingsList result = new PostingsList();

    /// Convert PostingsLists to linkedLists for fast iteration
    Set<Integer> p1_set = p1.keySet();
    LinkedList<Integer> p1_linked = new LinkedList<Integer>();
    p1_linked.addAll(p1_set);
    java.util.Collections.sort(p1_linked);

    Set<Integer> p2_set = p2.keySet();
    LinkedList<Integer> p2_linked = new LinkedList<Integer>();
    p2_linked.addAll(p2_set);
    java.util.Collections.sort(p2_linked);

    while(!p1_linked.isEmpty() && !p2_linked.isEmpty())
    {
      if (p1_linked.getFirst().equals(p2_linked.getFirst()))
      {
        /// Common doc. Check offsets
        LinkedList<Integer> offsets1 = p1.get(p1_linked.getFirst()).offsets;
        java.util.Collections.sort(offsets1);
        LinkedList<Integer> offsets2 = p2.get(p2_linked.getFirst()).offsets;
        java.util.Collections.sort(offsets2);

        while(!offsets1.isEmpty() && !offsets2.isEmpty())
        {
          if (offsets2.getFirst().equals(offsets1.getFirst() + k))
          {
            result.put(p1_linked.getFirst(), p1.get(p1_linked.getFirst()));
            break;
          }
          else if (offsets2.getFirst() > offsets1.getFirst() + k)
          {
            offsets1.removeFirst();
          }
          else
          {
            offsets2.removeFirst();
          }
        }

        p1_linked.removeFirst();
        p2_linked.removeFirst();
      }
      else if (p1_linked.getFirst() > p2_linked.getFirst())
      {
        p2_linked.removeFirst();
      }
      else
      {
        p1_linked.removeFirst();
      }
    }

    return result;
  }


  /// **************************************************************************
  /// **                        INDEXING AND LOGISTICS                        **
  /// **************************************************************************

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


  /**
   * Save mappings, docIDs and docLengths file (in case of "SAVE AND QUIT")
   */
  public void saveAndQuit()
  {
    /// Save mappings
    String filename = FOLDER + "/mappings";
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
    /// Save docIDs
    filename = FOLDER + "/docIDs";
    try
    {
      FileOutputStream fout = new FileOutputStream(filename);
      ObjectOutputStream oos = new ObjectOutputStream(fout);
      oos.writeObject(docIDs);
      oos.close();
      fout.close();
    }
    catch (FileNotFoundException er)
    {
      System.err.println("File " + filename + " not found");
    }
    catch (IOException er)
    {
      System.err.println("Error initializing stream");
    }
    /// Save docLengths
    filename = FOLDER + "/docLengths";
    try
    {
      FileOutputStream fout = new FileOutputStream(filename);
      ObjectOutputStream oos = new ObjectOutputStream(fout);
      oos.writeObject(docLengths);
      oos.close();
      fout.close();
    }
    catch (FileNotFoundException er)
    {
      System.err.println("File " + filename + " not found");
    }
    catch (IOException er)
    {
      System.err.println("Error initializing stream");
    }
  }

  /**
   *  When a whole document has been indexed, the index is distributed into
   *  binary files and cleared
   */
  public void updateFiles()
  {
    Set<String> tokens = index.keySet();
    for (String token : tokens)
    {
      if (mappings.containsKey(token))
      {
        /// Make filename
        String filename;
        filename = String.format(mappings.get(token));
        writeToFile(token, getPostings(token), filename);
      }
      else
      {
        /// New token. Write it in file
        String filename;
        if (file_token_counter >= file_limit)
        {
          // System.out.println("file counter: " + file_counter);
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
        }
        writeToFile(token, getPostings(token), filename);

        /// Update mappings
        mappings.put(token, filename);

        file_token_counter++;
      }
    }
    // System.out.println("CLEARING INDEX!");
    index = new HashMap<String, PostingsList>();
  }


  /** Reads object from given file */
  public void readFromFile(String filename)
  {
    Scanner sc = null;
    try
    {
      sc = new Scanner(new File(filename));
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }

    String token = new String();
    int docID = -1;
    double score = -1.0;

    while (sc.hasNextLine())
    {

      String line = sc.nextLine();
      String[] words = line.split("\\s");

      if (words[0].equals("token"))
      {
        token = words[1];
      }
      else if (words[0].equals("docID"))
      {
        docID = Integer.parseInt(words[1]);
      }
      else
      {
        for (int i = 0; i < words.length; i++)
        {
          int offset = Integer.parseInt(words[i]);
          /// Like inserting token, docID and offset
          if (index.containsKey(token))
          {
            /// Token exists in index
            /// We need to check if the docID exists and update it
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
              new_entry.score = score;
              list.put(docID, new_entry);
            }

            /// Add token to index
            index.put(token, list);
          }
          else
          {
            /// Token doesn't exist in index
            PostingsEntry new_entry = new PostingsEntry(docID, offset);
            PostingsList new_list = new PostingsList();
            new_list.put(docID, new_entry);
            index.put(token, new_list);;
          }
        }
      }

    }
  }

  /** Writes given list to the given file */
  public void writeToFile(String token, PostingsList list, String filename)
  {
    BufferedWriter bw = null;
    FileWriter fw = null;

    try
    {
      File file = new File(filename);

      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }

      // true = append file
      fw = new FileWriter(file.getAbsoluteFile(), true);
      bw = new BufferedWriter(fw);

      bw.write("token " + token + "\n");
      // For all DocIDs
      for (int docID : list.keySet())
      {
        bw.write("docID " + docID + "\n");
        LinkedList<Integer> offsets = list.get(docID).offsets;
        for (int i = 0; i < offsets.size(); i++)
        {
          bw.write(offsets.get(i) + " ");
        }
        bw.write("\n");
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    finally
    {
      try
      {
        if (bw != null)
        {
          bw.close();
        }

        if (fw != null)
        {
          fw.close();
        }

      }
      catch (IOException ex)
      {
        ex.printStackTrace();
      }
    }
  }


  /** Returns size of index */
  public int getSize()
  {
    return index.size();
  }

}

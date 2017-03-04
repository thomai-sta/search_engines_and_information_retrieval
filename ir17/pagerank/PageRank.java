/*
*   This file is part of the computer assignment for the
*   Information Retrieval course at KTH.
*
*   First version:  Johan Boye, 2012
*/

import java.util.*;
import java.io.*;

public class PageRank
{

  /**
   *   Maximal number of documents. We're assuming here that we
   *   don't have more docs than we can keep in main memory.
   */
  final static int MAX_NUMBER_OF_DOCS = 2000000;

  /**
   *   Mapping from document names to document numbers.
   */
  Hashtable<String, Integer> docNumber = new Hashtable<String, Integer>();

  /**
   *   Mapping from document numbers to document names
   */
  String[] docName = new String[MAX_NUMBER_OF_DOCS];

  /**
   *   A memory-efficient representation of the adjacent matrix.
   *   The outlinks are represented as a Hashtable, whose keys are
   *   the numbers of the documents linked from.
   *
   *   The value corresponding to key i is a Hashtable whose keys are
   *   all the numbers of documents j that i links to.
   *
   *   If there are no outlinks from i, then the value corresponding
   *   key i is null.
   */
  Hashtable<Integer, Hashtable<Integer, Boolean>> link =
   new Hashtable<Integer, Hashtable<Integer, Boolean>>();

  /**
   *   The number of outlinks from each node.
   */
  int[] out = new int[MAX_NUMBER_OF_DOCS];

  /**
   *   The number of documents with no outlinks.
   */
  int numberOfSinks = 0;

  /**
   *   The probability that the surfer will be bored, stop
   *   following links, and take a random jump somewhere.
   */
  final static double BORED = 0.15;

  /**
   *   Convergence criterion: Transition probabilities do not
   *   change more that EPSILON from one iteration to another.
   */
  final static double EPSILON = 0.00001;

  /**
   *   Never do more than this number of iterations regardless
   *   of whether the transistion probabilities converge or not.
   */
  final static int MAX_NUMBER_OF_ITERATIONS = 1000;


  /* --------------------------------------------- */


  public PageRank(String filename)
  {
    int noOfDocs = readDocs(filename);
    computePagerank(noOfDocs);
  }


  /* --------------------------------------------- */


  /**
   *   Reads the documents and creates the docs table. When this method
   *   finishes executing then the @code{out} vector of outlinks is
   *   initialised for each doc, and the @code{p} matrix is filled with
   *   zeroes (that indicate direct links) and NO_LINK (if there is no
   *   direct link. <p>
   *
   *   @return the number of documents read.
   */
  int readDocs(String filename)
  {
    int fileIndex = 0;
    try
    {
      System.err.print("Reading file... ");
      BufferedReader in = new BufferedReader(new FileReader(filename));
      String line;
      while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS)
      {
        int index = line.indexOf(";");
        String title = line.substring(0, index);
        Integer fromdoc = docNumber.get(title);
        //  Have we seen this document before?
        if (fromdoc == null)
        {
          // This is a previously unseen doc, so add it to the table.
          fromdoc = fileIndex++;
          docNumber.put(title, fromdoc);
          docName[fromdoc] = title;
        }
        // Check all outlinks.
        StringTokenizer tok =
         new StringTokenizer(line.substring(index + 1), ",");
        while (tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS)
        {
          String otherTitle = tok.nextToken();
          Integer otherDoc = docNumber.get(otherTitle);
          if (otherDoc == null)
          {
            // This is a previousy unseen doc, so add it to the table.
            otherDoc = fileIndex++;
            docNumber.put(otherTitle, otherDoc);
            docName[otherDoc] = otherTitle;
          }
          // Set the probability to 0 for now, to indicate that there is
          // a link from fromdoc to otherDoc.
          if (link.get(fromdoc) == null)
          {
            link.put(fromdoc, new Hashtable<Integer, Boolean>());
          }
          if (link.get(fromdoc).get(otherDoc) == null)
          {
            link.get(fromdoc).put(otherDoc, true);
            out[fromdoc]++;
          }
        }
      }
      if (fileIndex >= MAX_NUMBER_OF_DOCS)
      {
        System.err.print("stopped reading since documents table is full.");
      }
      else
      {
        System.err.print("done.");
      }
      // Compute the number of sinks.
      for (int i = 0; i < fileIndex; i++)
      {
        if (out[i] == 0)
        {
          numberOfSinks++;
        }
      }
    }
    catch (FileNotFoundException e)
    {
      System.err.println("File " + filename + " not found!");
    }
    catch (IOException e)
    {
      System.err.println("Error reading file " + filename);
    }
    System.err.println("Read " + fileIndex + " number of documents");
    return fileIndex;
  }


  /* --------------------------------------------- */


  /*
   *   Computes the pagerank of each document.
   */
  void computePagerank(int numberOfDocs)
  {
    // numberOfDocs = 1000;
    double[] x = new double[numberOfDocs];
    double[] x_new = new double[numberOfDocs];
    int[] indices = new int[numberOfDocs];
    double[] col = new double[numberOfDocs];
    int t = 0;
    boolean converged = true;


    for (int i = 0; i < numberOfDocs; i++)
    {
      indices[i] = i;
      x[i] = 1.0 / (double) numberOfDocs;
      x_new[i] = 0.0;
    }

    while(t++ < MAX_NUMBER_OF_ITERATIONS)
    {
      System.out.println("t = " + t);
      for (int doc_j = 0; doc_j < numberOfDocs; doc_j++)
      {
        for (int row = 0; row  < numberOfDocs; row++)
        {
          if (link.get(row) == null)
          {
            col[row] = 1.0 / (double) numberOfDocs;
          }
          else
          {
            double val = 0.0;
            if (link.get(row).containsKey(doc_j))
            {
              val =  (1 - BORED) / (double) out[row];
            }
            val = val + (double) BORED / (double) numberOfDocs;
            col[row] = val;
          }
        }
        /// We have P's column. Do vector multiplication.
        for (int i = 0; i < numberOfDocs; i++)
        {
          x_new[doc_j] = x_new[doc_j] + col[i] * x[i];
        }
      }

      /// Check x_new - x ======================================================
      converged = true;
      for (int i = 0; i < numberOfDocs; i++)
      {
        if (Math.abs(x[i] - x_new[i]) > EPSILON)
        {
          converged = false;
        }
        x[i] = x_new[i];
        x_new[i] = 0.0;
      }
      if (converged)
      {
        break;
      }
    }

    /// x contains the Pageranks. Sort it in descending order and keep indices (docIDs)
    BubbleSort(x, indices);
    for (int i = 0; i < 30; i++)
    {
      System.out.println(docName[indices[i]] + ": " + x[i]);
    }

    /// write txt with indices, rank
    BufferedWriter bw = null;
    FileWriter fw = null;

    HashMap<String, Double> pageranks = new HashMap<String, Double>();

    try
    {
      File file = new File("pageranks_names.txt");

      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }

      // true = append file
      fw = new FileWriter(file.getAbsoluteFile(), true);
      bw = new BufferedWriter(fw);

      for (int i = 0; i < indices.length; i++)
      {
        pageranks.put(docName[indices[i]], x[i]);
        bw.write(docName[indices[i]] + " " + x[i] + "\n");
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

    /// SAVE HASHMAP
    String filename = "pageranks_hashmap";
    try
    {
      FileOutputStream fout = new FileOutputStream(filename);
      ObjectOutputStream oos = new ObjectOutputStream(fout);
      oos.writeObject(pageranks);
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

  public static void BubbleSort(double[] num, int[] indices)
  {
    int j;
    boolean flag = true;   // set flag to true to begin first pass
    double temp;   // holding variable
    int temp_int;   // holding variable

    while (flag)
    {
      flag = false;    // set flag to false awaiting a possible swap
      for(j = 0; j < num.length - 1; j++)
      {
        if (num[j] < num[j + 1])   // change to > for ascending sort
        {
          temp = num[j];                // swap elements
          num[j] = num[j + 1];
          num[j + 1] = temp;

          temp_int = indices[j];                // swap indices
          indices[j] = indices[j + 1];
          indices[j + 1] = temp_int;

          flag = true;              // shows a swap occurred
        }
      }
    }
  }



  /* --------------------------------------------- */


  public static void main(String[] args)
  {
    if (args.length != 1)
    {
      System.err.println("Please give the name of the link file");
    }
    else
    {
      new PageRank(args[0]);
    }
  }
}

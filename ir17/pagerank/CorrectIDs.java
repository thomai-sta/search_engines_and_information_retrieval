/*
*   This file is part of the computer assignment for the
*   Information Retrieval course at KTH.
*
*   First version:  Johan Boye, 2012
*/

import java.util.*;
import java.io.*;
import java.util.HashMap;

public class CorrectIDs
{

  HashMap<Integer, Double> ranks = new HashMap<Integer, Double>();
  HashMap<Integer, String> names = new HashMap<Integer, String>();
  HashMap<String, Double> name_ranks = new HashMap<String, Double>();


  public CorrectIDs(String[] filenames)
  {
    readFiles(filenames);
    correct();
  }

  public void readFiles(String[] filenames)
  {
    /// Read in computed ranks =================================================
    File f = new File(filenames[0]);
    if(f.exists() && !f.isDirectory())
    {
      try
      {
        FileInputStream fin = new FileInputStream(filenames[0]);
        ObjectInputStream ois = new ObjectInputStream(fin);
        HashMap<String, Double> temp = (HashMap<String, Double>) ois.readObject();
        for (String key : temp.keySet())
        {
          ranks.put(Integer.parseInt(key), temp.get(key));
        }
        ois.close();
        fin.close();
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    /// Read doc names =========================================================
    Scanner sc = null;
    try
    {
      sc = new Scanner(new File(filenames[1]));
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }

    while (sc.hasNextLine())
    {

      String line = sc.nextLine();
      String[] words = line.split(";");

      names.put(Integer.parseInt(words[0]), words[1]);
    }
  }

  public void correct()
  {
    double[] x = new double[ranks.size()];
    String[] names_array = new String[ranks.size()];
    int idx = 0;
    for (Integer key : names.keySet())
    {
      if (names.get(key) != null && ranks.get(key) != null)
      {
        name_ranks.put(names.get(key), ranks.get(key));
        names_array[idx] = names.get(key);
        x[idx++] = ranks.get(key);
      }
    }
    /// Sort and print to check
    /// Bubblesort
    int j;
    boolean flag = true;
    double temp;
    String temp_string;

    while (flag)
    {
      flag = false;
      for(j = 0; j < x.length - 1; j++)
      {
        if (x[j] < x[j + 1])
        {
          temp = x[j];
          x[j] = x[j + 1];
          x[j + 1] = temp;

          temp_string = names_array[j];
          names_array[j] = names_array[j + 1];
          names_array[j + 1] = temp_string;

          flag = true;
        }
      }
    }

    /// write txt with name and rank
    BufferedWriter bw = null;
    FileWriter fw = null;

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

      for (int i = 0; i < names_array.length; i++)
      {
        bw.write(names_array[i] + " " + x[i] + "\n");
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
    String filename = "pageranks_names_hashmap";
    try
    {
      FileOutputStream fout = new FileOutputStream(filename);
      ObjectOutputStream oos = new ObjectOutputStream(fout);
      oos.writeObject(name_ranks);
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


  public static void main(String[] args)
  {
    if (args.length != 2)
    {
      System.err.println("Please give the name of the files");
    }
    else
    {
      new CorrectIDs(args);
    }
  }
}

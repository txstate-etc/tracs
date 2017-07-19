package edu.txstate.tracs.jdbc;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.net.URL;

import org.springframework.core.io.Resource;

public class Queries 
{
  private Properties queries;
  private URL queryFileResource;

  public Queries(URL queryFile)
  {
    queryFileResource = queryFile;
    /*try {
      getQueries();
    } catch(Exception e) {}*/
  }

  public String get(String queryName) throws QueryNotFoundException
  {
    if (queries == null)
      getQueries();

    if (queries.containsKey(queryName))
      return queries.getProperty(queryName);

    throw new QueryNotFoundException("Query " + queryName + " not found in queries.sql");
  }

  private void getQueries() throws QueryNotFoundException
  {
    InputStream inputStream = null;
    try
    {
      inputStream = queryFileResource.openStream();
    }
    catch (IOException ex)
    {
      throw new QueryNotFoundException("There was error loading the query file.", ex);
    }

    if (inputStream == null)
    {
      throw new QueryNotFoundException("Could not find queries.sql");
    }

    QueryFileReader reader = new QueryFileReader(inputStream);

    try
    {
      queries = reader.read();
    }
    catch (IOException ex)
    {
      throw new QueryNotFoundException("There was an error reading queries.sql.", ex);
    }
    finally
    {
      try
      {
        inputStream.close();
      }
      catch (IOException ex)
      {
      }
    }
  }
}

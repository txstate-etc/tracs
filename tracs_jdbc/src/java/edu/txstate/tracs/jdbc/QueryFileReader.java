package edu.txstate.tracs.jdbc;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.lang.StringBuilder;
import java.util.Properties;

public class QueryFileReader
{
  private static final String NEW_LINE = System.getProperty("line.separator");

	private LineNumberReader reader;
  private Properties queries;

  private String currentLine;
  private String currentQueryName;
  private StringBuilder currentQuery;
  private boolean readingQuery;

	public QueryFileReader(InputStream stream)
	{
		reader = new LineNumberReader(new InputStreamReader(stream));
    queries = new Properties();
    readingQuery = false;
	}

	public Properties read() throws IOException
	{
    while ((currentLine = reader.readLine()) != null)
    {
      currentLine = currentLine.trim();

      // Ignore empty line or comment
      if (currentLine.length() < 1 || currentLine.startsWith("--"))
        continue;

      if (!readingQuery)
      {
        readQueryHeader();
      }
      else
      {
        if (currentLine.equals("}"))
        {
          endQuery();
        }
        else
        {
          addCurrentLineToQuery();
        }
      }
    }

    return queries;
	}

  private void readQueryHeader() throws IOException
  {
    int braceIndex = currentLine.indexOf("{");

    // Ignore the line if it doesn't start a query
    if (braceIndex == -1) return;

    // Query must have a name
    if (braceIndex == 0)
    {
      throw new IOException("Query starting at line " + reader.getLineNumber() + " has no name.");
    }

    currentQueryName = currentLine.substring(0, braceIndex).trim();
    currentQuery = new StringBuilder();

    // If opening brace doesn't end the line, the rest of the line should be part of the query
    if (braceIndex != currentLine.length() - 1)
    {
      String rightOfBrace = currentLine.substring(braceIndex + 1, currentLine.length()).trim();
      currentQuery.append(rightOfBrace);
      currentQuery.append(NEW_LINE);
    }

    readingQuery = true;
  }

  private void addCurrentLineToQuery()
  {
    currentQuery.append(currentLine);
    currentQuery.append(NEW_LINE);
  }

  private void endQuery()
  {
    queries.setProperty(currentQueryName, currentQuery.toString());
    currentQuery = null;
    readingQuery = false;
  }

}

package edu.txstate.tracs.jdbc;

public class QueryNotFoundException extends Exception
{
  QueryNotFoundException(String message) 
  {
    super(message);
  }

  public QueryNotFoundException(String message, Throwable cause)
  {
    super(message, cause);
  }
}

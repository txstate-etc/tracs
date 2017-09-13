package edu.txstate.tracs.jdbc;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;


public class BaseJdbcDao extends NamedParameterJdbcDaoSupport
{
  private static final Logger LOG = LoggerFactory.getLogger(BaseJdbcDao.class);

  private Queries queries;

  public BaseJdbcDao()
  {
    queries = new Queries(getClass().getClassLoader().getResource("queries.sql"));
  }

  protected String queryForStringNullAsEmpty(String queryName, Object arg)
  {
    return queryForStringNullAsEmpty(queryName, new Object[] { arg });
  }

  protected String queryForStringNullAsEmpty(String queryName, Object[] args)
  {
    try
    {
      String query = queries.get(queryName);
      String result = (String) getJdbcTemplate().queryForObject(query, args, String.class);
      if (result == null) result = "";
      return result;
    }
    catch (QueryNotFoundException ex)
    {
      LOG.error("Can't find query " + queryName);
      ex.printStackTrace();
      return null;
    }
    catch (DataAccessException ex)
    {
      LOG.error("There was an error executing query " + queryName + "with args " + args.toString());
      ex.printStackTrace();
      return null;
    }
  }

  protected String queryForString(String queryName, Object arg)
  {
    return (String) queryForObject(queryName, arg, String.class);
  }

  protected String queryForString(String queryName, Object[] args)
  {
    return (String) queryForObject(queryName, args, String.class);
  }

  protected Object queryForObject(String queryName, Object arg, Class requiredType)
  {
    return queryForObject(queryName, new Object[] { arg }, requiredType);
  }

  protected Object queryForObject(String queryName, Object[] args, Class requiredType)
  {
    try
    {
      String query = queries.get(queryName);
      return getJdbcTemplate().queryForObject(query, args, requiredType);
    }
    catch (QueryNotFoundException ex)
    {
      LOG.error("Can't find query " + queryName);
      ex.printStackTrace();
      return null;
    }
    catch (DataAccessException ex)
    {
      LOG.error("There was an error executing query " + queryName + " with args " + args.toString());
      ex.printStackTrace();
      return null;
    }
  }

  protected List<Map<String, Object>> queryForList(String queryName, MapSqlParameterSource parameters)
  {
    try
    {
      String query = queries.get(queryName);
      LOG.info("Getting query : " + queryName + " with parameters " + parameters.toString());
      return getNamedParameterJdbcTemplate().queryForList(query, parameters);
    }
    catch (QueryNotFoundException ex)
    {
      LOG.error("Can't find query " + queryName);
      ex.printStackTrace();
      return null;
    }
    catch (DataAccessException ex)
    {
      LOG.error("There was an error executing query " + queryName + " with parameters " + parameters.toString());
      ex.printStackTrace();
      return null;
    }
  }

  protected Object queryForObject(String queryName, MapSqlParameterSource parameters, Class requiredType)
  {
    try
    {
      String query = queries.get(queryName);
      return getNamedParameterJdbcTemplate().queryForObject(query, parameters, requiredType);
    }
    catch (QueryNotFoundException ex)
    {
      LOG.error("Can't find query " + queryName);
      ex.printStackTrace();
      return null;
    }
    catch (DataAccessException ex)
    {
      LOG.error("There was an error executing query " + queryName + " with parameters " + parameters.toString());
      ex.printStackTrace();
      return null;
    }
  }

  protected Object queryForObject(String queryName, Object arg, RowMapper mapper)
  {
    return queryForObject(queryName, new Object[] { arg }, mapper);
  }

  protected Object queryForObject(String queryName, Object[] args, RowMapper mapper)
  {
    try
    {
      String query = queries.get(queryName);
      return getJdbcTemplate().queryForObject(query, args, mapper);
    }
    catch (QueryNotFoundException ex)
    {
      LOG.error("Can't find query " + queryName);
      ex.printStackTrace();
      return null;
    }
    catch (DataAccessException ex)
    {
      LOG.error("There was an error executing query " + queryName + " with args " + args.toString());
      ex.printStackTrace();
      return null;
    }
  }

  protected List queryForList(String queryName, Object arg, Class requiredType)
  {
    return queryForList(queryName, new Object[] { arg }, requiredType);
  }

  protected List queryForList(String queryName, Object[] args, Class requiredType)
  {
    try 
    {
      String query = queries.get(queryName);
      return getJdbcTemplate().queryForList(query, args, requiredType);
    }
    catch (QueryNotFoundException ex)
    {
      LOG.error("Can't find query " + queryName);
      ex.printStackTrace();
      return null;
    }
    catch (DataAccessException ex)
    {
      LOG.error("There was an error executing query " + queryName + "with args " + args.toString());
      ex.printStackTrace();
      return null;
    }
  }

  protected SqlRowSet queryForRowSet(String queryName, Object arg)
  {
    return queryForRowSet(queryName, new Object[] { arg });
  }

  protected SqlRowSet queryForRowSet(String queryName, Object[] args)
  {
    try 
    {
      String query = queries.get(queryName);
      return getJdbcTemplate().queryForRowSet(query, args);
    }
    catch (QueryNotFoundException ex)
    {
      LOG.error("Can't find query " + queryName);
      ex.printStackTrace();
      return null;
    }
    catch (DataAccessException ex)
    {
      LOG.error("There was an error executing query " + queryName + "with args " + args.toString());
      ex.printStackTrace();
      return null;
    }
  }

  protected List query(String queryName, Object[] args, RowMapper mapper)
  {
    try
    {
      String query = queries.get(queryName);
      return getJdbcTemplate().query(query, args, mapper);
    }
    catch (QueryNotFoundException ex)
    {
      LOG.error("Can't find query " + queryName);
      ex.printStackTrace();
      return null;
    }
    catch (DataAccessException ex)
    {
      LOG.error("There was an error executing query " + queryName + "with args " + args.toString());
      ex.printStackTrace();
      return null;
    }
  }

  protected int update(String queryName, Object arg)
  {
    return update(queryName, new Object[] { arg });
  }

  protected int update(String queryName, Object[] args)
  {
    try
    {
      String query = queries.get(queryName);
      return getJdbcTemplate().update(query, args);
    }
    catch (QueryNotFoundException ex)
    {
      LOG.error("Can't find query " + queryName);
      ex.printStackTrace();
      return 0;
    }
    catch (DataAccessException ex)
    {
      LOG.error("There was an error executing query " + queryName + " with args " + args.toString());
      ex.printStackTrace();
      return 0;
    }
  }
}

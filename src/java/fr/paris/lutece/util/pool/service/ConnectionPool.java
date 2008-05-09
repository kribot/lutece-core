/*
 * Copyright (c) 2002-2008, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.util.pool.service;

import fr.paris.lutece.portal.service.util.AppException;

import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;


/**
 * This class manages a database connection pool.
 */
public class ConnectionPool
{
    private static final String DEFAULT_CHECK_VALID_CONNECTION_SQL = "SELECT 1";
    private String _strUrl;
    private String _strUser;
    private String _strPassword;
    private int _nMaxConns;
    private int _nTimeOut;
    private Logger _logger;
    private int _nCheckedOut;
    private List<Connection> _freeConnections = new ArrayList<Connection>(  );
    private String _strCheckValidConnectionSql; // Added in v1.4

    /**
     * Constructor
     *
     * @param strName Nom du pool
     * @param strUrl JDBC Data source URL
     * @param strUser SQL User
     * @param strPassword SQL Password
     * @param nMaxConns Max connections
     * @param nInitConns Initials connections
     * @param nTimeOut Timeout to get a connection
     * @param logger the Logger object
     * @param strCheckValidConnectionSql The SQL syntax used for check connexion validatation
     */
    public ConnectionPool( String strName, String strUrl, String strUser, String strPassword, int nMaxConns,
        int nInitConns, int nTimeOut, Logger logger, String strCheckValidConnectionSql )
    {
        _strUrl = strUrl;
        _strUser = strUser;
        _strPassword = strPassword;
        _nMaxConns = nMaxConns;
        _nTimeOut = ( nTimeOut > 0 ) ? nTimeOut : 5;
        _logger = logger;
        initPool( nInitConns );
        _logger.info( "New pool created : " + strName );

        _strCheckValidConnectionSql = ( ( strCheckValidConnectionSql != null ) &&
            !strCheckValidConnectionSql.equals( "" ) ) ? strCheckValidConnectionSql : DEFAULT_CHECK_VALID_CONNECTION_SQL;

        String lf = System.getProperty( "line.separator" );
        _logger.debug( lf + " url=" + strUrl + lf + " user=" + _strUser + lf + " password=" + _strPassword + lf +
            " initconns=" + nInitConns + lf + " maxconns=" + _nMaxConns + lf + " logintimeout=" + _nTimeOut );
        _logger.debug( getStats(  ) );
    }

    /**
     * Initializes the pool
     *
     * @param initConns Number of connections to create at the initialisation
     */
    private void initPool( int initConns )
    {
        for ( int i = 0; i < initConns; i++ )
        {
            try
            {
                Connection pc = newConnection(  );
                _freeConnections.add( pc );
            }
            catch ( SQLException e )
            {
                throw new AppException( "SQL Error executing command : " + e.toString(  ) );
            }
        }
    }

    /**
     * Returns a connection from the pool.
     *
     * @return An open connection
     * @throws SQLException The SQL exception
     */
    public Connection getConnection(  ) throws SQLException
    {
        _logger.debug( "Request for connection received" );

        try
        {
            return getConnection( _nTimeOut * 1000 );
        }
        catch ( SQLException e )
        {
            _logger.error( "Exception getting connection", e );
            throw e;
        }
    }

    /**
     * Returns a connection from the pool.
     *
     * @param timeout The maximum time to wait for a connection
     * @return An open connection
     * @throws SQLException The SQL exception
     */
    private Connection getConnection( long timeout ) throws SQLException
    {
        // Get a pooled Connection from the cache or a new one.
        // Wait if all are checked out and the max limit has
        // been reached.
        long remaining = timeout;
        Connection conn = getPooledConnection(  );

        if ( conn == null )
        {
            throw new AppException( "Connection pool error : max connections reached (" + _nMaxConns +
                "). No more connection available" );
        }

        // Check if the Connection is still OK
        if ( !isConnectionOK( conn ) )
        {
            // It was bad. Try again with the remaining timeout
            _logger.error( "Removed selected bad connection from pool" );

            return getConnection( remaining );
        }

        _nCheckedOut++;
        _logger.debug( "Delivered connection from pool" );
        _logger.debug( getStats(  ) );

        return conn;
    }

    /**
     * Checks a connection to see if it's still alive
     *
     * @param conn The connection to check
     * @return true if the connection is OK, otherwise false.
     */
    private boolean isConnectionOK( Connection conn )
    {
        Statement testStmt = null;

        try
        {
            if ( !conn.isClosed(  ) )
            {
                // Try to createStatement to see if it's really alive
                testStmt = conn.createStatement(  );
                testStmt.executeQuery( _strCheckValidConnectionSql );
                testStmt.close(  );
            }
            else
            {
                return false;
            }
        }
        catch ( SQLException e )
        {
            if ( testStmt != null )
            {
                try
                {
                    testStmt.close(  );
                }
                catch ( SQLException se )
                {
                    throw new AppException( "ConnectionService : SQL Error executing command : " + se.toString(  ) );
                }
            }

            _logger.error( "Pooled Connection was not okay", e );

            return false;
        }

        return true;
    }

    /**
     * Gets a connection from the pool
     *
     * @return An opened connection
     * @throws SQLException The exception
     */
    private synchronized Connection getPooledConnection(  )
        throws SQLException
    {
        Connection conn = null;

        if ( _freeConnections.size(  ) > 0 )
        {
            // Pick the first Connection in the Vector
            // to get round-robin usage
            conn = (Connection) _freeConnections.get( 0 );
            _freeConnections.remove( 0 );
        }
        else if ( ( _nMaxConns == 0 ) || ( _nCheckedOut < _nMaxConns ) )
        {
            conn = newConnection(  );
        }

        return conn;
    }

    /**
     * Creates a new connection
     *
     * @return The new created connection
     * @throws SQLException The SQL exception
     */
    private Connection newConnection(  ) throws SQLException
    {
        Connection conn = null;

        if ( _strUser == null )
        {
            conn = DriverManager.getConnection( _strUrl );
        }
        else
        {
            conn = DriverManager.getConnection( _strUrl, _strUser, _strPassword );
        }

        _logger.info( "New connection created. Connections count is : " + ( getConnectionCount(  ) + 1 ) );

        return conn;
    }

    /**
     * Returns a connection to pool.
     *
     * @param conn The released connection to return to pool
     */
    public synchronized void freeConnection( Connection conn )
    {
        // Put the connection at the end of the Vector
        _freeConnections.add( conn );
        _nCheckedOut--;
        _logger.debug( "Returned connection to pool" );
        _logger.debug( getStats(  ) );
    }

    /**
     * Releases the pool by closing all its connections.
     */
    public synchronized void release(  )
    {
        for ( Connection connection : _freeConnections )
        {
            try
            {
                connection.close(  );
                _logger.debug( "Closed connection" );
            }
            catch ( SQLException e )
            {
                _logger.error( "Couldn't close connection", e );
            }
        }

        _freeConnections.removeAll( _freeConnections );
    }

    /**
     * Returns stats on pool's connections
     *
     * @return Stats as String.
     */
    private String getStats(  )
    {
        return "Total connections: " + getConnectionCount(  ) + " Available: " + getFreeConnectionCount(  ) +
        " Checked-out: " + getBusyConnectionCount(  );
    }

    /**
     * Returns the number of connections opened by the pool (available or busy)
     * @return A connection count
     */
    public int getConnectionCount(  )
    {
        return getFreeConnectionCount(  ) + getBusyConnectionCount(  );
    }

    /**
     * Returns the number of free connections of the pool (available or busy)
     * @return A connection count
     */
    public int getFreeConnectionCount(  )
    {
        return _freeConnections.size(  );
    }

    /**
     * Returns the number of busy connections of the pool (available or busy)
     * @return A connection count
     */
    public int getBusyConnectionCount(  )
    {
        return _nCheckedOut;
    }

    /**
     * Returns the maximum number of connections of the pool
     * @return A connection count
     */
    public int getMaxConnectionCount(  )
    {
        return _nMaxConns;
    }
}

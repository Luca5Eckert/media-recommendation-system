"""
Database connection manager with connection pooling
"""

import psycopg2
from psycopg2 import pool
import logging
from contextlib import contextmanager

logger = logging.getLogger(__name__)


class DatabaseConnection:
    """
    Manages PostgreSQL database connections with connection pooling
    for optimal performance and resource management
    """
    
    def __init__(self, host, port, database, user, password, minconn=1, maxconn=10):
        """
        Initialize database connection pool
        
        Args:
            host: Database host
            port: Database port
            database: Database name
            user: Database user
            password: Database password
            minconn: Minimum number of connections in pool
            maxconn: Maximum number of connections in pool
        """
        self.host = host
        self.port = port
        self.database = database
        self.user = user
        self.password = password
        
        try:
            logger.info(f"Initializing connection pool to {host}:{port}/{database}")
            self.pool = psycopg2.pool.ThreadedConnectionPool(
                minconn=minconn,
                maxconn=maxconn,
                host=host,
                port=port,
                database=database,
                user=user,
                password=password,
                connect_timeout=10
            )
            logger.info("Database connection pool initialized successfully")
        except Exception as e:
            logger.error(f"Failed to create connection pool: {str(e)}")
            raise
    
    @contextmanager
    def get_connection(self):
        """
        Context manager for getting database connections from pool
        Automatically returns connection to pool after use
        
        Usage:
            with db.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT * FROM users")
        """
        conn = None
        try:
            conn = self.pool.getconn()
            yield conn
        except Exception as e:
            if conn:
                conn.rollback()
            logger.error(f"Database error: {str(e)}")
            raise
        finally:
            if conn:
                self.pool.putconn(conn)
    
    @contextmanager
    def get_cursor(self, cursor_factory=None):
        """
        Context manager for getting database cursor
        Automatically commits and closes cursor after use
        
        Args:
            cursor_factory: Optional cursor factory (e.g., RealDictCursor)
        
        Usage:
            with db.get_cursor() as cursor:
                cursor.execute("SELECT * FROM users")
                results = cursor.fetchall()
        """
        with self.get_connection() as conn:
            cursor = conn.cursor(cursor_factory=cursor_factory) if cursor_factory else conn.cursor()
            try:
                yield cursor
                conn.commit()
            except Exception as e:
                conn.rollback()
                logger.error(f"Transaction error: {str(e)}")
                raise
            finally:
                cursor.close()
    
    def execute_query(self, query, params=None, fetch_one=False, cursor_factory=None):
        """
        Execute a query and return results
        
        Args:
            query: SQL query to execute
            params: Query parameters (tuple or dict)
            fetch_one: If True, returns single row, otherwise all rows
            cursor_factory: Optional cursor factory
        
        Returns:
            Query results
        """
        with self.get_cursor(cursor_factory=cursor_factory) as cursor:
            cursor.execute(query, params)
            if fetch_one:
                return cursor.fetchone()
            return cursor.fetchall()
    
    def execute_many(self, query, params_list):
        """
        Execute same query with multiple parameter sets (batch operation)
        
        Args:
            query: SQL query to execute
            params_list: List of parameter tuples/dicts
        
        Returns:
            Number of rows affected
        """
        with self.get_cursor() as cursor:
            cursor.executemany(query, params_list)
            return cursor.rowcount
    
    def close_all_connections(self):
        """Close all connections in the pool"""
        if self.pool:
            self.pool.closeall()
            logger.info("All database connections closed")

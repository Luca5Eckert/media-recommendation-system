"""
Data Access Layer for user-related queries
Connects to user_db
"""

import logging
from psycopg2.extras import RealDictCursor

logger = logging.getLogger(__name__)


class UserRepository:
    """Repository for accessing user data from user_db"""
    
    def __init__(self, db_connection):
        """
        Initialize user repository
        
        Args:
            db_connection: DatabaseConnection instance configured for user_db
        """
        self.db = db_connection
    
    def get_user_preferences(self, user_id):
        """
        Get user preferences including favorite genres
        
        Args:
            user_id: UUID of the user
        
        Returns:
            Dictionary with user preferences or None if not found
        """
        try:
            query = """
                SELECT 
                    up.id,
                    up.user_id,
                    COALESCE(array_agg(DISTINCT ug.genre_name) FILTER (WHERE ug.genre_name IS NOT NULL), ARRAY[]::text[]) as genres
                FROM user_preferences up
                LEFT JOIN tb_user_genre ug ON ug.user_id = up.user_id
                WHERE up.user_id = %s
                GROUP BY up.id, up.user_id
            """
            
            result = self.db.execute_query(
                query,
                (user_id,),
                fetch_one=True,
                cursor_factory=RealDictCursor
            )
            
            if result:
                return dict(result)
            return None
            
        except Exception as e:
            logger.error(f"Error fetching user preferences for user {user_id}: {str(e)}")
            return None
    
    def get_batch_user_preferences(self, user_ids):
        """
        Get preferences for multiple users in a single query (performance optimization)
        
        Args:
            user_ids: List of user UUIDs
        
        Returns:
            Dictionary mapping user_id to preferences
        """
        try:
            if not user_ids:
                return {}
            
            query = """
                SELECT 
                    up.user_id,
                    COALESCE(array_agg(DISTINCT ug.genre_name) FILTER (WHERE ug.genre_name IS NOT NULL), ARRAY[]::text[]) as genres
                FROM user_preferences up
                LEFT JOIN tb_user_genre ug ON ug.user_id = up.user_id
                WHERE up.user_id = ANY(%s)
                GROUP BY up.user_id
            """
            
            results = self.db.execute_query(
                query,
                (list(user_ids),),
                cursor_factory=RealDictCursor
            )
            
            # Convert to dictionary
            preferences_map = {}
            for row in results:
                preferences_map[str(row['user_id'])] = {
                    'user_id': str(row['user_id']),
                    'genres': row['genres'] or []
                }
            
            return preferences_map
            
        except Exception as e:
            logger.error(f"Error fetching batch user preferences: {str(e)}")
            return {}

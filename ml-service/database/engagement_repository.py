"""
Data Access Layer for engagement/interaction queries
Connects to engagement_db
"""

import logging
from psycopg2.extras import RealDictCursor
from datetime import datetime, timedelta

logger = logging.getLogger(__name__)


class EngagementRepository:
    """Repository for accessing user interaction data from engagement_db"""
    
    def __init__(self, db_connection):
        """
        Initialize engagement repository
        
        Args:
            db_connection: DatabaseConnection instance configured for engagement_db
        """
        self.db = db_connection
    
    def get_user_interactions(self, user_id, days=90):
        """
        Get user's interaction history
        
        Args:
            user_id: UUID of the user
            days: Number of days to look back (default: 90)
        
        Returns:
            List of interaction dictionaries
        """
        try:
            cutoff_date = datetime.now() - timedelta(days=days)
            
            query = """
                SELECT 
                    user_id,
                    media_id,
                    type,
                    interaction_value,
                    timestamp
                FROM interaction
                WHERE user_id = %s 
                AND timestamp >= %s
                ORDER BY timestamp DESC
            """
            
            results = self.db.execute_query(
                query,
                (user_id, cutoff_date),
                cursor_factory=RealDictCursor
            )
            
            interactions = []
            for row in results:
                interactions.append({
                    'user_id': str(row['user_id']),
                    'media_id': str(row['media_id']),
                    'type': row['type'],
                    'interaction_value': float(row['interaction_value']),
                    'timestamp': row['timestamp'].isoformat() if row['timestamp'] else None
                })
            
            return interactions
            
        except Exception as e:
            logger.error(f"Error fetching user interactions for user {user_id}: {str(e)}")
            return []
    
    def get_user_interaction_summary(self, user_id, days=90):
        """
        Get aggregated interaction summary for a user
        
        Args:
            user_id: UUID of the user
            days: Number of days to look back
        
        Returns:
            Dictionary with interaction statistics
        """
        try:
            cutoff_date = datetime.now() - timedelta(days=days)
            
            query = """
                SELECT 
                    media_id,
                    COUNT(*) as interaction_count,
                    SUM(CASE WHEN type = 'LIKE' THEN 1 ELSE 0 END) as like_count,
                    SUM(CASE WHEN type = 'DISLIKE' THEN 1 ELSE 0 END) as dislike_count,
                    SUM(CASE WHEN type = 'WATCH' THEN 1 ELSE 0 END) as watch_count,
                    SUM(interaction_value) as total_value,
                    MAX(timestamp) as last_interaction
                FROM interaction
                WHERE user_id = %s 
                AND timestamp >= %s
                GROUP BY media_id
                ORDER BY total_value DESC, last_interaction DESC
            """
            
            results = self.db.execute_query(
                query,
                (user_id, cutoff_date),
                cursor_factory=RealDictCursor
            )
            
            summary = {}
            for row in results:
                summary[str(row['media_id'])] = {
                    'media_id': str(row['media_id']),
                    'interaction_count': row['interaction_count'],
                    'like_count': row['like_count'],
                    'dislike_count': row['dislike_count'],
                    'watch_count': row['watch_count'],
                    'total_value': float(row['total_value']),
                    'last_interaction': row['last_interaction'].isoformat() if row['last_interaction'] else None
                }
            
            return summary
            
        except Exception as e:
            logger.error(f"Error fetching interaction summary for user {user_id}: {str(e)}")
            return {}
    
    def get_similar_users_interactions(self, user_id, limit=100, days=90):
        """
        Get interactions from users with similar taste (collaborative filtering data)
        Finds users who interacted with same media and returns their other interactions
        
        Args:
            user_id: UUID of the user
            limit: Maximum number of similar user interactions to return
            days: Number of days to look back
        
        Returns:
            List of media IDs that similar users interacted with
        """
        try:
            cutoff_date = datetime.now() - timedelta(days=days)
            
            query = """
                WITH user_media AS (
                    -- Get media that current user interacted with positively
                    SELECT DISTINCT media_id
                    FROM interaction
                    WHERE user_id = %s 
                    AND timestamp >= %s
                    AND (type = 'LIKE' OR type = 'WATCH')
                ),
                similar_users AS (
                    -- Find users who also interacted with same media
                    SELECT DISTINCT i.user_id
                    FROM interaction i
                    INNER JOIN user_media um ON i.media_id = um.media_id
                    WHERE i.user_id != %s
                    AND i.timestamp >= %s
                    AND (i.type = 'LIKE' OR i.type = 'WATCH')
                    LIMIT 50
                ),
                similar_user_interactions AS (
                    -- Get other media that similar users liked
                    SELECT 
                        i.media_id,
                        COUNT(DISTINCT i.user_id) as user_count,
                        SUM(i.interaction_value) as total_value
                    FROM interaction i
                    INNER JOIN similar_users su ON i.user_id = su.user_id
                    WHERE i.timestamp >= %s
                    AND (i.type = 'LIKE' OR i.type = 'WATCH')
                    AND i.media_id NOT IN (SELECT media_id FROM user_media)
                    GROUP BY i.media_id
                    ORDER BY user_count DESC, total_value DESC
                    LIMIT %s
                )
                SELECT 
                    media_id,
                    user_count,
                    total_value
                FROM similar_user_interactions
            """
            
            results = self.db.execute_query(
                query,
                (user_id, cutoff_date, user_id, cutoff_date, cutoff_date, limit),
                cursor_factory=RealDictCursor
            )
            
            recommendations = []
            for row in results:
                recommendations.append({
                    'media_id': str(row['media_id']),
                    'user_count': row['user_count'],
                    'total_value': float(row['total_value']),
                    'score': row['user_count'] * float(row['total_value'])
                })
            
            return recommendations
            
        except Exception as e:
            logger.error(f"Error fetching similar users interactions for user {user_id}: {str(e)}")
            return []

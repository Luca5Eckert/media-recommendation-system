"""
Data Access Layer for catalog-related queries
Connects to catalog_db
"""

import logging
from psycopg2.extras import RealDictCursor

logger = logging.getLogger(__name__)


class CatalogRepository:
    """Repository for accessing media catalog data from catalog_db"""
    
    def __init__(self, db_connection):
        """
        Initialize catalog repository
        
        Args:
            db_connection: DatabaseConnection instance configured for catalog_db
        """
        self.db = db_connection
    
    def get_all_media(self, limit=1000):
        """
        Get all available media with their genres
        Uses LEFT JOIN and aggregation to avoid N+1 query problem
        
        Args:
            limit: Maximum number of media items to return
        
        Returns:
            List of media dictionaries
        """
        try:
            query = """
                SELECT 
                    m.id,
                    m.title,
                    m.description,
                    m.release_year,
                    m.media_type,
                    m.cover_url,
                    COALESCE(array_agg(DISTINCT mg.genre_name) FILTER (WHERE mg.genre_name IS NOT NULL), ARRAY[]::text[]) as genres
                FROM medias m
                LEFT JOIN tb_media_genre mg ON mg.media_id = m.id
                WHERE m.deleted_at IS NULL
                GROUP BY m.id, m.title, m.description, m.release_year, m.media_type, m.cover_url
                ORDER BY m.create_at DESC
                LIMIT %s
            """
            
            results = self.db.execute_query(
                query,
                (limit,),
                cursor_factory=RealDictCursor
            )
            
            media_list = []
            for row in results:
                media_list.append({
                    'id': str(row['id']),
                    'title': row['title'],
                    'description': row['description'],
                    'release_year': row['release_year'],
                    'media_type': row['media_type'],
                    'cover_url': row['cover_url'],
                    'genres': row['genres'] or []
                })
            
            return media_list
            
        except Exception as e:
            logger.error(f"Error fetching all media: {str(e)}")
            return []
    
    def get_media_by_ids(self, media_ids):
        """
        Get media details by IDs (batch query to avoid N+1)
        
        Args:
            media_ids: List of media UUIDs
        
        Returns:
            Dictionary mapping media_id to media details
        """
        try:
            if not media_ids:
                return {}
            
            query = """
                SELECT 
                    m.id,
                    m.title,
                    m.description,
                    m.release_year,
                    m.media_type,
                    m.cover_url,
                    COALESCE(array_agg(DISTINCT mg.genre_name) FILTER (WHERE mg.genre_name IS NOT NULL), ARRAY[]::text[]) as genres
                FROM medias m
                LEFT JOIN tb_media_genre mg ON mg.media_id = m.id
                WHERE m.id = ANY(%s) AND m.deleted_at IS NULL
                GROUP BY m.id, m.title, m.description, m.release_year, m.media_type, m.cover_url
            """
            
            results = self.db.execute_query(
                query,
                (list(media_ids),),
                cursor_factory=RealDictCursor
            )
            
            media_map = {}
            for row in results:
                media_map[str(row['id'])] = {
                    'id': str(row['id']),
                    'title': row['title'],
                    'description': row['description'],
                    'release_year': row['release_year'],
                    'media_type': row['media_type'],
                    'cover_url': row['cover_url'],
                    'genres': row['genres'] or []
                }
            
            return media_map
            
        except Exception as e:
            logger.error(f"Error fetching media by IDs: {str(e)}")
            return {}
    
    def get_media_by_genres(self, genres, limit=100, exclude_ids=None):
        """
        Get media that match specified genres
        
        Args:
            genres: List of genre names
            limit: Maximum number of results
            exclude_ids: List of media IDs to exclude
        
        Returns:
            List of media dictionaries
        """
        try:
            if not genres:
                return []
            
            exclude_clause = ""
            params = [genres]
            
            if exclude_ids and len(exclude_ids) > 0:
                exclude_clause = "AND m.id != ALL(%s)"
                params.append(list(exclude_ids))
            
            params.append(limit)
            
            # Build query with string concatenation for safety
            query = """
                SELECT 
                    m.id,
                    m.title,
                    m.description,
                    m.release_year,
                    m.media_type,
                    m.cover_url,
                    COALESCE(array_agg(DISTINCT mg.genre_name) FILTER (WHERE mg.genre_name IS NOT NULL), ARRAY[]::text[]) as genres,
                    COUNT(DISTINCT CASE WHEN mg.genre_name = ANY(%s) THEN mg.genre_name END) as genre_match_count
                FROM medias m
                LEFT JOIN tb_media_genre mg ON mg.media_id = m.id
                WHERE m.deleted_at IS NULL """ + exclude_clause + """
                GROUP BY m.id, m.title, m.description, m.release_year, m.media_type, m.cover_url
                HAVING COUNT(DISTINCT CASE WHEN mg.genre_name = ANY(%s) THEN mg.genre_name END) > 0
                ORDER BY genre_match_count DESC, m.create_at DESC
                LIMIT %s
            """
            
            # Add genres parameter again for HAVING clause
            params.insert(1, genres)
            
            results = self.db.execute_query(
                query,
                tuple(params),
                cursor_factory=RealDictCursor
            )
            
            media_list = []
            for row in results:
                media_list.append({
                    'id': str(row['id']),
                    'title': row['title'],
                    'description': row['description'],
                    'release_year': row['release_year'],
                    'media_type': row['media_type'],
                    'cover_url': row['cover_url'],
                    'genres': row['genres'] or [],
                    'genre_match_count': row['genre_match_count']
                })
            
            return media_list
            
        except Exception as e:
            logger.error(f"Error fetching media by genres: {str(e)}")
            return []

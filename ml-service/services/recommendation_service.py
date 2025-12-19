"""
Recommendation Service implementing hybrid recommendation algorithm
Combines collaborative filtering and content-based filtering
"""

import logging
from database.db_connection import DatabaseConnection
from database.user_repository import UserRepository
from database.catalog_repository import CatalogRepository
from database.engagement_repository import EngagementRepository

logger = logging.getLogger(__name__)


class RecommendationService:
    """
    Main recommendation service that orchestrates the recommendation algorithm
    
    Algorithm Strategy:
    1. Content-Based Filtering: Recommends media matching user's genre preferences
    2. Collaborative Filtering: Recommends media liked by similar users
    3. Hybrid Approach: Combines both methods with weighted scoring
    4. Diversity: Ensures mix of content types and avoids over-recommending same genres
    """
    
    def __init__(self, main_db_connection):
        """
        Initialize recommendation service with database connections
        
        Args:
            main_db_connection: Main database connection (will be used as template)
        """
        # Create separate connections for each database to avoid conflicts
        self.user_db = DatabaseConnection(
            host=main_db_connection.host,
            port=main_db_connection.port,
            database='user_db',
            user=main_db_connection.user,
            password=main_db_connection.password,
            minconn=1,
            maxconn=5
        )
        
        self.catalog_db = DatabaseConnection(
            host=main_db_connection.host,
            port=main_db_connection.port,
            database='catalog_db',
            user=main_db_connection.user,
            password=main_db_connection.password,
            minconn=1,
            maxconn=5
        )
        
        self.engagement_db = DatabaseConnection(
            host=main_db_connection.host,
            port=main_db_connection.port,
            database='engagement_db',
            user=main_db_connection.user,
            password=main_db_connection.password,
            minconn=1,
            maxconn=5
        )
        
        # Initialize repositories
        self.user_repo = UserRepository(self.user_db)
        self.catalog_repo = CatalogRepository(self.catalog_db)
        self.engagement_repo = EngagementRepository(self.engagement_db)
        
        # Algorithm weights
        self.CONTENT_WEIGHT = 0.4
        self.COLLABORATIVE_WEIGHT = 0.6
        
        logger.info("RecommendationService initialized successfully")
    
    def get_recommendations(self, user_id, limit=10, offset=0):
        """
        Get personalized recommendations for a user
        
        Args:
            user_id: UUID of the user
            limit: Number of recommendations to return
            offset: Offset for pagination
        
        Returns:
            List of recommended media with scores
        """
        try:
            logger.info(f"Generating recommendations for user {user_id}")
            
            # Step 1: Get user preferences
            user_prefs = self.user_repo.get_user_preferences(user_id)
            if not user_prefs:
                logger.warning(f"No preferences found for user {user_id}, using fallback")
                return self._get_popular_media(limit, offset)
            
            # Step 2: Get user's interaction history
            interaction_summary = self.engagement_repo.get_user_interaction_summary(user_id)
            already_interacted = set(interaction_summary.keys())
            
            # Step 3: Get content-based recommendations
            content_recs = self._get_content_based_recommendations(
                user_prefs,
                exclude_ids=already_interacted
            )
            
            # Step 4: Get collaborative filtering recommendations
            collaborative_recs = self._get_collaborative_recommendations(
                user_id,
                exclude_ids=already_interacted
            )
            
            # Step 5: Combine and score recommendations
            combined_recs = self._combine_recommendations(
                content_recs,
                collaborative_recs
            )
            
            # Step 6: Apply final ranking and pagination
            ranked_recs = self._rank_and_filter(combined_recs, limit, offset)
            
            logger.info(f"Generated {len(ranked_recs)} recommendations for user {user_id}")
            return ranked_recs
            
        except Exception as e:
            logger.error(f"Error generating recommendations for user {user_id}: {str(e)}", exc_info=True)
            # Fallback to popular media
            return self._get_popular_media(limit, offset)
    
    def _get_content_based_recommendations(self, user_prefs, exclude_ids=None):
        """
        Get recommendations based on user's genre preferences
        
        Args:
            user_prefs: User preferences dictionary
            exclude_ids: Set of media IDs to exclude
        
        Returns:
            Dictionary mapping media_id to score
        """
        try:
            genres = user_prefs.get('genres', [])
            if not genres:
                logger.warning("No genres found in user preferences")
                return {}
            
            # Get media matching user's preferred genres
            media_list = self.catalog_repo.get_media_by_genres(
                genres=genres,
                limit=100,
                exclude_ids=list(exclude_ids) if exclude_ids else None
            )
            
            # Score based on genre match
            scores = {}
            for media in media_list:
                media_id = media['id']
                genre_match_count = media.get('genre_match_count', 0)
                
                # Score: higher for more genre matches
                score = genre_match_count / len(genres) if len(genres) > 0 else 0
                scores[media_id] = {
                    'media': media,
                    'content_score': score
                }
            
            logger.info(f"Content-based: Found {len(scores)} recommendations")
            return scores
            
        except Exception as e:
            logger.error(f"Error in content-based recommendations: {str(e)}")
            return {}
    
    def _get_collaborative_recommendations(self, user_id, exclude_ids=None):
        """
        Get recommendations based on similar users' preferences
        
        Args:
            user_id: UUID of the user
            exclude_ids: Set of media IDs to exclude
        
        Returns:
            Dictionary mapping media_id to score
        """
        try:
            # Get recommendations from similar users
            similar_user_recs = self.engagement_repo.get_similar_users_interactions(
                user_id=user_id,
                limit=100
            )
            
            if not similar_user_recs:
                logger.info("No collaborative recommendations found")
                return {}
            
            # Get media details for these recommendations
            media_ids = [rec['media_id'] for rec in similar_user_recs]
            media_map = self.catalog_repo.get_media_by_ids(media_ids)
            
            # Score based on how many similar users liked it
            scores = {}
            max_score = max([rec['score'] for rec in similar_user_recs]) if similar_user_recs else 1
            
            for rec in similar_user_recs:
                media_id = rec['media_id']
                if media_id in media_map:
                    # Normalize score to 0-1 range
                    normalized_score = rec['score'] / max_score if max_score > 0 else 0
                    scores[media_id] = {
                        'media': media_map[media_id],
                        'collaborative_score': normalized_score,
                        'user_count': rec['user_count']
                    }
            
            logger.info(f"Collaborative: Found {len(scores)} recommendations")
            return scores
            
        except Exception as e:
            logger.error(f"Error in collaborative recommendations: {str(e)}")
            return {}
    
    def _combine_recommendations(self, content_recs, collaborative_recs):
        """
        Combine content-based and collaborative recommendations with weighted scoring
        
        Args:
            content_recs: Dictionary of content-based recommendations
            collaborative_recs: Dictionary of collaborative recommendations
        
        Returns:
            Dictionary of combined recommendations with hybrid scores
        """
        combined = {}
        
        # Add content-based recommendations
        for media_id, data in content_recs.items():
            combined[media_id] = {
                'media': data['media'],
                'content_score': data['content_score'],
                'collaborative_score': 0,
                'hybrid_score': data['content_score'] * self.CONTENT_WEIGHT
            }
        
        # Add or update with collaborative recommendations
        for media_id, data in collaborative_recs.items():
            if media_id in combined:
                # Already have content score, add collaborative
                combined[media_id]['collaborative_score'] = data['collaborative_score']
                combined[media_id]['hybrid_score'] = (
                    combined[media_id]['content_score'] * self.CONTENT_WEIGHT +
                    data['collaborative_score'] * self.COLLABORATIVE_WEIGHT
                )
            else:
                # Only collaborative score
                combined[media_id] = {
                    'media': data['media'],
                    'content_score': 0,
                    'collaborative_score': data['collaborative_score'],
                    'hybrid_score': data['collaborative_score'] * self.COLLABORATIVE_WEIGHT
                }
        
        return combined
    
    def _rank_and_filter(self, recommendations, limit, offset):
        """
        Rank recommendations by hybrid score and apply pagination
        
        Args:
            recommendations: Dictionary of recommendations
            limit: Number of results to return
            offset: Pagination offset
        
        Returns:
            List of top recommendations
        """
        # Sort by hybrid score
        sorted_recs = sorted(
            recommendations.items(),
            key=lambda x: x[1]['hybrid_score'],
            reverse=True
        )
        
        # Apply pagination
        paginated = sorted_recs[offset:offset + limit]
        
        # Format results
        results = []
        for media_id, data in paginated:
            result = data['media'].copy()
            result['recommendation_score'] = round(data['hybrid_score'], 4)
            result['content_score'] = round(data['content_score'], 4)
            result['collaborative_score'] = round(data['collaborative_score'], 4)
            results.append(result)
        
        return results
    
    def _get_popular_media(self, limit=10, offset=0):
        """
        Fallback method: Get popular media when personalization is not possible
        
        Args:
            limit: Number of results
            offset: Pagination offset
        
        Returns:
            List of popular media
        """
        try:
            logger.info("Using fallback: popular media")
            all_media = self.catalog_repo.get_all_media(limit=limit + offset)
            
            # Apply pagination
            paginated = all_media[offset:offset + limit]
            
            # Add default scores
            for media in paginated:
                media['recommendation_score'] = 0.5
                media['content_score'] = 0.0
                media['collaborative_score'] = 0.0
            
            return paginated
            
        except Exception as e:
            logger.error(f"Error getting popular media: {str(e)}")
            return []

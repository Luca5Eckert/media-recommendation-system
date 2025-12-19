"""
ML Service for Media Recommendation System
Provides REST API for personalized media recommendations
"""

from flask import Flask, jsonify, request
from flask_cors import CORS
import logging
import os
from dotenv import load_dotenv

from services.recommendation_service import RecommendationService
from database.db_connection import DatabaseConnection

# Load environment variables
load_dotenv()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize Flask app
app = Flask(__name__)
CORS(app)

# Initialize database connection
db_connection = DatabaseConnection(
    host=os.getenv('DB_HOST', 'localhost'),
    port=int(os.getenv('DB_PORT', '5432')),
    database=os.getenv('DB_NAME', 'catalog_db'),
    user=os.getenv('DB_USER', 'user'),
    password=os.getenv('DB_PASSWORD', 'password')
)

# Initialize recommendation service
recommendation_service = RecommendationService(db_connection)


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'service': 'ml-service',
        'version': '1.0.0'
    }), 200


@app.route('/api/recommendations/<user_id>', methods=['GET'])
def get_recommendations(user_id):
    """
    Get personalized media recommendations for a user
    
    Args:
        user_id: UUID of the user
    
    Query Parameters:
        limit: Number of recommendations to return (default: 10)
        offset: Offset for pagination (default: 0)
    
    Returns:
        JSON response with recommended media list
    """
    try:
        # Get query parameters
        limit = request.args.get('limit', default=10, type=int)
        offset = request.args.get('offset', default=0, type=int)
        
        # Validate input
        if limit < 1 or limit > 100:
            return jsonify({
                'error': 'Invalid limit parameter. Must be between 1 and 100'
            }), 400
        
        if offset < 0:
            return jsonify({
                'error': 'Invalid offset parameter. Must be non-negative'
            }), 400
        
        # Get recommendations
        logger.info(f"Getting recommendations for user {user_id} (limit={limit}, offset={offset})")
        recommendations = recommendation_service.get_recommendations(
            user_id=user_id,
            limit=limit,
            offset=offset
        )
        
        return jsonify({
            'user_id': user_id,
            'recommendations': recommendations,
            'count': len(recommendations),
            'limit': limit,
            'offset': offset
        }), 200
        
    except ValueError as e:
        logger.error(f"Invalid user_id format: {user_id}")
        return jsonify({
            'error': f'Invalid user_id format: {str(e)}'
        }), 400
    except Exception as e:
        logger.error(f"Error getting recommendations for user {user_id}: {str(e)}", exc_info=True)
        return jsonify({
            'error': 'Internal server error',
            'message': str(e)
        }), 500


@app.route('/api/recommendations/batch', methods=['POST'])
def get_batch_recommendations():
    """
    Get recommendations for multiple users in a single request
    
    Request Body:
        {
            "user_ids": ["uuid1", "uuid2", ...],
            "limit": 10
        }
    
    Returns:
        JSON response with recommendations for each user
    """
    try:
        data = request.get_json()
        
        if not data or 'user_ids' not in data:
            return jsonify({
                'error': 'Missing user_ids in request body'
            }), 400
        
        user_ids = data['user_ids']
        limit = data.get('limit', 10)
        
        if not isinstance(user_ids, list) or len(user_ids) == 0:
            return jsonify({
                'error': 'user_ids must be a non-empty list'
            }), 400
        
        if len(user_ids) > 50:
            return jsonify({
                'error': 'Maximum 50 users per batch request'
            }), 400
        
        logger.info(f"Getting batch recommendations for {len(user_ids)} users")
        
        results = {}
        for user_id in user_ids:
            try:
                recommendations = recommendation_service.get_recommendations(
                    user_id=user_id,
                    limit=limit
                )
                results[user_id] = {
                    'recommendations': recommendations,
                    'count': len(recommendations)
                }
            except Exception as e:
                logger.error(f"Error getting recommendations for user {user_id}: {str(e)}")
                results[user_id] = {
                    'error': str(e),
                    'recommendations': [],
                    'count': 0
                }
        
        return jsonify({
            'results': results,
            'total_users': len(user_ids)
        }), 200
        
    except Exception as e:
        logger.error(f"Error in batch recommendations: {str(e)}", exc_info=True)
        return jsonify({
            'error': 'Internal server error',
            'message': str(e)
        }), 500


@app.errorhandler(404)
def not_found(error):
    """Handle 404 errors"""
    return jsonify({
        'error': 'Endpoint not found'
    }), 404


@app.errorhandler(500)
def internal_error(error):
    """Handle 500 errors"""
    logger.error(f"Internal server error: {str(error)}", exc_info=True)
    return jsonify({
        'error': 'Internal server error'
    }), 500


if __name__ == '__main__':
    port = int(os.getenv('PORT', '5000'))
    debug = os.getenv('DEBUG', 'False').lower() == 'true'
    
    logger.info(f"Starting ML Service on port {port}")
    app.run(host='0.0.0.0', port=port, debug=debug)

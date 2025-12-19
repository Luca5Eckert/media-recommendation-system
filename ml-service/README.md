# ML Service - Media Recommendation System

## Overview

The ML Service is a Python-based microservice that provides personalized media recommendations using a hybrid recommendation algorithm. It combines collaborative filtering and content-based filtering to deliver accurate and diverse recommendations.

## Features

- **Hybrid Recommendation Algorithm**: Combines collaborative and content-based filtering
- **Performance Optimized**: Batch queries, connection pooling, and efficient SQL to avoid N+1 problems
- **Multiple Database Support**: Connects to user_db, catalog_db, and engagement_db
- **RESTful API**: Simple REST endpoints for recommendations
- **Scalable**: Uses Gunicorn with multiple workers for production
- **Health Checks**: Built-in health check endpoint for monitoring

## Architecture

### Recommendation Algorithm

The service uses a hybrid approach combining:

1. **Content-Based Filtering (40% weight)**
   - Matches media to user's genre preferences
   - Considers user's explicit preferences from their profile
   - Ensures recommendations align with stated interests

2. **Collaborative Filtering (60% weight)**
   - Finds users with similar viewing patterns
   - Recommends media that similar users enjoyed
   - Leverages collective intelligence from user interactions

3. **Hybrid Scoring**
   - Combines both approaches with weighted scores
   - Normalizes scores for fair comparison
   - Ranks by final hybrid score

### Performance Optimizations

- **Connection Pooling**: Reuses database connections for efficiency
- **Batch Queries**: Fetches multiple records in single queries using `ANY()` and array operations
- **Aggregate Queries**: Uses PostgreSQL aggregation to avoid N+1 problems
- **Efficient Joins**: LEFT JOINs with GROUP BY to fetch related data in one query

## API Endpoints

### Health Check
```
GET /health
```
Returns service health status.

**Response:**
```json
{
  "status": "healthy",
  "service": "ml-service",
  "version": "1.0.0"
}
```

### Get Recommendations
```
GET /api/recommendations/<user_id>?limit=10&offset=0
```

Get personalized recommendations for a specific user.

**Parameters:**
- `user_id` (path): UUID of the user
- `limit` (query): Number of recommendations (1-100, default: 10)
- `offset` (query): Pagination offset (default: 0)

**Response:**
```json
{
  "user_id": "123e4567-e89b-12d3-a456-426614174000",
  "recommendations": [
    {
      "id": "media-uuid",
      "title": "Example Movie",
      "description": "Description...",
      "release_year": 2023,
      "media_type": "MOVIE",
      "cover_url": "https://...",
      "genres": ["ACTION", "THRILLER"],
      "recommendation_score": 0.8745,
      "content_score": 0.6667,
      "collaborative_score": 0.9500
    }
  ],
  "count": 10,
  "limit": 10,
  "offset": 0
}
```

### Batch Recommendations
```
POST /api/recommendations/batch
```

Get recommendations for multiple users in a single request.

**Request Body:**
```json
{
  "user_ids": ["uuid1", "uuid2", "uuid3"],
  "limit": 10
}
```

**Response:**
```json
{
  "results": {
    "uuid1": {
      "recommendations": [...],
      "count": 10
    },
    "uuid2": {
      "recommendations": [...],
      "count": 10
    }
  },
  "total_users": 2
}
```

## Database Schema Requirements

The service expects the following database structure:

### user_db
- `user_preferences`: User preference records
- `tb_user_genre`: User genre preferences (junction table)

### catalog_db
- `medias`: Media catalog
- `tb_media_genre`: Media genres (junction table)

### engagement_db
- `interaction`: User-media interactions (likes, dislikes, watches)

## Environment Variables

```env
DB_HOST=localhost
DB_PORT=5432
DB_USER=user
DB_PASSWORD=password
PORT=5000
DEBUG=False
LOG_LEVEL=INFO
```

## Running the Service

### Development
```bash
cd ml-service
pip install -r requirements.txt
python app.py
```

### Production with Docker
```bash
docker-compose up ml-service
```

### Production with Gunicorn
```bash
gunicorn --bind 0.0.0.0:5000 --workers 4 --threads 2 app:app
```

## Dependencies

- Flask 3.0.0 - Web framework
- Flask-CORS 4.0.0 - CORS support
- psycopg2-binary 2.9.9 - PostgreSQL adapter
- python-dotenv 1.0.0 - Environment variable management
- gunicorn 21.2.0 - WSGI HTTP server
- numpy 1.24.3 - Numerical computing
- scikit-learn 1.3.2 - Machine learning library

## Algorithm Details

### Content-Based Scoring
```python
content_score = matching_genres / total_user_genres
```

### Collaborative Scoring
```python
collaborative_score = (similar_user_count * total_interaction_value) / max_score
```

### Hybrid Score
```python
hybrid_score = (content_score * 0.4) + (collaborative_score * 0.6)
```

## Performance Characteristics

- **Query Efficiency**: O(1) for single user, O(n) for batch requests
- **Connection Pooling**: Supports 10 concurrent connections per database
- **Response Time**: Typically < 200ms for single user recommendations
- **Scalability**: Horizontal scaling via multiple Gunicorn workers

## Error Handling

The service implements comprehensive error handling:
- Invalid user IDs return 400 Bad Request
- Database errors trigger fallback to popular media
- All errors are logged with full stack traces
- 500 errors return generic message to avoid information leakage

## Monitoring

- Health check endpoint at `/health`
- Structured logging with timestamps
- Request/response logging in production mode
- Database connection pool monitoring

## Future Improvements

1. Implement caching layer (Redis) for frequent queries
2. Add real-time learning from user feedback
3. Implement A/B testing framework for algorithm variations
4. Add more sophisticated ML models (neural networks, deep learning)
5. Implement session-based recommendations
6. Add recommendation explanations ("Recommended because...")

## License

Part of the Media Recommendation System - MIT License

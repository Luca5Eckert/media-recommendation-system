# ML Service Integration Guide

## Quick Start

The ML Service is now integrated into the Media Recommendation System. It provides intelligent, personalized media recommendations through a RESTful API.

## Starting the Service

### With Docker Compose (Recommended)

```bash
# Start all services including ML service
docker-compose up -d

# Check ML service health
curl http://localhost:5000/health

# View ML service logs
docker logs ml-service -f
```

### Development Mode (Local)

```bash
cd ml-service

# Install dependencies
pip install -r requirements.txt

# Set environment variables
export DB_HOST=localhost
export DB_PORT=5432
export DB_USER=user
export DB_PASSWORD=password
export PORT=5000

# Run the service
python app.py
```

## API Usage Examples

### 1. Health Check

```bash
curl http://localhost:5000/health
```

**Response:**
```json
{
  "status": "healthy",
  "service": "ml-service",
  "version": "1.0.0"
}
```

### 2. Get Recommendations for a User

```bash
# Replace USER_UUID with actual user ID from your database
curl "http://localhost:5000/api/recommendations/USER_UUID?limit=10"
```

**Example Response:**
```json
{
  "user_id": "123e4567-e89b-12d3-a456-426614174000",
  "recommendations": [
    {
      "id": "media-uuid-1",
      "title": "Inception",
      "description": "A mind-bending thriller...",
      "release_year": 2010,
      "media_type": "MOVIE",
      "cover_url": "https://example.com/cover.jpg",
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

### 3. Batch Recommendations

```bash
curl -X POST http://localhost:5000/api/recommendations/batch \
  -H "Content-Type: application/json" \
  -d '{
    "user_ids": ["uuid1", "uuid2", "uuid3"],
    "limit": 5
  }'
```

### 4. Python Client Example

```python
import requests

# Get recommendations
response = requests.get(
    'http://localhost:5000/api/recommendations/USER_UUID',
    params={'limit': 10}
)

if response.status_code == 200:
    data = response.json()
    for rec in data['recommendations']:
        print(f"{rec['title']} - Score: {rec['recommendation_score']}")
```

### 5. JavaScript/Node.js Client Example

```javascript
// Using fetch API
const userId = 'USER_UUID';
const response = await fetch(`http://localhost:5000/api/recommendations/${userId}?limit=10`);
const data = await response.json();

console.log('Recommendations:', data.recommendations);
```

## Integration with Other Services

### From User Service

```java
// Example: Call ML service from Java Spring Boot
RestTemplate restTemplate = new RestTemplate();
String mlServiceUrl = "http://ml-service:5000/api/recommendations/" + userId;
ResponseEntity<RecommendationResponse> response = restTemplate.getForEntity(
    mlServiceUrl + "?limit=10",
    RecommendationResponse.class
);
```

### From Frontend

```typescript
// React/TypeScript example
const getRecommendations = async (userId: string, limit: number = 10) => {
  try {
    const response = await fetch(
      `http://localhost:5000/api/recommendations/${userId}?limit=${limit}`
    );
    const data = await response.json();
    return data.recommendations;
  } catch (error) {
    console.error('Error fetching recommendations:', error);
    return [];
  }
};
```

## Testing the Service

### 1. Get User IDs from Database

```bash
# Connect to PostgreSQL
docker exec -it media-db psql -U user -d user_db

# Query for user IDs
SELECT id, name, email FROM tb_users LIMIT 5;
```

### 2. Run Manual Tests

```bash
cd ml-service

# Run structure validation
python3 test_structure.py

# Run API tests (requires service to be running)
python3 test_api.py
```

### 3. Test with curl

```bash
# Replace with actual user ID
USER_ID="paste-uuid-here"

# Get recommendations
curl "http://localhost:5000/api/recommendations/${USER_ID}?limit=5"
```

## Performance Characteristics

- **Response Time**: Typically < 200ms for single user recommendations
- **Throughput**: Supports multiple concurrent requests via Gunicorn workers
- **Database Connections**: Connection pooling with 10 connections per database
- **Scalability**: Horizontally scalable by running multiple containers

## Troubleshooting

### Service Won't Start

```bash
# Check logs
docker logs ml-service

# Verify database is running
docker ps | grep media-db

# Test database connectivity
docker exec -it media-db psql -U user -d catalog_db -c "SELECT 1;"
```

### No Recommendations Returned

**Possible causes:**
1. User has no preferences set
2. No media in catalog
3. No user interactions recorded

**Solution:**
- Check if user preferences exist: `SELECT * FROM user_preferences WHERE user_id = 'UUID';`
- Check if media exists: `SELECT COUNT(*) FROM medias WHERE deleted_at IS NULL;`
- Check interactions: `SELECT COUNT(*) FROM interaction WHERE user_id = 'UUID';`

### Database Connection Errors

```bash
# Verify environment variables
docker exec ml-service env | grep DB_

# Test connection from ML service container
docker exec ml-service python -c "import psycopg2; conn = psycopg2.connect(host='postgres', port=5432, user='user', password='password', database='catalog_db'); print('Connected!')"
```

## Monitoring

### Health Checks

```bash
# Check if service is healthy
curl http://localhost:5000/health

# Watch logs in real-time
docker logs ml-service -f --tail 100
```

### Performance Metrics

Monitor these logs:
- Request processing time
- Database query execution time
- Recommendation count per request
- Error rates

## Security Considerations

1. **Database Credentials**: Store in environment variables, never in code
2. **API Rate Limiting**: Consider adding rate limiting for production
3. **Input Validation**: All inputs are validated before processing
4. **Error Handling**: Errors return generic messages to avoid information leakage

## Production Deployment

### Environment Variables

```env
DB_HOST=production-db-host
DB_PORT=5432
DB_USER=ml_service_user
DB_PASSWORD=strong-password-here
PORT=5000
DEBUG=False
LOG_LEVEL=INFO
```

### Scaling

```bash
# Scale ML service to 3 replicas
docker-compose up -d --scale ml-service=3
```

### Load Balancing

Configure nginx or another load balancer to distribute requests:

```nginx
upstream ml_service {
    server ml-service-1:5000;
    server ml-service-2:5000;
    server ml-service-3:5000;
}

server {
    location /api/recommendations {
        proxy_pass http://ml_service;
    }
}
```

## Next Steps

1. ✅ ML Service is implemented and ready to use
2. Add authentication/authorization for API endpoints
3. Implement caching layer (Redis) for frequently requested recommendations
4. Add A/B testing framework for algorithm improvements
5. Implement real-time model training based on user feedback
6. Add recommendation explanations ("Recommended because...")

## Support

For issues or questions:
1. Check the [ML Service README](ml-service/README.md)
2. Review logs: `docker logs ml-service`
3. Open an issue on GitHub

## Algorithm Details

The service uses a **hybrid recommendation algorithm**:

- **40% Content-Based**: Matches media to user's genre preferences
- **60% Collaborative Filtering**: Recommends based on similar users' behavior

### Scoring Formula

```
content_score = matching_genres / total_user_genres
collaborative_score = (similar_users * interaction_value) / max_score
hybrid_score = (content_score × 0.4) + (collaborative_score × 0.6)
```

### Performance Optimizations

1. **Connection Pooling**: Reuses database connections
2. **Batch Queries**: Uses PostgreSQL `ANY()` for multiple IDs
3. **Aggregate Queries**: Single query with GROUP BY instead of N+1
4. **Efficient Joins**: LEFT JOIN with aggregation for related data

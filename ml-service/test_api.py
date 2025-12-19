#!/usr/bin/env python3
"""
Manual API test examples for ML Service
This script shows how to call the ML Service API endpoints
"""

import requests
import json
import sys


def test_health_check(base_url):
    """Test the health check endpoint"""
    print("\n" + "="*60)
    print("Testing Health Check Endpoint")
    print("="*60)
    
    try:
        response = requests.get(f"{base_url}/health", timeout=5)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")
        return response.status_code == 200
    except Exception as e:
        print(f"Error: {e}")
        return False


def test_single_recommendation(base_url, user_id, limit=10):
    """Test getting recommendations for a single user"""
    print("\n" + "="*60)
    print(f"Testing Single User Recommendations")
    print(f"User ID: {user_id}")
    print("="*60)
    
    try:
        response = requests.get(
            f"{base_url}/api/recommendations/{user_id}",
            params={'limit': limit},
            timeout=10
        )
        print(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print(f"\nUser ID: {data.get('user_id')}")
            print(f"Count: {data.get('count')}")
            print(f"Limit: {data.get('limit')}")
            print(f"\nRecommendations:")
            
            for i, rec in enumerate(data.get('recommendations', [])[:3], 1):
                print(f"\n{i}. {rec.get('title')} ({rec.get('release_year')})")
                print(f"   Type: {rec.get('media_type')}")
                print(f"   Genres: {', '.join(rec.get('genres', []))}")
                print(f"   Score: {rec.get('recommendation_score')}")
                print(f"   - Content: {rec.get('content_score')}")
                print(f"   - Collaborative: {rec.get('collaborative_score')}")
            
            if len(data.get('recommendations', [])) > 3:
                print(f"\n   ... and {len(data.get('recommendations', [])) - 3} more")
            
            return True
        else:
            print(f"Error Response: {response.text}")
            return False
            
    except Exception as e:
        print(f"Error: {e}")
        return False


def test_batch_recommendations(base_url, user_ids, limit=5):
    """Test getting recommendations for multiple users"""
    print("\n" + "="*60)
    print(f"Testing Batch Recommendations")
    print(f"User IDs: {user_ids}")
    print("="*60)
    
    try:
        response = requests.post(
            f"{base_url}/api/recommendations/batch",
            json={'user_ids': user_ids, 'limit': limit},
            headers={'Content-Type': 'application/json'},
            timeout=15
        )
        print(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print(f"\nTotal Users: {data.get('total_users')}")
            
            for user_id, result in data.get('results', {}).items():
                print(f"\n--- User: {user_id} ---")
                if 'error' in result:
                    print(f"Error: {result['error']}")
                else:
                    print(f"Recommendations: {result.get('count')}")
                    if result.get('recommendations'):
                        first_rec = result['recommendations'][0]
                        print(f"First: {first_rec.get('title')} (score: {first_rec.get('recommendation_score')})")
            
            return True
        else:
            print(f"Error Response: {response.text}")
            return False
            
    except Exception as e:
        print(f"Error: {e}")
        return False


def main():
    """Run manual tests"""
    # Configuration
    base_url = "http://localhost:5000"
    
    # Example UUIDs - replace with actual UUIDs from your database
    example_user_id = "123e4567-e89b-12d3-a456-426614174000"
    example_user_ids = [
        "123e4567-e89b-12d3-a456-426614174000",
        "223e4567-e89b-12d3-a456-426614174001"
    ]
    
    print("="*60)
    print("ML Service - Manual API Tests")
    print("="*60)
    print(f"\nBase URL: {base_url}")
    print(f"\nNote: Replace example UUIDs with actual user IDs from your database")
    print("      To get user IDs, query the user_db: SELECT id FROM tb_users LIMIT 5;")
    
    # Run tests
    results = []
    
    # Test 1: Health check
    results.append(("Health Check", test_health_check(base_url)))
    
    # Test 2: Single recommendation (will fail with example UUID)
    print("\n[INFO] The following tests will fail if you use example UUIDs")
    print("[INFO] Replace with actual UUIDs from your database to test properly")
    results.append(("Single Recommendation", test_single_recommendation(base_url, example_user_id)))
    
    # Test 3: Batch recommendations (will fail with example UUIDs)
    results.append(("Batch Recommendations", test_batch_recommendations(base_url, example_user_ids)))
    
    # Summary
    print("\n" + "="*60)
    print("Test Summary")
    print("="*60)
    for name, result in results:
        status = "✓ PASS" if result else "✗ FAIL"
        print(f"{status} - {name}")
    
    passed = sum(1 for _, r in results if r)
    print(f"\nResults: {passed}/{len(results)} tests passed")
    
    return 0 if passed > 0 else 1


if __name__ == '__main__':
    sys.exit(main())

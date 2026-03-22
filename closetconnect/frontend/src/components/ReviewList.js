"use client";

import { useState, useEffect } from "react";

export default function ReviewList({ clothingId }) {
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [averageRating, setAverageRating] = useState(0);
  const [reviewCount, setReviewCount] = useState(0);

  useEffect(() => {
    fetchReviews();
    fetchRatingStats();
  }, [clothingId]);

  const fetchReviews = async () => {
    try {
      console.log(`Fetching reviews for clothing ID: ${clothingId}`);
      const response = await fetch(`http://localhost:8080/api/clothing-reviews/clothing/${clothingId}`);
      console.log(`Response status: ${response.status}`);
      
      if (response.ok) {
        const data = await response.json();
        console.log('Reviews data:', data);
        setReviews(data);
        setError(null); // Clear any previous errors
      } else {
        const errorText = await response.text();
        console.error(`Failed to fetch reviews: ${response.status} - ${errorText}`);
        setError(`Failed to fetch reviews: ${response.status} ${response.statusText}`);
      }
    } catch (err) {
      console.error('Network error fetching reviews:', err);
      setError(`Network error: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const fetchRatingStats = async () => {
    try {
      console.log(`Fetching rating stats for clothing ID: ${clothingId}`);
      const [avgResponse, countResponse] = await Promise.all([
        fetch(`http://localhost:8080/api/clothing-reviews/clothing/${clothingId}/average-rating`),
        fetch(`http://localhost:8080/api/clothing-reviews/clothing/${clothingId}/count`)
      ]);

      console.log(`Average rating response: ${avgResponse.status}`);
      console.log(`Count response: ${countResponse.status}`);

      if (avgResponse.ok) {
        const avg = await avgResponse.json();
        console.log('Average rating:', avg);
        setAverageRating(avg);
      } else {
        console.error(`Failed to fetch average rating: ${avgResponse.status}`);
      }

      if (countResponse.ok) {
        const count = await countResponse.json();
        console.log('Review count:', count);
        setReviewCount(count);
      } else {
        console.error(`Failed to fetch review count: ${countResponse.status}`);
      }
    } catch (err) {
      console.error("Failed to fetch rating stats:", err);
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const renderStars = (rating) => {
    return (
      <div className="flex space-x-1">
        {[1, 2, 3, 4, 5].map((star) => (
          <span
            key={star}
            className={`text-lg ${
              star <= rating ? "text-yellow-400" : "text-gray-300"
            }`}
          >
            ★
          </span>
        ))}
      </div>
    );
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="animate-pulse">
          <div className="h-4 bg-gray-200 rounded w-1/4 mb-4"></div>
          <div className="space-y-3">
            <div className="h-3 bg-gray-200 rounded"></div>
            <div className="h-3 bg-gray-200 rounded w-5/6"></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-4">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-rose-900">Reviews</h3>
        <div className="flex items-center space-x-2">
          {renderStars(Math.round(averageRating))}
          <span className="text-sm text-gray-600">
            {averageRating.toFixed(1)} ({reviewCount} review{reviewCount !== 1 ? 's' : ''})
          </span>
        </div>
      </div>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

      {reviews.length === 0 ? (
        <div className="text-center py-8">
          <div className="text-4xl mb-2">⭐</div>
          <p className="text-gray-600">No reviews yet</p>
          <p className="text-sm text-gray-500">Be the first to review this item!</p>
        </div>
      ) : (
        <div className="space-y-3">
          {reviews.map((review) => (
            <div key={review.reviewId} className="border-b border-gray-200 pb-3 last:border-b-0">
              <div className="flex items-center justify-between mb-1">
                <div className="flex items-center space-x-2">
                  <span className="font-medium text-rose-900 text-sm">
                    {review.reviewer?.userName || 'Anonymous'}
                  </span>
                  {renderStars(review.review)}
                </div>
                <span className="text-xs text-gray-500">
                  {formatDate(review.createdAt)}
                </span>
              </div>
              {review.reviewText && (
                <p className="text-gray-700 text-sm leading-relaxed">
                  {review.reviewText}
                </p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

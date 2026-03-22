"use client";

import { useState } from "react";

export default function ReviewForm({ clothingId, onReviewSubmitted }) {
  const [rating, setRating] = useState(0);
  const [reviewText, setReviewText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);

    try {
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");

      if (!token || !userId) {
        throw new Error("You must be logged in to submit a review");
      }

      const response = await fetch("http://localhost:8080/api/clothing-reviews", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          clothingId: clothingId,
          reviewerId: parseInt(userId),
          rating: rating,
          reviewText: reviewText,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Failed to submit review");
      }

      // Reset form
      setRating(0);
      setReviewText("");
      
      // Notify parent component
      if (onReviewSubmitted) {
        onReviewSubmitted();
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-4">
      <h3 className="text-lg font-semibold text-rose-900 mb-3">Write a Review</h3>
      
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-3 py-2 rounded mb-3 text-sm">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-3">
        <div>
          <label className="block text-sm font-medium text-rose-700 mb-1">
            Rating *
          </label>
          <div className="flex space-x-1">
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                key={star}
                type="button"
                onClick={() => setRating(star)}
                className={`text-xl ${
                  star <= rating
                    ? "text-yellow-400"
                    : "text-gray-300 hover:text-yellow-400"
                } transition-colors`}
              >
                ★
              </button>
            ))}
          </div>
          {rating === 0 && (
            <p className="text-xs text-red-600 mt-1">Please select a rating</p>
          )}
        </div>

        <div>
          <textarea
            value={reviewText}
            onChange={(e) => setReviewText(e.target.value)}
            className="w-full px-3 py-2 border border-rose-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-transparent text-sm"
            rows="3"
            placeholder="Share your experience with this clothing item..."
          />
        </div>

        <div className="flex justify-end space-x-2">
          <button
            type="button"
            onClick={() => {
              setRating(0);
              setReviewText("");
              setError(null);
            }}
            className="px-3 py-1 text-sm text-rose-600 border border-rose-300 rounded hover:bg-rose-50 transition"
          >
            Clear
          </button>
          <button
            type="submit"
            disabled={isSubmitting || rating === 0}
            className="px-3 py-1 text-sm bg-rose-500 text-white rounded hover:bg-rose-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? "Submitting..." : "Submit"}
          </button>
        </div>
      </form>
    </div>
  );
}

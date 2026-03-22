"use client";

import { useState, useEffect } from "react";

export default function ReturnReviewModal({ 
  isOpen, 
  onClose, 
  rentalId, 
  clothingId, 
  ownerId,
  onReviewComplete 
}) {
  const [showReviews, setShowReviews] = useState(false);
  const [itemRating, setItemRating] = useState(0);
  const [itemReviewText, setItemReviewText] = useState("");
  const [ownerRating, setOwnerRating] = useState(0);
  const [ownerReviewText, setOwnerReviewText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [clothingCard, setClothingCard] = useState(null);
  const [owner, setOwner] = useState(null);
  const [loadingDetails, setLoadingDetails] = useState(false);

  // Reset state when modal closes
  useEffect(() => {
    if (!isOpen) {
      setShowReviews(false);
      setItemRating(0);
      setItemReviewText("");
      setOwnerRating(0);
      setOwnerReviewText("");
      setError(null);
      setClothingCard(null);
      setOwner(null);
      setLoadingDetails(false);
    }
  }, [isOpen]);

  // Fetch clothing card and owner details when modal opens and IDs are available
  useEffect(() => {
    if (isOpen && (clothingId || ownerId)) {
      fetchDetails();
    }
  }, [isOpen, clothingId, ownerId]);

  const fetchDetails = async () => {
    setLoadingDetails(true);
    
    // Fetch clothing card first
    if (clothingId && !clothingCard) {
      try {
        const response = await fetch(`http://localhost:8080/api/clothing-cards/${clothingId}`);
        if (response.ok) {
          const card = await response.json();
          setClothingCard(card);
          // If ownerId not provided, get it from clothing card
          const actualOwnerId = ownerId || card.owner?.userId;
          if (actualOwnerId && !owner) {
            await fetchOwner(actualOwnerId);
          }
        }
      } catch (err) {
        console.error("Error fetching clothing card:", err);
      }
    }
    
    // Fetch owner if ownerId is provided and we don't have owner yet
    if (ownerId && !owner) {
      await fetchOwner(ownerId);
    }
    
    setLoadingDetails(false);
  };

  const fetchOwner = async (id) => {
    try {
      const token = localStorage.getItem("token");
      const headers = {};
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
      const response = await fetch(`http://localhost:8080/api/profile/${id}`, {
        headers
      });
      if (response.ok) {
        const user = await response.json();
        setOwner(user);
      }
    } catch (err) {
      console.error("Error fetching owner:", err);
    }
  };

  const handleSkip = () => {
    onClose();
    if (onReviewComplete) {
      onReviewComplete();
    }
  };

  const handleSubmitReviews = async () => {
    setIsSubmitting(true);
    setError(null);

    try {
      const token = localStorage.getItem("token");
      const userId = parseInt(localStorage.getItem("userId"));

      if (!token || !userId) {
        throw new Error("You must be logged in to submit reviews");
      }

      const errors = [];

      // Submit item review if rating is provided (text is optional)
      if (itemRating > 0) {
        try {
          const reviewPayload = {
            clothingId: clothingId,
            reviewerId: userId,
            rating: itemRating,
            reviewText: itemReviewText.trim() || null,
          };
          
          console.log("Submitting item review:", reviewPayload);
          
          const response = await fetch("http://localhost:8080/api/clothing-reviews", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(reviewPayload),
          });

          if (!response.ok) {
            const errorData = await response.json().catch(() => ({ error: "Failed to submit item review" }));
            const errorMessage = errorData.error || `HTTP ${response.status}: Failed to submit item review`;
            console.error("Item review error:", errorMessage, errorData);
            errors.push(`Item review: ${errorMessage}`);
          } else {
            const reviewData = await response.json();
            console.log("Item review submitted successfully:", reviewData);
          }
        } catch (err) {
          console.error("Item review exception:", err);
          errors.push(`Item review: ${err.message}`);
        }
      }

      // Submit owner review if rating is provided (text is optional)
      if (ownerRating > 0 && rentalId && ownerId) {
        try {
          const reviewPayload = {
            rentalId: rentalId,
            reviewedUserId: ownerId,
            reviewerId: userId,
            rating: ownerRating,
            reviewText: ownerReviewText.trim() || null,
          };
          
          console.log("Submitting owner review:", reviewPayload);
          
          const response = await fetch("http://localhost:8080/api/user-reviews", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(reviewPayload),
          });

          if (!response.ok) {
            const errorData = await response.json().catch(() => ({ error: "Failed to submit owner review" }));
            const errorMessage = errorData.error || `HTTP ${response.status}: Failed to submit owner review`;
            console.error("Owner review error:", errorMessage, errorData);
            errors.push(`Owner review: ${errorMessage}`);
          } else {
            const reviewData = await response.json();
            console.log("Owner review submitted successfully:", reviewData);
          }
        } catch (err) {
          console.error("Owner review exception:", err);
          errors.push(`Owner review: ${err.message}`);
        }
      } else {
        console.log("Skipping owner review - missing data:", { ownerRating, rentalId, ownerId });
      }

      // If there are errors, show them but still close modal if at least one review succeeded
      if (errors.length > 0) {
        // If both reviews were attempted and both failed, show error
        const attemptedReviews = (itemRating > 0 ? 1 : 0) + 
                                (ownerRating > 0 && rentalId && ownerId ? 1 : 0);
        if (errors.length === attemptedReviews && attemptedReviews > 0) {
          // Show error with full details
          const errorMessage = errors.join("; ");
          console.error("All reviews failed:", errorMessage);
          setError(errorMessage);
          setIsSubmitting(false);
          return;
        } else {
          // At least one succeeded, show warning but proceed
          console.warn("Some reviews failed:", errors.join("; "));
          // Still show error so user knows
          if (errors.length > 0) {
            alert(`Warning: Some reviews failed to submit:\n${errors.join("\n")}`);
          }
        }
      }

      // Close modal and refresh
      onClose();
      if (onReviewComplete) {
        onReviewComplete();
      }
    } catch (err) {
      setError(err.message || "Failed to submit reviews");
      setIsSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          {/* Header */}
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-2xl font-semibold text-rose-900">
              {showReviews ? "Leave Reviews" : "Item Returned Successfully!"}
            </h2>
            <button
              onClick={handleSkip}
              className="text-gray-400 hover:text-gray-600 transition"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {!showReviews ? (
            /* Initial Success Message */
            <div className="text-center py-8">
              <div className="text-6xl mb-4">✅</div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">
                Item Returned Successfully
              </h3>
              <p className="text-gray-600 mb-6">
                Would you like to leave a review for this item and the owner?
              </p>
              <div className="flex gap-3 justify-center">
                <button
                  onClick={handleSkip}
                  className="px-6 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition font-medium"
                >
                  Skip Reviews
                </button>
                <button
                  onClick={() => setShowReviews(true)}
                  className="px-6 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition font-medium"
                >
                  Leave Reviews
                </button>
              </div>
            </div>
          ) : (
            /* Review Forms */
            <div className="space-y-6">
              {error && (
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
                  {error}
                </div>
              )}

              {/* Item Review */}
              <div className="border border-gray-200 rounded-lg p-4">
                <div className="mb-3">
                  <h3 className="text-lg font-semibold text-gray-900">
                    Review the Item
                  </h3>
                  {loadingDetails ? (
                    <p className="text-sm text-gray-500 mt-1">Loading item details...</p>
                  ) : clothingCard ? (
                    <p className="text-sm text-gray-600 mt-1">
                      Reviewing: <span className="font-medium text-gray-900">{clothingCard.title}</span>
                    </p>
                  ) : (
                    <p className="text-sm text-gray-500 mt-1">Item #{clothingId || "N/A"}</p>
                  )}
                </div>
                <div className="mb-3">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Rating *
                  </label>
                  <div className="flex space-x-1">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <button
                        key={star}
                        type="button"
                        onClick={() => setItemRating(star)}
                        className={`text-2xl ${
                          star <= itemRating
                            ? "text-yellow-400"
                            : "text-gray-300 hover:text-yellow-400"
                        } transition-colors`}
                      >
                        ★
                      </button>
                    ))}
                  </div>
                </div>
                <textarea
                  value={itemReviewText}
                  onChange={(e) => setItemReviewText(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-transparent"
                  rows="3"
                  placeholder="Share your experience with this item... (optional)"
                />
                <p className="text-xs text-gray-500 mt-1">Review text is optional, but rating is required</p>
              </div>

              {/* Owner Review */}
              <div className="border border-gray-200 rounded-lg p-4">
                <div className="mb-3">
                  <h3 className="text-lg font-semibold text-gray-900">
                    Review the Owner
                  </h3>
                  {loadingDetails ? (
                    <p className="text-sm text-gray-500 mt-1">Loading owner details...</p>
                  ) : owner ? (
                    <p className="text-sm text-gray-600 mt-1">
                      Reviewing: <span className="font-medium text-gray-900">{owner.userName}</span>
                    </p>
                  ) : clothingCard?.owner?.userName ? (
                    <p className="text-sm text-gray-600 mt-1">
                      Reviewing: <span className="font-medium text-gray-900">{clothingCard.owner.userName}</span>
                    </p>
                  ) : (
                    <p className="text-sm text-gray-500 mt-1">Loading owner information...</p>
                  )}
                </div>
                <div className="mb-3">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Rating *
                  </label>
                  <div className="flex space-x-1">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <button
                        key={star}
                        type="button"
                        onClick={() => setOwnerRating(star)}
                        className={`text-2xl ${
                          star <= ownerRating
                            ? "text-yellow-400"
                            : "text-gray-300 hover:text-yellow-400"
                        } transition-colors`}
                      >
                        ★
                      </button>
                    ))}
                  </div>
                </div>
                <textarea
                  value={ownerReviewText}
                  onChange={(e) => setOwnerReviewText(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-transparent"
                  rows="3"
                  placeholder="Share your experience with the owner... (optional)"
                />
                <p className="text-xs text-gray-500 mt-1">Review text is optional, but rating is required</p>
              </div>

              {/* Action Buttons */}
              <div className="flex gap-3 justify-end">
                <button
                  onClick={handleSkip}
                  disabled={isSubmitting}
                  className="px-6 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition font-medium disabled:opacity-50"
                >
                  Skip
                </button>
                <button
                  onClick={handleSubmitReviews}
                  disabled={isSubmitting || (itemRating === 0 && ownerRating === 0)}
                  className="px-6 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isSubmitting ? "Submitting..." : "Submit Reviews"}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}


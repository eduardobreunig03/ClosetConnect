"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Header from "../../../components/Header";
import Link from "next/link";

export default function RentalDetailPage() {
  const params = useParams();
  const router = useRouter();
  const rentalId = params.id;
  
  const [loading, setLoading] = useState(true);
  const [rental, setRental] = useState(null);
  const [clothingCard, setClothingCard] = useState(null);
  const [error, setError] = useState(null);
  const [currentUserId, setCurrentUserId] = useState(null);
  const [existingOwnerReview, setExistingOwnerReview] = useState(null);
  const [existingItemReview, setExistingItemReview] = useState(null);
  const [existingRenterReview, setExistingRenterReview] = useState(null);
  const [loadingReviews, setLoadingReviews] = useState(true);
  const [ownerRating, setOwnerRating] = useState(0);
  const [ownerReviewText, setOwnerReviewText] = useState("");
  const [itemRating, setItemRating] = useState(0);
  const [itemReviewText, setItemReviewText] = useState("");
  const [renterRating, setRenterRating] = useState(0);
  const [renterReviewText, setRenterReviewText] = useState("");
  const [submittingOwnerReview, setSubmittingOwnerReview] = useState(false);
  const [submittingItemReview, setSubmittingItemReview] = useState(false);
  const [submittingRenterReview, setSubmittingRenterReview] = useState(false);

  useEffect(() => {
    const savedUserId = localStorage.getItem("userId");
    setCurrentUserId(savedUserId ? parseInt(savedUserId) : null);
    
    if (rentalId) {
      fetchRentalDetails();
    }
  }, [rentalId]);

  const fetchRentalDetails = async () => {
    try {
      setLoading(true);
      const response = await fetch(`http://localhost:8080/api/rentals/${rentalId}`);
      
      if (!response.ok) {
        if (response.status === 404) {
          setError("Rental transaction not found");
        } else {
          setError("Failed to load rental details");
        }
        return;
      }
      
      const rentalData = await response.json();
      setRental(rentalData);
      
      // Fetch clothing card details if not fully loaded
      const clothingId = rentalData.clothingCard?.clothingId || 
                        (typeof rentalData.clothingCard === 'number' ? rentalData.clothingCard : null);
      
      // If clothingCard is an object with full details, use it directly
      if (rentalData.clothingCard && rentalData.clothingCard.clothingId && rentalData.clothingCard.title) {
        setClothingCard(rentalData.clothingCard);
      } else if (clothingId) {
        // Otherwise fetch the full details
        const clothingResponse = await fetch(`http://localhost:8080/api/clothing-cards/${clothingId}`);
        if (clothingResponse.ok) {
          const clothingData = await clothingResponse.json();
          setClothingCard(clothingData);
        }
      }
    } catch (err) {
      console.error("Error fetching rental details:", err);
      setError("Failed to load rental details");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (rental && currentUserId && rental.returned) {
      fetchExistingReviews();
    } else {
      setLoadingReviews(false);
    }
  }, [rental, currentUserId]);

  const fetchExistingReviews = async () => {
    if (!rental || !currentUserId) return;
    
    try {
      setLoadingReviews(true);
      
      const isRenterCheck = rental.renter?.userId === currentUserId;
      
      // Fetch user reviews for this rental
      const userReviewsResponse = await fetch(`http://localhost:8080/api/user-reviews/rental/${rental.rentalId}`);
      if (userReviewsResponse.ok) {
        const userReviews = await userReviewsResponse.json();
        
        if (isRenterCheck) {
          // If user is renter, find their review of the owner
          const ownerReview = userReviews.find(r => r.reviewer?.userId === currentUserId && r.reviewedUser?.userId === rental.clothingCard?.owner?.userId);
          if (ownerReview) {
            setExistingOwnerReview(ownerReview);
          }
        } else {
          // If user is owner, find their review of the renter
          const renterReview = userReviews.find(r => r.reviewer?.userId === currentUserId && r.reviewedUser?.userId === rental.renter?.userId);
          if (renterReview) {
            setExistingRenterReview(renterReview);
          }
        }
      }
      
      // Fetch clothing reviews for this item by current user (only for renters)
      if (isRenterCheck) {
        const clothingId = rental.clothingCard?.clothingId || 
                           (typeof rental.clothingCard === 'number' ? rental.clothingCard : null);
        if (clothingId) {
          const clothingReviewsResponse = await fetch(`http://localhost:8080/api/clothing-reviews/clothing/${clothingId}`);
          if (clothingReviewsResponse.ok) {
            const clothingReviews = await clothingReviewsResponse.json();
            // Find review by current user
            const itemReview = clothingReviews.find(r => r.reviewer?.userId === currentUserId);
            if (itemReview) {
              setExistingItemReview(itemReview);
            }
          }
        }
      }
    } catch (err) {
      console.error("Error fetching existing reviews:", err);
    } finally {
      setLoadingReviews(false);
    }
  };

  const handleSubmitOwnerReview = async () => {
    if (!rental || !currentUserId || ownerRating < 1 || ownerRating > 5) {
      return;
    }
    
    setSubmittingOwnerReview(true);
    try {
      const ownerId = rental.clothingCard?.owner?.userId || 
                     (rental.clothingCard?.owner ? rental.clothingCard.owner : null);
      
      if (!ownerId) {
        alert("Unable to find owner information");
        return;
      }
      
      const token = localStorage.getItem("token");
      const response = await fetch(`http://localhost:8080/api/user-reviews`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          rentalId: rental.rentalId,
          reviewedUserId: ownerId,
          reviewerId: currentUserId,
          rating: ownerRating,
          reviewText: ownerReviewText.trim() || null
        }),
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: "Failed to submit review" }));
        throw new Error(errorData.message || "Failed to submit review");
      }
      
      // Refresh reviews
      await fetchExistingReviews();
      setOwnerRating(0);
      setOwnerReviewText("");
      alert("Review submitted successfully!");
    } catch (err) {
      alert(err.message || "Failed to submit review");
    } finally {
      setSubmittingOwnerReview(false);
    }
  };

  const handleSubmitItemReview = async () => {
    if (!rental || !currentUserId || itemRating < 1 || itemRating > 5) {
      return;
    }
    
    setSubmittingItemReview(true);
    try {
      const clothingId = rental.clothingCard?.clothingId || 
                         (typeof rental.clothingCard === 'number' ? rental.clothingCard : null);
      
      if (!clothingId) {
        alert("Unable to find item information");
        return;
      }
      
      const token = localStorage.getItem("token");
      const response = await fetch(`http://localhost:8080/api/clothing-reviews`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          clothingId: clothingId,
          reviewerId: currentUserId,
          rating: itemRating,
          reviewText: itemReviewText.trim() || null
        }),
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: "Failed to submit review" }));
        throw new Error(errorData.message || "Failed to submit review");
      }
      
      // Refresh reviews
      await fetchExistingReviews();
      setItemRating(0);
      setItemReviewText("");
      alert("Review submitted successfully!");
    } catch (err) {
      alert(err.message || "Failed to submit review");
    } finally {
      setSubmittingItemReview(false);
    }
  };

  const handleSubmitRenterReview = async () => {
    if (!rental || !currentUserId || renterRating < 1 || renterRating > 5) {
      return;
    }
    
    setSubmittingRenterReview(true);
    try {
      const renterId = rental.renter?.userId || 
                     (rental.renter ? rental.renter : null);
      
      if (!renterId) {
        alert("Unable to find renter information");
        return;
      }
      
      const token = localStorage.getItem("token");
      const response = await fetch(`http://localhost:8080/api/user-reviews`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          rentalId: rental.rentalId,
          reviewedUserId: renterId,
          reviewerId: currentUserId,
          rating: renterRating,
          reviewText: renterReviewText.trim() || null
        }),
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: "Failed to submit review" }));
        throw new Error(errorData.message || "Failed to submit review");
      }
      
      // Refresh reviews
      await fetchExistingReviews();
      setRenterRating(0);
      setRenterReviewText("");
      alert("Review submitted successfully!");
    } catch (err) {
      alert(err.message || "Failed to submit review");
    } finally {
      setSubmittingRenterReview(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric"
    });
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleString("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });
  };

  const getRentalStatusBadge = (returned) => {
    return returned ? 
      <span className="px-4 py-2 bg-gray-100 text-gray-800 rounded-full text-sm font-medium">Returned</span> :
      <span className="px-4 py-2 bg-blue-100 text-blue-800 rounded-full text-sm font-medium">Active Rental</span>;
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50">
        <Header />
        <div className="max-w-4xl mx-auto px-4 py-8">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-rose-500 mx-auto"></div>
            <p className="mt-4 text-rose-700">Loading rental details...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error || !rental) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50">
        <Header />
        <div className="max-w-4xl mx-auto px-4 py-8">
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="text-center">
              <p className="text-red-600 mb-4">{error || "Rental not found"}</p>
              <button
                onClick={() => router.push("/my-rentals")}
                className="px-6 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition"
              >
                Back to My Rentals
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const isRenter = rental.renter?.userId === currentUserId;
  const owner = rental.clothingCard?.owner;
  const renter = rental.renter;

  return (
    <div className="min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50">
      <Header />
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Back Button */}
        <div className="mb-6">
          <button
            onClick={() => router.back()}
            className="flex items-center text-rose-600 hover:text-rose-700 font-medium"
          >
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back to My Rentals
          </button>
        </div>

        {/* Header */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex justify-between items-start mb-4">
            <div>
              <h1 className="text-3xl font-serif font-semibold text-rose-900 mb-2">
                Rental Transaction Details
              </h1>
              <div className="flex items-center gap-3">
                <span className="text-gray-600">Order ID:</span>
                <span className="text-xl font-bold text-rose-600">#{rental.rentalId}</span>
                {getRentalStatusBadge(rental.returned)}
              </div>
            </div>
          </div>
        </div>

        {/* Transaction Information */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-semibold text-rose-900 mb-4 flex items-center">
            <svg className="w-6 h-6 mr-2 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            Transaction Information
          </h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <div>
                <p className="text-sm font-medium text-gray-600 mb-1">Rental Date</p>
                <p className="text-gray-900">{formatDate(rental.rentalDate)}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-600 mb-1">Return Date</p>
                <p className="text-gray-900">{formatDate(rental.returnDate)}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-600 mb-1">Transaction Created</p>
                <p className="text-gray-900">{formatDateTime(rental.createdAt)}</p>
              </div>
            </div>
            
            <div className="space-y-4">
              <div>
                <p className="text-sm font-medium text-gray-600 mb-1">Your Role</p>
                <p className="text-gray-900 font-semibold">
                  {isRenter ? "Renter" : "Owner"}
                </p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-600 mb-1">
                  {isRenter ? "Item Owner" : "Renter"}
                </p>
                <p className="text-gray-900">
                  {isRenter ? (owner?.userName || "Unknown") : (renter?.userName || "Unknown")}
                </p>
                {(isRenter ? owner?.userId : renter?.userId) && (
                  <Link
                    href={`/user-profile/${isRenter ? owner?.userId : renter?.userId}`}
                    className="text-rose-600 hover:text-rose-700 text-sm font-medium mt-1 inline-block"
                  >
                    View Profile →
                  </Link>
                )}
              </div>
              <div>
                <p className="text-sm font-medium text-gray-600 mb-1">Status</p>
                <div className="mt-1">
                  {getRentalStatusBadge(rental.returned)}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Item Information */}
        {clothingCard && (
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <h2 className="text-xl font-semibold text-rose-900 mb-4 flex items-center">
              <svg className="w-6 h-6 mr-2 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
              </svg>
              Rented Item
            </h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-4">
              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">{clothingCard.title}</h3>
                <p className="text-gray-700 mb-4">{clothingCard.description}</p>
              </div>
              
              <div className="space-y-3">
                <div>
                  <p className="text-sm font-medium text-gray-600">Brand</p>
                  <p className="text-gray-900">{clothingCard.brand || "N/A"}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-600">Size</p>
                  <p className="text-gray-900">{clothingCard.size}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-600">Location</p>
                  <p className="text-gray-900">{clothingCard.locationOfClothing || "N/A"}</p>
                </div>
              </div>
            </div>

            <div className="bg-gray-50 rounded-lg p-4 mb-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <p className="text-sm font-medium text-gray-600">Rental Cost</p>
                  <p className="text-2xl font-bold text-rose-600">${clothingCard.cost}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-600">Deposit</p>
                  <p className="text-xl font-semibold text-gray-900">${clothingCard.deposit}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-600">Total</p>
                  <p className="text-2xl font-bold text-gray-900">${clothingCard.cost + clothingCard.deposit}</p>
                </div>
              </div>
            </div>

            <Link
              href={`/clothingcard/${clothingCard.clothingId}`}
              className="inline-flex items-center px-4 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition-colors"
            >
              View Item Details
              <svg className="w-5 h-5 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </Link>
          </div>
        )}

        {/* Additional Information */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-semibold text-rose-900 mb-4 flex items-center">
            <svg className="w-6 h-6 mr-2 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Additional Information
          </h2>
          
          <div className="space-y-3 text-sm text-gray-600">
            <p>
              <span className="font-medium text-gray-900">Transaction ID:</span> {rental.rentalId}
            </p>
            <p>
              <span className="font-medium text-gray-900">Last Updated:</span> {formatDateTime(rental.updatedAt)}
            </p>
            {rental.returned && (
              <p className="text-green-600 font-medium">
                ✓ This item has been returned
              </p>
            )}
            {!rental.returned && (
              <p className="text-blue-600 font-medium">
                ⏳ This rental is currently active
              </p>
            )}
          </div>
        </div>

        {/* Review Sections for Renters - Only show if rental is returned and user is renter */}
        {rental.returned && isRenter && currentUserId && (
          <>
            {/* Review Owner Section */}
            <div className="bg-white rounded-lg shadow-md p-6 mb-6">
              <h2 className="text-xl font-semibold text-rose-900 mb-4 flex items-center">
                <svg className="w-6 h-6 mr-2 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                Review Owner
              </h2>
              
              {loadingReviews ? (
                <div className="text-center py-4">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500 mx-auto"></div>
                  <p className="mt-2 text-rose-700 text-sm">Loading review...</p>
                </div>
              ) : existingOwnerReview ? (
                <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-sm font-medium text-gray-700">Your Review</span>
                    <div className="flex items-center">
                      {[...Array(5)].map((_, i) => (
                        <svg
                          key={i}
                          className={`w-5 h-5 ${i < existingOwnerReview.rating ? 'text-yellow-400' : 'text-gray-300'}`}
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                        </svg>
                      ))}
                    </div>
                  </div>
                  {existingOwnerReview.reviewText && (
                    <p className="text-sm text-gray-700 mt-2">{existingOwnerReview.reviewText}</p>
                  )}
                  <p className="text-xs text-gray-500 mt-2">
                    Reviewed on {formatDate(existingOwnerReview.createdAt)}
                  </p>
                </div>
              ) : (
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Rating <span className="text-red-500">*</span>
                    </label>
                    <div className="flex gap-1">
                      {[1, 2, 3, 4, 5].map((rating) => (
                        <button
                          key={rating}
                          type="button"
                          onClick={() => setOwnerRating(rating)}
                          className={`w-10 h-10 rounded transition ${
                            ownerRating >= rating
                              ? 'text-yellow-400 bg-yellow-50'
                              : 'text-gray-300 hover:text-yellow-300'
                          }`}
                        >
                          <svg className="w-full h-full" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                          </svg>
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Review (Optional)
                    </label>
                    <textarea
                      value={ownerReviewText}
                      onChange={(e) => setOwnerReviewText(e.target.value)}
                      placeholder="Share your experience with this owner..."
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-rose-500 resize-none"
                      rows={3}
                    />
                  </div>

                  <button
                    onClick={handleSubmitOwnerReview}
                    disabled={submittingOwnerReview || !ownerRating}
                    className="px-4 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {submittingOwnerReview ? 'Submitting...' : 'Submit Review'}
                  </button>
                </div>
              )}
            </div>

            {/* Review Item Section */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-xl font-semibold text-rose-900 mb-4 flex items-center">
                <svg className="w-6 h-6 mr-2 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                </svg>
                Review Item
              </h2>
              
              {loadingReviews ? (
                <div className="text-center py-4">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500 mx-auto"></div>
                  <p className="mt-2 text-rose-700 text-sm">Loading review...</p>
                </div>
              ) : existingItemReview ? (
                <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-sm font-medium text-gray-700">Your Review</span>
                    <div className="flex items-center">
                      {[...Array(5)].map((_, i) => (
                        <svg
                          key={i}
                          className={`w-5 h-5 ${i < existingItemReview.review ? 'text-yellow-400' : 'text-gray-300'}`}
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                        </svg>
                      ))}
                    </div>
                  </div>
                  {existingItemReview.reviewText && (
                    <p className="text-sm text-gray-700 mt-2">{existingItemReview.reviewText}</p>
                  )}
                  <p className="text-xs text-gray-500 mt-2">
                    Reviewed on {formatDate(existingItemReview.createdAt)}
                  </p>
                </div>
              ) : (
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Rating <span className="text-red-500">*</span>
                    </label>
                    <div className="flex gap-1">
                      {[1, 2, 3, 4, 5].map((rating) => (
                        <button
                          key={rating}
                          type="button"
                          onClick={() => setItemRating(rating)}
                          className={`w-10 h-10 rounded transition ${
                            itemRating >= rating
                              ? 'text-yellow-400 bg-yellow-50'
                              : 'text-gray-300 hover:text-yellow-300'
                          }`}
                        >
                          <svg className="w-full h-full" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                          </svg>
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Review (Optional)
                    </label>
                    <textarea
                      value={itemReviewText}
                      onChange={(e) => setItemReviewText(e.target.value)}
                      placeholder="Share your experience with this item..."
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-rose-500 resize-none"
                      rows={3}
                    />
                  </div>

                  <button
                    onClick={handleSubmitItemReview}
                    disabled={submittingItemReview || !itemRating}
                    className="px-4 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {submittingItemReview ? 'Submitting...' : 'Submit Review'}
                  </button>
                </div>
              )}
            </div>
          </>
        )}

        {/* Review Renter Section for Owners - Only show if rental is returned and user is owner */}
        {rental.returned && !isRenter && currentUserId && (
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold text-rose-900 mb-4 flex items-center">
              <svg className="w-6 h-6 mr-2 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              Review Renter
            </h2>
            <div className="mb-4 pb-4 border-b border-gray-200">
              <p className="text-sm text-gray-600">
                Reviewing: <span className="font-medium text-gray-900">{renter?.userName || "Unknown"}</span>
              </p>
            </div>
            
            {loadingReviews ? (
              <div className="text-center py-4">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500 mx-auto"></div>
                <p className="mt-2 text-rose-700 text-sm">Loading review...</p>
              </div>
            ) : existingRenterReview ? (
              <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                <div className="flex items-center justify-between mb-3">
                  <span className="text-sm font-medium text-gray-700">Your Review</span>
                  <div className="flex items-center">
                    {[...Array(5)].map((_, i) => (
                      <svg
                        key={i}
                        className={`w-5 h-5 ${i < existingRenterReview.rating ? 'text-yellow-400' : 'text-gray-300'}`}
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                      </svg>
                    ))}
                  </div>
                </div>
                {existingRenterReview.reviewText && (
                  <p className="text-sm text-gray-700 mt-2">{existingRenterReview.reviewText}</p>
                )}
                <p className="text-xs text-gray-500 mt-2">
                  Reviewed on {formatDate(existingRenterReview.createdAt)}
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Rating <span className="text-red-500">*</span>
                  </label>
                  <div className="flex gap-1">
                    {[1, 2, 3, 4, 5].map((rating) => (
                      <button
                        key={rating}
                        type="button"
                        onClick={() => setRenterRating(rating)}
                        className={`w-10 h-10 rounded transition ${
                          renterRating >= rating
                            ? 'text-yellow-400 bg-yellow-50'
                            : 'text-gray-300 hover:text-yellow-300'
                        }`}
                      >
                        <svg className="w-full h-full" fill="currentColor" viewBox="0 0 20 20">
                          <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                        </svg>
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Review (Optional)
                  </label>
                  <textarea
                    value={renterReviewText}
                    onChange={(e) => setRenterReviewText(e.target.value)}
                    placeholder="Share your experience with this renter..."
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-rose-500 resize-none"
                    rows={3}
                  />
                </div>

                <button
                  onClick={handleSubmitRenterReview}
                  disabled={submittingRenterReview || !renterRating}
                  className="px-4 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {submittingRenterReview ? 'Submitting...' : 'Submit Review'}
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}


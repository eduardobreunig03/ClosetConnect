"use client";

import { useEffect, useState } from "react";
import Header from "../../components/Header";
import Link from "next/link";
import { useRouter } from "next/navigation";
import ReturnReviewModal from "../../components/ReturnReviewModal";

export default function MyRentalsPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState(null);
  const [userId, setUserId] = useState(null);
  const [outgoingRequests, setOutgoingRequests] = useState([]);
  const [rentalHistory, setRentalHistory] = useState([]);
  const [clothingCards, setClothingCards] = useState({});
  const [loadingRequests, setLoadingRequests] = useState(true);
  const [loadingRentals, setLoadingRentals] = useState(true);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [selectedRental, setSelectedRental] = useState(null);
  const [existingReviews, setExistingReviews] = useState({}); // rentalId -> review data
  const [showReviewForm, setShowReviewForm] = useState({}); // rentalId -> boolean
  const [reviewRatings, setReviewRatings] = useState({}); // rentalId -> rating
  const [reviewTexts, setReviewTexts] = useState({}); // rentalId -> text
  const [submittingReviews, setSubmittingReviews] = useState({}); // rentalId -> boolean

  useEffect(() => {
    const savedToken = localStorage.getItem("token");
    const savedUserId = localStorage.getItem("userId");
    setToken(savedToken);
    setUserId(savedUserId);
    setLoading(false);
  }, []);

  useEffect(() => {
    if (userId) {
      fetchOutgoingRequests();
      fetchRentalHistory();
    }
  }, [userId]);

  useEffect(() => {
    // Fetch existing reviews for returned rentals
    if (userId && rentalHistory.length > 0) {
      fetchExistingReviews();
    }
  }, [rentalHistory, userId]);

  useEffect(() => {
    // Fetch clothing card details for requests
    const requestClothingIds = outgoingRequests.map(req => req.clothingId);
    if (requestClothingIds.length > 0) {
      fetchClothingCards(requestClothingIds);
    }
  }, [outgoingRequests]);

  useEffect(() => {
    // Fetch clothing card details for rentals
    const rentalClothingIds = rentalHistory
      .map(rental => {
        if (rental.clothingCard?.clothingId) {
          return rental.clothingCard.clothingId;
        } else if (typeof rental.clothingCard === 'number') {
          return rental.clothingCard;
        }
        return null;
      })
      .filter(Boolean);
    if (rentalClothingIds.length > 0) {
      fetchClothingCards(rentalClothingIds);
    }
  }, [rentalHistory]);

  const fetchOutgoingRequests = async () => {
    try {
      setLoadingRequests(true);
      const token = localStorage.getItem("token");
      const headers = {};
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
      
      const response = await fetch(`http://localhost:8080/api/requests/requester/${userId}`, {
        headers: headers
      });
      if (response.ok) {
        const requests = await response.json();
        setOutgoingRequests(requests);
      }
    } catch (err) {
      console.error("Error fetching outgoing requests:", err);
    } finally {
      setLoadingRequests(false);
    }
  };

  const fetchRentalHistory = async () => {
    try {
      setLoadingRentals(true);
      // Only fetch rentals where user is the renter (not the owner)
      const response = await fetch(`http://localhost:8080/api/rentals/renter/${userId}`);
      if (response.ok) {
        const rentals = await response.json();
        setRentalHistory(rentals);
      }
    } catch (err) {
      console.error("Error fetching rental history:", err);
    } finally {
      setLoadingRentals(false);
    }
  };

  const fetchClothingCards = async (clothingIds) => {
    const uniqueIds = [...new Set(clothingIds)];
    for (const clothingId of uniqueIds) {
      if (!clothingCards[clothingId]) {
        try {
          const response = await fetch(`http://localhost:8080/api/clothing-cards/${clothingId}`);
          if (response.ok) {
            const clothingCard = await response.json();
            setClothingCards(prev => ({ ...prev, [clothingId]: clothingCard }));
          }
        } catch (err) {
          console.error(`Error fetching clothing card ${clothingId}:`, err);
        }
      }
    }
  };

  const fetchExistingReviews = async () => {
    const returnedRentals = rentalHistory.filter(r => r.returned);
    const reviewMap = {};
    
    for (const rental of returnedRentals) {
      try {
        const response = await fetch(`http://localhost:8080/api/user-reviews/rental/${rental.rentalId}`);
        if (response.ok) {
          const reviews = await response.json();
          // Find review by current user (as reviewer)
          const userReview = reviews.find(r => r.reviewer?.userId === parseInt(userId));
          if (userReview) {
            reviewMap[rental.rentalId] = userReview;
          }
        }
      } catch (err) {
        console.error(`Error fetching reviews for rental ${rental.rentalId}:`, err);
      }
    }
    
    setExistingReviews(reviewMap);
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric"
    });
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });
  };

  const getStatusBadge = (approved) => {
    if (approved === null || approved === undefined) {
      return <span className="px-3 py-1 bg-yellow-100 text-yellow-800 rounded-full text-xs font-medium">Pending</span>;
    }
    return approved ? 
      <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-xs font-medium">Approved</span> :
      <span className="px-3 py-1 bg-red-100 text-red-800 rounded-full text-xs font-medium">Rejected</span>;
  };

  const getRentalStatusBadge = (returned) => {
    return returned ? 
      <span className="px-3 py-1 bg-gray-100 text-gray-800 rounded-full text-xs font-medium">Returned</span> :
      <span className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-xs font-medium">Active</span>;
  };

  const handleReturnItem = async (rental) => {
    try {
      const response = await fetch(`http://localhost:8080/api/rentals/${rental.rentalId}/return`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          userId: parseInt(userId)
        }),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ error: "Failed to return item" }));
        throw new Error(errorData.error || "Failed to return item");
      }

      // Get the updated rental data
      const updatedRental = await response.json();
      
      // Extract clothing ID
      const clothingId = updatedRental.clothingCard?.clothingId || 
                        (typeof updatedRental.clothingCard === 'number' ? updatedRental.clothingCard : null);
      
      // Fetch the full rental details to get owner info if not included
      let ownerId = updatedRental.clothingCard?.owner?.userId;
      
      if (!ownerId) {
        // Try to fetch the rental again to get full details
        try {
          const rentalResponse = await fetch(`http://localhost:8080/api/rentals/${rental.rentalId}`);
          if (rentalResponse.ok) {
            const fullRental = await rentalResponse.json();
            ownerId = fullRental.clothingCard?.owner?.userId || 
                     (fullRental.clothingCard?.owner ? fullRental.clothingCard.owner : null);
          }
        } catch (fetchErr) {
          console.error("Error fetching rental details:", fetchErr);
        }
      }

      // If still no ownerId, try to get it from the clothing card
      if (!ownerId && clothingId) {
        try {
          const cardResponse = await fetch(`http://localhost:8080/api/clothing-cards/${clothingId}`);
          if (cardResponse.ok) {
            const clothingCard = await cardResponse.json();
            ownerId = clothingCard.owner?.userId || clothingCard.owner;
          }
        } catch (fetchErr) {
          console.error("Error fetching clothing card:", fetchErr);
        }
      }

      // Also try to get ownerId from the original rental data if we still don't have it
      if (!ownerId && rental.clothingCard?.owner?.userId) {
        ownerId = rental.clothingCard.owner.userId;
      }

      setSelectedRental({
        rentalId: updatedRental.rentalId,
        clothingId: clothingId,
        ownerId: ownerId
      });

      console.log("Opening review modal with:", {
        rentalId: updatedRental.rentalId,
        clothingId: clothingId,
        ownerId: ownerId,
        rentalData: updatedRental
      });

      // Show review modal
      setShowReviewModal(true);
    } catch (err) {
      alert(err.message || "Failed to return item");
      console.error("Error returning item:", err);
    }
  };

  const handleReviewComplete = () => {
    // Refresh rental history
    fetchRentalHistory();
    setShowReviewModal(false);
    setSelectedRental(null);
  };

  const handleOpenReviewForm = (rentalId) => {
    setShowReviewForm(prev => ({ ...prev, [rentalId]: true }));
  };

  const handleCloseReviewForm = (rentalId) => {
    setShowReviewForm(prev => {
      const newState = { ...prev };
      delete newState[rentalId];
      return newState;
    });
    setReviewRatings(prev => {
      const newState = { ...prev };
      delete newState[rentalId];
      return newState;
    });
    setReviewTexts(prev => {
      const newState = { ...prev };
      delete newState[rentalId];
      return newState;
    });
  };

  const handleSubmitOwnerReview = async (rental) => {
    const rentalId = rental.rentalId;
    let ownerId = rental.clothingCard?.owner?.userId || 
                   (rental.clothingCard?.owner ? rental.clothingCard.owner : null);
    
    // If owner ID not found in rental, try to get it from clothing card
    if (!ownerId) {
      const clothingId = rental.clothingCard?.clothingId || 
                        (typeof rental.clothingCard === 'number' ? rental.clothingCard : null);
      if (clothingId && clothingCards[clothingId]) {
        ownerId = clothingCards[clothingId].owner?.userId || clothingCards[clothingId].owner;
      }
    }
    
    if (!ownerId) {
      alert("Unable to find owner information");
      return;
    }

    const rating = reviewRatings[rentalId];
    const reviewText = reviewTexts[rentalId]?.trim() || null;

    if (!rating || rating < 1 || rating > 5) {
      alert("Please select a rating between 1 and 5");
      return;
    }

    setSubmittingReviews(prev => ({ ...prev, [rentalId]: true }));

    try {
      const response = await fetch(`http://localhost:8080/api/user-reviews`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          rentalId: rentalId,
          reviewedUserId: ownerId,
          reviewerId: parseInt(userId),
          rating: rating,
          reviewText: reviewText
        }),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: "Failed to submit review" }));
        throw new Error(errorData.message || "Failed to submit review");
      }

      // Refresh reviews and close form
      await fetchExistingReviews();
      handleCloseReviewForm(rentalId);
      alert("Review submitted successfully!");
    } catch (err) {
      alert(err.message || "Failed to submit review");
    } finally {
      setSubmittingReviews(prev => {
        const newState = { ...prev };
        delete newState[rentalId];
        return newState;
      });
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50">
        <Header />
        <div className="max-w-7xl mx-auto px-4 py-8">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-rose-500 mx-auto"></div>
            <p className="mt-4 text-rose-700">Loading your rentals...</p>
          </div>
        </div>
      </div>
    );
  }

  if (!token || !userId) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50">
        <Header />
        <div className="max-w-7xl mx-auto px-4 py-8">
          <div className="text-center">
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
              <p className="font-semibold">Please log in to view your rentals</p>
            </div>
            <Link
              href="/login"
              className="inline-block mt-4 px-6 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition"
            >
              Log In
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50">
      <Header />
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-4xl font-serif font-semibold text-rose-900 mb-2">
            My Rentals
          </h1>
          <p className="text-rose-700">
            Manage your rental requests and view items you are renting or have rented
          </p>
        </div>

        {/* Outgoing Requests Section */}
        <div className="mb-8">
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-2xl font-semibold text-rose-900 mb-4 flex items-center">
              <svg className="w-6 h-6 mr-3 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Outgoing Requests
            </h2>
            
            {loadingRequests ? (
              <div className="text-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500 mx-auto"></div>
                <p className="mt-2 text-rose-700 text-sm">Loading requests...</p>
              </div>
            ) : outgoingRequests.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-gray-500">No outgoing requests yet</p>
                <p className="text-gray-400 text-sm mt-2">Browse items to make your first request!</p>
              </div>
            ) : (
              <div className="space-y-4">
                {outgoingRequests.map((request) => {
                  const clothingCard = clothingCards[request.clothingId];
                  return (
                    <div key={request.requestId} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                      <div className="flex justify-between items-start mb-3">
                        <div className="flex-1">
                          <h3 className="text-lg font-semibold text-gray-900">
                            {clothingCard ? clothingCard.title : `Item #${request.clothingId}`}
                          </h3>
                          <p className="text-sm text-gray-600 mt-1">
                            Requested on {formatDate(request.createdAt)}
                          </p>
                        </div>
                        {getStatusBadge(request.approved)}
                      </div>
                      
                      {clothingCard && (
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-3 p-3 bg-gray-50 rounded">
                          <div>
                            <p className="text-xs font-medium text-gray-600">Brand & Size</p>
                            <p className="text-gray-900">{clothingCard.brand} - {clothingCard.size}</p>
                          </div>
                          <div>
                            <p className="text-xs font-medium text-gray-600">Price</p>
                            <p className="text-gray-900">${clothingCard.cost} + ${clothingCard.deposit} deposit</p>
                          </div>
                          <div>
                            <p className="text-xs font-medium text-gray-600">Requested Date</p>
                            <p className="text-gray-900">{formatDate(request.availabilityRangeRequest)}</p>
                          </div>
                        </div>
                      )}
                      
                      <div className="flex gap-2">
                        <button
                          onClick={() => router.push(`/clothingcard/${request.clothingId}`)}
                          className="px-4 py-2 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
                        >
                          View Item
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Rental History Section */}
        <div className="mb-8">
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-2xl font-semibold text-rose-900 mb-4 flex items-center">
              <svg className="w-6 h-6 mr-3 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
              </svg>
              Rental History
            </h2>
            
            {loadingRentals ? (
              <div className="text-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500 mx-auto"></div>
                <p className="mt-2 text-rose-700 text-sm">Loading rental history...</p>
              </div>
            ) : rentalHistory.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-gray-500">No rental history yet</p>
                <p className="text-gray-400 text-sm mt-2">Your completed rentals will appear here</p>
              </div>
            ) : (
              <div className="space-y-4">
                {rentalHistory.map((rental) => {
                  const clothingId = rental.clothingCard?.clothingId || (typeof rental.clothingCard === 'number' ? rental.clothingCard : null);
                  const clothingCard = rental.clothingCard?.clothingId ? rental.clothingCard : (clothingId ? clothingCards[clothingId] : null);
                  const isActive = !rental.returned;
                  
                  return (
                    <div
                      key={rental.rentalId}
                      className="border border-gray-200 rounded-lg p-5 hover:shadow-lg transition-all"
                    >
                      <div className="flex justify-between items-start mb-3">
                        <Link
                          href={`/rental/${rental.rentalId}`}
                          className="flex-1 hover:opacity-80 transition-opacity"
                        >
                          <div className="flex items-center gap-3 mb-2">
                            <span className="text-xs font-medium text-gray-500">Order ID:</span>
                            <span className="text-sm font-semibold text-rose-600">#{rental.rentalId}</span>
                            {getRentalStatusBadge(rental.returned)}
                          </div>
                          <h3 className="text-lg font-semibold text-gray-900">
                            {clothingCard ? clothingCard.title : `Item #${clothingId || "N/A"}`}
                          </h3>
                          <p className="text-sm text-gray-600 mt-1">
                            Rented from {rental.clothingCard?.owner?.userName || "Unknown"}
                          </p>
                        </Link>
                      </div>
                      
                      {clothingCard && (
                        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-3 p-3 bg-gray-50 rounded">
                          <div>
                            <p className="text-xs font-medium text-gray-600">Brand & Size</p>
                            <p className="text-gray-900">{clothingCard.brand} - {clothingCard.size}</p>
                          </div>
                          <div>
                            <p className="text-xs font-medium text-gray-600">Rental Date</p>
                            <p className="text-gray-900">{formatDate(rental.rentalDate)}</p>
                          </div>
                          <div>
                            <p className="text-xs font-medium text-gray-600">Return Date</p>
                            <p className="text-gray-900">{formatDate(rental.returnDate)}</p>
                          </div>
                          <div>
                            <p className="text-xs font-medium text-gray-600">Total Cost</p>
                            <p className="text-gray-900 font-semibold">${clothingCard.cost}</p>
                          </div>
                        </div>
                      )}
                      
                      <div className="flex items-center justify-between">
                        <Link
                          href={`/rental/${rental.rentalId}`}
                          className="flex items-center text-sm text-rose-600 font-medium hover:text-rose-700"
                        >
                          View Details
                          <svg className="w-4 h-4 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                          </svg>
                        </Link>
                        {isActive && (
                          <button
                            onClick={() => handleReturnItem(rental)}
                            className="px-4 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition text-sm font-medium"
                          >
                            Return Item
                          </button>
                        )}
                      </div>

                      {/* Owner Review Section - Only for returned rentals */}
                      {rental.returned && (
                        <div className="mt-4 pt-4 border-t border-gray-200">
                          {existingReviews[rental.rentalId] ? (
                            <div className="bg-gray-50 rounded-lg p-3">
                              <div className="flex items-center justify-between mb-2">
                                <span className="text-sm font-medium text-gray-700">Your Review of Owner</span>
                                <div className="flex items-center">
                                  {[...Array(5)].map((_, i) => (
                                    <svg
                                      key={i}
                                      className={`w-4 h-4 ${i < existingReviews[rental.rentalId].rating ? 'text-yellow-400' : 'text-gray-300'}`}
                                      fill="currentColor"
                                      viewBox="0 0 20 20"
                                    >
                                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                                    </svg>
                                  ))}
                                </div>
                              </div>
                              {existingReviews[rental.rentalId].reviewText && (
                                <p className="text-sm text-gray-600 mt-1">{existingReviews[rental.rentalId].reviewText}</p>
                              )}
                            </div>
                          ) : (
                            !showReviewForm[rental.rentalId] ? (
                              <button
                                onClick={() => handleOpenReviewForm(rental.rentalId)}
                                className="px-4 py-2 bg-rose-100 text-rose-700 rounded-lg hover:bg-rose-200 transition text-sm font-medium"
                              >
                                Leave a Review for Owner
                              </button>
                            ) : (
                              <div className="bg-rose-50 rounded-lg p-4 border border-rose-200">
                                <h4 className="text-sm font-semibold text-rose-900 mb-3">
                                  Review Owner: {rental.clothingCard?.owner?.userName || "Owner"}
                                </h4>
                                
                                <div className="mb-3">
                                  <label className="block text-sm font-medium text-gray-700 mb-2">
                                    Rating <span className="text-red-500">*</span>
                                  </label>
                                  <div className="flex gap-1">
                                    {[1, 2, 3, 4, 5].map((rating) => (
                                      <button
                                        key={rating}
                                        type="button"
                                        onClick={() => setReviewRatings(prev => ({ ...prev, [rental.rentalId]: rating }))}
                                        className={`w-8 h-8 rounded transition ${
                                          reviewRatings[rental.rentalId] >= rating
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

                                <div className="mb-3">
                                  <label className="block text-sm font-medium text-gray-700 mb-2">
                                    Review (Optional)
                                  </label>
                                  <textarea
                                    value={reviewTexts[rental.rentalId] || ''}
                                    onChange={(e) => setReviewTexts(prev => ({ ...prev, [rental.rentalId]: e.target.value }))}
                                    placeholder="Share your experience with this owner..."
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-rose-500 resize-none"
                                    rows={3}
                                  />
                                </div>

                                <div className="flex gap-2">
                                  <button
                                    onClick={() => handleSubmitOwnerReview(rental)}
                                    disabled={submittingReviews[rental.rentalId] || !reviewRatings[rental.rentalId]}
                                    className="px-4 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                                  >
                                    {submittingReviews[rental.rentalId] ? 'Submitting...' : 'Submit Review'}
                                  </button>
                                  <button
                                    onClick={() => handleCloseReviewForm(rental.rentalId)}
                                    disabled={submittingReviews[rental.rentalId]}
                                    className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition text-sm font-medium disabled:opacity-50"
                                  >
                                    Cancel
                                  </button>
                                </div>
                              </div>
                            )
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Return Review Modal */}
      {showReviewModal && selectedRental && (
        <ReturnReviewModal
          isOpen={showReviewModal}
          onClose={() => {
            setShowReviewModal(false);
            setSelectedRental(null);
          }}
          rentalId={selectedRental.rentalId}
          clothingId={selectedRental.clothingId}
          ownerId={selectedRental.ownerId}
          onReviewComplete={handleReviewComplete}
        />
      )}
    </div>
  );
}

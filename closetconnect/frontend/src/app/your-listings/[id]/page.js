"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Header from "../../../components/Header";
import ReviewSection from "../../../components/ReviewSection";
import Link from "next/link";

export default function YourListingDetailPage({ params }) {
  const [listing, setListing] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [token, setToken] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({});
  const router = useRouter();
  const [currentUserId, setCurrentUserId] = useState(null);
  const [returnedRentals, setReturnedRentals] = useState([]);
  const [loadingRentals, setLoadingRentals] = useState(false);
  const [renterReviews, setRenterReviews] = useState({}); // rentalId -> review data
  const [renterRatings, setRenterRatings] = useState({}); // rentalId -> rating
  const [renterReviewTexts, setRenterReviewTexts] = useState({}); // rentalId -> text
  const [submittingReviews, setSubmittingReviews] = useState({}); // rentalId -> boolean

  useEffect(() => {
    const savedToken = localStorage.getItem("token");
    const savedUserId = localStorage.getItem("userId");
    setToken(savedToken);
    setCurrentUserId(savedUserId ? parseInt(savedUserId) : null);

    if (!savedToken) {
      setError("Please log in to view your listing");
      setLoading(false);
      return;
    }

    fetchListingDetails(params.id, savedToken);
  }, [params.id]);

  useEffect(() => {
    if (listing && currentUserId) {
      fetchReturnedRentals();
    }
  }, [listing, currentUserId]);

  useEffect(() => {
    if (returnedRentals.length > 0 && currentUserId) {
      fetchExistingRenterReviews();
    }
  }, [returnedRentals, currentUserId]);

  const fetchListingDetails = async (listingId, token) => {
    try {
      const res = await fetch(`http://localhost:8080/api/clothing-cards/${listingId}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        throw new Error(`Error ${res.status}: Failed to fetch listing`);
      }

      const data = await res.json();
      setListing(data);
      setEditForm({
        title: data.title || "",
        description: data.description || "",
        cost: data.cost || "",
        deposit: data.deposit || "",
        size: data.size || "",
        brand: data.brand || "",
        locationOfClothing: data.locationOfClothing || "",
        availability: data.availability
      });
    } catch (err) {
      setError(err.message || "Failed to fetch listing details");
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = () => {
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
    setEditForm({
      title: listing.title || "",
      description: listing.description || "",
      cost: listing.cost || "",
      deposit: listing.deposit || "",
      size: listing.size || "",
      brand: listing.brand || "",
      locationOfClothing: listing.locationOfClothing || "",
      availability: listing.availability
    });
  };

  const handleSaveEdit = async () => {
    try {
      // Create a complete object with all existing fields and updated ones
      const updatedData = {
        ...listing, // Keep all existing fields
        ...editForm, // Override with edited fields
        clothingId: listing.clothingId, // Ensure ID is preserved
        owner: listing.owner, // Preserve owner relationship
        datePosted: listing.datePosted, // Preserve original date
        createdAt: listing.createdAt, // Preserve creation date
        updatedAt: new Date().toISOString() // Update timestamp
      };

      const res = await fetch(`http://localhost:8080/api/clothing-cards/${params.id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(updatedData),
      });

      if (!res.ok) {
        const errorText = await res.text();
        console.error('Update error:', errorText);
        throw new Error(`Error ${res.status}: Failed to update listing - ${errorText}`);
      }

      const updatedListing = await res.json();
      setListing(updatedListing);
      setIsEditing(false);
    } catch (err) {
      console.error('Save edit error:', err);
      setError(err.message || "Failed to update listing");
    }
  };

  const handleDeleteListing = async () => {
    if (!confirm("Are you sure you want to delete this listing? This action cannot be undone.")) {
      return;
    }

    try {
      const res = await fetch(`http://localhost:8080/api/clothing-cards/${params.id}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        throw new Error(`Error ${res.status}: Failed to delete listing`);
      }

      router.push("/your-listings");
    } catch (err) {
      setError(err.message || "Failed to delete listing");
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setEditForm(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const fetchReturnedRentals = async () => {
    if (!listing || !currentUserId) return;
    
    try {
      setLoadingRentals(true);
      // Fetch all rentals for this owner and filter for returned ones for this specific item
      const response = await fetch(`http://localhost:8080/api/rentals/owner/${currentUserId}`);
      if (response.ok) {
        const allRentals = await response.json();
        // Filter for rentals of this specific item that are returned
        const itemRentals = allRentals.filter(rental => {
          const clothingId = rental.clothingCard?.clothingId || 
                            (typeof rental.clothingCard === 'number' ? rental.clothingCard : null);
          return clothingId === listing.clothingId && rental.returned;
        });
        setReturnedRentals(itemRentals);
      }
    } catch (err) {
      console.error("Error fetching returned rentals:", err);
    } finally {
      setLoadingRentals(false);
    }
  };

  const fetchExistingRenterReviews = async () => {
    if (!returnedRentals.length || !currentUserId) return;
    
    const reviewMap = {};
    for (const rental of returnedRentals) {
      try {
        const response = await fetch(`http://localhost:8080/api/user-reviews/rental/${rental.rentalId}`);
        if (response.ok) {
          const reviews = await response.json();
          // Find review by current user (as reviewer/owner)
          const ownerReview = reviews.find(r => r.reviewer?.userId === currentUserId);
          if (ownerReview) {
            reviewMap[rental.rentalId] = ownerReview;
          }
        }
      } catch (err) {
        console.error(`Error fetching reviews for rental ${rental.rentalId}:`, err);
      }
    }
    setRenterReviews(reviewMap);
  };

  const handleSubmitRenterReview = async (rental) => {
    const rentalId = rental.rentalId;
    const renterId = rental.renter?.userId || 
                    (rental.renter ? rental.renter : null);
    
    if (!renterId) {
      alert("Unable to find renter information");
      return;
    }

    const rating = renterRatings[rentalId];
    const reviewText = renterReviewTexts[rentalId]?.trim() || null;

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
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          rentalId: rentalId,
          reviewedUserId: renterId,
          reviewerId: currentUserId,
          rating: rating,
          reviewText: reviewText
        }),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: "Failed to submit review" }));
        throw new Error(errorData.message || "Failed to submit review");
      }

      // Refresh reviews and clear form
      await fetchExistingRenterReviews();
      setRenterRatings(prev => {
        const newState = { ...prev };
        delete newState[rentalId];
        return newState;
      });
      setRenterReviewTexts(prev => {
        const newState = { ...prev };
        delete newState[rentalId];
        return newState;
      });
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

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric"
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50">
        <Header />
        <div className="max-w-7xl mx-auto px-4 py-8">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-rose-500 mx-auto"></div>
            <p className="mt-4 text-rose-700">Loading listing details...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50">
        <Header />
        <div className="max-w-7xl mx-auto px-4 py-8">
          <div className="text-center">
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
              <p className="font-semibold">Error</p>
              <p>{error}</p>
            </div>
            <Link
              href="/your-listings"
              className="inline-block mt-4 px-6 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition"
            >
              Back to Your Listings
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (!listing) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50">
        <Header />
        <div className="max-w-7xl mx-auto px-4 py-8">
          <div className="text-center">
            <p className="text-rose-700">Listing not found</p>
            <Link
              href="/your-listings"
              className="inline-block mt-4 px-6 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition"
            >
              Back to Your Listings
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
        {/* Header with navigation */}
        <div className="mb-6">
          <Link
            href="/your-listings"
            className="inline-flex items-center text-rose-600 hover:text-rose-800 transition mb-4"
          >
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back to Your Listings
          </Link>
          <h1 className="text-4xl font-serif font-semibold text-rose-900 mb-2">
            {isEditing ? "Edit Listing" : "Your Listing Details"}
          </h1>
        </div>

        <div className="grid lg:grid-cols-2 gap-8 mb-8">
          {/* Left: Image */}
          <div className="w-full h-96 lg:h-full flex items-center justify-center bg-gray-100 rounded-2xl overflow-hidden">
            {listing.images && listing.images.length > 0 ? (
              <img
                src={`data:image/jpeg;base64,${listing.images[0].imageData || listing.images[0].image}`}
                alt={listing.title}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="text-gray-500 text-center">
                <div className="text-6xl mb-4">👗</div>
                <p>No Image Available</p>
              </div>
            )}
          </div>

          {/* Right: Details */}
          <div className="p-6 flex flex-col justify-between space-y-6 bg-gradient-to-b from-pink-100 via-rose-200 to-pink-100 text-rose-900 rounded-2xl">
            {isEditing ? (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-semibold mb-2">Title</label>
                  <input
                    type="text"
                    name="title"
                    value={editForm.title}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 border border-rose-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-semibold mb-2">Description</label>
                  <textarea
                    name="description"
                    value={editForm.description}
                    onChange={handleInputChange}
                    rows={3}
                    className="w-full px-3 py-2 border border-rose-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-semibold mb-2">Cost ($/day)</label>
                    <input
                      type="number"
                      name="cost"
                      value={editForm.cost}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-rose-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-semibold mb-2">Deposit ($)</label>
                    <input
                      type="number"
                      name="deposit"
                      value={editForm.deposit}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-rose-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-semibold mb-2">Size</label>
                    <input
                      type="text"
                      name="size"
                      value={editForm.size}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-rose-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-semibold mb-2">Brand</label>
                    <input
                      type="text"
                      name="brand"
                      value={editForm.brand}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-rose-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-semibold mb-2">Location</label>
                  <input
                    type="text"
                    name="locationOfClothing"
                    value={editForm.locationOfClothing}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 border border-rose-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
                  />
                </div>

                <div className="flex items-center">
                  <input
                    type="checkbox"
                    name="availability"
                    checked={editForm.availability}
                    onChange={handleInputChange}
                    className="mr-2"
                  />
                  <label className="text-sm font-semibold">Available for rent</label>
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                <h2 className="text-2xl font-bold">{listing.title}</h2>
                <p className="text-gray-700">{listing.description}</p>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <p>
                    <span className="font-semibold">Size:</span> {listing.size}
                  </p>
                  <p>
                    <span className="font-semibold">Brand:</span> {listing.brand || "N/A"}
                  </p>
                  <p>
                    <span className="font-semibold">Available:</span>{" "}
                    {listing.availability ? "Yes" : "No"}
                  </p>
                  <p>
                    <span className="font-semibold">Posted:</span>{" "}
                    {new Date(listing.datePosted).toLocaleDateString()}
                  </p>
                </div>
                {listing.locationOfClothing && (
                  <p className="text-sm">
                    <span className="font-semibold">Location:</span>{" "}
                    {listing.locationOfClothing}
                  </p>
                )}
              </div>
            )}

            <div className="space-y-4">
              <div className="text-center">
                <p className="text-3xl font-bold text-rose-600">
                  ${isEditing ? editForm.cost : listing.cost}
                </p>
                <p className="text-rose-800">
                  + ${isEditing ? editForm.deposit : listing.deposit} deposit
                </p>
              </div>
              
              {isEditing ? (
                <div className="space-y-2">
                  <button
                    onClick={handleSaveEdit}
                    className="w-full py-3 bg-green-600 text-white rounded-full font-semibold hover:bg-green-700 transition-colors"
                  >
                    Save Changes
                  </button>
                  <button
                    onClick={handleCancelEdit}
                    className="w-full py-3 border-2 border-gray-400 text-gray-700 rounded-full font-semibold hover:border-gray-600 transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              ) : (
                <div className="space-y-2">
                  <button
                    onClick={handleEdit}
                    className="w-full py-3 bg-rose-600 text-white rounded-full font-semibold hover:bg-rose-700 transition-colors"
                  >
                    Edit Listing
                  </button>
                  <button
                    onClick={handleDeleteListing}
                    className="w-full py-3 border-2 border-red-600 text-red-600 rounded-full font-semibold hover:bg-red-600 hover:text-white transition-colors"
                  >
                    Delete Listing
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Reviews Section */}
        <div className="max-w-4xl mx-auto mb-8">
          <ReviewSection 
            clothingId={listing.clothingId} 
            clothingOwnerId={listing.owner?.userId}
          />
        </div>

        {/* Review Renters Section */}
        {returnedRentals.length > 0 && (
          <div className="max-w-4xl mx-auto mb-8">
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-2xl font-semibold text-rose-900 mb-4 flex items-center">
                <svg className="w-6 h-6 mr-2 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                Review Renters
              </h2>
              
              {loadingRentals ? (
                <div className="text-center py-4">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500 mx-auto"></div>
                  <p className="mt-2 text-rose-700 text-sm">Loading rental history...</p>
                </div>
              ) : (
                <div className="space-y-6">
                  {returnedRentals.map((rental) => {
                    const renter = rental.renter?.userId ? rental.renter : null;
                    const renterName = renter?.userName || "Unknown";
                    const existingReview = renterReviews[rental.rentalId];
                    
                    return (
                      <div key={rental.rentalId} className="border border-gray-200 rounded-lg p-4">
                        <div className="mb-4">
                          <div className="flex items-center justify-between mb-2">
                            <h3 className="text-lg font-semibold text-gray-900">Rental #{rental.rentalId}</h3>
                            <span className="text-sm text-gray-600">
                              Returned on {formatDate(rental.returnDate)}
                            </span>
                          </div>
                          <p className="text-sm text-gray-600">
                            Rented to: <span className="font-medium">{renterName}</span>
                          </p>
                        </div>

                        {existingReview ? (
                          <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                            <div className="flex items-center justify-between mb-3">
                              <span className="text-sm font-medium text-gray-700">Your Review</span>
                              <div className="flex items-center">
                                {[...Array(5)].map((_, i) => (
                                  <svg
                                    key={i}
                                    className={`w-5 h-5 ${i < existingReview.rating ? 'text-yellow-400' : 'text-gray-300'}`}
                                    fill="currentColor"
                                    viewBox="0 0 20 20"
                                  >
                                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                                  </svg>
                                ))}
                              </div>
                            </div>
                            {existingReview.reviewText && (
                              <p className="text-sm text-gray-700 mt-2">{existingReview.reviewText}</p>
                            )}
                            <p className="text-xs text-gray-500 mt-2">
                              Reviewed on {formatDate(existingReview.createdAt)}
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
                                    onClick={() => setRenterRatings(prev => ({ ...prev, [rental.rentalId]: rating }))}
                                    className={`w-10 h-10 rounded transition ${
                                      renterRatings[rental.rentalId] >= rating
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
                                value={renterReviewTexts[rental.rentalId] || ''}
                                onChange={(e) => setRenterReviewTexts(prev => ({ ...prev, [rental.rentalId]: e.target.value }))}
                                placeholder="Share your experience with this renter..."
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-rose-500 resize-none"
                                rows={3}
                              />
                            </div>

                            <button
                              onClick={() => handleSubmitRenterReview(rental)}
                              disabled={submittingReviews[rental.rentalId] || !renterRatings[rental.rentalId]}
                              className="px-4 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                              {submittingReviews[rental.rentalId] ? 'Submitting...' : 'Submit Review'}
                            </button>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Rental Requests Section (Placeholder) */}
        <div className="max-w-4xl mx-auto">
          <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-2xl font-semibold text-rose-900 mb-4">Rental Requests</h3>
          </div>
        </div>
      </div>
    </div>
  );
}

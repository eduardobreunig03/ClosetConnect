"use client";

import { useEffect, useState } from "react";
import Header from "../../components/Header";
import Link from "next/link";

export default function YourListingsPage() {
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [token, setToken] = useState(null);
  const [userId, setUserId] = useState(null);
  const [rentedOutItems, setRentedOutItems] = useState([]);
  const [returnedRentals, setReturnedRentals] = useState([]);
  const [loadingRentals, setLoadingRentals] = useState(true);
  const [loadingReturnedRentals, setLoadingReturnedRentals] = useState(true);
  const [clothingCards, setClothingCards] = useState({});
  const [renters, setRenters] = useState({});

  useEffect(() => {
    const savedToken = localStorage.getItem("token");
    const savedUserId = localStorage.getItem("userId");
    setToken(savedToken);
    setUserId(savedUserId);

    if (!savedToken || !savedUserId) {
      setError("Please log in to view your listings");
      setLoading(false);
      return;
    }

    fetchUserListings(savedToken, savedUserId);
    fetchRentedOutItems(savedUserId);
    fetchReturnedRentals(savedUserId);
  }, []);

  const fetchUserListings = async (token, userId) => {
    try {
      const res = await fetch(`http://localhost:8080/api/clothing-cards/user/${userId}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        throw new Error(`Error ${res.status}: Failed to fetch listings`);
      }

      const data = await res.json();
      setListings(data);
    } catch (err) {
      setError(err.message || "Failed to fetch your listings");
    } finally {
      setLoading(false);
    }
  };

  const fetchRentedOutItems = async (userId) => {
    try {
      setLoadingRentals(true);
      // Fetch only active (not returned) rentals
      const response = await fetch(`http://localhost:8080/api/rentals/owner/${userId}/active`);
      if (response.ok) {
        const rentals = await response.json();
        setRentedOutItems(rentals);
        
        // Fetch clothing card and renter details
        const clothingIds = rentals.map(rental => rental.clothingCard?.clothingId || rental.clothingCard).filter(Boolean);
        const renterIds = rentals.map(rental => rental.renter?.userId || rental.renter).filter(Boolean);
        
        for (const clothingId of clothingIds) {
          if (!clothingCards[clothingId]) {
            try {
              const cardResponse = await fetch(`http://localhost:8080/api/clothing-cards/${clothingId}`);
              if (cardResponse.ok) {
                const card = await cardResponse.json();
                setClothingCards(prev => ({ ...prev, [clothingId]: card }));
              }
            } catch (err) {
              console.error(`Error fetching clothing card ${clothingId}:`, err);
            }
          }
        }
        
        for (const renterId of renterIds) {
          if (!renters[renterId]) {
            try {
              const userResponse = await fetch(`http://localhost:8080/api/users/${renterId}`);
              if (userResponse.ok) {
                const user = await userResponse.json();
                setRenters(prev => ({ ...prev, [renterId]: user }));
              }
            } catch (err) {
              console.error(`Error fetching renter ${renterId}:`, err);
            }
          }
        }
      }
    } catch (err) {
      console.error("Error fetching rented out items:", err);
    } finally {
      setLoadingRentals(false);
    }
  };

  const fetchReturnedRentals = async (userId) => {
    try {
      setLoadingReturnedRentals(true);
      const response = await fetch(`http://localhost:8080/api/rentals/owner/${userId}/returned`);
      if (response.ok) {
        const rentals = await response.json();
        setReturnedRentals(rentals);
        
        // Fetch clothing card and renter details
        const clothingIds = rentals.map(rental => rental.clothingCard?.clothingId || rental.clothingCard).filter(Boolean);
        const renterIds = rentals.map(rental => rental.renter?.userId || rental.renter).filter(Boolean);
        
        for (const clothingId of clothingIds) {
          if (!clothingCards[clothingId]) {
            try {
              const cardResponse = await fetch(`http://localhost:8080/api/clothing-cards/${clothingId}`);
              if (cardResponse.ok) {
                const card = await cardResponse.json();
                setClothingCards(prev => ({ ...prev, [clothingId]: card }));
              }
            } catch (err) {
              console.error(`Error fetching clothing card ${clothingId}:`, err);
            }
          }
        }
        
        for (const renterId of renterIds) {
          if (!renters[renterId]) {
            try {
              const userResponse = await fetch(`http://localhost:8080/api/users/${renterId}`);
              if (userResponse.ok) {
                const user = await userResponse.json();
                setRenters(prev => ({ ...prev, [renterId]: user }));
              }
            } catch (err) {
              console.error(`Error fetching renter ${renterId}:`, err);
            }
          }
        }
      }
    } catch (err) {
      console.error("Error fetching returned rentals:", err);
    } finally {
      setLoadingReturnedRentals(false);
    }
  };

  const handleDeleteListing = async (listingId) => {
    if (!confirm("Are you sure you want to delete this listing?")) {
      return;
    }

    try {
      const res = await fetch(`http://localhost:8080/api/clothing-cards/${listingId}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        throw new Error(`Error ${res.status}: Failed to delete listing`);
      }

      // Remove the listing from state
      setListings(listings.filter(listing => listing.clothingId !== listingId));
    } catch (err) {
      setError(err.message || "Failed to delete listing");
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

  const getRentalStatusBadge = (returned) => {
    return returned ? 
      <span className="px-3 py-1 bg-gray-100 text-gray-800 rounded-full text-xs font-medium">Returned</span> :
      <span className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-xs font-medium">Active</span>;
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50">
        <Header />
        <div className="max-w-7xl mx-auto px-4 py-8">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-rose-500 mx-auto"></div>
            <p className="mt-4 text-rose-700">Loading your listings...</p>
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
              href="/home"
              className="inline-block mt-4 px-6 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition"
            >
              Return Home
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
        <div className="mb-8">
          <h1 className="text-4xl font-serif font-semibold text-rose-900 mb-2">
            Your Closet
          </h1>
          <p className="text-rose-700">
            Manage your clothing listings and track their rental status
          </p>
        </div>

        {/* Items Currently Being Rented Out */}
        <div className="mb-8">
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-2xl font-semibold text-rose-900 mb-4 flex items-center">
              <svg className="w-6 h-6 mr-3 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              Items Currently Being Rented Out
            </h2>
            
            {loadingRentals ? (
              <div className="text-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500 mx-auto"></div>
                <p className="mt-2 text-rose-700 text-sm">Loading rentals...</p>
              </div>
            ) : rentedOutItems.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-gray-500">No items are currently being rented out</p>
                <p className="text-gray-400 text-sm mt-2">When someone rents your items, they'll appear here</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {rentedOutItems.map((rental) => {
                  const clothingId = rental.clothingCard?.clothingId || (typeof rental.clothingCard === 'number' ? rental.clothingCard : null);
                  const clothingCard = rental.clothingCard?.clothingId ? rental.clothingCard : (clothingId ? clothingCards[clothingId] : null);
                  const renter = rental.renter?.userId ? rental.renter : (rental.renter ? renters[rental.renter] : null);
                  
                  return (
                    <Link
                      key={rental.rentalId}
                      href={`/rental/${rental.rentalId}`}
                      className="block border border-gray-200 rounded-lg p-4 hover:shadow-md transition-all hover:border-rose-300"
                    >
                      <div className="flex justify-between items-start mb-2">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-xs font-medium text-gray-500">Rental ID:</span>
                            <span className="text-xs font-semibold text-rose-600">#{rental.rentalId}</span>
                            {getRentalStatusBadge(rental.returned)}
                          </div>
                          <h3 className="text-lg font-semibold text-gray-900">
                            {clothingCard ? clothingCard.title : `Item #${clothingId || "N/A"}`}
                          </h3>
                          <p className="text-sm text-gray-600 mt-1">
                            Rented to: {renter?.userName || "Unknown"}
                          </p>
                        </div>
                      </div>
                      
                      {clothingCard && (
                        <div className="grid grid-cols-2 gap-3 mt-3 p-2 bg-gray-50 rounded text-sm">
                          <div>
                            <p className="text-xs font-medium text-gray-600">Rental Date</p>
                            <p className="text-gray-900">{formatDate(rental.rentalDate)}</p>
                          </div>
                          <div>
                            <p className="text-xs font-medium text-gray-600">Return Date</p>
                            <p className="text-gray-900">{formatDate(rental.returnDate)}</p>
                          </div>
                        </div>
                      )}
                      
                      <div className="flex items-center text-sm text-rose-600 font-medium mt-3">
                        View Rental Details
                        <svg className="w-4 h-4 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                        </svg>
                      </div>
                    </Link>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Rental History - Returned Items */}
        <div className="mb-8">
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-2xl font-semibold text-rose-900 mb-4 flex items-center">
              <svg className="w-6 h-6 mr-3 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Rental History
            </h2>
            
            {loadingReturnedRentals ? (
              <div className="text-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500 mx-auto"></div>
                <p className="mt-2 text-rose-700 text-sm">Loading rental history...</p>
              </div>
            ) : returnedRentals.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-gray-500">No rental history yet</p>
                <p className="text-gray-400 text-sm mt-2">Items you've rented out that have been returned will appear here</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {returnedRentals.map((rental) => {
                  const clothingId = rental.clothingCard?.clothingId || (typeof rental.clothingCard === 'number' ? rental.clothingCard : null);
                  const clothingCard = rental.clothingCard?.clothingId ? rental.clothingCard : (clothingId ? clothingCards[clothingId] : null);
                  const renter = rental.renter?.userId ? rental.renter : (rental.renter ? renters[rental.renter] : null);
                  
                  return (
                    <Link
                      key={rental.rentalId}
                      href={`/rental/${rental.rentalId}`}
                      className="block border border-gray-200 rounded-lg p-4 hover:shadow-md transition-all hover:border-rose-300"
                    >
                      <div className="flex justify-between items-start mb-2">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-xs font-medium text-gray-500">Rental ID:</span>
                            <span className="text-xs font-semibold text-rose-600">#{rental.rentalId}</span>
                            <span className="px-3 py-1 bg-gray-100 text-gray-800 rounded-full text-xs font-medium">Returned</span>
                          </div>
                          <h3 className="text-lg font-semibold text-gray-900">
                            {clothingCard ? clothingCard.title : `Item #${clothingId || "N/A"}`}
                          </h3>
                          <p className="text-sm text-gray-600 mt-1">
                            Rented to: {renter?.userName || "Unknown"}
                          </p>
                        </div>
                      </div>
                      
                      {clothingCard && (
                        <div className="grid grid-cols-2 gap-3 mt-3 p-2 bg-gray-50 rounded text-sm">
                          <div>
                            <p className="text-xs font-medium text-gray-600">Rental Date</p>
                            <p className="text-gray-900">{formatDate(rental.rentalDate)}</p>
                          </div>
                          <div>
                            <p className="text-xs font-medium text-gray-600">Returned Date</p>
                            <p className="text-gray-900">{formatDate(rental.returnDate)}</p>
                          </div>
                        </div>
                      )}
                      
                      <div className="flex items-center text-sm text-rose-600 font-medium mt-3">
                        View Rental Details
                        <svg className="w-4 h-4 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                        </svg>
                      </div>
                    </Link>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* All Your Listings */}
        <div className="mb-8">
          <h2 className="text-2xl font-semibold text-rose-900 mb-4 flex items-center">
            <svg className="w-6 h-6 mr-3 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
            All Your Listings
          </h2>

        {listings.length === 0 ? (
          <div className="text-center py-12">
            <div className="bg-white rounded-lg shadow-md p-8 max-w-md mx-auto">
              <div className="text-6xl mb-4">👗</div>
              <h3 className="text-xl font-semibold text-rose-900 mb-2">
                No listings yet
              </h3>
              <p className="text-rose-600 mb-6">
                Start sharing your wardrobe by adding your first listing!
              </p>
              <Link
                href="/add-listing"
                className="inline-block px-6 py-3 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition font-semibold"
              >
                Add Your First Listing
              </Link>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {listings.map((listing) => (
              <div key={listing.clothingId} className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow">
                <div className="aspect-w-16 aspect-h-12">
                  {listing.images && listing.images.length > 0 ? (
                    <img
                      src={`data:image/jpeg;base64,${listing.images[0].imageData}`}
                      alt={listing.title}
                      className="w-full h-48 object-cover"
                    />
                  ) : (
                    <div className="w-full h-48 bg-rose-100 flex items-center justify-center">
                      <span className="text-rose-400 text-4xl">👗</span>
                    </div>
                  )}
                </div>
                
                <div className="p-4">
                  <h3 className="text-lg font-semibold text-rose-900 mb-2 line-clamp-2">
                    {listing.title}
                  </h3>
                  
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-2xl font-bold text-rose-600">
                      ${listing.cost}
                    </span>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      listing.availability 
                        ? 'bg-green-100 text-green-800' 
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {listing.availability ? 'Available' : 'Rented'}
                    </span>
                  </div>

                  <p className="text-rose-600 text-sm mb-4 line-clamp-2">
                    {listing.description}
                  </p>

                  <div className="flex gap-2">
                    <Link
                      href={`/your-listings/${listing.clothingId}`}
                      className="flex-1 text-center px-3 py-2 bg-rose-100 text-rose-700 rounded-lg hover:bg-rose-200 transition text-sm font-medium"
                    >
                      Manage Listing
                    </Link>
                    <button
                      onClick={() => handleDeleteListing(listing.clothingId)}
                      className="px-3 py-2 bg-red-100 text-red-700 rounded-lg hover:bg-red-200 transition text-sm font-medium"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {listings.length > 0 && (
          <div className="mt-8 text-center">
            <Link
              href="/add-listing"
              className="inline-block px-6 py-3 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition font-semibold"
            >
              Add Another Listing
            </Link>
          </div>
        )}
        </div>
      </div>
    </div>
  );
}

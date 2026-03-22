"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Header from "../../../components/Header";
import ProfileImage from "../../../components/ProfileImage";
import Link from "next/link";

export default function UserProfilePage() {
  const params = useParams();
  const userId = params.id;

  const [user, setUser] = useState(null);
  const [userListings, setUserListings] = useState([]);
  const [userReviews, setUserReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchUserData();
  }, [userId]);

  const fetchUserData = async () => {
    try {
      setLoading(true);
      
      // Fetch user details
      const token = localStorage.getItem("token");
      const headers = {};
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
      const userRes = await fetch(`http://localhost:8080/api/profile/${userId}`, {
        headers
      });
      if (!userRes.ok) throw new Error(`Error ${userRes.status}: Failed to fetch user`);
      const userData = await userRes.json();
      setUser(userData);

      // Fetch user's listings
      const listingsRes = await fetch(`http://localhost:8080/api/clothing-cards/user/${userId}`);
      if (listingsRes.ok) {
        const listingsData = await listingsRes.json();
        setUserListings(listingsData);
      }

      // Fetch user reviews
      const reviewsRes = await fetch(`http://localhost:8080/api/user-reviews/user/${userId}`);
      if (reviewsRes.ok) {
        const reviewsData = await reviewsRes.json();
        setUserReviews(reviewsData);
      }

    } catch (err) {
      setError(err.message || "Failed to fetch user data");
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50">
        <Header />
        <div className="max-w-7xl mx-auto px-4 py-8">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-rose-500 mx-auto"></div>
            <p className="mt-4 text-rose-700">Loading user profile...</p>
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
              href="/dashboard"
              className="inline-block mt-4 px-6 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition"
            >
              Back to Browse
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50">
        <Header />
        <div className="max-w-7xl mx-auto px-4 py-8">
          <div className="text-center">
            <p className="text-rose-700">User not found</p>
            <Link
              href="/dashboard"
              className="inline-block mt-4 px-6 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition"
            >
              Back to Browse
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
        {/* User Profile Header */}
        <div className="bg-white rounded-xl shadow-lg p-8 mb-8">
          <div className="flex flex-col md:flex-row items-center md:items-start gap-6">
            {/* Profile Image */}
            <div className="flex-shrink-0">
              <ProfileImage 
                user={user} 
                userId={userId}
                size="w-24 h-24" 
                showUpload={false}
              />
            </div>

            {/* User Info */}
            <div className="flex-1 text-center md:text-left">
              <h1 className="text-3xl font-serif font-semibold text-rose-900 mb-2">
                {user.userName}
              </h1>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm mb-4">
                <p>
                  <span className="font-semibold text-rose-700">Email:</span> {user.email}
                </p>
                <p>
                  <span className="font-semibold text-rose-700">Rating:</span> {user.rating || 0}
                </p>
                <p>
                  <span className="font-semibold text-rose-700">Joined:</span>{" "}
                  {new Date(user.createdAt).toLocaleDateString()}
                </p>
                <p>
                  <span className="font-semibold text-rose-700">Listings:</span> {userListings.length}
                </p>
              </div>

              {user.bio && (
                <p className="text-gray-700 mb-4">
                  <span className="font-semibold text-rose-700">Bio:</span> {user.bio}
                </p>
              )}

              {user.address && (
                <p className="text-gray-700">
                  <span className="font-semibold text-rose-700">Location:</span> {user.address}
                </p>
              )}
            </div>
          </div>
        </div>

        {/* User's Listings */}
        <div className="bg-white rounded-xl shadow-lg p-6 mb-8">
          <h2 className="text-2xl font-semibold text-rose-900 mb-6 flex items-center">
            <svg className="w-6 h-6 mr-3 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
            {user.userName}'s Listings
          </h2>
          
          {userListings.length === 0 ? (
            <div className="text-center py-8">
              <div className="text-6xl mb-4">👗</div>
              <h3 className="text-lg font-semibold text-rose-800 mb-2">No listings yet</h3>
              <p className="text-rose-600">
                {user.userName} hasn't listed any items yet.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {userListings.map((listing) => (
                <Link
                  key={listing.clothingId}
                  href={`/clothingcard/${listing.clothingId}`}
                  className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow border border-rose-200"
                >
                  <div className="aspect-w-16 aspect-h-12">
                    {listing.images && listing.images.length > 0 ? (
                      <img
                        src={`data:image/jpeg;base64,${listing.images[0].imageData || listing.images[0].image}`}
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

                    <p className="text-rose-600 text-sm mb-2 line-clamp-2">
                      {listing.description}
                    </p>

                    <div className="text-sm text-gray-600">
                      <p>Size: {listing.size}</p>
                      {listing.brand && <p>Brand: {listing.brand}</p>}
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* User Reviews */}
        <div className="bg-white rounded-xl shadow-lg p-6">
          <h2 className="text-2xl font-semibold text-rose-900 mb-6 flex items-center">
            <svg className="w-6 h-6 mr-3 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
            </svg>
            Reviews for {user.userName}
          </h2>
          
          {userReviews.length === 0 ? (
            <div className="text-center py-8">
              <div className="text-6xl mb-4">⭐</div>
              <h3 className="text-lg font-semibold text-rose-800 mb-2">No reviews yet</h3>
              <p className="text-rose-600">
                {user.userName} hasn't received any reviews yet.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {userReviews.map((review) => (
                <div key={review.reviewId} className="border border-rose-200 rounded-lg p-4">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center">
                      <div className="flex text-yellow-400">
                        {[...Array(5)].map((_, i) => (
                          <svg
                            key={i}
                            className={`w-4 h-4 ${i < review.rating ? 'text-yellow-400' : 'text-gray-300'}`}
                            fill="currentColor"
                            viewBox="0 0 20 20"
                          >
                            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                          </svg>
                        ))}
                      </div>
                      <span className="ml-2 text-sm text-rose-600">
                        {review.rating}/5
                      </span>
                    </div>
                    <span className="text-sm text-gray-500">
                      {new Date(review.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                  
                  {review.reviewText && (
                    <p className="text-gray-700 mb-2">{review.reviewText}</p>
                  )}
                  
                  <div className="text-sm text-rose-600">
                    <span className="font-semibold">Reviewed by:</span> {review.reviewer?.userName || 'Anonymous'}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

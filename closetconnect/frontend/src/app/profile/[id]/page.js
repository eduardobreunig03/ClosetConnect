"use client";

import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import Header from "../../../components/Header";
import ProfileImage from "../../../components/ProfileImage";
import SingleClothingCard from "../../../components/SingleClothingCard";

export default function ProfilePage() {
  const params = useParams();
  const userId = params.id;
  const router = useRouter();

  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [token, setToken] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [bio, setBio] = useState("");
  const [address, setAddress] = useState("");
  const [userReviews, setUserReviews] = useState([]);
  const [reviewsLoading, setReviewsLoading] = useState(false);
  const [wishlist, setWishlist] = useState([]);

  useEffect(() => {
    const savedToken = localStorage.getItem("token");
    setToken(savedToken);

    const fetchUser = async () => {
      try {
        const token = localStorage.getItem("token");
        const headers = {};
        if (token) {
          headers.Authorization = `Bearer ${token}`;
        }
        const res = await fetch(`http://localhost:8080/api/profile/${userId}`, {
          headers
        });
        if (!res.ok) throw new Error(`Error ${res.status}`);
        const data = await res.json();
        console.log('Fetched user data:', data);
        console.log('User ID from URL params:', userId);
        console.log('User ID from data:', data.userId);
        setUser(data);
        setBio(data.bio);
        setAddress(data.address);
      } catch (err) {
        setError(err.message || "Failed to fetch user");
      } finally {
        setLoading(false);
      }
    };

    // Load wishlist from localStorage
    const savedWishlist = JSON.parse(localStorage.getItem('wishlist') || '[]');
    setWishlist(savedWishlist);

    fetchUser();
    fetchUserReviews();
  }, [userId]);

  const fetchUserReviews = async () => {
    try {
      setReviewsLoading(true);
      const res = await fetch(`http://localhost:8080/api/user-reviews/user/${userId}`);
      if (res.ok) {
        const reviews = await res.json();
        setUserReviews(reviews);
      }
    } catch (err) {
      console.error("Failed to fetch user reviews:", err);
    } finally {
      setReviewsLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      const res = await fetch(`http://localhost:8080/api/profile/${userId}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ bio, address }),
      });
      if (!res.ok) throw new Error(`Error ${res.status}`);
      const updatedUser = await res.json();
      setUser(updatedUser);
      setIsEditing(false);
    } catch (err) {
      setError(err.message || "Failed to update profile");
    }
  };

  if (loading)
    return <p className="text-center mt-10 text-rose-900">Loading…</p>;
  if (error) return <p className="text-center mt-10 text-red-500">{error}</p>;
  if (!user)
    return <p className="text-center mt-10 text-rose-900">No user found</p>;

  return (
    <main className="bg-pink-50 text-rose-900 min-h-screen">
      <Header />

      <section className="py-12 px-6 flex justify-center relative">
        <div className="bg-white rounded-xl shadow-lg p-8 max-w-xl w-full text-center space-y-4 relative">
          {/* Edit Button */}
          {token && !isEditing && (
            <button
              className="absolute top-4 left-4 bg-rose-500 text-white px-3 py-1 rounded hover:bg-rose-600 transition"
              onClick={() => setIsEditing(true)}
            >
              Edit
            </button>
          )}

          {isEditing && (
            <div className="absolute top-4 left-4 flex space-x-2">
              <button
                className="bg-rose-500 text-white px-3 py-1 rounded hover:bg-rose-600 transition"
                onClick={handleSave}
              >
                Save
              </button>
              <button
                className="bg-gray-300 text-gray-700 px-3 py-1 rounded hover:bg-gray-400 transition"
                onClick={() => {
                  setIsEditing(false);
                  setBio(user.bio);
                  setAddress(user.address);
                }}
              >
                Cancel
              </button>
            </div>
          )}

          <div className="flex justify-center mb-4">
            <ProfileImage 
              user={user} 
              userId={userId}
              size="w-32 h-32" 
              showUpload={isEditing}
              onImageUpload={async () => {
                // Refresh user data after image upload
                const token = localStorage.getItem("token");
                const headers = {};
                if (token) {
                  headers.Authorization = `Bearer ${token}`;
                }
                const userRes = await fetch(`http://localhost:8080/api/profile/${userId}`, {
                  headers
                });
                if (userRes.ok) {
                  const updatedUser = await userRes.json();
                  setUser(updatedUser);
                }
              }}
            />
          </div>

          <h2 className="text-3xl font-serif font-semibold">
            {user.userName}
          </h2>

          {/* Editable fields */}
          {isEditing ? (
            <div className="space-y-2 mt-4 text-left">
              <label className="block">
                <strong>Bio:</strong>
                <textarea
                  value={bio}
                  onChange={(e) => setBio(e.target.value)}
                  className="w-full border rounded p-2 mt-1"
                />
              </label>
              <label className="block">
                <strong>Address:</strong>
                <input
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className="w-full border rounded p-2 mt-1"
                />
              </label>
            </div>
          ) : (
            <div className="space-y-2 mt-4 text-left">
              <p>
                <strong>Bio:</strong> {user.bio}
              </p>
              <p>
                <strong>Address:</strong> {user.address}
              </p>
            </div>
          )}

          <div className="space-y-2 mt-4 text-left">
            <p>
              <strong>Email:</strong> {user.email}
            </p>
            <p>
              <strong>Rating:</strong> {user.rating}
            </p>
            <p>
              <strong>Joined:</strong>{" "}
              {new Date(user.createdAt).toLocaleDateString()}
            </p>
          </div>
        </div>
      </section>

      {/* User Reviews Section */}
      <section className="py-8 px-6">
        <div className="max-w-4xl mx-auto">
          <div className="bg-white rounded-xl shadow-lg p-6">
            <h3 className="text-2xl font-semibold text-rose-900 mb-6 flex items-center">
              <svg className="w-6 h-6 mr-3 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
              </svg>
              User Reviews
            </h3>
            
            {reviewsLoading ? (
              <div className="text-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500 mx-auto"></div>
                <p className="mt-2 text-rose-600">Loading reviews...</p>
              </div>
            ) : userReviews.length === 0 ? (
              <div className="text-center py-8">
                <div className="text-6xl mb-4">⭐</div>
                <h4 className="text-lg font-semibold text-rose-800 mb-2">No reviews yet</h4>
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
      </section>

      {/* Wishlist Section */}
      <section className="py-8 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="mb-6">
            <h2 className="text-2xl font-bold text-rose-900 mb-2">My Wishlist</h2>
            <p className="text-gray-600">
              {wishlist.length} {wishlist.length === 1 ? 'item' : 'items'} saved
            </p>
          </div>

          {wishlist.length === 0 ? (
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-12 text-center">
              <div className="max-w-md mx-auto">
                <div className="w-16 h-16 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center">
                  <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                </div>
                <h3 className="text-lg font-medium text-gray-900 mb-2">Your wishlist is empty</h3>
                <p className="text-gray-500 mb-6">
                  Start exploring and add items you love to your wishlist!
                </p>
                <button
                  onClick={() => router.push('/dashboard')}
                  className="px-6 py-3 bg-rose-600 text-white rounded-lg hover:bg-rose-700 transition-colors font-medium"
                >
                  Browse Items
                </button>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {wishlist.map((item) => {
                const clothingId = item.clothingId ?? item.id;
                return (
                  <div key={clothingId} className="relative">
                    <div 
                      onClick={() => router.push(`/clothingcard/${clothingId}`)}
                      className="cursor-pointer"
                    >
                      <SingleClothingCard 
                        clothingCard={item} 
                        currentUserId={userId}
                        onDelete={() => {
                          const updatedWishlist = wishlist.filter(wishlistItem => 
                            wishlistItem.clothingId !== clothingId && wishlistItem.id !== clothingId
                          );
                          setWishlist(updatedWishlist);
                          localStorage.setItem('wishlist', JSON.stringify(updatedWishlist));
                        }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </section>
    </main>
  );
}

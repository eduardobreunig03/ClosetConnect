"use client";

import { useState, useEffect, use, useMemo } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Header from "../../../components/Header";
import ProfileImage from "../../../components/ProfileImage";
import ReviewSection from "../../../components/ReviewSection";
import Map from "../../../components/Map";
import { ChevronLeftIcon, ChevronRightIcon } from "@heroicons/react/24/solid";

export default function ClothingCardPage({ params }) {
  const { id } = use(params);
  const router = useRouter();
  const [clothingCard, setClothingCard] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [hasRequested, setHasRequested] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [coords, setCoords] = useState(null);
  const [locationError, setLocationError] = useState(false);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  useEffect(() => {
    const getCurrentUser = () => {
      try {
        const token = localStorage.getItem("token");
        const userId = localStorage.getItem("userId");

        if (!token || !userId) {
          setLoading(false);
          return;
        }

        const userData = {
          id: parseInt(userId),
          token: token,
        };

        setCurrentUser(userData);
      } catch (err) {
        console.error("Error getting current user:", err);
      }
    };
    const fetchCoords = async () => {
      if (!clothingCard?.locationOfClothing) return;

      try {
        const address = encodeURIComponent(clothingCard.locationOfClothing);
        const res = await fetch(
          `https://maps.googleapis.com/maps/api/geocode/json?address=${address}&key=${process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY}`
        );

        const data = await res.json();
        if (data.status === "OK") {
          const location = data.results[0].geometry.location;
          setCoords(location);
        } else {
          console.error("Geocoding failed:", data.status);
          setLocationError(true);
        }
      } catch (err) {
        console.error("Error fetching coordinates:", err);
        setLocationError(true);
      }
    };
    fetchCoords();

    getCurrentUser();
  }, [clothingCard]);

  useEffect(() => {
    if (id) {
      fetchClothingCard();
    }
  }, [id]);

  useEffect(() => {
    if (currentUser && clothingCard) {
      checkExistingRequest();
    }
  }, [currentUser, clothingCard]);

  const fetchClothingCard = async () => {
    try {
      const res = await fetch(
        `http://localhost:8080/api/clothing-cards/${id}`,
        {
          cache: "no-store",
        }
      );

      if (!res.ok) {
        throw new Error("Failed to fetch clothing card");
      }

      const data = await res.json();
      setClothingCard(data);
      // Reset image index when clothing card changes
      setCurrentImageIndex(0);
    } catch (err) {
      console.error("Error fetching clothing card:", err);
      setError("Failed to load clothing card");
    } finally {
      setLoading(false);
    }
  };

  // Process all images for the clothing card
  const allImages = useMemo(() => {
    if (clothingCard?.images && Array.isArray(clothingCard.images) && clothingCard.images.length > 0) {
      return clothingCard.images.map(img => {
        const data = img?.imageData || img?.image;
        if (typeof data === "string" && data.length > 0)
          return `data:image/jpeg;base64,${data}`;
        return null;
      }).filter(Boolean);
    }
    return [];
  }, [clothingCard?.images]);

  // Start with cover image if available
  useEffect(() => {
    if (clothingCard?.coverImageId && Array.isArray(clothingCard.images)) {
      const coverIndex = clothingCard.images.findIndex(
        (img) => img.imageId === clothingCard.coverImageId
      );
      if (coverIndex !== -1) {
        setCurrentImageIndex(coverIndex);
      }
    }
  }, [clothingCard?.coverImageId, clothingCard?.images]);

  const currentImageSrc = allImages[currentImageIndex] || (allImages.length > 0 ? allImages[0] : null);
  const hasMultipleImages = allImages.length > 1;

  const nextImage = () => {
    setCurrentImageIndex((prev) => (prev + 1) % allImages.length);
  };

  const prevImage = () => {
    setCurrentImageIndex((prev) => (prev - 1 + allImages.length) % allImages.length);
  };

  const checkExistingRequest = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/requests/check/${id}/${currentUser.id}`
      );
      if (response.ok) {
        const data = await response.json();
        setHasRequested(data.hasRequested);
      }
    } catch (err) {
      console.error("Error checking existing request:", err);
    }
  };

  if (loading) {
    return (
      <>
        <Header />
        <main className="min-h-screen bg-gray-50 flex items-center justify-center">
          <p className="text-gray-600">Loading...</p>
        </main>
      </>
    );
  }

  if (error || !clothingCard) {
    return (
      <>
        <Header />
        <main className="min-h-screen bg-gray-50 flex items-center justify-center">
          <div className="text-center">
            <p className="text-red-600 mb-4">
              {error || "Clothing card not found"}
            </p>
            <button
              onClick={() => router.push("/dashboard")}
              className="bg-rose-600 text-white px-6 py-2 rounded-lg hover:bg-rose-700 transition-colors"
            >
              Back to Dashboard
            </button>
          </div>
        </main>
      </>
    );
  }

  return (
    <>
      <Header />
      <main className="min-h-screen bg-gray-50 p-6">
        <div className="max-w-6xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-8 mb-8">
            {/* Left: Image Gallery */}
            <div className="w-full space-y-4">
              {/* Main Image Display */}
              <div className="relative w-full h-96 lg:h-[500px] flex items-center justify-center bg-gray-100 rounded-2xl overflow-hidden group">
                {currentImageSrc ? (
                  <>
                    <img
                      src={currentImageSrc}
                      alt={clothingCard.description || clothingCard.title}
                      className="w-full h-full object-cover transition-opacity duration-300"
                    />
                    
                    {/* Navigation arrows (only show if multiple images) */}
                    {hasMultipleImages && (
                      <>
                        <button
                          onClick={prevImage}
                          className="absolute left-4 top-1/2 -translate-y-1/2 bg-white/90 hover:bg-white text-gray-800 p-3 rounded-full shadow-lg opacity-0 group-hover:opacity-100 transition-opacity z-10"
                          aria-label="Previous image"
                        >
                          <ChevronLeftIcon className="h-6 w-6" />
                        </button>
                        <button
                          onClick={nextImage}
                          className="absolute right-4 top-1/2 -translate-y-1/2 bg-white/90 hover:bg-white text-gray-800 p-3 rounded-full shadow-lg opacity-0 group-hover:opacity-100 transition-opacity z-10"
                          aria-label="Next image"
                        >
                          <ChevronRightIcon className="h-6 w-6" />
                        </button>
                        
                        {/* Image counter */}
                        <div className="absolute top-4 right-4 bg-black/60 text-white px-3 py-1 rounded-full text-sm">
                          {currentImageIndex + 1} / {allImages.length}
                        </div>
                      </>
                    )}
                  </>
                ) : (
                  <div className="text-gray-500 text-center">
                    <div className="text-6xl mb-4">👗</div>
                    <p>No Image Available</p>
                  </div>
                )}
              </div>

              {/* Thumbnail Navigation (only show if multiple images) */}
              {hasMultipleImages && allImages.length > 1 && (
                <div className="flex gap-2 overflow-x-auto pb-2">
                  {allImages.map((imgSrc, index) => (
                    <button
                      key={index}
                      onClick={() => setCurrentImageIndex(index)}
                      className={`flex-shrink-0 w-20 h-20 rounded-lg overflow-hidden border-2 transition-all ${
                        index === currentImageIndex
                          ? "border-rose-600 ring-2 ring-rose-300"
                          : "border-gray-300 hover:border-rose-400"
                      }`}
                    >
                      <img
                        src={imgSrc}
                        alt={`Thumbnail ${index + 1}`}
                        className="w-full h-full object-cover"
                      />
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Right: Info + CTA */}
            <div className="p-6 flex flex-col justify-between space-y-6 bg-gradient-to-b from-pink-100 via-rose-200 to-pink-100 text-rose-900 rounded-2xl">
              <div className="space-y-4">
                <h2 className="text-2xl font-bold">{clothingCard.title}</h2>
                <p className="text-gray-700">{clothingCard.description}</p>
                {/* Owner Info */}
                <div className="bg-white/50 rounded-lg p-4 mb-4">
                  <div className="flex items-center gap-3">
                    <ProfileImage
                      user={clothingCard.owner}
                      userId={clothingCard.owner?.userId}
                      size="w-12 h-12"
                      showUpload={false}
                    />
                    <div className="flex-1">
                      <Link
                        href={`/user-profile/${clothingCard.owner?.userId}`}
                        className="text-lg font-bold text-gray-900 hover:text-rose-600 transition-colors"
                      >
                        {clothingCard.owner?.userName}
                      </Link>
                      <div className="flex items-center gap-2 mt-1">
                        <div className="flex text-red-500">
                          {[...Array(5)].map((_, i) => (
                            <svg
                              key={i}
                              className={`w-3 h-3 ${
                                i < (clothingCard.owner?.rating || 0)
                                  ? "text-red-500"
                                  : "text-gray-300"
                              }`}
                              fill="currentColor"
                              viewBox="0 0 20 20"
                            >
                              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                            </svg>
                          ))}
                        </div>
                        <span className="text-sm text-gray-600">
                          ({clothingCard.owner?.rating || 0})
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 text-sm">
                  <p>
                    <span className="font-semibold">Size:</span>{" "}
                    {clothingCard.size}
                  </p>
                  <p>
                    <span className="font-semibold">Available:</span>{" "}
                    {clothingCard.availability ? "Yes" : "No"}
                  </p>
                  <p>
                    <span className="font-semibold">Brand:</span>{" "}
                    {clothingCard.brand || "N/A"}
                  </p>
                </div>
                <p className="text-sm">
                  <span className="font-semibold">Posted:</span>{" "}
                  {new Date(clothingCard.datePosted).toLocaleDateString()}
                </p>
                {clothingCard.locationOfClothing && (
                  <>
                    <p className="text-sm">
                      <span className="font-semibold">Location:</span>{" "}
                      {clothingCard.locationOfClothing}
                    </p>

                    {/* Map Placeholder */}
                    <div className="mt-4">
                      {coords ? (
                        <Map center={coords} />
                      ) : locationError ? (
                        <p className="text-red-600 font-medium">
                          Could not find location
                        </p>
                      ) : (
                        <p>Loading map...</p>
                      )}
                    </div>
                  </>
                )}
              </div>

              <div className="space-y-4">
                <div className="text-center">
                  <p className="text-3xl font-bold text-rose-600">
                    ${clothingCard.cost}
                  </p>
                  <p className="text-rose-800">
                    + ${clothingCard.deposit} deposit
                  </p>
                </div>

                {/* Request Status */}
                {currentUser && (
                  <div className="mb-4">
                    {hasRequested ? (
                      <div className="p-3 bg-yellow-100 border border-yellow-300 rounded-lg">
                        <p className="text-yellow-800 text-sm font-medium">
                          ✓ You have already requested this item
                        </p>
                        <button
                          onClick={() => router.push("/requests")}
                          className="text-yellow-800 text-sm underline hover:text-yellow-900 mt-1"
                        >
                          View your requests
                        </button>
                      </div>
                    ) : currentUser.id === clothingCard.owner?.userId ? (
                      <button
                        onClick={() =>
                          router.push(
                            `/edit-listing/${clothingCard.clothingId}`
                          )
                        }
                        className="w-full py-3 bg-rose-400 text-white rounded-full font-semibold hover:bg-rose-500 transition-colors"
                      >
                        Edit Item
                      </button>
                    ) : (
                      <a
                        href={`/rent-request/${id}`}
                        className="w-full py-3 bg-rose-600 text-white rounded-full font-semibold hover:bg-rose-700 transition-colors text-center block"
                      >
                        Request to Rent Now
                      </a>
                    )}
                  </div>
                )}

                {!currentUser && (
                  <div className="space-y-2">
                    <p className="text-sm text-rose-800">
                      Please log in to request this item
                    </p>
                    <a
                      href="/login"
                      className="w-full py-3 bg-rose-600 text-white rounded-full font-semibold hover:bg-rose-700 transition-colors text-center block"
                    >
                      Log In to Request
                    </a>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Reviews Section */}
          <div className="max-w-4xl mx-auto">
            <ReviewSection
              clothingId={clothingCard.clothingId}
              clothingOwnerId={clothingCard.owner?.userId}
            />
          </div>
        </div>
      </main>
    </>
  );
}

// Component for displaying a single clothing card
import { TrashIcon, HeartIcon, ChevronLeftIcon, ChevronRightIcon } from "@heroicons/react/24/solid";
import { HeartIcon as HeartOutlineIcon } from "@heroicons/react/24/outline";
import { useRouter } from "next/navigation";
import { useState, useEffect } from "react";

export default function SingleClothingCard({
  clothingCard,
  currentUserId,
  onDelete,
}) {
  const router = useRouter();
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  
  const clothingId = clothingCard.clothingId ?? clothingCard.id;
  
  // Check if item is in wishlist on component mount
  useEffect(() => {
    const wishlist = JSON.parse(localStorage.getItem('wishlist') || '[]');
    setIsWishlisted(wishlist.some(item => 
      item.clothingId === clothingId || item.id === clothingId
    ));
  }, [clothingId]);

  // Reset image index when clothingCard changes
  useEffect(() => {
    setCurrentImageIndex(0);
  }, [clothingId]);

  console.log(currentUserId);
  const isOwner = String(clothingCard.owner.userId) === String(currentUserId);
  console.log(isOwner);

  // Process all images
  const allImages = (() => {
    if (Array.isArray(clothingCard.images) && clothingCard.images.length > 0) {
      return clothingCard.images.map(img => {
        const data = img?.imageData || img?.image;
        if (typeof data === "string" && data.length > 0)
          return `data:image/jpeg;base64,${data}`;
        if (Array.isArray(data)) {
          try {
            const u8 = new Uint8Array(data);
            const blob = new Blob([u8], { type: "image/jpeg" });
            return URL.createObjectURL(blob);
          } catch {}
        }
        return null;
      }).filter(Boolean);
    }
    return ["/silkdress.png"];
  })();

  // Start with cover image if available
  useEffect(() => {
    if (clothingCard.coverImageId && Array.isArray(clothingCard.images)) {
      const coverIndex = clothingCard.images.findIndex(
        (img) => img.imageId === clothingCard.coverImageId
      );
      if (coverIndex !== -1) {
        setCurrentImageIndex(coverIndex);
      }
    }
  }, [clothingCard.coverImageId, clothingCard.images]);

  const imgSrc = allImages[currentImageIndex] || allImages[0] || "/silkdress.png";
  const hasMultipleImages = allImages.length > 1;

  const nextImage = (e) => {
    e.stopPropagation();
    e.preventDefault();
    setCurrentImageIndex((prev) => (prev + 1) % allImages.length);
  };
 
  const prevImage = (e) => {
    e.stopPropagation();
    e.preventDefault();
    setCurrentImageIndex((prev) => (prev - 1 + allImages.length) % allImages.length);
  };

  function handleDelete(id, event) {
    event.stopPropagation();
    event.preventDefault();
    if (confirm("Are you sure you want to delete this item?")) {
      fetch(`http://localhost:8080/api/clothing-cards/${id}`, {
        method: "DELETE",
      })
        .then((res) => {
          if (!res.ok) throw new Error("Failed to delete");
          alert("Item deleted successfully");
          onDelete();
        })
        .catch((err) => {
          console.error(err);
          alert("Error deleting item");
        });
    }
  }

  function handleWishlistToggle(event) {
    event.stopPropagation();
    event.preventDefault();
    
    const wishlist = JSON.parse(localStorage.getItem('wishlist') || '[]');
    
    if (isWishlisted) {
      // Remove from wishlist
      const updatedWishlist = wishlist.filter(item => 
        item.clothingId !== clothingId && item.id !== clothingId
      );
      localStorage.setItem('wishlist', JSON.stringify(updatedWishlist));
      setIsWishlisted(false);
    } else {
      // Add to wishlist
      const updatedWishlist = [...wishlist, clothingCard];
      localStorage.setItem('wishlist', JSON.stringify(updatedWishlist));
      setIsWishlisted(true);
    }
  }
  return (
    <div className="relative max-w-sm bg-white rounded-xl shadow-md overflow-hidden border border-rose-200">
      <div className="relative w-full h-64 overflow-hidden group">
        <img
          src={imgSrc}
          alt={clothingCard.tag || clothingCard.brand || "Clothing item"}
          className="w-full h-64 object-cover transition-opacity duration-300"
        />
        
        {/* Image navigation arrows (only show if multiple images) */}
        {hasMultipleImages && (
          <>
            <button
              onClick={prevImage}
              className="absolute left-2 top-1/2 -translate-y-1/2 bg-white/80 hover:bg-white text-gray-800 p-1.5 rounded-full shadow-md opacity-0 group-hover:opacity-100 transition-opacity"
              aria-label="Previous image"
            >
              <ChevronLeftIcon className="h-5 w-5" />
            </button>
            <button
              onClick={nextImage}
              className="absolute right-2 top-1/2 -translate-y-1/2 bg-white/80 hover:bg-white text-gray-800 p-1.5 rounded-full shadow-md opacity-0 group-hover:opacity-100 transition-opacity"
              aria-label="Next image"
            >
              <ChevronRightIcon className="h-5 w-5" />
            </button>
            
            {/* Image indicator dots */}
            <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1.5">
              {allImages.map((_, index) => (
                <button
                  key={index}
                  onClick={(e) => {
                    e.stopPropagation();
                    e.preventDefault();
                    setCurrentImageIndex(index);
                  }}
                  className={`h-2 rounded-full transition-all ${
                    index === currentImageIndex
                      ? "w-6 bg-white"
                      : "w-2 bg-white/60 hover:bg-white/80"
                  }`}
                  aria-label={`Go to image ${index + 1}`}
                />
              ))}
            </div>
          </>
        )}
      </div>
      
      {/* Action buttons */}
      <div className="absolute top-2 right-2 flex gap-2">
        {/* Wishlist button */}
        <button
          onClick={handleWishlistToggle}
          className={`p-1 rounded-full transition ${
            isWishlisted
              ? "bg-red-500 text-white hover:bg-red-600"
              : "bg-white/90 text-gray-600 hover:bg-white hover:text-red-500"
          }`}
          aria-label={isWishlisted ? "Remove from wishlist" : "Add to wishlist"}
        >
          {isWishlisted ? (
            <HeartIcon className="h-4 w-4" />
          ) : (
            <HeartOutlineIcon className="h-4 w-4" />
          )}
        </button>

        {/* Delete button (only for owners) */}
        {isOwner && (
          <button
            onClick={(e) => handleDelete(clothingCard.clothingId, e)}
            className="bg-red-600 text-white p-1 rounded-full hover:bg-red-700 transition"
            aria-label="Delete"
          >
            <TrashIcon className="h-4 w-4" />
          </button>
        )}
      </div>

      <div className="p-4">
        <h2 className="text-xl font-serif font-semibold text-rose-800">
          {clothingCard.title ||
            clothingCard.brand ||
            clothingCard.tag ||
            `Item #${clothingCard.clothingId}`}
        </h2>
        <p className="text-sm text-gray-600 mb-2">
          {clothingCard.datePosted
            ? `Posted: ${new Date(
                clothingCard.datePosted
              ).toLocaleDateString()}`
            : null}
        </p>
        <p className="text-lg text-rose-600 font-medium">
          Rental Price: ${clothingCard.cost}/day
        </p>
        <p className="text-sm text-gray-700 mt-2">Size: {clothingCard.size}</p>

        {/* 👤 Owner Info */}
        {clothingCard.owner?.userName && (
          <p className="text-sm text-gray-700 mt-2">
            <span className="font-semibold">Owner:</span>{" "}
            {clothingCard.owner.userName}
          </p>
        )}

        <div className="mt-3">
          <span
            className={`px-2 py-1 text-xs rounded-full ${
              clothingCard.availability
                ? "bg-green-100 text-green-800"
                : "bg-red-100 text-red-800"
            }`}
          >
            {clothingCard.availability ? "Available" : "Unavailable"}
          </span>
        </div>
      </div>
    </div>
  );
}

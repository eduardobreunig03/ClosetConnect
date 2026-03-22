"use client";

import { useState } from "react";

export default function ProfileImage({ 
  user, 
  size = "w-12 h-12", 
  showUpload = false, 
  onImageUpload,
  className = "",
  userId = null // Allow passing userId directly as fallback
}) {
  const [uploadingImage, setUploadingImage] = useState(false);
  const [imageError, setImageError] = useState("");

  const handleImageUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      setImageError("Please select an image file");
      return;
    }

    // Validate file size (5MB max)
    if (file.size > 5 * 1024 * 1024) {
      setImageError("File size must be less than 5MB");
      return;
    }

    setUploadingImage(true);
    setImageError("");

    try {
      const formData = new FormData();
      formData.append('file', file);

      const actualUserId = userId || user.userId;
      const url = `http://localhost:8080/api/profile/${actualUserId}/profile-image`;
      console.log('Uploading to URL:', url);
      console.log('User object:', user);
      console.log('User ID from user object:', user.userId);
      console.log('User ID from prop:', userId);
      console.log('Actual User ID being used:', actualUserId);

      const res = await fetch(url, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
        body: formData,
      });

      console.log('Response status:', res.status);
      console.log('Response ok:', res.ok);

      if (!res.ok) {
        const errorData = await res.json();
        console.log('Error data:', errorData);
        throw new Error(errorData.error || `Error ${res.status}`);
      }

      // Call the parent's onImageUpload callback if provided
      if (onImageUpload) {
        await onImageUpload();
      }
    } catch (err) {
      console.error('Upload error:', err);
      setImageError(err.message || "Failed to upload image");
    } finally {
      setUploadingImage(false);
    }
  };

  return (
    <div className={`relative ${className}`}>
      {user.profileImage ? (
        <img
          src={`data:image/png;base64,${user.profileImage}`}
          alt={`${user.userName}'s profile`}
          className={`${size} rounded-full object-cover`}
        />
      ) : (
        <div className={`${size} rounded-full bg-rose-200 flex items-center justify-center text-lg font-bold text-rose-700`}>
          {user.userName ? user.userName.split(' ').map(name => name[0]).join('').toUpperCase().slice(0, 2) : '?'}
        </div>
      )}
      
      {/* Upload button */}
      {showUpload && (
        <div className="absolute -bottom-1 -right-1">
          <label className="bg-rose-500 text-white p-1 rounded-full cursor-pointer hover:bg-rose-600 transition shadow-lg">
            <input
              type="file"
              accept="image/*"
              onChange={handleImageUpload}
              disabled={uploadingImage}
              className="hidden"
            />
            {uploadingImage ? (
              <span className="text-xs">⏳</span>
            ) : (
              <span className="text-xs">📷</span>
            )}
          </label>
        </div>
      )}

      {/* Image upload error */}
      {imageError && (
        <div className="absolute top-full left-0 mt-1 text-red-500 text-xs whitespace-nowrap">
          {imageError}
        </div>
      )}
    </div>
  );
}

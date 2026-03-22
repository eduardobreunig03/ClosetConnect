"use client";

import ReviewList from "./ReviewList";

export default function ReviewSection({ clothingId, clothingOwnerId }) {
  return (
    <div className="space-y-6">
      <ReviewList clothingId={clothingId} />
    </div>
  );
}

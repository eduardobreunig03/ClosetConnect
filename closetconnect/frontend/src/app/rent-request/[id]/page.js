"use client";

import { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import Header from "../../../components/Header";

export default function RentRequestPage({ params }) {
  const { id } = use(params);
  const router = useRouter();
  const [formData, setFormData] = useState({
    startDate: "",
    endDate: "",
    message: "",
    contactInfo: ""
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [clothingCard, setClothingCard] = useState(null);

  useEffect(() => {
    const getCurrentUser = () => {
      try {
        const token = localStorage.getItem("token");
        const userId = localStorage.getItem("userId");
 
        console.log('Token:', token);
        console.log('User ID in pageee:', userId);

        if (!token || !userId) {
          setError("Please log in to make a rent request");
          setLoading(false);
          return;
        }

        // Create a simple user object with the ID from localStorage
        const userData = {
          id: parseInt(userId),
          token: token
        };
        
        setCurrentUser(userData);
      } catch (err) {
        setError("Please log in to make a rent request");
      } finally {
        setLoading(false);
      }
    };

    const fetchClothingCard = async () => {
      try {
        const clothingCardId = id;
        console.log('Clothing Card ID:', clothingCardId);
        
        const res = await fetch(`http://localhost:8080/api/clothing-cards/${clothingCardId}`, {
          cache: "no-store",
        });

        if (!res.ok) {
          throw new Error("Failed to fetch clothing card");
        }

        const cardData = await res.json();
      
        setClothingCard(cardData);
      } catch (err) {
        console.error('Error fetching clothing card:', err);
        setError("Failed to load clothing card details");
      }
    };

    getCurrentUser();
    fetchClothingCard();
    console.log("CCCCC",clothingCard);
  }, [id]);

  const handleChange = (e) => {
    const newFormData = {
      ...formData,
      [e.target.name]: e.target.value
    };
    
    // Validate date range when dates are changed
    if ((e.target.name === "startDate" || e.target.name === "endDate") && 
        newFormData.startDate && newFormData.endDate) {
      const startDate = new Date(newFormData.startDate);
      const endDate = new Date(newFormData.endDate);
      
      if (endDate < startDate) {
        setError("End date must be after start date");
      } else if (endDate.toDateString() === startDate.toDateString()) {
        // Allow same date (rental for same day)
        setError("");
      } else {
        setError(""); // Clear error if dates are valid
      }
    }
    
    setFormData(newFormData);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError("");

    // Validate date range before submission
    if (formData.startDate && formData.endDate) {
      const startDate = new Date(formData.startDate);
      const endDate = new Date(formData.endDate);
      
      if (endDate < startDate) {
        setError("End date must be after or equal to start date");
        setIsSubmitting(false);
        return;
      }
    }

    // Ensure both dates are provided and valid
    const trimmedStartDate = formData.startDate ? formData.startDate.trim() : "";
    const trimmedEndDate = formData.endDate ? formData.endDate.trim() : "";
    
    if (!trimmedStartDate) {
      setError("Start date is required");
      setIsSubmitting(false);
      return;
    }
    
    if (!trimmedEndDate) {
      setError("End date is required");
      setIsSubmitting(false);
      return;
    }

    // Validate date format
    const startDateObj = new Date(trimmedStartDate);
    const endDateObj = new Date(trimmedEndDate);
    
    if (isNaN(startDateObj.getTime())) {
      setError("Invalid start date format");
      setIsSubmitting(false);
      return;
    }
    
    if (isNaN(endDateObj.getTime())) {
      setError("Invalid end date format");
      setIsSubmitting(false);
      return;
    }
    
    // Validate date range again
    if (endDateObj < startDateObj) {
      setError("End date must be after or equal to start date");
      setIsSubmitting(false);
      return;
    }

    // Prepare the request body with properly formatted dates
    const requestBody = {
      clothingId: parseInt(id),
      fromUserId: currentUser.id,
      toUserId: clothingCard.owner.userId,
      startDate: trimmedStartDate + "T00:00:00",
      endDate: trimmedEndDate + "T23:59:59",
      requesterContactInfo: formData.contactInfo,
      commentsToOwner: formData.message
    };
    
    console.log("Sending request with body:", requestBody);
    console.log("StartDate value:", requestBody.startDate);
    console.log("EndDate value:", requestBody.endDate);

    try {
      const token = localStorage.getItem("token");
      const headers = {
        "Content-Type": "application/json",
      };
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
      
      const response = await fetch(`http://localhost:8080/api/requests`, {
        method: "POST",
        headers: headers,
        body: JSON.stringify(requestBody),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || "Failed to submit rent request");
      }

      alert("Rent request submitted successfully!");
      router.push(`/clothingcard/${id}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return (
      <>
        <Header />
        <main className="min-h-screen bg-gray-50 py-8 flex items-center justify-center">
          <p className="text-gray-600">Loading...</p>
        </main>
      </>
    );
  }

  if (!currentUser) {
    return (
      <>
        <Header />
        <main className="min-h-screen bg-gray-50 py-8 flex items-center justify-center">
          <div className="text-center">
            <p className="text-red-600 mb-4">{error}</p>
            <button
              onClick={() => router.push('/login')}
              className="bg-rose-600 text-white px-6 py-2 rounded-lg hover:bg-rose-700 transition-colors"
            >
              Go to Login
            </button>
          </div>
        </main>
      </>
    );
  }

  return (
    <>
      <Header />
      <main className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-2xl mx-auto px-6">
          <div className="bg-white rounded-2xl shadow-lg p-8">
            <div className="text-center mb-8">
              <h1 className="text-3xl font-bold text-gray-900 mb-2">
                Request to Rent
              </h1>
              {clothingCard && (
                <div className="mb-4 p-4 bg-gray-50 rounded-lg">
                  <h2 className="text-xl font-semibold text-gray-800 mb-2">
                    {clothingCard.title}
                  </h2>
                  <p className="text-gray-600 mb-2">
                    <strong>Owner:</strong> {clothingCard.owner?.userName || 'Unknown'}
                  </p>
                  <p className="text-gray-600 mb-2">
                    <strong>Price:</strong> ${clothingCard.cost} + ${clothingCard.deposit} deposit
                  </p>
                  <p className="text-gray-600">
                    <strong>Size:</strong> {clothingCard.size} | <strong>Brand:</strong> {clothingCard.brand}
                  </p>
                </div>
              )}
              <p className="text-gray-600">
                Fill out the form below to request this item
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Rental Period */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="startDate" className="block text-sm font-medium text-gray-700 mb-2">
                    Start Date *
                  </label>
                  <input
                    type="date"
                    id="startDate"
                    name="startDate"
                    value={formData.startDate}
                    onChange={handleChange}
                    required
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-transparent text-gray-900 placeholder-gray-500"
                  />
                </div>
                <div>
                  <label htmlFor="endDate" className="block text-sm font-medium text-gray-700 mb-2">
                    End Date *
                  </label>
                  <input
                    type="date"
                    id="endDate"
                    name="endDate"
                    value={formData.endDate}
                    onChange={handleChange}
                    required
                    min={formData.startDate || undefined}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-transparent text-gray-900 placeholder-gray-500"
                  />
                  {formData.startDate && formData.endDate && new Date(formData.endDate) < new Date(formData.startDate) && (
                    <p className="text-red-500 text-sm mt-1">End date must be after or equal to start date</p>
                  )}
                </div>
              </div>

              {/* Contact Information */}
              <div>
                <label htmlFor="contactInfo" className="block text-sm font-medium text-gray-700 mb-2">
                  Contact Information *
                </label>
                <input
                  type="text"
                  id="contactInfo"
                  name="contactInfo"
                  value={formData.contactInfo}
                  onChange={handleChange}
                  placeholder="Phone number or email"
                  required
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-transparent text-gray-900 placeholder-gray-500"
                />
              </div>

              {/* Message */}
              <div>
                <label htmlFor="message" className="block text-sm font-medium text-gray-700 mb-2">
                  Message to Owner
                </label>
                <textarea
                  id="message"
                  name="message"
                  value={formData.message}
                  onChange={handleChange}
                  rows={4}
                  placeholder="Tell the owner about your rental needs..."
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-transparent resize-none text-gray-900 placeholder-gray-500"
                />
              </div>

              {/* Error Message */}
              {error && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <p className="text-red-600 text-sm">{error}</p>
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex gap-4 pt-4">
                <button
                  type="button"
                  onClick={() => router.back()}
                  className="flex-1 py-3 px-6 border-2 border-gray-300 text-gray-700 rounded-lg font-semibold hover:border-gray-400 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting || (formData.startDate && formData.endDate && new Date(formData.endDate) < new Date(formData.startDate))}
                  className="flex-1 py-3 px-6 bg-rose-600 text-white rounded-lg font-semibold hover:bg-rose-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isSubmitting ? "Submitting..." : "Submit Request"}
                </button>
              </div>
            </form>

            {/* Rental Terms */}
            <div className="mt-8 p-4 bg-gray-50 rounded-lg">
              <h3 className="font-semibold text-gray-900 mb-2">Rental Terms</h3>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>• Rental period must be at least 1 day</li>
                <li>• Payment is due upon approval of request</li>
                <li>• Deposit will be returned after item is returned in good condition</li>
                <li>• Owner has 24 hours to respond to your request</li>
              </ul>
            </div>
          </div>
        </div>
      </main>
    </>
  );
}

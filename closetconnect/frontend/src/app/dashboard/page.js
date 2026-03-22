"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Header from "../../components/Header";
import ClothingCards from "../../components/ClothingCards";
import FilterBar from "@/components/FilterBar";

export default function DashboardPage() {
  const router = useRouter();
  const [currentUser, setCurrentUser] = useState(null);
  const [requestStats, setRequestStats] = useState({
    sentRequests: 0,
    receivedRequests: 0,
  });
  const [loading, setLoading] = useState(true);

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
      } finally {
        setLoading(false);
      }
    };

    getCurrentUser();
  }, []);

  useEffect(() => {
    if (currentUser) {
      fetchRequestStats();
    }
  }, [currentUser]);

  const fetchRequestStats = async () => {
    try {
      const token = localStorage.getItem("token");
      const headers = {};
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
      
      // Fetch sent requests
      const sentResponse = await fetch(
        `http://localhost:8080/api/requests/requester/${currentUser.id}`,
        { headers: headers }
      );
      const sentRequests = sentResponse.ok ? await sentResponse.json() : [];

      // Fetch received requests
      const receivedResponse = await fetch(
        `http://localhost:8080/api/requests/owner/${currentUser.id}`,
        { headers: headers }
      );
      const receivedRequests = receivedResponse.ok
        ? await receivedResponse.json()
        : [];

      setRequestStats({
        sentRequests: sentRequests.length,
        receivedRequests: receivedRequests.length,
      });
    } catch (err) {
      console.error("Error fetching request stats:", err);
    }
  };

  if (loading) {
    return (
      <>
        <Header />
        <main className="p-6 bg-pink-50 min-h-screen flex items-center justify-center">
          <p className="text-gray-600">Loading...</p>
        </main>
      </>
    );
  }

  return (
    <>
      <Header />
      <main className="p-6 bg-pink-50 min-h-screen">
        <h1 className="text-4xl font-serif font-bold text-rose-900 mb-6 text-center">
          Dashboard
        </h1>

        {/* Always-visible search and filter bar */}
        <div className="mb-8">
          <FilterBar targetPath="/dashboard" />
        </div>

        {/* Request Stats Cards */}
        {currentUser && (
          <div className="mb-8">
            <h2 className="text-2xl font-semibold text-gray-900 mb-6">Request Overview</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Sent Requests */}
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-600">Sent Requests</p>
                    <p className="text-2xl font-bold text-gray-900">{requestStats.sentRequests}</p>
                  </div>
                  <div className="p-3 bg-blue-100 rounded-full">
                    <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                    </svg>
                  </div>
                </div>
              </div>

              {/* Received Requests */}
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-600">Received Requests</p>
                    <p className="text-2xl font-bold text-gray-900">{requestStats.receivedRequests}</p>
                  </div>
                  <div className="p-3 bg-green-100 rounded-full">
                    <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 4.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                    </svg>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}


        {/* Clothing Cards */}
        <div>
          <ClothingCards />
        </div>
      </main>
    </>
  );
}

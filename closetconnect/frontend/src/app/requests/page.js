"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Header from "../../components/Header";

export default function RequestsPage() {
  const router = useRouter();
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("sent"); // "sent" or "received"
  const [sentRequests, setSentRequests] = useState([]);
  const [receivedRequests, setReceivedRequests] = useState([]);
  const [clothingCards, setClothingCards] = useState({});
  const [users, setUsers] = useState({});
  const [error, setError] = useState("");

  useEffect(() => {
    const getCurrentUser = () => {
      try {
        const token = localStorage.getItem("token");
        const userId = localStorage.getItem("userId");

        if (!token || !userId) {
          setError("Please log in to view requests");
          setLoading(false);
          return;
        }

        const userData = {
          id: parseInt(userId),
          token: token
        };
        
        setCurrentUser(userData);
      } catch (err) {
        setError("Please log in to view requests");
      } finally {
        setLoading(false);
      }
    };

    getCurrentUser();
  }, []);

  useEffect(() => {
    if (currentUser) {
      fetchSentRequests();
      fetchReceivedRequests();
    }
  }, [currentUser]);

  useEffect(() => {
    if (sentRequests.length > 0 || receivedRequests.length > 0) {
      fetchAdditionalData();
    }
  }, [sentRequests, receivedRequests]);

  const fetchAdditionalData = async () => {
    const allRequests = [...sentRequests, ...receivedRequests];
    const clothingIds = [...new Set(allRequests.map(req => req.clothingId))];
    const userIds = [...new Set(allRequests.map(req => req.fromUserId))];

    // Fetch clothing card details
    for (const clothingId of clothingIds) {
      try {
        const response = await fetch(`http://localhost:8080/api/clothing-cards/${clothingId}`);
        if (response.ok) {
          const clothingCard = await response.json();
          setClothingCards(prev => ({ ...prev, [clothingId]: clothingCard }));
        }
      } catch (err) {
        console.error(`Error fetching clothing card ${clothingId}:`, err);
      }
    }

    // Fetch user details
    for (const userId of userIds) {
      try {
        const response = await fetch(`http://localhost:8080/api/users/${userId}`);
        if (response.ok) {
          const user = await response.json();
          setUsers(prev => ({ ...prev, [userId]: user }));
        }
      } catch (err) {
        console.error(`Error fetching user ${userId}:`, err);
      }
    }
  };

  const fetchSentRequests = async () => {
    try {
      const token = localStorage.getItem("token");
      const headers = {};
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
      
      const response = await fetch(`http://localhost:8080/api/requests/requester/${currentUser.id}`, {
        headers: headers
      });
      if (response.ok) {
        const requests = await response.json();
        setSentRequests(requests);
      }
    } catch (err) {
      console.error("Error fetching sent requests:", err);
    }
  };

  const fetchReceivedRequests = async () => {
    try {
      const token = localStorage.getItem("token");
      const headers = {};
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
      
      const response = await fetch(`http://localhost:8080/api/requests/owner/${currentUser.id}`, {
        headers: headers
      });
      if (response.ok) {
        const requests = await response.json();
        setReceivedRequests(requests);
      }
    } catch (err) {
      console.error("Error fetching received requests:", err);
    }
  };

  const handleApproveRequest = async (requestId, approved) => {
    try {
      const token = localStorage.getItem("token");
      const headers = {
        "Content-Type": "application/json",
      };
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
      
      const response = await fetch(`http://localhost:8080/api/requests/${requestId}/approve`, {
        method: "PUT",
        headers: headers,
        body: JSON.stringify({ approved }),
      });

      if (response.ok || response.status === 204) {
        // Refresh the received requests (rejected requests are now deleted)
        fetchReceivedRequests();
        // Refresh sent requests to see if the request still exists
        fetchSentRequests();
        if (approved) {
          alert('Request approved successfully!');
        } else {
          alert('Request rejected and removed!');
        }
      } else {
        alert("Failed to update request");
      }
    } catch (err) {
      console.error("Error updating request:", err);
      alert("Failed to update request");
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString();
  };

  const getStatusBadge = (approved) => {
    if (approved === null || approved === undefined) {
      return <span className="px-2 py-1 bg-yellow-100 text-yellow-800 rounded-full text-xs font-medium">Pending</span>;
    }
    if (approved === true) {
      return <span className="px-2 py-1 bg-green-100 text-green-800 rounded-full text-xs font-medium">Approved</span>;
    }
    return <span className="px-2 py-1 bg-red-100 text-red-800 rounded-full text-xs font-medium">Rejected</span>;
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
        <div className="max-w-6xl mx-auto px-6">
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">Requests</h1>
            <p className="text-gray-600">Manage your rental requests</p>
          </div>

          {/* Tab Navigation */}
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 mb-6">
            <div className="flex">
              <button
                onClick={() => setActiveTab("sent")}
                className={`flex-1 py-4 px-6 text-center font-medium transition-colors ${
                  activeTab === "sent"
                    ? "text-rose-600 border-b-2 border-rose-600 bg-rose-50"
                    : "text-gray-600 hover:text-gray-900"
                }`}
              >
                Sent Requests ({sentRequests.length})
              </button>
              <button
                onClick={() => setActiveTab("received")}
                className={`flex-1 py-4 px-6 text-center font-medium transition-colors ${
                  activeTab === "received"
                    ? "text-rose-600 border-b-2 border-rose-600 bg-rose-50"
                    : "text-gray-600 hover:text-gray-900"
                }`}
              >
                Received Requests ({receivedRequests.length})
              </button>
            </div>
          </div>

          {/* Sent Requests Tab */}
          {activeTab === "sent" && (
            <div className="space-y-4">
              {sentRequests.length === 0 ? (
                <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-8 text-center">
                  <p className="text-gray-500 text-lg">No sent requests yet</p>
                  <p className="text-gray-400 mt-2">Browse clothing items to make your first request!</p>
                </div>
              ) : (
                sentRequests.map((request) => {
                  const clothingCard = clothingCards[request.clothingId];
                  return (
                    <div key={request.requestId} className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                      <div className="flex justify-between items-start mb-4">
                        <div>
                          <h3 className="text-lg font-semibold text-gray-900">
                            {clothingCard ? clothingCard.title : `Item #${request.clothingId}`}
                          </h3>
                          <p className="text-sm text-gray-600">Sent on {formatDate(request.createdAt)}</p>
                        </div>
                        {getStatusBadge(request.approved)}
                      </div>
                      
                      {clothingCard && (
                        <div className="mb-4 p-4 bg-gray-50 rounded-lg">
                          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                            <div>
                              <p className="text-sm font-medium text-gray-700">Brand & Size</p>
                              <p className="text-gray-900">{clothingCard.brand} - {clothingCard.size}</p>
                            </div>
                            <div>
                              <p className="text-sm font-medium text-gray-700">Price</p>
                              <p className="text-gray-900">${clothingCard.cost} + ${clothingCard.deposit} deposit</p>
                            </div>
                            <div>
                              <p className="text-sm font-medium text-gray-700">Owner</p>
                              <p className="text-gray-900">{clothingCard.owner?.userName || 'Unknown'}</p>
                            </div>
                          </div>
                        </div>
                      )}
                      
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                        <div>
                          <p className="text-sm font-medium text-gray-700">Requested Date</p>
                          <p className="text-gray-900">{formatDate(request.availabilityRangeRequest)}</p>
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-700">Your Contact Info</p>
                          <p className="text-gray-900">{request.requesterContactInfo}</p>
                        </div>
                        <div className="md:col-span-2">
                          <p className="text-sm font-medium text-gray-700">Message to Owner</p>
                          <p className="text-gray-900">{request.commentsToOwner || "No message"}</p>
                        </div>
                      </div>

                      <div className="flex gap-2">
                        <button
                          onClick={() => router.push(`/clothingcard/${request.clothingId}`)}
                          className="px-4 py-2 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
                        >
                          View Item
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          )}

          {/* Received Requests Tab */}
          {activeTab === "received" && (
            <div className="space-y-4">
              {receivedRequests.length === 0 ? (
                <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-8 text-center">
                  <p className="text-gray-500 text-lg">No received requests yet</p>
                  <p className="text-gray-400 mt-2">Requests for your items will appear here!</p>
                </div>
              ) : (
                receivedRequests.map((request) => {
                  const clothingCard = clothingCards[request.clothingId];
                  const requester = users[request.fromUserId];
                  return (
                    <div key={request.requestId} className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                      <div className="flex justify-between items-start mb-4">
                        <div>
                          <h3 className="text-lg font-semibold text-gray-900">
                            {clothingCard ? clothingCard.title : `Item #${request.clothingId}`}
                          </h3>
                          <p className="text-sm text-gray-600">Received on {formatDate(request.createdAt)}</p>
                        </div>
                        {getStatusBadge(request.approved)}
                      </div>
                      
                      {clothingCard && (
                        <div className="mb-4 p-4 bg-gray-50 rounded-lg">
                          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                            <div>
                              <p className="text-sm font-medium text-gray-700">Brand & Size</p>
                              <p className="text-gray-900">{clothingCard.brand} - {clothingCard.size}</p>
                            </div>
                            <div>
                              <p className="text-sm font-medium text-gray-700">Price</p>
                              <p className="text-gray-900">${clothingCard.cost} + ${clothingCard.deposit} deposit</p>
                            </div>
                            <div>
                              <p className="text-sm font-medium text-gray-700">Your Item</p>
                              <p className="text-gray-900">✓ Listed by you</p>
                            </div>
                          </div>
                        </div>
                      )}
                      
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                        <div>
                          <p className="text-sm font-medium text-gray-700">Requested Date</p>
                          <p className="text-gray-900">{formatDate(request.availabilityRangeRequest)}</p>
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-700">Requester</p>
                          <p className="text-gray-900">{requester ? requester.userName : 'Unknown User'}</p>
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-700">Contact Info</p>
                          <p className="text-gray-900">{request.requesterContactInfo}</p>
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-700">Message</p>
                          <p className="text-gray-900">{request.commentsToOwner || "No message"}</p>
                        </div>
                      </div>

                      <div className="flex gap-2">
                        <button
                          onClick={() => router.push(`/clothingcard/${request.clothingId}`)}
                          className="px-4 py-2 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
                        >
                          View Item
                        </button>
                        {requester && (
                          <button
                            onClick={() => router.push(`/profile/${request.fromUserId}`)}
                            className="px-4 py-2 text-sm bg-blue-100 text-blue-700 rounded-lg hover:bg-blue-200 transition-colors"
                          >
                            View Requester Profile
                          </button>
                        )}
                        {(request.approved === null || request.approved === undefined) && (
                          <>
                            <button
                              onClick={() => handleApproveRequest(request.requestId, true)}
                              className="px-4 py-2 text-sm bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
                            >
                              Approve
                            </button>
                            <button
                              onClick={() => handleApproveRequest(request.requestId, false)}
                              className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
                            >
                              Reject
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          )}
        </div>
      </main>
    </>
  );
}

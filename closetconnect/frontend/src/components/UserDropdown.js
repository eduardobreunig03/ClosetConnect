"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

export default function UserDropdown({ userId }) {
  const [isOpen, setIsOpen] = useState(false);
  const [userName, setUserName] = useState("");
  const [userEmail, setUserEmail] = useState("");
  const [loading, setLoading] = useState(true);
  const dropdownRef = useRef(null);
  const router = useRouter();

  useEffect(() => {
    const fetchUserData = async () => {
      if (!userId) return;
      
      try {
        const token = localStorage.getItem("token");
        const headers = {};
        if (token) {
          headers.Authorization = `Bearer ${token}`;
        }
        const res = await fetch(`http://localhost:8080/api/profile/${userId}`, {
          headers
        });
        if (res.ok) {
          const data = await res.json();
          setUserName(data.userName || data.name || "User");
          setUserEmail(data.email || "");
        }
      } catch (error) {
        console.error("Failed to fetch user data:", error);
        setUserName("User");
      } finally {
        setLoading(false);
      }
    };

    fetchUserData();
  }, [userId]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleLogout = async () => {
    setIsOpen(false);
    
    const token = localStorage.getItem("token");
    const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
    
    // Call backend logout endpoint to blacklist the token
    if (token) {
      try {
        await fetch(`${API_BASE}/api/auth/logout`, {
          method: "POST",
          headers: {
            "Authorization": `Bearer ${token}`,
            "Content-Type": "application/json"
          }
        });
        console.log("✅ Token blacklisted on server");
      } catch (error) {
        console.error("⚠️  Failed to blacklist token on server:", error);
        // Continue with logout even if server call fails
      }
    }
    
    // Always remove token from localStorage
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    
    // Dispatch custom event to notify Header component
    window.dispatchEvent(new CustomEvent('authChange'));
    router.push("/home");
  };

  const getInitials = (name) => {
    if (!name) return "U";
    return name
      .split(" ")
      .map(word => word.charAt(0))
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  if (loading) {
    return (
      <div className="w-10 h-10 rounded-full bg-rose-200 animate-pulse flex items-center justify-center">
        <div className="w-6 h-6 bg-rose-300 rounded-full"></div>
      </div>
    );
  }

  return (
    <div className="relative" ref={dropdownRef}>
      {/* User Circle Button with Dropdown Arrow */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-3 py-2 rounded-full bg-gradient-to-br from-rose-400 to-rose-600 text-white font-semibold text-sm hover:from-rose-500 hover:to-rose-700 transition-all duration-200 shadow-md hover:shadow-lg focus:outline-none focus:ring-2 focus:ring-rose-300 focus:ring-offset-2"
        aria-label="User menu"
      >
        <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center">
          {getInitials(userName)}
        </div>
        <svg 
          className={`w-4 h-4 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} 
          fill="none" 
          stroke="currentColor" 
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {/* Dropdown Menu */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-64 bg-white rounded-lg shadow-xl border border-rose-100 py-2 z-50 animate-in slide-in-from-top-2 duration-200">
          {/* User Info */}
          <div className="px-4 py-3 border-b border-rose-100">
            <p className="text-sm font-semibold text-rose-900">{userName}</p>
            <p className="text-xs text-rose-600 truncate">{userEmail}</p>
          </div>

          {/* Menu Items */}
          <div className="py-1">
            <Link
              href={`/profile/${userId}`}
              className="flex items-center px-4 py-2 text-sm text-rose-700 hover:bg-rose-50 transition-colors duration-150"
              onClick={() => setIsOpen(false)}
            >
              <svg className="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              View Profile
            </Link>
            
            <Link
              href="/your-listings"
              className="flex items-center px-4 py-2 text-sm text-rose-700 hover:bg-rose-50 transition-colors duration-150"
              onClick={() => setIsOpen(false)}
            >
              <svg className="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
              My Closet
            </Link>
            
            <Link
              href="/my-rentals"
              className="flex items-center px-4 py-2 text-sm text-rose-700 hover:bg-rose-50 transition-colors duration-150"
              onClick={() => setIsOpen(false)}
            >
              <svg className="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3a2 2 0 012-2h4a2 2 0 012 2v4m-6 0h6m-6 0l-2 2m8-2l2 2m-2-2v10a2 2 0 01-2 2H8a2 2 0 01-2-2V9a2 2 0 012-2h8a2 2 0 012 2v8a2 2 0 01-2 2H8a2 2 0 01-2-2V9z" />
              </svg>
              My Rentals
            </Link>
            

          </div>

          {/* Logout Button */}
          <div className="border-t border-rose-100 pt-1">
            <button
              onClick={handleLogout}
              className="flex items-center w-full px-4 py-2 text-sm text-rose-600 hover:bg-rose-50 transition-colors duration-150"
            >
              <svg className="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
              Log Out
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

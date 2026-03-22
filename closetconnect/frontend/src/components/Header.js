"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import UserDropdown from "./UserDropdown";

export default function Header() {
  const [hasToken, setHasToken] = useState(false);
  const [userId, setUserId] = useState(null);
  const router = useRouter();

  useEffect(() => {
    const check = () => {
      const token = localStorage.getItem("token");
      setHasToken(Boolean(token));
      setUserId(localStorage.getItem("userId"));
    };
    check();

    const onStorage = (e) => {
      if (e.key === "token" || e.key === "userId") check();
    };
    
    const onAuthChange = () => {
      check();
    };

    window.addEventListener("storage", onStorage);
    window.addEventListener("authChange", onAuthChange);
    return () => {
      window.removeEventListener("storage", onStorage);
      window.removeEventListener("authChange", onAuthChange);
    };
  }, []);

  return (
    <header className="bg-gradient-to-r from-pink-100 via-rose-200 to-pink-100 text-rose-900 py-6 px-4 shadow-md rounded-b-xl">
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        <Link href="/home" className="hover:opacity-80 transition-opacity duration-200">
          <h1 className="text-4xl font-serif font-semibold tracking-wide cursor-pointer">
            CLOSET CONNECT
          </h1>
        </Link>
        <nav className="flex items-center gap-4 text-lg font-light">
          <Link
            href="/dashboard"
            className="px-4 py-2 rounded-lg bg-white text-rose-700 hover:bg-rose-50 hover:text-rose-800 transition-all duration-200 font-medium shadow-md hover:shadow-lg border border-rose-200"
          >
            Browse
          </Link>
          {hasToken && (
            <Link
              href="/requests"
              className="px-4 py-2 rounded-lg bg-white text-rose-700 hover:bg-rose-50 hover:text-rose-800 transition-all duration-200 font-medium shadow-md hover:shadow-lg border border-rose-200"
            >
              Requests
            </Link>
          )}

          {!hasToken ? (
            <div className="flex items-center gap-3">
              <Link
                href="/login"
                className="px-4 py-2 rounded-lg bg-white/90 text-rose-700 hover:bg-white hover:text-rose-800 transition-all duration-200 font-medium shadow-md hover:shadow-lg border border-rose-200"
              >
                Log In
              </Link>
              <Link
                href="/signup"
                className="px-6 py-2.5 rounded-full bg-gradient-to-r from-rose-500 to-rose-600 text-white hover:from-rose-600 hover:to-rose-700 transition-all duration-200 font-medium shadow-md hover:shadow-lg hover:scale-105"
              >
                Sign Up
              </Link>
            </div>
          ) : (
            <div className="flex items-center gap-3">
              <Link
                href="/add-listing"
                className="px-6 py-2.5 rounded-full bg-gradient-to-r from-rose-500 to-rose-600 text-white hover:from-rose-600 hover:to-rose-700 transition-all duration-200 font-medium shadow-md hover:shadow-lg hover:scale-105"
              >
                Add Listing
              </Link>
              <UserDropdown userId={userId} />
            </div>
          )}
        </nav>
      </div>
    </header>
  );
}

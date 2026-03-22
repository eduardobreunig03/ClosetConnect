"use client";

import Link from "next/link";

export default function Footer() {
  return (
    <footer className="bg-gradient-to-r from-rose-100 via-pink-100 to-rose-100 text-rose-900 py-8 px-4 mt-auto">
      <div className="max-w-7xl mx-auto">
        <div className="flex flex-col md:flex-row items-center justify-between">
          {/* Logo/Brand */}
          <div className="mb-4 md:mb-0">
            <Link href="/home" className="hover:opacity-80 transition-opacity duration-200">
              <h2 className="text-2xl font-serif font-semibold tracking-wide">
                CLOSET CONNECT
              </h2>
            </Link>
            <p className="text-sm text-rose-600 mt-1">
              Share your style, rent your wardrobe
            </p>
          </div>

          {/* Navigation Links */}
          <nav className="flex items-center gap-6 text-sm">
            <Link
              href="/about"
              className="hover:text-rose-600 transition duration-200"
            >
              About
            </Link>
            <Link
              href="/dashboard"
              className="hover:text-rose-600 transition duration-200"
            >
              Browse
            </Link>
            <button
              className="hover:text-rose-600 transition duration-200 cursor-pointer"
              onClick={() => alert('My Rentals feature coming soon!')}
            >
              My Rentals
            </button>
          </nav>
        </div>

        {/* Copyright */}
        <div className="border-t border-rose-200 mt-6 pt-4 text-center">
          <p className="text-xs text-rose-600">
            © 2024 Closet Connect. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
}

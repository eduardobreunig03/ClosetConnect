"use client";

import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { useState } from "react";

export default function FilterBar({ targetPath }) {
  const router = useRouter();
  const params = useSearchParams();
  const pathname = targetPath || usePathname();

  const [q, setQ] = useState(params.get("q") || "");
  const [availability, setAvailability] = useState(
    params.get("availability") === "true"
  );
  const [gender, setGender] = useState(params.get("gender") || "");
  const [location, setLocation] = useState(params.get("location") || "");
  const [sort, setSort] = useState(params.get("sort") || "popularity");

  const apply = () => {
    const usp = new URLSearchParams(params.toString());
    q ? usp.set("q", q) : usp.delete("q");
    availability ? usp.set("availability", "true") : usp.delete("availability");
    gender ? usp.set("gender", gender) : usp.delete("gender");
    location ? usp.set("location", location) : usp.delete("location");
    usp.set("sort", sort);
    usp.set("page", "0");
    router.push(`${pathname}?${usp.toString()}`);
  };

  const clear = () => router.push(pathname);

  return (
    <div className="w-full bg-gradient-to-r from-pink-100 to-white p-4 rounded-xl shadow-md mb-6">
      <div className="flex flex-wrap items-center gap-4">
        <input
          className="px-4 py-2 border rounded-xl flex-1 min-w-[220px] shadow-sm"
          placeholder="Search title, brand, tags…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />

        {/* Gender Dropdown */}
        <div className="relative">
          <select
            className="appearance-none px-4 py-2 border border-gray-300 rounded-xl shadow-sm bg-white text-gray-700 focus:outline-none focus:ring-2 focus:ring-pink-300 pr-10"
            value={gender}
            onChange={(e) => setGender(e.target.value)}
          >
            <option value="">Gender (tag)</option>
            <option value="men">Men</option>
            <option value="women">Women</option>
            <option value="unisex">Unisex</option>
          </select>
          <div className="absolute inset-y-0 right-0 flex items-center px-2 pointer-events-none">
            <svg
              className="w-4 h-4 text-gray-500"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M19 9l-7 7-7-7"
              />
            </svg>
          </div>
        </div>

        <input
          className="px-4 py-2 border rounded-xl min-w-[160px] shadow-sm"
          placeholder="Location"
          value={location}
          onChange={(e) => setLocation(e.target.value)}
        />

        <label className="flex items-center gap-2 px-4 py-2 border rounded-xl shadow-sm">
          <input
            type="checkbox"
            checked={availability}
            onChange={(e) => setAvailability(e.target.checked)}
          />
          Available
        </label>

        {/* Sort Dropdown */}
        <div className="relative">
          <select
            className="appearance-none px-4 py-2 border border-gray-300 rounded-xl shadow-sm bg-white text-gray-700 focus:outline-none focus:ring-2 focus:ring-pink-300 pr-10"
            value={sort}
            onChange={(e) => setSort(e.target.value)}
          >
            <option value="popularity">Popularity</option>
            <option value="rating_desc">Rating</option>
            <option value="newest">Newest</option>
            <option value="price_asc">Price ↑</option>
            <option value="price_desc">Price ↓</option>
          </select>
          <div className="absolute inset-y-0 right-0 flex items-center px-2 pointer-events-none">
            <svg
              className="w-4 h-4 text-gray-500"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M19 9l-7 7-7-7"
              />
            </svg>
          </div>
        </div>

        <button
          onClick={apply}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-pink-100 to-white text-rose-900 font-medium hover:from-pink-200 hover:to-white hover:border-rose-400 shadow-sm transition"
        >
          Apply
        </button>
        <button
          onClick={clear}
          className="px-4 py-2 rounded-xl borde text-red-500 hover:bg-red-50 shadow-sm"
        >
          Clear
        </button>
      </div>
    </div>
  );
}

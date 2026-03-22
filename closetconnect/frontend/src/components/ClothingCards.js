"use client";

import { useEffect, useMemo, useState } from "react";
import { useSearchParams, useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import SingleClothingCard from "./SingleClothingCard";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "";

export default function ClothingCards() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const [pageData, setPageData] = useState(null);
  const [clothingCards, setClothingCards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentUserId, setCurrentUserId] = useState(null);
  const [hasToken, setHasToken] = useState(false);

  const queryString = useMemo(() => {
    const p = new URLSearchParams();

    for (const key of [
      "q",
      "availability",
      "gender",
      "location",
      "size",
      "brand",
      "minCost",
      "maxCost",
      "availableFrom",
      "sort",
    ]) {
      const v = params.get(key);
      if (v != null && v !== "") p.set(key, v);
    }

    p.set("page", params.get("page") ?? "0");
    p.set("sizePage", params.get("sizePage") ?? "12");

    return p.toString();
  }, [params]);

  useEffect(() => {
    let alive = true;

    (async () => {
      setLoading(true);
      setError(null);

      try {
        const url = `${API_BASE}/api/clothing-cards/filter?${queryString}`;
        const res = await fetch(url, { cache: "no-store" });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const json = await res.json();

        const normalized = Array.isArray(json)
          ? {
              content: json,
              totalElements: json.length,
              totalPages: 1,
              number: 0,
              size: json.length,
            }
          : json;

        if (alive) setPageData(normalized);
      } catch (e) {
        try {
          const res2 = await fetch(`${API_BASE}/api/clothing-cards`, {
            cache: "no-store",
          });
          if (!res2.ok) throw new Error(`HTTP ${res2.status}`);
          const arr = await res2.json();
          if (alive)
            setPageData({
              content: Array.isArray(arr) ? arr : [],
              totalElements: Array.isArray(arr) ? arr.length : 0,
              totalPages: 1,
              number: 0,
              size: Array.isArray(arr) ? arr.length : 0,
            });
        } catch (e2) {
          if (alive) setError(`Failed to fetch clothing cards: ${e.message}`);
          console.error("Filter fetch error:", e);
        }
      } finally {
        if (alive) setLoading(false);
      }
    })();
    const check = () => {
      const token = localStorage.getItem("token");
      setHasToken(Boolean(token));
      setCurrentUserId(localStorage.getItem("userId")); // get user ID from localStorage
    };
    check();

    return () => {
      alive = false;
    };
  }, [queryString]);

  const setParam = (key, value) => {
    const usp = new URLSearchParams(params.toString());
    if (value == null || value === "") usp.delete(key);
    else usp.set(key, String(value));
    router.push(`${pathname}?${usp.toString()}`);
  };

  // UI states
  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[200px]">
        <div className="text-lg text-gray-600">Loading clothing cards…</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[200px] p-4">
        <div className="text-lg text-red-600 mb-4">{error}</div>
        <button
          onClick={() => router.refresh()}
          className="px-4 py-2 bg-rose-600 text-white rounded-lg hover:bg-rose-700 transition-colors"
        >
          Try Again
        </button>
      </div>
    );
  }

  if (!pageData || !pageData.content || pageData.content.length === 0) {
    return (
      <div className="flex justify-center items-center min-h-[200px]">
        <div className="text-lg text-gray-600">No clothing cards found.</div>
      </div>
    );
  }

  const { content, totalElements, totalPages, number } = pageData;

  return (
    <div className="p-6 space-y-4">
      <div className="text-sm text-gray-600">
        Showing page {number + 1} of {totalPages} • {totalElements} items
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {content.map((card) => {
          const id = card.clothingId ?? card.id;
          return (
            <Link key={id} href={`/clothingcard/${id}`}>
              <SingleClothingCard
                clothingCard={card}
                currentUserId={currentUserId}
                onDelete={() => {
                  setPageData((prev) => ({
                    ...prev,
                    content: prev.content.filter(
                      (c) => c.clothingId !== card.clothingId
                    ),
                    totalElements: prev.totalElements - 1,
                  }));
                }}
              />
            </Link>
          );
        })}
      </div>

      <Pagination
        totalPages={totalPages}
        page={number}
        onChange={(p) => setParam("page", p)}
      />
    </div>
  );
}

function Pagination({ totalPages, page, onChange }) {
  if (totalPages <= 1) return null;
  const prev = () => onChange(Math.max(0, page - 1));
  const next = () => onChange(Math.min(totalPages - 1, page + 1));

  return (
    <div className="flex items-center justify-center gap-2 mt-4">
      <button
        className="px-3 py-1 rounded-lg border disabled:opacity-50"
        onClick={prev}
        disabled={page <= 0}
      >
        Prev
      </button>
      <span className="text-sm">
        Page {page + 1} / {totalPages}
      </span>
      <button
        className="px-3 py-1 rounded-lg border disabled:opacity-50"
        onClick={next}
        disabled={page >= totalPages - 1}
      >
        Next
      </button>
    </div>
  );
}

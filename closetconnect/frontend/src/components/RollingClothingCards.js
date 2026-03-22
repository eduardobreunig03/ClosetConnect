"use client";

import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import SingleClothingCard from "./SingleClothingCard";
import Link from "next/link";

export default function RollingClothingCards() {
  const [clothingCards, setClothingCards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchClothingCards = async () => {
    try {
      setLoading(true);
      const response = await fetch("http://localhost:8080/api/clothing-cards");

      if (!response.ok)
        throw new Error(`HTTP error! status: ${response.status}`);

      const data = await response.json();
      setClothingCards(data);
      setError(null);
    } catch (err) {
      setError("Failed to fetch clothing cards: " + err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClothingCards();
  }, []);

  if (loading)
    return (
      <div className="flex justify-center items-center min-h-[200px] text-gray-600">
        Loading clothing cards...
      </div>
    );
  if (error)
    return (
      <div className="flex flex-col items-center justify-center min-h-[200px] p-4">
        <div className="text-lg text-red-600 mb-4">{error}</div>
        <button
          onClick={fetchClothingCards}
          className="px-4 py-2 bg-rose-600 text-white rounded-lg hover:bg-rose-700 transition-colors"
        >
          Try Again
        </button>
      </div>
    );
  if (clothingCards.length === 0)
    return (
      <div className="flex justify-center items-center min-h-[200px] text-gray-600">
        No clothing cards found.
      </div>
    );

  const rollingCards = [...clothingCards, ...clothingCards];

  return (
    <div className="overflow-hidden w-full py-6">
      <motion.div
        className="flex gap-6"
        animate={{ x: ["0%", "-50%"] }}
        transition={{ repeat: Infinity, duration: 20, ease: "linear" }}
      >
        {rollingCards.map((card, index) => (
          <Link key={index} href={`/clothingcard/${card.clothingId}`}>
            <div className="min-w-[250px] cursor-pointer hover:shadow-lg transition-shadow">
              <SingleClothingCard clothingCard={card} />
            </div>
          </Link>
        ))}
      </motion.div>
    </div>
  );
}

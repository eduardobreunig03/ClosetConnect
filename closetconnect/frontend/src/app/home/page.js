"use client";

import React, { useEffect, useState } from "react";
import Header from "../../components/Header";
import RollingClothingCards from "../../components/RollingClothingCards";
import Link from "next/link";

export default function HomePage() {
  const [message, setMessage] = useState("");

  useEffect(() => {
    fetch("http://localhost:8080/hello")
      .then((res) => res.text())
      .then((data) => setMessage(data))
      .catch((err) => console.error("Error fetching backend:", err));
  }, []);

  return (
    <main>
      <Header />

      {/* Full-width Banner Image with Overlay */}
      <section className="relative w-full">
        <img
          src="/mainimage.png"
          alt="Luxury fashion banner"
          className="w-full h-[700px] object-cover"
        />

        {/* Overlay Content */}
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center text-white bg-black/40">
          <h1 className="text-4xl md:text-6xl font-bold mb-4">
            Luxury Fashion Collection
          </h1>
          <p className="text-lg md:text-xl max-w-2xl mb-6">
            Discover timeless styles and exclusive designs curated for you.
          </p>
          <Link href="/dashboard">
            <button className="px-6 py-3 bg-rose-600 rounded-full font-semibold hover:bg-rose-700 transition-colors">
              Shop Now
            </button>
          </Link>
        </div>
      </section>

      {/* Rolling Clothing Cards Section */}
      <section className="px-6 py-10 text-center">
        <h2 className="text-3xl font-bold mb-6 text-gray-800">
          Discover Our Latest Collection
        </h2>
        <RollingClothingCards />
      </section>

      {/* More Content Below */}
      <section className="px-6 py-12 bg-gray-50">
        <h3 className="text-2xl font-semibold mb-6 text-gray-800 text-center">
          More to Explore
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-6xl mx-auto">
          <div className="bg-white shadow-md rounded-xl p-6">
            <h4 className="text-lg font-semibold mb-2">✨ Trending Now</h4>
            <p className="text-gray-600">
              See what’s hot this season and shop the most popular styles.
            </p>
          </div>

          <div className="bg-white shadow-md rounded-xl p-6">
            <h4 className="text-lg font-semibold mb-2">👗 Style Guides</h4>
            <p className="text-gray-600">
              Learn how to pair your outfits with confidence using our expert
              tips.
            </p>
          </div>

          <div className="bg-white shadow-md rounded-xl p-6">
            <h4 className="text-lg font-semibold mb-2">
              📩 Join Our Newsletter
            </h4>
            <p className="text-gray-600">
              Stay in the loop with exclusive offers and fashion updates.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}

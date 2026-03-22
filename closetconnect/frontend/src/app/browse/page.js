import FilterBar from "@/components/FilterBar";

function buildSearchParams(obj) {
  const usp = new URLSearchParams();
  if (!obj) return usp;
  for (const key in obj) {
    const val = obj[key];
    if (typeof val === "string") {
      usp.set(key, val);
    } else if (Array.isArray(val)) {
      val.forEach((v) => usp.append(key, String(v)));
    }
  }
  return usp;
}

async function fetchPage(spObj) {
  const usp = buildSearchParams(spObj);
  if (!usp.has("sizePage")) usp.set("sizePage", "12");

  // Using your Next rewrite: /api/* -> http://localhost:8080/api/*
  const res = await fetch(
    `http://localhost:3000/api/clothing-cards/filter?${usp.toString()}`,
    { cache: "no-store" }
  );
  if (!res.ok) throw new Error("Failed to fetch clothing cards");
  return res.json();
}

export default async function Browse({ searchParams }) {
  // ✅ Next 15: await the promise
  const sp = await searchParams;

  const data = await fetchPage(sp);
  const items = data?.content || [];
  const page = data?.number ?? 0;
  const totalPages = data?.totalPages ?? 1;

  // Build links using the already-awaited sp
  const makeHref = (nextPage) => {
    const usp = buildSearchParams(sp);
    usp.set("page", String(nextPage));
    return `/browse?${usp.toString()}`;
  };

  return (
    <div>
      <FilterBar />

      <div className="max-w-6xl mx-auto p-4 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
        {items.map((c) => (
          <div key={c.clothingId} className="border rounded-2xl p-3">
            <div className="text-sm text-gray-500">{c.brand} · {c.size}</div>
            <div className="font-semibold">{c.title}</div>
            <div className="text-sm">{c.locationOfClothing}</div>
            <div className="mt-2 text-sm">Cost: ${c.cost} · Deposit: ${c.deposit}</div>
            <div className="text-sm">Rating: {c.rating} · {c.availability ? "Available" : "Unavailable"}</div>
          </div>
        ))}
        {items.length === 0 && (
          <div className="col-span-full text-center text-gray-500 py-16">
            No items match your filters.
          </div>
        )}
      </div>

      <div className="max-w-6xl mx-auto p-4 flex items-center justify-between">
        <a
          className={`px-3 py-2 rounded border ${page <= 0 ? "opacity-50 pointer-events-none" : "hover:bg-gray-50"}`}
          href={makeHref(Math.max(0, page - 1))}
        >
          ← Prev
        </a>
        <span className="text-sm">Page {page + 1} / {Math.max(1, totalPages)}</span>
        <a
          className={`px-3 py-2 rounded border ${page >= totalPages - 1 ? "opacity-50 pointer-events-none" : "hover:bg-gray-50"}`}
          href={makeHref(page + 1)}
        >
          Next →
        </a>
      </div>
    </div>
  );
}

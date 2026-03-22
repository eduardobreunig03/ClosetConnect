"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { use } from "react";
import Header from "../../../components/Header";

export default function EditListingPage({ params }) {
  const { id } = use(params);
  const router = useRouter();
  const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

  const [hasToken, setHasToken] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [errors, setErrors] = useState({
    title: "",
    cost: "",
    deposit: "",
    size: "",
    availabilityDay: "",
    description: "",
    imageFile: "",
  });

  const [form, setForm] = useState({
    title: "",
    cost: "",
    deposit: "",
    size: "",
    availabilityDay: "",
    description: "",
    tag: "",
    brand: "",
    locationOfClothing: "",
    imageFiles: [],
  });
  const [imagePreviews, setImagePreviews] = useState([]);
  const fileInputRef = useRef(null);

  // Focus listener
  useEffect(() => {
    const onFocusBack = () => setOpeningPicker(false);
    window.addEventListener("focus", onFocusBack);
    return () => window.removeEventListener("focus", onFocusBack);
  }, []);

  // Check token
  useEffect(() => {
    const token = localStorage.getItem("token");
    setHasToken(Boolean(token));
  }, []);

  // Fetch listing data
  useEffect(() => {
    if (!id) return;
    const token = localStorage.getItem("token");
    if (!token) return;

    fetch(`${API_BASE}/api/clothing-cards/${id}`)
      .then((res) => res.json())
      .then((data) => {
        setForm({
          title: data.title || "",
          cost: data.cost || "",
          deposit: data.deposit || "",
          size: data.size || "",
          description: data.description || "",
          tag: data.tag || "",
          brand: data.brand || "",
          locationOfClothing: data.locationOfClothing || "",
          availabilityDay: data.availabilityDay?.split("T")[0] || "",
          imageFiles: [],
        });
        if (data.images?.length > 0) {
          setImagePreviews(
            data.images.map((img) => `data:image/jpeg;base64,${img.image}`)
          );
        }
      })
      .catch((err) => console.error("Error loading listing:", err));
  }, [id]);

  const canSubmit = useMemo(() => {
    return (
      hasToken &&
      form.title &&
      form.cost &&
      form.deposit &&
      form.size &&
      form.description &&
      form.availabilityDay &&
      Array.isArray(form.imageFiles)
    );
  }, [hasToken, form]);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setErrors({
      title: "",
      cost: "",
      deposit: "",
      size: "",
      availabilityDay: "",
      description: "",
      imageFile: "",
    });

    const newErrors = { ...errors };
    const token = localStorage.getItem("token");
    const ownerId = localStorage.getItem("userId");

    if (!token || !ownerId) {
      setError("Please log in before editing a listing.");
      return;
    }

    const costNum = Number(String(form.cost).replace(/[^0-9.]/g, ""));
    const depNum = Number(String(form.deposit).replace(/[^0-9.]/g, ""));
    if (!form.title.trim()) newErrors.title = "Title is required";
    if (!form.cost || isNaN(costNum)) newErrors.cost = "Enter a valid amount";
    if (!form.deposit || isNaN(depNum))
      newErrors.deposit = "Enter a valid amount";
    if (!form.size.trim()) newErrors.size = "Required";
    if (!form.availabilityDay) newErrors.availabilityDay = "Required";
    if (!form.description.trim()) newErrors.description = "Required";

    if (Object.values(newErrors).some(Boolean)) {
      setErrors(newErrors);
      return;
    }

    setSubmitting(true);
    try {
      // Convert images to Base64
      const base64Images = await Promise.all(form.imageFiles.map(fileToBase64));

      const payload = {
        owner: { userId: Number(ownerId) },
        title: form.title,
        cost: costNum,
        deposit: depNum,
        size: form.size,
        availability: true,
        availabilityDay: form.availabilityDay
          ? `${form.availabilityDay}T00:00:00`
          : null,
        description: form.description,
        image: [],
        rating: 0,
        tag: form.tag || null,
        brand: form.brand || null,
        locationOfClothing: form.locationOfClothing || null,
      };

      // Update listing
      const res = await fetch(`${API_BASE}/api/clothing-cards/${id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(
          data?.error || data?.message || `Update failed (${res.status})`
        );
      }

      const updated = await res.json();

      // Upload new images
      if (updated?.clothingId && base64Images.length > 0) {
        const imgRes = await fetch(
          `${API_BASE}/api/clothing-cards/${updated.clothingId}/images`,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(base64Images),
          }
        );
        if (!imgRes.ok) {
          const d = await imgRes.json().catch(() => ({}));
          throw new Error(
            d?.error || d?.message || `Image upload failed (${imgRes.status})`
          );
        }
      }

      setTimeout(() => router.push("/dashboard"), 800);
    } catch (err) {
      setError(err.message || "Something went wrong");
    } finally {
      setSubmitting(false);
    }
  }

  function updateField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function onPickImages(files) {
    const arr = Array.from(files || []);
    if (!arr.length) return;
    setForm((prev) => ({ ...prev, imageFiles: [...prev.imageFiles, ...arr] }));
    setImagePreviews((prev) => [
      ...prev,
      ...arr.map((f) => URL.createObjectURL(f)),
    ]);
  }

  function removeImageAt(index) {
    setImagePreviews((prev) => prev.filter((_, i) => i !== index));
    setForm((prev) => ({
      ...prev,
      imageFiles: prev.imageFiles.filter((_, i) => i !== index),
    }));
  }

  if (!hasToken) {
    return (
      <main>
        <Header />
        <div className="max-w-2xl mx-auto p-6">
          <div className="rounded-xl border bg-white/80 p-6 shadow">
            Please log in to edit a listing.
          </div>
        </div>
      </main>
    );
  }

  return (
    <main>
      <Header />
      <div className="max-w-3xl mx-auto p-6">
        <h1 className="text-3xl font-bold mb-4">Edit Listing</h1>

        <form onSubmit={handleSubmit} className="space-y-5">
          <form
            onSubmit={handleSubmit}
            className="space-y-6 bg-white p-6 rounded-2xl shadow-md"
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <Field label="Title" required>
                <input
                  type="text"
                  value={form.title}
                  onChange={(e) => updateField("title", e.target.value)}
                  className={`w-full rounded-xl border px-4 py-2 focus:ring-2 focus:ring-rose-400 focus:border-rose-500 transition ${
                    errors.title
                      ? "border-red-400 focus:ring-red-300"
                      : "border-gray-300"
                  }`}
                  placeholder="Elegant Silk Dress"
                />
              </Field>

              <Field label="Cost ($)" required>
                <input
                  type="number"
                  value={form.cost}
                  onChange={(e) => updateField("cost", e.target.value)}
                  className={`w-full rounded-xl border px-4 py-2 focus:ring-2 focus:ring-rose-400 focus:border-rose-500 transition ${
                    errors.cost
                      ? "border-red-400 focus:ring-red-300"
                      : "border-gray-300"
                  }`}
                  placeholder="50"
                />
              </Field>

              <Field label="Deposit ($)" required>
                <input
                  type="number"
                  value={form.deposit}
                  onChange={(e) => updateField("deposit", e.target.value)}
                  className={`w-full rounded-xl border px-4 py-2 focus:ring-2 focus:ring-rose-400 focus:border-rose-500 transition ${
                    errors.deposit
                      ? "border-red-400 focus:ring-red-300"
                      : "border-gray-300"
                  }`}
                  placeholder="20"
                />
              </Field>

              <Field label="Size" required>
                <input
                  type="text"
                  value={form.size}
                  onChange={(e) => updateField("size", e.target.value)}
                  className={`w-full rounded-xl border px-4 py-2 focus:ring-2 focus:ring-rose-400 focus:border-rose-500 transition ${
                    errors.size
                      ? "border-red-400 focus:ring-red-300"
                      : "border-gray-300"
                  }`}
                  placeholder="M / 10 / Large"
                />
              </Field>

              <Field label="Availability Day" required>
                <input
                  type="date"
                  value={form.availabilityDay}
                  onChange={(e) =>
                    updateField("availabilityDay", e.target.value)
                  }
                  className={`w-full rounded-xl border px-4 py-2 focus:ring-2 focus:ring-rose-400 focus:border-rose-500 transition ${
                    errors.availabilityDay
                      ? "border-red-400 focus:ring-red-300"
                      : "border-gray-300"
                  }`}
                />
              </Field>

              <Field label="Tag">
                <input
                  type="text"
                  value={form.tag}
                  onChange={(e) => updateField("tag", e.target.value)}
                  className="w-full rounded-xl border px-4 py-2 border-gray-300 focus:ring-2 focus:ring-rose-400 focus:border-rose-500 transition"
                  placeholder="Formal, Casual"
                />
              </Field>

              <Field label="Brand">
                <input
                  type="text"
                  value={form.brand}
                  onChange={(e) => updateField("brand", e.target.value)}
                  className="w-full rounded-xl border px-4 py-2 border-gray-300 focus:ring-2 focus:ring-rose-400 focus:border-rose-500 transition"
                  placeholder="Zara"
                />
              </Field>

              <Field label="Location">
                <input
                  type="text"
                  value={form.locationOfClothing}
                  onChange={(e) =>
                    updateField("locationOfClothing", e.target.value)
                  }
                  className="w-full rounded-xl border px-4 py-2 border-gray-300 focus:ring-2 focus:ring-rose-400 focus:border-rose-500 transition"
                  placeholder="Sydney"
                />
              </Field>
            </div>

            <Field label="Description" required>
              <textarea
                value={form.description}
                onChange={(e) => updateField("description", e.target.value)}
                className={`w-full rounded-xl border px-4 py-2 focus:ring-2 focus:ring-rose-400 focus:border-rose-500 transition ${
                  errors.description
                    ? "border-red-400 focus:ring-red-300"
                    : "border-gray-300"
                }`}
                placeholder="Write a short description"
                rows={4}
              />
            </Field>

            {error && <Alert type="error" message={error} />}
            {success && <Alert type="success" message={success} />}
          </form>

          {error && <Alert type="error" message={error} />}
          {success && <Alert type="success" message={success} />}

          <button
            type="submit"
            disabled={!canSubmit || submitting}
            className="px-5 py-2.5 rounded-xl text-white bg-rose-600 hover:bg-rose-700 disabled:opacity-60"
          >
            {submitting ? "Updating…" : "Update Listing"}
          </button>
        </form>
      </div>
    </main>
  );
}

function Field({ label, required, children }) {
  return (
    <label className="block text-sm">
      <span className="mb-1 inline-block font-medium">
        {label}
        {required ? " *" : ""}
      </span>
      <div>{children}</div>
    </label>
  );
}

function Alert({ type, message }) {
  const bg = type === "error" ? "bg-red-50" : "bg-emerald-50";
  const border = type === "error" ? "border-red-200" : "border-emerald-200";
  const text = type === "error" ? "text-red-700" : "text-emerald-700";
  return (
    <div
      className={`rounded-xl border ${border} ${bg} px-3 py-2 text-sm ${text}`}
    >
      {message}
    </div>
  );
}

function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

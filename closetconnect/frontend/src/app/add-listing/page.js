"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Header from "../../components/Header";
import { useJsApiLoader, Autocomplete } from "@react-google-maps/api";
import AddressAutocomplete from "../../components/AddressAutocomplete";

export default function AddListingPage() {
  const router = useRouter();
  const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

  const { isLoaded } = useJsApiLoader({
    googleMapsApiKey: process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY,
    libraries: ["places"],
  });

  const [hasToken, setHasToken] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [errors, setErrors] = useState({
    cost: "",
    deposit: "",
    size: "",
    availabilityDay: "",
    description: "",
    imageFile: "",
  });

  const [form, setForm] = useState({
    cost: "",
    deposit: "",
    size: "",
    availabilityDay: "",
    description: "",
    tag: "",
    brand: "",
    locationOfClothing: "",
    title: "",
    gender: false,
    imageFiles: [],
  });
  const [imagePreviews, setImagePreviews] = useState([]);

  const fileInputRef = useRef(null);
  const [openingPicker, setOpeningPicker] = useState(false);
  const lastPickerClickTs = useRef(0);
  const autocompleteRef = useRef(null);

  useEffect(() => {
    function onFocusBack() {
      setOpeningPicker(false);
    }
    window.addEventListener("focus", onFocusBack);
    return () => window.removeEventListener("focus", onFocusBack);
  }, []);

  useEffect(() => {
    const token = localStorage.getItem("token");
    setHasToken(Boolean(token));
  }, []);

  const canSubmit = useMemo(() => {
    return (
      hasToken &&
      form.title &&
      form.cost &&
      form.deposit &&
      form.size &&
      form.description &&
      form.availabilityDay &&
      Array.isArray(form.imageFiles) &&
      form.imageFiles.length > 0
    );
  }, [hasToken, form]);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setErrors({
      cost: "",
      deposit: "",
      size: "",
      availabilityDay: "",
      description: "",
      imageFile: "",
    });

    const newErrors = {
      cost: "",
      deposit: "",
      size: "",
      availabilityDay: "",
      description: "",
      imageFile: "",
    };
    const hasTokenLocal = Boolean(localStorage.getItem("token"));
    const ownerIdLocal = localStorage.getItem("userId");
    const costNum = Number(String(form.cost).replace(/[^0-9.]/g, ""));
    const depNum = Number(String(form.deposit).replace(/[^0-9.]/g, ""));
    if (!(hasTokenLocal && ownerIdLocal)) {
      setError("Please log in before creating a listing.");
      return;
    }
    if (!form.cost || isNaN(costNum)) newErrors.cost = "Enter a valid amount";
    if (!form.deposit || isNaN(depNum))
      newErrors.deposit = "Enter a valid amount";
    if (!form.size.trim()) newErrors.size = "Required";
    if (!form.availabilityDay) newErrors.availabilityDay = "Required";
    if (!form.description.trim()) newErrors.description = "Required";
    if (!form.title.trim()) newErrors.description = "Title is required";
    if (!form.imageFiles || form.imageFiles.length === 0)
      newErrors.imageFile = "Select at least one image";

    const hasAny = Object.values(newErrors).some(Boolean);
    if (hasAny) {
      setErrors(newErrors);
      return;
    }

    setSubmitting(true);
    try {
      const base64Images = [];
      for (const f of form.imageFiles) {
        const b64 = await fileToBase64(f);
        base64Images.push(b64);
      }

      const ownerId = ownerIdLocal;
      const availabilityIso = form.availabilityDay
        ? `${form.availabilityDay}T00:00:00`
        : null;

      const payload = {
        owner: ownerId ? { userId: Number(ownerId) } : null,
        title: String(form.title),
        cost: Number(String(form.cost).replace(/[^0-9.]/g, "")),
        deposit: Number(String(form.deposit).replace(/[^0-9.]/g, "")),
        size: String(form.size),
        availability: true,
        availabilityDay: availabilityIso,
        description: form.description,
        image: [],
        rating: 0,
        tag: form.tag || null,
        brand: form.brand || null,
        locationOfClothing: form.locationOfClothing || null,
        gender: form.gender,
      };

      const res = await fetch(`${API_BASE}/api/clothing-cards`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(
          data?.error || data?.message || `Create failed (${res.status})`
        );
      }

      const created = await res.json();
      if (created?.clothingId && base64Images.length > 0) {
        const upRes = await fetch(
          `${API_BASE}/api/clothing-cards/${created.clothingId}/images`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(base64Images),
          }
        );
        if (!upRes.ok) {
          const d = await upRes.json().catch(() => ({}));
          throw new Error(
            d?.error || d?.message || `Image upload failed (${upRes.status})`
          );
        }
        const uploaded = await upRes.json().catch(() => []);
        const cover = uploaded?.[0]?.imageId;
        if (cover) {
          await fetch(
            `${API_BASE}/api/clothing-cards/${created.clothingId}/cover?imageId=${cover}`,
            {
              method: "PATCH",
            }
          );
        }
      }

      setSuccess("Listing created! Redirecting…");
      setTimeout(() => router.push(`/dashboard`), 800);
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
    if (arr.length === 0) {
      setOpeningPicker(false);
      return;
    }
    setForm((prev) => ({ ...prev, imageFiles: [...prev.imageFiles, ...arr] }));
    setImagePreviews((prev) => [
      ...prev,
      ...arr.map((f) => URL.createObjectURL(f)),
    ]);
    setOpeningPicker(false);
  }

  function removeImageAt(index) {
    setImagePreviews((prev) => prev.filter((_, i) => i !== index));
    setForm((prev) => ({
      ...prev,
      imageFiles: prev.imageFiles.filter((_, i) => i !== index),
    }));
  }

  function moveImage(index, direction) {
    setImagePreviews((prev) => {
      const next = prev.slice();
      const newIndex = index + direction;
      if (newIndex < 0 || newIndex >= next.length) return prev;
      const tmp = next[index];
      next[index] = next[newIndex];
      next[newIndex] = tmp;
      return next;
    });
    setForm((prev) => {
      const files = prev.imageFiles.slice();
      const newIndex = index + direction;
      if (newIndex < 0 || newIndex >= files.length) return prev;
      const tmp = files[index];
      files[index] = files[newIndex];
      files[newIndex] = tmp;
      return { ...prev, imageFiles: files };
    });
  }

  if (!isLoaded) return <p>Loading Google Maps…</p>;

  if (!hasToken) {
    return (
      <main>
        <Header />
        <div className="max-w-2xl mx-auto p-6">
          <div className="rounded-xl border bg-white/80 p-6 shadow">
            Please log in to add a listing.
          </div>
        </div>
      </main>
    );
  }

  return (
    <main>
      <Header />

      <div className="max-w-3xl mx-auto p-6">
        <h1 className="text-3xl font-bold mb-4">Add a Listing</h1>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Field label="Title" required>
              <input
                type="text"
                value={form.title}
                onChange={(e) => updateField("title", e.target.value)}
                className={`w-full rounded-xl border px-3 py-2 ${
                  !form.title ? "border-red-400 focus:border-red-500" : ""
                }`}
                placeholder="e.g. Elegant Silk Dress"
              />
            </Field>
            <Field label="Cost (per day)" required>
              <div className="relative">
                <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-neutral-500">
                  $
                </span>
                <input
                  inputMode="decimal"
                  pattern="[0-9]*"
                  value={form.cost}
                  onChange={(e) =>
                    updateField("cost", e.target.value.replace(/[^0-9.]/g, ""))
                  }
                  className={`w-full rounded-xl border px-8 py-2 appearance-none [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none ${
                    errors.cost ? "border-red-400 focus:border-red-500" : ""
                  }`}
                  placeholder="0.00"
                />
              </div>
              {errors.cost ? (
                <p className="mt-1 text-xs text-red-600">{errors.cost}</p>
              ) : null}
            </Field>
            <Field label="Deposit" required>
              <div className="relative">
                <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-neutral-500">
                  $
                </span>
                <input
                  inputMode="decimal"
                  pattern="[0-9]*"
                  value={form.deposit}
                  onChange={(e) =>
                    updateField(
                      "deposit",
                      e.target.value.replace(/[^0-9.]/g, "")
                    )
                  }
                  className={`w-full rounded-xl border px-8 py-2 appearance-none [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none ${
                    errors.deposit ? "border-red-400 focus:border-red-500" : ""
                  }`}
                  placeholder="0.00"
                />
              </div>
              {errors.deposit ? (
                <p className="mt-1 text-xs text-red-600">{errors.deposit}</p>
              ) : null}
            </Field>
            <Field label="Size" required>
              <input
                type="text"
                placeholder="S / M / L / 8 / 10"
                value={form.size}
                onChange={(e) => updateField("size", e.target.value)}
                className={`w-full rounded-xl border px-3 py-2 ${
                  errors.size ? "border-red-400 focus:border-red-500" : ""
                }`}
              />
              {errors.size ? (
                <p className="mt-1 text-xs text-red-600">{errors.size}</p>
              ) : null}
            </Field>
            <Field label="Available from" required>
              <input
                type="date"
                value={form.availabilityDay}
                onChange={(e) => updateField("availabilityDay", e.target.value)}
                className={`w-full rounded-xl border px-3 py-2 ${
                  errors.availabilityDay
                    ? "border-red-400 focus:border-red-500"
                    : ""
                }`}
              />
              {errors.availabilityDay ? (
                <p className="mt-1 text-xs text-red-600">
                  {errors.availabilityDay}
                </p>
              ) : null}
            </Field>
            <Field label="Gender" required>
              <select
                value={form.gender}
                onChange={(e) =>
                  updateField("gender", e.target.value === "true")
                }
                className="w-full rounded-xl border px-3 py-2"
              >
                <option value={false}>Female</option>
                <option value={true}>Male</option>
              </select>
            </Field>
          </div>

          <Field label="Description" required>
            <textarea
              rows={5}
              value={form.description}
              onChange={(e) => updateField("description", e.target.value)}
              className={`w-full rounded-xl border px-3 py-2 ${
                errors.description ? "border-red-400 focus:border-red-500" : ""
              }`}
            />
            {errors.description ? (
              <p className="mt-1 text-xs text-red-600">{errors.description}</p>
            ) : null}
          </Field>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Field label="Tag">
              <input
                type="text"
                value={form.tag}
                onChange={(e) => updateField("tag", e.target.value)}
                className="w-full rounded-xl border px-3 py-2"
              />
            </Field>
            <Field label="Brand">
              <input
                type="text"
                value={form.brand}
                onChange={(e) => updateField("brand", e.target.value)}
                className="w-full rounded-xl border px-3 py-2"
              />
            </Field>
            <Field label="Location">
              <AddressAutocomplete
                value={form.locationOfClothing}
                onChange={(val) => updateField("locationOfClothing", val)}
              />
            </Field>
          </div>

          <Field label="Images" required>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => {
                  const now = Date.now();
                  if (openingPicker || now - lastPickerClickTs.current < 500)
                    return;
                  lastPickerClickTs.current = now;
                  setOpeningPicker(true);
                  if (fileInputRef.current) {
                    fileInputRef.current.value = "";
                    fileInputRef.current.click();
                  }
                }}
                className="inline-flex items-center rounded-xl bg-rose-600 px-4 py-2 text-white hover:bg-rose-700"
              >
                Add photos
              </button>
              <span className="text-xs text-neutral-500">
                You can add multiple and reorder them below
              </span>
              <input
                ref={fileInputRef}
                id="file-input-hidden"
                type="file"
                accept="image/*"
                multiple
                onChange={(e) => onPickImages(e.target.files)}
                className="hidden"
              />
            </div>

            {imagePreviews.length > 0 ? (
              <div
                className="mt-3 grid grid-cols-2 md:grid-cols-3 gap-3 w-full"
                onClick={(e) => e.stopPropagation()}
                onDragOver={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                }}
                onDrop={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  const files = e.dataTransfer.files;
                  if (files && files.length > 0) onPickImages(files);
                }}
              >
                {imagePreviews.map((src, idx) => (
                  <div
                    key={idx}
                    className="relative group"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <img
                      src={src}
                      alt={`Preview ${idx + 1}`}
                      className="h-32 w-full object-cover rounded-lg border"
                    />
                    <span className="absolute left-1.5 bottom-1.5 rounded-full bg-black/65 text-white text-[11px] leading-none px-1.5 py-1">
                      {idx + 1}
                    </span>
                    <button
                      type="button"
                      aria-label="Remove image"
                      onClick={() => removeImageAt(idx)}
                      className="absolute right-1 top-1 hidden group-hover:block rounded-full bg-black/60 text-white text-xs px-1.5 py-0.5"
                    >
                      ✕
                    </button>
                    <div className="absolute left-1 top-1 hidden gap-1 group-hover:flex">
                      <button
                        type="button"
                        aria-label="Move up"
                        onClick={() => moveImage(idx, -1)}
                        className="rounded-full bg-black/60 text-white text-xs px-1.5 py-0.5"
                      >
                        ↑
                      </button>
                      <button
                        type="button"
                        aria-label="Move down"
                        onClick={() => moveImage(idx, 1)}
                        className="rounded-full bg-black/60 text-white text-xs px-1.5 py-0.5"
                      >
                        ↓
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="mt-3 h-32 w-full grid place-items-center rounded-xl border border-dashed text-neutral-400">
                No images selected
              </div>
            )}
          </Field>

          {error ? <p className="text-red-600 text-sm">{error}</p> : null}
          {success ? <p className="text-green-600 text-sm">{success}</p> : null}

          <div className="flex justify-end">
            <button
              type="submit"
              disabled={!canSubmit || submitting}
              className="rounded-xl bg-rose-600 px-6 py-2 font-semibold text-white disabled:opacity-50"
            >
              {submitting ? "Submitting…" : "Create Listing"}
            </button>
          </div>
        </form>
      </div>
    </main>
  );
}

function Field({ label, required, children }) {
  return (
    <label className="block text-sm font-medium text-neutral-800">
      {label}
      {required && <span className="text-rose-600">*</span>}
      <div className="mt-1">{children}</div>
    </label>
  );
}

function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result.split(",")[1]);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

export default function Page() {
  const router = useRouter();

  const [email, setEmail]       = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading]   = useState(false);
  const [err, setErr]           = useState("");
  const [ok, setOk]             = useState("");

  const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

  async function handleLogin(e) {
    e.preventDefault();
    setErr(""); setOk("");

    if (!email.trim() || !password.trim()) {
      return setErr("Email + password, fam.");
    }

    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        if (res.status === 400 || res.status === 401 || res.status === 403) {
          throw new Error("Wrong email or password, try again");
        }
        throw new Error(data?.error || data?.message || `Login failed (${res.status})`);
      }

      const data = await res.json(); // { token, id, email, name }
      localStorage.setItem("token", data.token);
      localStorage.setItem("userId", data.id); // ✅ store the user ID
      setOk("Welcome back ✨ Redirecting…");
      setTimeout(() => router.push("/home"), 600);
    } catch (e2) {
      setErr(e2.message || "Couldn’t log you in.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="relative min-h-screen bg-gradient-to-b from-rose-50 via-white to-rose-50 dark:from-neutral-900 dark:via-neutral-950 dark:to-neutral-900">
      {/* soft blobs */}
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -top-24 -left-24 h-72 w-72 rounded-full bg-rose-300/30 blur-3xl dark:bg-rose-500/20" />
        <div className="absolute -bottom-24 -right-24 h-72 w-72 rounded-full bg-pink-300/30 blur-3xl dark:bg-pink-500/20" />
      </div>

      <div className="mx-auto grid max-w-6xl grid-cols-1 gap-8 px-6 py-10 md:grid-cols-2 md:py-16">
        {/* Left image / vibe */}
        <section className="relative hidden overflow-hidden rounded-2xl md:block">
          <img
            src="/mainimage.png"
            alt="Closet-Connect — curated fits"
            className="h-full w-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/40 to-transparent" />
          <div className="absolute inset-0 flex flex-col justify-end p-8 text-white">
            <h1 className="text-3xl font-bold md:text-4xl">Closet-Connect</h1>
            <p className="mt-2 max-w-md text-white/85">
              Sign in to manage listings, wishlist drops, and alerts.
            </p>
          </div>
        </section>

        {/* Right: login card */}
        <section className="flex items-center">
          <div className="w-full rounded-2xl border border-black/5 bg-white/80 p-6 shadow-xl backdrop-blur-md dark:border-white/10 dark:bg-neutral-900/70 md:p-8">
            <div className="mb-6 text-center">
              <div className="mx-auto mb-3 grid h-12 w-12 place-items-center rounded-2xl bg-rose-100 dark:bg-rose-900/40">
                <span className="text-rose-600 dark:text-rose-400 text-xl">⎆</span>
              </div>
              <h2 className="text-2xl font-semibold tracking-tight">Welcome back</h2>
              <p className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">
                Let’s get you back to your closet.
              </p>
            </div>

            <form onSubmit={handleLogin} className="space-y-4">
              {/* Email */}
              <div className="grid gap-2">
                <label className="text-sm font-medium">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  className="w-full rounded-xl border border-neutral-200 bg-white/70 px-4 py-2.5 text-[15px] outline-none transition focus:border-rose-400 focus:bg-white focus:shadow-[0_0_0_4px_rgba(244,114,182,0.15)] dark:border-neutral-700 dark:bg-neutral-900 dark:focus:border-rose-500"
                />
              </div>

              {/* Password */}
              <div className="grid gap-2">
                <label className="text-sm font-medium">Password</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••"
                  className="w-full rounded-xl border border-neutral-200 bg-white/70 px-4 py-2.5 text-[15px] outline-none transition focus:border-rose-400 focus:bg-white focus:shadow-[0_0_0_4px_rgba(244,114,182,0.15)] dark:border-neutral-700 dark:bg-neutral-900 dark:focus:border-rose-500"
                />
                <div className="flex items-center justify-between text-xs">
                  <span className="text-neutral-500">Use a strong pass, king 👑</span>
                  <Link href="/forgot" className="text-rose-600 hover:underline dark:text-rose-400">
                    Forgot password?
                  </Link>
                </div>
              </div>

              {/* alerts */}
              {err && (
                <div className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900/50 dark:bg-red-900/20 dark:text-red-300">
                  {err}
                </div>
              )}
              {ok && (
                <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-900/20 dark:text-emerald-300">
                  {ok}
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="group relative w-full rounded-xl bg-gradient-to-r from-rose-600 to-pink-600 px-4 py-2.5 font-semibold text-white shadow-md transition hover:brightness-110 disabled:opacity-60"
              >
                <span className="inline-flex items-center justify-center gap-2">
                  {loading ? "Logging in…" : "Log In"}
                  <span className="transition-transform group-hover:translate-x-0.5">→</span>
                </span>
                <span className="pointer-events-none absolute inset-x-3 -bottom-[2px] h-[2px] rounded-full bg-white/40 blur-[2px]" />
              </button>

              <p className="text-center text-sm">
                New here?{" "}
                <Link
                  href="/signup"
                  className="font-medium text-rose-600 underline-offset-4 hover:underline dark:text-rose-400"
                >
                  Create an account
                </Link>
              </p>
            </form>
          </div>
        </section>
      </div>

      <footer className="px-6 pb-8 text-center text-xs text-neutral-500">
        © {new Date().getFullYear()} Closet-Connect
      </footer>
    </main>
  );
}

"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import { Mail, Lock, User, Eye, EyeOff, Shield, ArrowRight, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import Link from "next/link";
import { useRouter } from "next/navigation";

// If you already have an Axios wrapper like `@/lib/api`, you can swap fetch for that.
const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? ""; // e.g. http://localhost:8000 // e.g. http://localhost:8000

function passwordScore(pw: string) {
  let score = 0;
  if (pw.length >= 8) score++;
  if (/[A-Z]/.test(pw)) score++;
  if (/[0-9]/.test(pw)) score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;
  if (pw.length >= 12) score++;
  return score; // 0..5
}

function StrengthBar({ value }: { value: number }) {
  const labels = ["weak", "meh", "okay", "strong", "beast" ];
  const pct = (value / 5) * 100;
  return (
    <div className="space-y-1">
      <div className="h-2 w-full rounded-full bg-muted overflow-hidden">
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${pct}%` }}
          transition={{ type: "spring", stiffness: 120, damping: 15 }}
          className={`h-full rounded-full ${
            value <= 1
              ? "bg-red-500"
              : value === 2
              ? "bg-amber-500"
              : value === 3
              ? "bg-yellow-500"
              : value === 4
              ? "bg-green-500"
              : "bg-emerald-600"
          }`}
        />
      </div>
      <p className="text-xs text-muted-foreground">Password strength: {labels[Math.max(0, value-1)] ?? ""}</p>
    </div>
  );
}

export default function SignUpPage() {
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const router = useRouter();
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const score = passwordScore(password);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!name.trim()) return setError("Name is required.");
    if (!email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) return setError("Hit me with a valid email.");
    if (password.length < 8) return setError("Password needs at least 8 characters.");
    if (password !== confirm) return setError("Passwords aren’t twins.");

    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, email, password })
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data?.error || data?.message || `Sign up failed (${res.status})`);
      }

      const data = await res.json();
      localStorage.setItem("token", data.token);
      localStorage.setItem("userId", data.id); // ✅ store the user ID
      setSuccess("Account created! Redirecting… ✨");
      setEmail(""); setName(""); setUsername(""); setPassword(""); setConfirm("");
      router.push("/");
    } catch (err: any) {
      setError(err.message || "Something went sideways. Try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-[100dvh] w-full bg-gradient-to-b from-background to-muted/30 grid place-items-center p-4">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="w-full max-w-md"
      >
        <Card className="shadow-xl border border-border/60 backdrop-blur-sm bg-card/90">
          <CardHeader className="space-y-2 text-center">
            <div className="mx-auto h-12 w-12 grid place-items-center rounded-2xl bg-primary/10">
              <Sparkles className="h-6 w-6 text-primary" />
            </div>
            <CardTitle className="text-2xl md:text-3xl tracking-tight">Create your Closet‑Connect account</CardTitle>
            <p className="text-sm text-muted-foreground">Level up your fits. Save closets. Trade smarter.</p>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={onSubmit}>
              <div className="grid gap-2">
                <Label htmlFor="name">Name</Label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input id="name" placeholder="Muaz Khan" value={name} onChange={(e) => setName(e.target.value)} className="pl-9" required />
                </div>
              </div>

              <div className="grid gap-2">
                <Label htmlFor="email">Email</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input id="email" type="email" placeholder="you@closet.com" value={email} onChange={(e) => setEmail(e.target.value)} className="pl-9" required />
                </div>
              </div>

              <div className="grid gap-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="username">Username <span className="text-muted-foreground">(optional)</span></Label>
                  <span className="text-[10px] text-muted-foreground">You can change this later</span>
                </div>
                <Input id="username" placeholder="muazthefitlord" value={username} onChange={(e) => setUsername(e.target.value)} />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="password">Password</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="password"
                    type={showPw ? "text" : "password"}
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="pl-9 pr-10"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPw((s) => !s)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground"
                    aria-label={showPw ? "Hide password" : "Show password"}
                  >
                    {showPw ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                <StrengthBar value={score} />
                <p className="text-[11px] text-muted-foreground flex items-center gap-1">
                  <Shield className="h-3.5 w-3.5" /> Use 12+ chars with a mix of symbols & numbers for max sauce.
                </p>
              </div>

              <div className="grid gap-2">
                <Label htmlFor="confirm">Confirm Password</Label>
                <Input id="confirm" type="password" placeholder="••••••••" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
              </div>

              {error && (
                <div className="text-sm rounded-lg bg-destructive/10 text-destructive px-3 py-2">
                  {error}
                </div>
              )}
              {success && (
                <div className="text-sm rounded-lg bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 px-3 py-2">
                  {success}
                </div>
              )}

              <Button type="submit" className="w-full group" disabled={loading}>
                {loading ? "Cooking…" : (
                  <span className="flex items-center gap-1.5">
                    Create account <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
                  </span>
                )}
              </Button>

              <div className="text-xs text-muted-foreground text-center">
                By signing up, you agree to our <Link href="/terms" className="underline underline-offset-4 hover:text-foreground">Terms</Link> and <Link href="/privacy" className="underline underline-offset-4 hover:text-foreground">Privacy</Link>.
              </div>

              <div className="text-sm text-center">
                Already have an account? <Link href="/login" className="font-medium underline underline-offset-4">Log in</Link>
              </div>
            </form>
          </CardContent>
        </Card>

        <div className="mt-6 text-center text-xs text-muted-foreground">
          © {new Date().getFullYear()} Closet‑Connect. All drip reserved.
        </div>
      </motion.div>
    </div>
  );
}

/**
 * 🔌 Quick wiring notes
 * 1) Drop this file at: app/(auth)/signup/page.tsx (App Router) or src/pages/signup.tsx (Pages Router).
 * 2) Ensure shadcn/ui is set up with Button, Card, Input, Label. Tailwind enabled.
 * 3) Set NEXT_PUBLIC_API_BASE in .env.local to your backend origin (e.g. http://localhost:8000)
 * 4) Backend should accept POST /api/auth/register with JSON { name, email, password }. Login: POST /api/auth/login. WhoAmI: GET /api/auth/me (Authorization: Bearer <token>).
 */

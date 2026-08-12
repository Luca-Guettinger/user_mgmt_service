// app/api/login/route.ts
import { NextResponse } from "next/server"

// Server-side (inside the container) -> talk to the backend over the Docker network.
// Fall back to the public URL so `pnpm dev` on your laptop still works.
const API_URL = process.env.INTERNAL_API_URL ?? process.env.NEXT_PUBLIC_API_URL

export async function POST(req: Request) {
  try {
    const body = await req.json()

    const res = await fetch(`${API_URL}/users/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    })

    if (!res.ok) {
      // log the real reason instead of swallowing it
      console.error("login upstream failed", res.status, await res.text())
      return NextResponse.json(
        { message: "Invalid credentials" },
        { status: res.status === 401 || res.status === 403 ? 401 : 502 }
      )
    }

    // Token can arrive either as an Authorization header or in the JSON body,
    // depending on how the Spring backend is written. Accept both.
    const authHeader = res.headers.get("authorization")
    let token = authHeader?.replace(/^Bearer\s+/i, "")

    if (!token) {
      const data = await res.clone().json().catch(() => null)
      token = data?.token ?? data?.accessToken ?? data?.jwt
    }

    if (!token) {
      console.error("login: no token in response", [...res.headers.entries()])
      return NextResponse.json({ error: "No token" }, { status: 500 })
    }

    const response = NextResponse.json({ success: true })

    response.cookies.set("jwt", token, {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      path: "/",
      maxAge: 60 * 60 * 24 * 7,
    })

    return response
  } catch (err) {
    console.error("login route error:", err)
    return NextResponse.json({ message: "Server error" }, { status: 500 })
  }
}
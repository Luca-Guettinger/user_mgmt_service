// app/api/me/route.ts
import { cookies } from "next/headers"

const API_URL = process.env.INTERNAL_API_URL ?? process.env.NEXT_PUBLIC_API_URL

export async function GET() {
  const token = (await cookies()).get("jwt")?.value

  if (!token) {
    return Response.json({ error: "Unauthorized" }, { status: 401 })
  }

  try {
    const res = await fetch(`${API_URL}/users/me`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })

    if (!res.ok) {
      console.error("me upstream failed", res.status, await res.text())
      return Response.json({ error: "Failed to fetch user" }, { status: res.status })
    }

    return Response.json(await res.json())
  } catch (err) {
    console.error("me route error:", err)
    return Response.json({ error: "Server error" }, { status: 500 })
  }
}
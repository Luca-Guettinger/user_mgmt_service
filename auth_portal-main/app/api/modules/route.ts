import { cookies } from "next/headers"

// The browser cannot talk to the backend itself: the JWT is an httpOnly cookie,
// so client-side JavaScript can never read it. These two handlers run on the
// server, add the Authorization header, and pass the answer back.

const API = process.env.NEXT_PUBLIC_API_URL

async function token() {
  return (await cookies()).get("jwt")?.value
}

// The module catalogue.
export async function GET() {
  const jwt = await token()

  if (!jwt) {
    return Response.json({ error: "Unauthorized" }, { status: 401 })
  }

  const res = await fetch(`${API}/modules`, {
    headers: { Authorization: `Bearer ${jwt}` },
    cache: "no-store",
  })

  if (!res.ok) {
    return Response.json({ error: "Could not load modules" }, { status: res.status })
  }

  return Response.json(await res.json())
}

// Assign one module to whoever is logged in. Body: { moduleId }.
export async function POST(req: Request) {
  const jwt = await token()

  if (!jwt) {
    return Response.json({ error: "Unauthorized" }, { status: 401 })
  }

  const { moduleId } = await req.json()

  if (!moduleId) {
    return Response.json({ error: "moduleId is required" }, { status: 400 })
  }

  // Ask the backend who the caller is, so the browser never has to know or
  // send a user id - the JWT alone decides whose modules are changed.
  const me = await fetch(`${API}/users/me`, {
    headers: { Authorization: `Bearer ${jwt}` },
    cache: "no-store",
  })

  if (!me.ok) {
    return Response.json({ error: "Could not identify the user" }, { status: me.status })
  }

  const { id } = await me.json()

  const assigned = await fetch(`${API}/users/${id}/modules/${moduleId}`, {
    method: "PUT",
    headers: { Authorization: `Bearer ${jwt}` },
  })

  if (!assigned.ok) {
    // 404 unknown module, 503 module service unreachable.
    return Response.json({ error: "Could not assign the module" }, { status: assigned.status })
  }

  return Response.json({ success: true })
}

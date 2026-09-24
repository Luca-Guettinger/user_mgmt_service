import { cookies } from "next/headers"

// The browser cannot talk to the backend itself: the JWT is an httpOnly cookie,
// so client-side JavaScript can never read it. These two handlers run on the
// server, add the Authorization header, and pass the answer back.

const API = process.env.NEXT_PUBLIC_API_URL

async function token() {
  return (await cookies()).get("jwt")?.value
}

// Asks the backend who the caller is. The browser never sends a user id, so the
// JWT alone decides whose modules are read or changed.
async function userId(jwt: string) {
  const me = await fetch(`${API}/users/me`, {
    headers: { Authorization: `Bearer ${jwt}` },
    cache: "no-store",
  })
  return me.ok ? (await me.json()).id as string : null
}

// The catalogue, plus the ids this user already has, so the buttons can show
// the real state instead of only remembering the current page view.
export async function GET() {
  const jwt = await token()

  if (!jwt) {
    return Response.json({ error: "Unauthorized" }, { status: 401 })
  }

  const id = await userId(jwt)

  if (!id) {
    return Response.json({ error: "Could not identify the user" }, { status: 401 })
  }

  const auth = { headers: { Authorization: `Bearer ${jwt}` }, cache: "no-store" as RequestCache }
  const [all, mine] = await Promise.all([
    fetch(`${API}/modules`, auth),
    fetch(`${API}/users/${id}/modules`, auth),
  ])

  if (!all.ok || !mine.ok) {
    const status = all.ok ? mine.status : all.status
    return Response.json({ error: "Could not load modules" }, { status })
  }

  const assigned: { id: string }[] = await mine.json()

  return Response.json({
    modules: await all.json(),
    assigned: assigned.map((m) => m.id),
  })
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

  const id = await userId(jwt)

  if (!id) {
    return Response.json({ error: "Could not identify the user" }, { status: 401 })
  }

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

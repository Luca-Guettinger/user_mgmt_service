"use client"

import { useEffect, useState } from "react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"

type Module = {
  id: string
  code: string
  name: string
  description: string | null
}

// Everything here goes through /api/modules, never straight to the backend:
// the JWT is an httpOnly cookie that this code cannot read.
export function Modules() {
  const [modules, setModules] = useState<Module[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [assigned, setAssigned] = useState<string[]>([])

  // The route returns the catalogue and the ids this user already has, so a
  // reload or a fresh login shows the true state rather than a blank slate.
  const load = () =>
    fetch("/api/modules")
      .then((res) => {
        if (!res.ok) throw new Error(`Modules unavailable (${res.status})`)
        return res.json()
      })
      .then((data: { modules: Module[]; assigned: string[] }) => {
        setModules(data.modules)
        setAssigned(data.assigned)
      })

  useEffect(() => {
    load()
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  const assign = async (moduleId: string) => {
    setBusy(moduleId)
    setError(null)
    try {
      const res = await fetch("/api/modules", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ moduleId }),
      })
      if (!res.ok) throw new Error(`Could not assign (${res.status})`)
      // Re-read instead of guessing: what the database says is the truth, and
      // assigning is idempotent so pressing it twice is harmless.
      await load()
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(null)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Available modules</CardTitle>
        <CardDescription>
          Served by the module service and assigned through the user management service.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {loading && <p className="text-sm text-muted-foreground">Loading modules ...</p>}

        {error && <p className="text-sm text-destructive">{error}</p>}

        {!loading && !error && modules.length === 0 && (
          <p className="text-sm text-muted-foreground">No modules available.</p>
        )}

        {modules.map((module) => (
          <div
            key={module.id}
            className="flex items-center justify-between gap-4 rounded-lg border p-3"
          >
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <Badge variant="secondary">{module.code}</Badge>
                <span className="font-medium">{module.name}</span>
              </div>
              {module.description && (
                <p className="mt-1 text-sm text-muted-foreground">{module.description}</p>
              )}
            </div>
            <Button
              size="sm"
              variant={assigned.includes(module.id) ? "outline" : "default"}
              disabled={busy === module.id || assigned.includes(module.id)}
              onClick={() => assign(module.id)}
            >
              {busy === module.id
                ? "Assigning ..."
                : assigned.includes(module.id)
                  ? "Assigned"
                  : "Assign to me"}
            </Button>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}

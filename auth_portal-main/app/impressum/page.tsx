import Link from "next/link"
import {Button} from "@/components/ui/button"

export default function Impressum() {
    return (
        <div className="flex min-h-svh flex-col justify-center px-6 py-12 md:px-10">
            <div className="mx-auto w-full max-w-2xl space-y-8">

                <div className="space-y-2">
                    <h1 className="text-4xl font-bold tracking-tight">
                        Impressum
                    </h1>
                    <p className="text-muted-foreground">
                        Angaben gemäss Informationspflicht
                    </p>
                </div>

                <div className="space-y-6">
                    <section className="space-y-1">
                        <h2 className="text-lg font-semibold">Verantwortlich für den Inhalt</h2>
                        <p className="text-muted-foreground">
                            User Management Service<br />
                            Luca Güttinger<br />
                            Schweiz
                        </p>
                    </section>

                    <section className="space-y-1">
                        <h2 className="text-lg font-semibold">Kontakt</h2>
                        <p className="text-muted-foreground">
                            <a
                                href="mailto:martin@hoechli.net"
                                className="underline underline-offset-4"
                            >
                                martin@hoechli.net
                            </a>
                        </p>
                    </section>

                    <section className="space-y-1">
                        <h2 className="text-lg font-semibold">Haftungsausschluss</h2>
                        <p className="text-muted-foreground">
                            Die Inhalte dieser Seite wurden mit Sorgfalt erstellt. Für die Richtigkeit,
                            Vollständigkeit und Aktualität der Inhalte kann jedoch keine Gewähr
                            übernommen werden.
                        </p>
                    </section>
                </div>

                <Button asChild variant="outline">
                    <Link href="/dashboard">Zurück zum Dashboard</Link>
                </Button>

            </div>
        </div>
    )
}

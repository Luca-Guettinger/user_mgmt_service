# Build & Deploy

Four containers: `traefik` (proxy and HTTPS), `postgres`, `backend` (Spring Boot), `portal` (Next.js).

Traefik is configured by the files in `traefik/`, not by labels on the containers:

* `traefik/traefik.yml` has the ports, TLS and Let's Encrypt settings
* `traefik/dynamic.yml` has the routes

Routing is simple:

| Path | Goes to |
|---|---|
| `/backend/*` | `backend:8080`, with `/backend` stripped off |
| anything else | `portal:3000` |

## Running it locally

You need a `.env` file. Copy `.env.example` and fill in the passwords.

Ports 80 and 443 are usually not available on a dev machine, so override them in `.env`:

```env
HTTP_PORT=8880
HTTPS_PORT=8443
NEXT_PUBLIC_API_URL=https://localhost:8443/backend
```

The base compose file pulls prebuilt images, so add the dev override to build from source instead:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

Check that it works. The `-k` is needed because there is no real certificate for localhost:

```bash
curl -k https://localhost:8443/backend/actuator/health
curl -k -L https://localhost:8443/
```

Stop it with `docker compose down`, or `docker compose down -v` to also wipe the database.

The Traefik dashboard is on <http://localhost:8889/dashboard/>. Both routes should show up there under
the File provider.

## Deploying

Push to `main`. The workflow runs the tests, builds both images, pushes them to GHCR, then connects to
the server over SSH and restarts it. The server never builds anything, it only pulls the images. The
commands it runs are in the `Pull and restart` step of the workflow.

If the app does not come up afterwards, the workflow fails.

### Setting it up

On the server, so the deploy does not need a password for Docker:

```bash
sudo usermod -aG docker ubuntu
```

Then create a key for the deploy and put it on the server. The second command asks for your server
password, and it is the only time you need it:

```bash
ssh-keygen -t ed25519 -C gha-deploy -f ~/.ssh/nightnode-deploy -N ""

cat ~/.ssh/nightnode-deploy.pub | ssh ubuntu@compose.nightnode.io \
  "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
```

Check it worked. This should not ask for a password:

```bash
ssh -i ~/.ssh/nightnode-deploy ubuntu@compose.nightnode.io "docker compose version"
```

Now add these under Settings, Secrets and variables, Actions.

Secrets:

| Name | Value |
|---|---|
| `SSH_PRIVATE_KEY` | the contents of `~/.ssh/nightnode-deploy`, the file without `.pub` |
| `SSH_FINGERPRINT` | `ssh-keyscan -t ed25519 compose.nightnode.io \| ssh-keygen -lf -` and take the `SHA256:...` part |

Variables:

| Name | Value |
|---|---|
| `DEPLOY_HOST` | `compose.nightnode.io` |
| `DEPLOY_USER` | `ubuntu` |
| `DEPLOY_PATH` | `/home/ubuntu/git/user_mgmt_service` |
| `NEXT_PUBLIC_API_URL` | `https://compose.nightnode.io/backend` |

### Going back to an older version

Every commit on `main` has an image tagged with its SHA:

```bash
cd /home/ubuntu/git/user_mgmt_service
IMAGE_TAG=sha-<commit> docker compose up -d
```

## Two things that are easy to get wrong

The domain and the Let's Encrypt email are in `traefik/traefik.yml` and not in `.env`. Traefik ignores
environment variables when it has a config file, so putting them in `.env` does nothing.

`NEXT_PUBLIC_API_URL` is compiled into the portal when the image is built, so it has to be the public
URL. It cannot be `http://backend:8080`, because the browser has to be able to reach it too.

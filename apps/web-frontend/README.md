# Web Frontend

React and TypeScript user interface for the notebook chat application.

## Run locally

Start a port forward to the API Gateway in one terminal:

```powershell
kubectl port-forward service/api-gateway 8080:8080
```

Then start the frontend in another terminal:

```powershell
cd apps/web-frontend
npm.cmd install
npm.cmd run dev
```

Open the URL printed by Vite (normally `http://localhost:5173`). The Vite
development server forwards `/api` and `/actuator` requests to
`http://localhost:8080`. Set `GATEWAY_TARGET` in `.env.local` to use a different
gateway address.

## Deploy to Kubernetes

From the repository root, build the image and apply the frontend Deployment and
NodePort Service:

```powershell
docker build -t finalproject/web-frontend:local -f apps/web-frontend/Dockerfile apps/web-frontend
kubectl apply -f deployments/k8s/deployments/web-frontend.yaml
kubectl rollout status deployment/web-frontend --timeout=180s
```

Open `http://localhost:30081`. The frontend Service uses NodePort `30081`, and
the Nginx container forwards API requests to the `api-gateway` Service inside
the cluster. Make sure the API Gateway is running.

The app currently supports notebook and session management, sending chat
questions, and showing source details returned by the API. Conversation history
is held in the browser only because the backend does not yet expose a history
endpoint. Document upload and external search configuration are not currently
provided by the backend API.

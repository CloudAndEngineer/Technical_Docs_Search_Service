# Kubernetes local deployment

The manifests use the current namespace and expect a default StorageClass.

To enable Gemini API calls from `chat-api`, create a Kubernetes Secret with a Google AI Studio API key. Do not commit the key:

```powershell
kubectl create secret generic gemini-credentials `
  --from-literal=api-key='<your-google-api-key>'
```

The `chat-api` Deployment reads the key as `GOOGLE_API_KEY` and uses `GEMINI_MODEL` to select the model (default: `gemini-3.8-flash`). The key reference is optional so the service can start before a key is configured.

Create the database credentials Secret before starting PostgreSQL. Use a private password and do not commit the generated Secret:

```powershell
kubectl create secret generic postgres-credentials `
  --from-literal=username=rag_app `
  --from-literal=password='<choose-a-password>'
```

Start PostgreSQL with pgvector and its persistent volume:

```powershell
kubectl apply -f deployments/k8s/statefulsets/postgres.yaml
kubectl rollout status statefulset/postgres
```

Build the chat-api image after packaging the app. For a local Kubernetes cluster, make the image available to that cluster before applying the Deployment:

```powershell
docker build -t finalproject/chat-api:local -f apps/chat-api/Dockerfile apps/chat-api
kubectl apply -f deployments/k8s/deployments/chat-api.yaml
```

Build and expose the API Gateway on NodePort `30080`:

```powershell
docker build -t finalproject/api-gateway:local -f apps/api-gateway/Dockerfile apps/api-gateway
kubectl apply -f deployments/k8s/deployments/api-gateway.yaml
```

The Gateway Service is a NodePort on `30080`. If the local Kubernetes node port is not reachable from the host, forward the Service locally:

```powershell
kubectl port-forward service/api-gateway 8080:8080
```

Then use `http://localhost:8080/api/v1/` for API requests.

Build and expose the web frontend on NodePort `30081`:

```powershell
docker build -t finalproject/web-frontend:local -f apps/web-frontend/Dockerfile apps/web-frontend
kubectl apply -f deployments/k8s/deployments/web-frontend.yaml
kubectl rollout status deployment/web-frontend --timeout=180s
```

Open `http://localhost:30081` in a browser. Nginx serves the frontend and
forwards `/api` and `/actuator` requests to the in-cluster `api-gateway` Service.
The API Gateway must be running first.

The PostgreSQL initialization script creates the shared notebook, session, and document-chunk tables, enables `vector`, and adds indexes for notebook filtering. PostgreSQL runs as one StatefulSet replica; `chat-api` and `ingestion-worker` connect through the `postgres` Service. The chat-api deployment sets `DB_URL` to `jdbc:postgresql://postgres:5432/rag_knowledge`.

The embedding column currently accepts `vector` values without a fixed dimension. Add an HNSW or IVFFlat index after choosing the embedding model and its dimension; pgvector approximate indexes need a consistent vector dimension.

The initialization script runs only when PostgreSQL initializes an empty data directory. If a persistent volume already contains a database, apply schema and extension changes through a database migration instead of expecting the initialization script to run again.

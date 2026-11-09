# Kubernetes local deployment

The manifests use the current namespace and expect a default StorageClass.

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

The PostgreSQL initialization script creates the shared notebook, session, and document-chunk tables, enables `vector`, and adds indexes for notebook filtering. PostgreSQL runs as one StatefulSet replica; `chat-api` and `ingestion-worker` connect through the `postgres` Service. The chat-api deployment sets `DB_URL` to `jdbc:postgresql://postgres:5432/rag_knowledge`.

The embedding column currently accepts `vector` values without a fixed dimension. Add an HNSW or IVFFlat index after choosing the embedding model and its dimension; pgvector approximate indexes need a consistent vector dimension.

The initialization script runs only when PostgreSQL initializes an empty data directory. If a persistent volume already contains a database, apply schema and extension changes through a database migration instead of expecting the initialization script to run again.

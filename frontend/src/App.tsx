import { useApiClient } from "./api/ApiClientContext";

export function App() {
  const apiClient = useApiClient();

  return (
    <main className="technical-shell" data-api-base-path={apiClient.basePath}>
      <div className="technical-shell__content">
        <p className="technical-shell__eyebrow">Preparación técnica</p>
        <h1>Running Coach</h1>
        <p>La base de la aplicación está preparada.</p>
      </div>
    </main>
  );
}

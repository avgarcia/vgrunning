import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import { ApiClientProvider } from "./api/ApiClientProvider";
import { App } from "./App";
import "./styles.css";

const rootElement = document.getElementById("root");

if (rootElement === null) {
  throw new Error("No se encontró el contenedor raíz de la aplicación.");
}

createRoot(rootElement).render(
  <StrictMode>
    <ApiClientProvider>
      <App />
    </ApiClientProvider>
  </StrictMode>,
);

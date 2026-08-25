import { createContext, useContext } from "react";
import { Configuration } from "@running-coach/api-client";

export const apiClientConfiguration = new Configuration({
  basePath: "/api",
  baseOptions: {
    withCredentials: true,
  },
});

export const ApiClientContext = createContext<Configuration | undefined>(undefined);

export function useApiClient(): Configuration {
  const configuration = useContext(ApiClientContext);
  if (configuration === undefined) {
    throw new Error("ApiClientProvider debe envolver la aplicación.");
  }
  return configuration;
}

import type { PropsWithChildren } from "react";

import { ApiClientContext, apiClientConfiguration } from "./ApiClientContext";

export function ApiClientProvider({ children }: PropsWithChildren) {
  return <ApiClientContext value={apiClientConfiguration}>{children}</ApiClientContext>;
}

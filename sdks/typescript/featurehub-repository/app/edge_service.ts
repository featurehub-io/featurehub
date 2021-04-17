
export interface EdgeService {
  contextChange(header: string): Promise<void>;
  clientEvaluated(): boolean;

  // do we need a new one if the header changes?
  requiresReplacementOnHeaderChange(): boolean;

  close(): void;

  poll(): Promise<void>;
}

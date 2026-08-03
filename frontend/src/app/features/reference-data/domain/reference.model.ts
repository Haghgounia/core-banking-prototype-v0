export interface PageResponse<T> {
  readonly items: readonly T[];
  readonly totalElements: number;
  readonly page: number;
  readonly size: number;
}

export interface LookupOption {
  readonly value: number;
  readonly code: string;
  readonly label: string;
}

export interface AncestorValue {
  readonly resource: string;
  readonly id: number;
  readonly label: string;
}

export interface ReferenceRecordResponse {
  readonly id: number;
  readonly values: Readonly<Record<string, unknown>>;
  readonly ancestors: readonly AncestorValue[];
}

export interface ReferenceSearchQuery {
  readonly text: string;
  readonly parentId: number | null;
  readonly active: boolean | null;
  readonly page: number;
  readonly size: number;
  readonly sortBy: string | null;
  readonly direction: 'asc' | 'desc';
}

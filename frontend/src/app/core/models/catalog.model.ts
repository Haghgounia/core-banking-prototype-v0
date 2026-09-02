export type CatalogStatus = 'ACTIVE' | 'PLANNED';

export interface CatalogItem {
  readonly resource: string;
  readonly category: 'GEOGRAPHY' | 'GENERAL' | 'EMPLOYMENT' | 'EDUCATION' | string;
  readonly title: string;
  readonly icon: string;
  readonly status: CatalogStatus;
  readonly parentResource: string | null;
}

export interface CatalogResponse {
  readonly items: readonly CatalogItem[];
}

export type FieldType = 'TEXT' | 'STRING_SELECT' | 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'LOOKUP' | 'DATE' | 'TIMESTAMP';

export interface SelectOption {
  readonly value: string | number;
  readonly label: string;
}

export interface ReferenceFieldDescriptor {
  readonly apiName: string;
  readonly columnName: string;
  readonly label: string;
  readonly type: FieldType;
  readonly required: boolean;
  readonly readOnly: boolean;
  readonly grid: boolean;
  readonly searchable: boolean;
  readonly maxLength: number | null;
  readonly defaultValue: unknown;
  readonly lookupResource: string | null;
  readonly options: readonly SelectOption[];
}

export interface ParentDescriptor {
  readonly resource: string;
  readonly apiField: string;
  readonly columnName: string;
  readonly label: string;
}

export interface ReferenceTableDescriptor {
  readonly resource: string;
  readonly category: string;
  readonly title: string;
  readonly icon: string;
  readonly schemaName: string;
  readonly tableName: string;
  readonly sequenceName: string;
  readonly idApiName: string;
  readonly idColumnName: string;
  readonly codeApiName: string;
  readonly nameApiName: string;
  readonly parent: ParentDescriptor | null;
  readonly fields: readonly ReferenceFieldDescriptor[];
}

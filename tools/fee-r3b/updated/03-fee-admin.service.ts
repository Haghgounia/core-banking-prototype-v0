import {HttpClient, HttpParams} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {firstValueFrom} from 'rxjs';
import {FeeCatalogResponse, FeeSelectOption, FeeTableDescriptor, FeeTablePage} from './fee-admin.models';

@Injectable({providedIn: 'root'})
export class FeeAdminService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/fees/admin';

  catalog(): Promise<FeeCatalogResponse> {
    return firstValueFrom(this.http.get<FeeCatalogResponse>(`${this.base}/catalog`));
  }

  descriptor(table: string): Promise<FeeTableDescriptor> {
    return firstValueFrom(this.http.get<FeeTableDescriptor>(`${this.base}/tables/${encodeURIComponent(table)}/descriptor`));
  }

  rows(table: string, query: {text?: string; page?: number; size?: number; filterColumn?: string; filterValue?: string | number} = {}): Promise<FeeTablePage> {
    let params = new HttpParams().set('page', query.page ?? 0).set('size', query.size ?? 25);
    if (query.text) params = params.set('text', query.text);
    if (query.filterColumn) params = params.set('filterColumn', query.filterColumn);
    if (query.filterValue !== undefined && query.filterValue !== null && `${query.filterValue}` !== '') params = params.set('filterValue', String(query.filterValue));
    return firstValueFrom(this.http.get<FeeTablePage>(`${this.base}/tables/${encodeURIComponent(table)}/rows`, {params}));
  }

  row(table: string, id: number): Promise<Readonly<Record<string, any>>> {
    return firstValueFrom(this.http.get<Readonly<Record<string, any>>>(`${this.base}/tables/${encodeURIComponent(table)}/rows/${id}`));
  }

  create(table: string, values: Readonly<Record<string, any>>): Promise<Readonly<Record<string, any>>> {
    return firstValueFrom(this.http.post<Readonly<Record<string, any>>>(`${this.base}/tables/${encodeURIComponent(table)}/rows`, values));
  }

  update(table: string, id: number, values: Readonly<Record<string, any>>): Promise<Readonly<Record<string, any>>> {
    return firstValueFrom(this.http.put<Readonly<Record<string, any>>>(`${this.base}/tables/${encodeURIComponent(table)}/rows/${id}`, values));
  }

  delete(table: string, id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.base}/tables/${encodeURIComponent(table)}/rows/${id}`));
  }

  lookup(table: string, column: string, text = '', versionId?: number): Promise<readonly FeeSelectOption[]> {
    let params = new HttpParams().set('limit', 500);
    if (text) params = params.set('text', text);
    if (versionId !== undefined) params = params.set('versionId', versionId);
    return firstValueFrom(this.http.get<readonly FeeSelectOption[]>(`${this.base}/tables/${encodeURIComponent(table)}/lookups/${encodeURIComponent(column)}`, {params}));
  }
}

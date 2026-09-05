import {HttpClient, HttpParams} from '@angular/common/http';
import {Injectable, inject} from '@angular/core';
import {firstValueFrom} from 'rxjs';
import {ReferenceTableDescriptor} from '../../../core/models/catalog.model';
import {ReferenceGateway} from '../application/reference.gateway';
import {LookupOption, PageResponse, ReferenceRecordResponse, ReferenceSearchQuery} from '../domain/reference.model';

@Injectable()
export class HttpReferenceGateway implements ReferenceGateway {
  private readonly http = inject(HttpClient);

  descriptor(resource: string): Promise<ReferenceTableDescriptor> {
    return firstValueFrom(this.http.get<ReferenceTableDescriptor>(`/api/v1/catalog/${resource}`));
  }

  search(resource: string, query: ReferenceSearchQuery): Promise<PageResponse<Readonly<Record<string, unknown>>>> {
    let params = new HttpParams()
      .set('text', query.text)
      .set('page', query.page)
      .set('size', query.size)
      .set('direction', query.direction);
    if (query.parentId !== null) params = params.set('parentId', query.parentId);
    if (query.active !== null) params = params.set('active', query.active);
    for (const [field, value] of Object.entries(query.filters)) {
      params = params.set(`filter.${field}`, String(value));
    }
    if (query.sortBy) params = params.set('sortBy', query.sortBy);
    return firstValueFrom(this.http.get<PageResponse<Readonly<Record<string, unknown>>>>(
      `/api/v1/reference/${resource}`, {params}
    ));
  }

  findById(resource: string, id: number): Promise<ReferenceRecordResponse> {
    return firstValueFrom(this.http.get<ReferenceRecordResponse>(`/api/v1/reference/${resource}/${id}`));
  }

  create(resource: string, values: Readonly<Record<string, unknown>>): Promise<ReferenceRecordResponse> {
    return firstValueFrom(this.http.post<ReferenceRecordResponse>(`/api/v1/reference/${resource}`, values));
  }

  update(resource: string, id: number, values: Readonly<Record<string, unknown>>): Promise<ReferenceRecordResponse> {
    return firstValueFrom(this.http.put<ReferenceRecordResponse>(`/api/v1/reference/${resource}/${id}`, values));
  }

  delete(resource: string, id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/v1/reference/${resource}/${id}`));
  }

  lookup(resource: string, parentId?: number | null, text = ''): Promise<readonly LookupOption[]> {
    let params = new HttpParams().set('limit', 5000);
    if (parentId !== undefined && parentId !== null) params = params.set('parentId', parentId);
    if (text) params = params.set('text', text);
    return firstValueFrom(this.http.get<readonly LookupOption[]>(`/api/v1/reference/${resource}/lookup`, {params}));
  }
}

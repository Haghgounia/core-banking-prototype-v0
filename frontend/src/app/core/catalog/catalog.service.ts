import {HttpClient} from '@angular/common/http';
import {Injectable, computed, inject, signal} from '@angular/core';
import {firstValueFrom} from 'rxjs';
import {CatalogItem, CatalogResponse, ReferenceTableDescriptor} from '../models/catalog.model';

@Injectable({providedIn: 'root'})
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly _items = signal<readonly CatalogItem[]>([]);
  private readonly _loaded = signal(false);
  private readonly _counts = signal<Readonly<Record<string, number>>>({});

  readonly items = this._items.asReadonly();
  readonly loaded = this._loaded.asReadonly();
  readonly counts = this._counts.asReadonly();
  readonly activeItems = computed(() => this._items().filter(item => item.status === 'ACTIVE'));

  async load(): Promise<void> {
    if (this._loaded()) return;
    const response = await firstValueFrom(this.http.get<CatalogResponse>('/api/v1/catalog'));
    this._items.set(response.items);
    this._loaded.set(true);
  }

  async loadCounts(): Promise<void> {
    const response = await firstValueFrom(this.http.get<Record<string, number>>('/api/v1/dashboard/counts'));
    this._counts.set(response);
  }

  count(resource: string): number | null {
    return this._counts()[resource] ?? null;
  }

  async descriptor(resource: string): Promise<ReferenceTableDescriptor> {
    return firstValueFrom(this.http.get<ReferenceTableDescriptor>(`/api/v1/catalog/${resource}`));
  }

  find(resource: string): CatalogItem | undefined {
    return this._items().find(item => item.resource === resource);
  }

  group(category: string): readonly CatalogItem[] {
    return this._items().filter(item => item.category === category);
  }
}

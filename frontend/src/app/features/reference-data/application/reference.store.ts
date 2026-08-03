import {Injectable, inject, signal} from '@angular/core';
import {ReferenceTableDescriptor} from '../../../core/models/catalog.model';
import {ReferenceGateway} from './reference.gateway';
import {ReferenceRecordResponse, ReferenceSearchQuery} from '../domain/reference.model';

@Injectable()
export class ReferenceStore {
  private readonly gateway = inject(ReferenceGateway);

  readonly resource = signal('');
  readonly descriptor = signal<ReferenceTableDescriptor | null>(null);
  readonly rows = signal<readonly Readonly<Record<string, unknown>>[]>([]);
  readonly total = signal(0);
  readonly selected = signal<ReferenceRecordResponse | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly query = signal<ReferenceSearchQuery>({
    text: '', parentId: null, active: null, page: 0, size: 20, sortBy: null, direction: 'asc'
  });

  async initialize(resource: string): Promise<void> {
    this.resource.set(resource);
    this.descriptor.set(null);
    this.rows.set([]);
    this.selected.set(null);
    this.query.set({text: '', parentId: null, active: null, page: 0, size: 20, sortBy: null, direction: 'asc'});
    this.error.set(null);
    try {
      this.descriptor.set(await this.gateway.descriptor(resource));
      await this.search();
    } catch (error) {
      this.error.set(this.message(error));
    }
  }

  async search(changes: Partial<ReferenceSearchQuery> = {}): Promise<void> {
    this.query.update(current => ({...current, ...changes}));
    this.loading.set(true);
    this.error.set(null);
    try {
      const page = await this.gateway.search(this.resource(), this.query());
      this.rows.set(page.items);
      this.total.set(page.totalElements);
    } catch (error) {
      this.error.set(this.message(error));
    } finally {
      this.loading.set(false);
    }
  }

  async select(id: number): Promise<ReferenceRecordResponse | null> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const record = await this.gateway.findById(this.resource(), id);
      this.selected.set(record);
      return record;
    } catch (error) {
      this.error.set(this.message(error));
      return null;
    } finally {
      this.loading.set(false);
    }
  }

  clearSelection(): void {
    this.selected.set(null);
  }

  async save(values: Readonly<Record<string, unknown>>): Promise<boolean> {
    this.saving.set(true);
    this.error.set(null);
    try {
      const selected = this.selected();
      if (selected) {
        await this.gateway.update(this.resource(), selected.id, values);
      } else {
        await this.gateway.create(this.resource(), values);
      }
      await this.search();
      return true;
    } catch (error) {
      this.error.set(this.message(error));
      return false;
    } finally {
      this.saving.set(false);
    }
  }

  async remove(id: number): Promise<boolean> {
    this.error.set(null);
    try {
      await this.gateway.delete(this.resource(), id);
      await this.search();
      return true;
    } catch (error) {
      this.error.set(this.message(error));
      return false;
    }
  }

  private message(error: unknown): string {
    return error instanceof Error ? error.message : 'خطای پیش‌بینی‌نشده';
  }
}

import {HttpClient} from '@angular/common/http';
import {Component, computed, inject, signal} from '@angular/core';
import {firstValueFrom} from 'rxjs';
import {RouterLink} from '@angular/router';
import {MatIconModule} from '@angular/material/icon';
import {CatalogService} from '../../core/catalog/catalog.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, MatIconModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  private readonly http = inject(HttpClient);
  readonly catalog = inject(CatalogService);
  readonly counts = signal<Partial<Record<string, number>>>({});
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly geographyItems = computed(() => this.catalog.items().filter(i =>
    i.status === 'ACTIVE' && i.category === 'GEOGRAPHY'
  ));
  readonly otherReferenceItems = computed(() => this.catalog.items().filter(i =>
    i.status === 'ACTIVE' &&
    i.category !== 'GEOGRAPHY' &&
    i.category !== 'DEPOSIT_PRODUCT_REFERENCE'
  ));
  readonly depositProductReferenceItems = computed(() => this.catalog.items().filter(i =>
    i.status === 'ACTIVE' && i.category === 'DEPOSIT_PRODUCT_REFERENCE'
  ));
  readonly plannedItems = computed(() => this.catalog.items().filter(i => i.status === 'PLANNED'));

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    try {
      await this.catalog.load();
      this.counts.set(await firstValueFrom(
        this.http.get<Record<string, number>>('/api/v1/dashboard/counts')
      ));
    } catch (error: unknown) {
      console.error('Dashboard loading failed', error);
      const status = this.httpStatus(error);
      this.error.set(status === 0
        ? 'ارتباط با Backend برقرار نشد. وضعیت اجرای سرویس و پورت 8091 را بررسی کنید.'
        : 'آمار داشبورد دریافت نشد. اتصال Oracle و دسترسی کاربر به Schemaهای GEO و DPS را بررسی کنید.');
    } finally {
      this.loading.set(false);
    }
  }

  private httpStatus(error: unknown): number | undefined {
    return typeof error === 'object' && error !== null && 'status' in error
      ? Number((error as {status?: unknown}).status)
      : undefined;
  }
}

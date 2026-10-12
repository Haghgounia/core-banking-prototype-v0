import {HttpErrorResponse} from '@angular/common/http';
import {Component, computed, inject, signal, ViewChild} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {MatIconModule} from '@angular/material/icon';
import {FeeAdminService} from './fee-admin.service';
import {FeeCatalogResponse, FeeTableCatalogItem} from './fee-admin.models';
import {feeWorkspace} from './fee-workspaces';
import {FeeCollectionStudioComponent} from './fee-collection-studio.component';

@Component({
  selector: 'app-fee-admin-workspace',
  standalone: true,
  imports: [RouterLink, MatIconModule, FeeCollectionStudioComponent],
  templateUrl: './fee-admin-workspace.component.html',
  styleUrl: './fee-admin-workspace.component.scss'
})
export class FeeAdminWorkspaceComponent {
  @ViewChild(FeeCollectionStudioComponent) private collectionStudio?: FeeCollectionStudioComponent;
  canLeave(): boolean { return this.collectionStudio?.canLeave() ?? true; }
  private readonly service = inject(FeeAdminService);
  private readonly route = inject(ActivatedRoute);
  readonly workspaceKey = signal('');
  readonly workspace = computed(() => feeWorkspace(this.workspaceKey()));
  readonly catalog = signal<FeeCatalogResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly catalogByTable = computed<Record<string, FeeTableCatalogItem>>(() => {
    const found: Record<string, FeeTableCatalogItem> = {};
    for (const group of this.catalog()?.groups ?? []) {
      for (const table of group.tables) found[table.tableName] = table;
    }
    return found;
  });
  readonly available = computed(() => this.workspace()?.tables.filter(t => this.catalogByTable()[t]?.available).length ?? 0);

  constructor() {
    this.route.paramMap.subscribe(params => this.workspaceKey.set(params.get('workspace') ?? ''));
    void this.load();
  }

  table(tableName: string): FeeTableCatalogItem | undefined { return this.catalogByTable()[tableName]; }

  private async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try { this.catalog.set(await this.service.catalog()); }
    catch (e) {
      this.error.set(e instanceof HttpErrorResponse && e.error && typeof e.error === 'object' && (e.error as {detail?: string}).detail
        ? (e.error as {detail: string}).detail : e instanceof Error ? e.message : 'خطا در دریافت فهرست جداول FEE');
    } finally { this.loading.set(false); }
  }
}

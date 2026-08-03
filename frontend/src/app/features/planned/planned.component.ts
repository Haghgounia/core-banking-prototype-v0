import {Component, inject, signal} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {MatIconModule} from '@angular/material/icon';
import {CatalogService} from '../../core/catalog/catalog.service';

@Component({
  selector: 'app-planned',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <section class="planned-page card">
      <mat-icon>construction</mat-icon>
      <h1>{{ title() }}</h1>
      <p>این دامنه در معماری و منو پیش‌بینی شده است، اما Descriptor و DDL آن هنوز فعال نشده‌اند.</p>
      <p class="muted">پس از تثبیت Vertical Slice جغرافیا، جدول مربوطه بدون ساخت Controller و Component جدید به Runtime اضافه می‌شود.</p>
    </section>
  `,
  styles: [`.planned-page{padding:48px;text-align:center}.planned-page>mat-icon{font-size:54px;width:54px;height:54px;color:var(--app-primary)}.planned-page h1{margin:16px 0 8px}`]
})
export class PlannedComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly catalog = inject(CatalogService);
  readonly title = signal('اطلاعات پایه');

  constructor() {
    void this.initialize();
  }

  private async initialize(): Promise<void> {
    await this.catalog.load();
    const resource = this.route.snapshot.paramMap.get('resource') ?? '';
    this.title.set(this.catalog.find(resource)?.title ?? 'اطلاعات پایه');
  }
}

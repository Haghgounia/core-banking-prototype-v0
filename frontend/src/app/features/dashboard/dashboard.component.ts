import {Component, inject, signal} from '@angular/core';
import {firstValueFrom} from 'rxjs';
import {RouterLink} from '@angular/router';
import {MatIconModule} from '@angular/material/icon';
import {CifService} from '../cif/cif.service';
import {CifDashboardSummary} from '../cif/cif.models';
import {Calendar2ReferenceService} from '../calendar2-reference/calendar2-reference.service';
import {Calendar2TodayEventMedia} from '../calendar2-reference/calendar2-reference.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, MatIconModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  private readonly cif = inject(CifService);
  private readonly calendar2 = inject(Calendar2ReferenceService);
  readonly cifSummary = signal<CifDashboardSummary | null>(null);
  readonly todayOccasionMedia = signal<readonly Calendar2TodayEventMedia[]>([]);
  readonly loading = signal(true);
  readonly cifError = signal<string | null>(null);

  constructor() {
    void this.load();
    void this.loadTodayOccasionMedia();
  }

  occasionImageUrl(mediaId: number): string { return this.calendar2.eventMediaContentUrl(mediaId); }

  private async loadTodayOccasionMedia(): Promise<void> {
    try { this.todayOccasionMedia.set(await firstValueFrom(this.calendar2.todayEventMedia())); }
    catch (error) {
      console.warn('CAL2 occasion media loading failed', error);
      this.todayOccasionMedia.set([]);
    }
  }

  private async load(): Promise<void> {
    try {
      this.cifSummary.set(await firstValueFrom(this.cif.dashboardSummary()));
    } catch (error) {
      console.error('CIF dashboard loading failed', error);
      this.cifError.set('آمار CIF دریافت نشد؛ این خطا مانع استفاده از سایر بخش‌های سامانه نمی‌شود.');
    } finally {
      this.loading.set(false);
    }
  }
}

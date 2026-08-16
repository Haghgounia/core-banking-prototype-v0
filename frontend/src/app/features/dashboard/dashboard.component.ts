import {Component, inject, signal} from '@angular/core';
import {firstValueFrom} from 'rxjs';
import {RouterLink} from '@angular/router';
import {MatIconModule} from '@angular/material/icon';
import {CifService} from '../cif/cif.service';
import {CifDashboardSummary} from '../cif/cif.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, MatIconModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  private readonly cif = inject(CifService);
  readonly cifSummary = signal<CifDashboardSummary | null>(null);
  readonly loading = signal(true);
  readonly cifError = signal<string | null>(null);

  constructor() {
    void this.load();
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

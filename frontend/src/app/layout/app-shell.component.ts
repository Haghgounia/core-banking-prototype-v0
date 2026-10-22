import {Component, computed, inject, OnDestroy, signal} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {firstValueFrom, Subscription} from 'rxjs';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {Calendar2PersonalService} from '../features/calendar2-reference/calendar2-personal.service';
import {Calendar2PersonalAlert} from '../features/calendar2-reference/calendar2-personal.models';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatSidenavModule} from '@angular/material/sidenav';
import {MatListModule} from '@angular/material/list';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatMenuModule} from '@angular/material/menu';
import {MatTooltipModule} from '@angular/material/tooltip';
import {ThemePreference, ThemeService} from '../core/theme/theme.service';
import {GENERATED_SYSTEM_VERSIONS} from '../features/system-specification/system-version.generated';
import {AppBreadcrumbComponent} from '../shared/ui/app-breadcrumb.component';
import {SidebarR22GroupComponent} from './sidebar-r22/sidebar-r22-group.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    MatSnackBarModule, RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatButtonModule, MatMenuModule, MatTooltipModule, AppBreadcrumbComponent, SidebarR22GroupComponent
  ],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss'
})
export class AppShellComponent implements OnDestroy {
  readonly theme = inject(ThemeService);
  private readonly router = inject(Router);
  private readonly reminders = inject(Calendar2PersonalService);
  private readonly snackBar = inject(MatSnackBar);
  readonly version = GENERATED_SYSTEM_VERSIONS.release;
  readonly sidebarCollapsed = signal(this.readSidebarPreference());
  readonly personalAlerts = signal<readonly Calendar2PersonalAlert[]>([]);
  readonly personalAlertsUnavailable = signal(false);
  readonly personalAlertsLoading = signal(false);
  readonly personalAlertCount = computed(() => this.personalAlerts().length);
  private readonly refreshTimer: ReturnType<typeof setInterval>;
  private readonly changeSubscription: Subscription;
  private notificationInitialized = false;
  private lastAlertSignature = '';

  constructor() {
    void this.refreshPersonalAlerts();
    this.refreshTimer = setInterval(() => void this.refreshPersonalAlerts(), 60_000);
    this.changeSubscription = this.reminders.changed$.subscribe(() => void this.refreshPersonalAlerts());
  }

  ngOnDestroy(): void {
    clearInterval(this.refreshTimer);
    this.changeSubscription.unsubscribe();
  }

  async refreshPersonalAlerts(): Promise<void> {
    if (this.personalAlertsLoading()) return;
    this.personalAlertsLoading.set(true);
    try {
      const inbox = await firstValueFrom(this.reminders.inbox());
      this.personalAlerts.set(inbox.alerts);
      this.personalAlertsUnavailable.set(false);
      // One unobtrusive message when a new due set appears; per-day acknowledgment is stored in Oracle.
      const signature = inbox.alerts.map(a => a.userCalendarEntryId + ':' + a.alertKind).sort().join('|');
      if (inbox.alerts.length && (!this.notificationInitialized || signature !== this.lastAlertSignature)) {
        const notice = this.snackBar.open(
          inbox.alerts.length === 1 ? `یادآوری: ${inbox.alerts[0].title}` : `${inbox.alerts.length} یادآوری برای بررسی دارید`,
          'مشاهده', {duration: 9000}
        );
        notice.onAction().subscribe(() => { void this.router.navigateByUrl('/calendar2/month-view'); });
      }
      this.lastAlertSignature = signature;
      this.notificationInitialized = true;
    } catch {
      // CAL2 R1 may not be installed in Oracle yet. The rest of the prototype must remain usable.
      this.personalAlertsUnavailable.set(true);
      this.personalAlerts.set([]);
    } finally { this.personalAlertsLoading.set(false); }
  }

  async acknowledgePersonalAlert(alert: Calendar2PersonalAlert): Promise<void> {
    try {
      await firstValueFrom(this.reminders.acknowledge(alert.userCalendarEntryId));
      this.reminders.changed();
    } catch {
      this.snackBar.open('ثبت وضعیت مشاهده یادآوری انجام نشد.', 'بستن', {duration: 6000});
    }
  }

  async completePersonalAlert(alert: Calendar2PersonalAlert): Promise<void> {
    try {
      await firstValueFrom(this.reminders.completion(alert.userCalendarEntryId, true));
      this.reminders.changed();
    } catch {
      this.snackBar.open('ثبت انجام‌شدن یادآوری انجام نشد.', 'بستن', {duration: 6000});
    }
  }

  alertDescription(alert: Calendar2PersonalAlert): string {
    switch (alert.alertKind) {
      case 'DUE': return 'موعد امروز · ساعت ' + alert.reminderTime;
      case 'OVERDUE': return `${Math.abs(alert.daysToDue)} روز از موعد گذشته`;
      default: return `${alert.daysToDue} روز تا موعد · ساعت ${alert.reminderTime}`;
    }
  }



  toggleSidebar(): void {
    const collapsed = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(collapsed);
    try { localStorage.setItem('core-banking.sidebar.collapsed', collapsed ? '1' : '0'); } catch { /* ignore storage restrictions */ }
  }

  private readSidebarPreference(): boolean {
    try {
      const stored = localStorage.getItem('core-banking.sidebar.collapsed');
      return stored === null ? true : stored === '1';
    } catch { return true; }
  }

  isReferenceSection(): boolean {
    const url = this.router.url;
    return url.startsWith('/reference-data')
      || url.startsWith('/cif/reference')
      || url.startsWith('/loan/reference-data')
      || url.startsWith('/deposit/reference-data')
      || url.startsWith('/four-deposits/reference-data')
      || url.startsWith('/geography-tree')
      || url.startsWith('/calendar/reference-data')
      || url.startsWith('/calendar2/')
      || url.startsWith('/planned/');
  }

  setTheme(preference: ThemePreference): void {
    this.theme.setPreference(preference);
  }
}

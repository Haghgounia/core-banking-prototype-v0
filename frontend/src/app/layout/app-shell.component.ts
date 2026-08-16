import {Component, inject} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatSidenavModule} from '@angular/material/sidenav';
import {MatListModule} from '@angular/material/list';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatMenuModule} from '@angular/material/menu';
import {MatTooltipModule} from '@angular/material/tooltip';
import {ThemePreference, ThemeService} from '../core/theme/theme.service';
import {GENERATED_SYSTEM_VERSIONS} from '../features/system-specification/system-version.generated';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatButtonModule, MatMenuModule, MatTooltipModule
  ],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss'
})
export class AppShellComponent {
  readonly theme = inject(ThemeService);
  private readonly router = inject(Router);
  readonly version = GENERATED_SYSTEM_VERSIONS.release;

  isReferenceSection(): boolean {
    const url = this.router.url;
    return url.startsWith('/reference-data')
      || url.startsWith('/cif/reference')
      || url.startsWith('/deposit/reference-data')
      || url.startsWith('/geography-tree')
      || url.startsWith('/planned/');
  }

  setTheme(preference: ThemePreference): void {
    this.theme.setPreference(preference);
  }
}

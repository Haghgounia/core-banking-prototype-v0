import {Component, inject} from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatSidenavModule} from '@angular/material/sidenav';
import {MatListModule} from '@angular/material/list';
import {MatIconModule} from '@angular/material/icon';
import {MatExpansionModule} from '@angular/material/expansion';
import {MatButtonModule} from '@angular/material/button';
import {MatMenuModule} from '@angular/material/menu';
import {MatTooltipModule} from '@angular/material/tooltip';
import {CatalogService} from '../core/catalog/catalog.service';
import {CatalogItem} from '../core/models/catalog.model';
import {ThemePreference, ThemeService} from '../core/theme/theme.service';
import {GENERATED_SYSTEM_VERSIONS} from '../features/system-specification/system-version.generated';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatExpansionModule, MatButtonModule,
    MatMenuModule, MatTooltipModule
  ],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss'
})
export class AppShellComponent {
  readonly catalog = inject(CatalogService);
  readonly theme = inject(ThemeService);
  readonly version = GENERATED_SYSTEM_VERSIONS.release;
  readonly groups = [
    {key: 'GEOGRAPHY', title: 'اطلاعات جغرافیایی', icon: 'account_tree'},
    {key: 'GENERAL', title: 'اطلاعات عمومی', icon: 'dataset'},
    {key: 'EMPLOYMENT', title: 'اشتغال و مشاغل', icon: 'work'},
    {key: 'EDUCATION', title: 'آموزش و تحصیلات', icon: 'school'},
    {key: 'DEPOSIT_PRODUCT_REFERENCE', title: 'اطلاعات پایه محصول سپرده', icon: 'savings'}
  ] as const;


  constructor() {
    void this.catalog.load();
  }

  routeFor(item: CatalogItem): readonly string[] {
    return item.status === 'ACTIVE'
      ? ['/reference-data', item.resource]
      : ['/planned', item.resource];
  }

  setTheme(preference: ThemePreference): void {
    this.theme.setPreference(preference);
  }
}

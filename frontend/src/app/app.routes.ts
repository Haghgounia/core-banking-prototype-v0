import {Routes} from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/app-shell.component').then(m => m.AppShellComponent),
    children: [
      {path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)},
      {path: 'geography-tree', loadComponent: () => import('./features/geography-tree/geography-tree.component').then(m => m.GeographyTreeComponent)},
      {path: 'system-specification', loadComponent: () => import('./features/system-specification/system-specification.component').then(m => m.SystemSpecificationComponent)},
      {path: 'reference/:resource', redirectTo: 'reference-data/:resource'},
      {path: 'reference-data/:resource', loadComponent: () => import('./features/reference-data/presentation/reference-page.component').then(m => m.ReferencePageComponent)},
      {path: 'planned/:resource', loadComponent: () => import('./features/planned/planned.component').then(m => m.PlannedComponent)},
      {path: '', pathMatch: 'full', redirectTo: 'dashboard'}
    ]
  },
  {path: '**', redirectTo: 'dashboard'}
];

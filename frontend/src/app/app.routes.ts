import {Routes} from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/app-shell.component').then(m => m.AppShellComponent),
    children: [
      {path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)},
      {path: 'cif/parties', loadComponent: () => import('./features/cif/party-list.component').then(m => m.PartyListComponent)},
      {path: 'cif/parties/new', loadComponent: () => import('./features/cif/party-create.component').then(m => m.PartyCreateComponent)},
      {path: 'cif/parties/:partyId/onboarding/contact-address', loadComponent: () => import('./features/cif/party-contact-address.component').then(m => m.PartyContactAddressComponent)},
      {path: 'cif/parties/:partyId/onboarding/financial-employment', loadComponent: () => import('./features/cif/party-financial-employment.component').then(m => m.PartyFinancialEmploymentComponent)},
      {path: 'cif/parties/:partyId/onboarding/identifiers-documents', loadComponent: () => import('./features/cif/party-identifiers-documents.component').then(m => m.PartyIdentifiersDocumentsComponent)},
      {path: 'cif/parties/:partyId', loadComponent: () => import('./features/cif/party-360.component').then(m => m.Party360Component)},

      {path: 'reference-data', loadComponent: () => import('./features/reference-hub/reference-hub.component').then(m => m.ReferenceHubComponent)},
      {path: 'reference-data/general', data: {scope: 'GENERAL'}, loadComponent: () => import('./features/reference-menu/reference-menu.component').then(m => m.ReferenceMenuComponent)},
      {path: 'reference-data/:resource', loadComponent: () => import('./features/reference-data/presentation/reference-page.component').then(m => m.ReferencePageComponent)},

      {path: 'deposit/reference-data', data: {scope: 'DEPOSIT'}, loadComponent: () => import('./features/reference-menu/reference-menu.component').then(m => m.ReferenceMenuComponent)},
      {path: 'deposit/reference-data/:resource', loadComponent: () => import('./features/reference-data/presentation/reference-page.component').then(m => m.ReferencePageComponent)},

      {path: 'cif/reference-data', data: {scope: 'PARTY'}, loadComponent: () => import('./features/reference-menu/reference-menu.component').then(m => m.ReferenceMenuComponent)},
      {path: 'cif/reference-data/:resource', loadComponent: () => import('./features/cif-reference/party-reference-page.component').then(m => m.PartyReferencePageComponent)},
      {path: 'cif/reference/:resource', redirectTo: 'cif/reference-data/:resource'},

      {path: 'geography-tree', loadComponent: () => import('./features/geography-tree/geography-tree.component').then(m => m.GeographyTreeComponent)},
      {path: 'system-specification', loadComponent: () => import('./features/system-specification/system-specification.component').then(m => m.SystemSpecificationComponent)},
      {path: 'reference/:resource', redirectTo: 'reference-data/:resource'},
      {path: 'planned/:resource', loadComponent: () => import('./features/planned/planned.component').then(m => m.PlannedComponent)},
      {path: '', pathMatch: 'full', redirectTo: 'dashboard'}
    ]
  },
  {path: '**', redirectTo: 'dashboard'}
];

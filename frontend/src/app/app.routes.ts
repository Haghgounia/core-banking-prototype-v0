import {Routes} from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/app-shell.component').then(m => m.AppShellComponent),
    children: [
      {path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)},
      {path: 'fee', loadComponent: () => import('./features/fee/fee-management.component').then(m => m.FeeManagementComponent)},
      {path: 'product-builder', loadComponent: () => import('./features/product-builder/product-builder-home.component').then(m => m.ProductBuilderHomeComponent)},
      {path: 'product-builder/products', loadComponent: () => import('./features/product-builder/product-list.component').then(m => m.ProductListComponent)},
      {path: 'product-builder/products/:productId', loadComponent: () => import('./features/product-builder/product-workspace.component').then(m => m.ProductWorkspaceComponent)},
      {path: 'product-builder/tables/:table', loadComponent: () => import('./features/product-builder/pdl-table.component').then(m => m.PdlTableComponent)},
      {path: 'cif/parties', loadComponent: () => import('./features/cif/party-list.component').then(m => m.PartyListComponent)},
      {path: 'cif/parties/new', loadComponent: () => import('./features/cif/party-create.component').then(m => m.PartyCreateComponent)},
      {path: 'cif/parties/:partyId/onboarding/contact-address', loadComponent: () => import('./features/cif/party-contact-address.component').then(m => m.PartyContactAddressComponent)},
      {path: 'cif/parties/:partyId/onboarding/financial-employment', loadComponent: () => import('./features/cif/party-financial-employment.component').then(m => m.PartyFinancialEmploymentComponent)},
      {path: 'cif/parties/:partyId/onboarding/identifiers-documents', loadComponent: () => import('./features/cif/party-identifiers-documents.component').then(m => m.PartyIdentifiersDocumentsComponent)},
      {path: 'cif/parties/:partyId/onboarding/classifications', loadComponent: () => import('./features/cif/party-classifications.component').then(m => m.PartyClassificationsComponent)},
      {path: 'cif/parties/:partyId/onboarding/relationships', loadComponent: () => import('./features/cif/party-relationships.component').then(m => m.PartyRelationshipsComponent)},
      {path: 'cif/parties/:partyId/onboarding/roles', loadComponent: () => import('./features/cif/party-roles.component').then(m => m.PartyRolesComponent)},
      {path: 'cif/parties/:partyId/onboarding/kyc-risk', loadComponent: () => import('./features/cif/party-kyc-risk.component').then(m => m.PartyKycRiskComponent)},
      {path: 'cif/parties/:partyId/onboarding/consents-preferences', loadComponent: () => import('./features/cif/party-consents-preferences.component').then(m => m.PartyConsentsPreferencesComponent)},
      {path: 'cif/parties/:partyId/operations/lifecycle-merge', loadComponent: () => import('./features/cif/party-lifecycle-merge.component').then(m => m.PartyLifecycleMergeComponent)},
      {path: 'cif/parties/:partyId', loadComponent: () => import('./features/cif/party-360.component').then(m => m.Party360Component)},

      {path: 'reference-data', loadComponent: () => import('./features/reference-hub/reference-hub.component').then(m => m.ReferenceHubComponent)},
      {path: 'reference-data/general', data: {scope: 'GENERAL'}, loadComponent: () => import('./features/reference-menu/reference-menu.component').then(m => m.ReferenceMenuComponent)},
      {path: 'calendar/reference-data', loadComponent: () => import('./features/calendar-reference/calendar-reference-menu.component').then(m => m.CalendarReferenceMenuComponent)},
      {path: 'calendar/reference-data/import', loadComponent: () => import('./features/calendar-reference/calendar-dataset-import.component').then(m => m.CalendarDatasetImportComponent)},
      {path: 'calendar/reference-data/:resource', loadComponent: () => import('./features/calendar-reference/calendar-reference-page.component').then(m => m.CalendarReferencePageComponent)},
      {path: 'calendar2/reference-data', loadComponent: () => import('./features/calendar2-reference/calendar2-reference-menu.component').then(m => m.Calendar2ReferenceMenuComponent)},
      {path: 'calendar2/month-view', loadComponent: () => import('./features/calendar2-reference/calendar2-month-view.component').then(m => m.Calendar2MonthViewComponent)},
      {path: 'calendar2/reference-data/import', loadComponent: () => import('./features/calendar2-reference/calendar2-dataset-import.component').then(m => m.Calendar2DatasetImportComponent)},
      {path: 'calendar2/reference-data/:resource', loadComponent: () => import('./features/calendar2-reference/calendar2-reference-page.component').then(m => m.Calendar2ReferencePageComponent)},
      {path: 'reference-data/:resource', loadComponent: () => import('./features/reference-data/presentation/reference-page.component').then(m => m.ReferencePageComponent)},

      {path: 'deposit/reference-data', data: {scope: 'DEPOSIT'}, loadComponent: () => import('./features/reference-menu/reference-menu.component').then(m => m.ReferenceMenuComponent)},
      {path: 'deposit/reference-data/:resource', loadComponent: () => import('./features/reference-data/presentation/reference-page.component').then(m => m.ReferencePageComponent)},

      {path: 'cif/reference-data', data: {scope: 'PARTY'}, loadComponent: () => import('./features/reference-menu/reference-menu.component').then(m => m.ReferenceMenuComponent)},
      {path: 'cif/reference-data/:resource', loadComponent: () => import('./features/cif-reference/party-reference-page.component').then(m => m.PartyReferencePageComponent)},
      {path: 'cif/reference/:resource', redirectTo: 'cif/reference-data/:resource'},

      {path: 'geography-tree', loadComponent: () => import('./features/geography-tree/geography-tree.component').then(m => m.GeographyTreeComponent)},
      {path: 'system-specification', loadComponent: () => import('./features/system-specification/system-specification.component').then(m => m.SystemSpecificationComponent)},
      {path: 'system/database-model-comparison', loadComponent: () => import('./features/database-model-comparison/database-model-comparison.component').then(m => m.DatabaseModelComparisonComponent)},
      {path: 'system/oracle-ea-xmi-export', loadComponent: () => import('./features/oracle-ea-xmi-export/oracle-ea-xmi-export.component').then(m => m.OracleEaXmiExportComponent)},
      {path: 'reference/:resource', redirectTo: 'reference-data/:resource'},
      {path: 'planned/:resource', loadComponent: () => import('./features/planned/planned.component').then(m => m.PlannedComponent)},
      {path: '', pathMatch: 'full', redirectTo: 'dashboard'}
    ]
  },
  {path: '**', redirectTo: 'dashboard'}
];

import {Component, DestroyRef, computed, inject, signal} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {FormControl, FormRecord, ReactiveFormsModule, Validators} from '@angular/forms';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {debounceTime, distinctUntilChanged} from 'rxjs';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatPaginatorModule, PageEvent} from '@angular/material/paginator';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatSelectModule} from '@angular/material/select';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatSortModule, Sort} from '@angular/material/sort';
import {MatTableModule} from '@angular/material/table';
import {MatTooltipModule} from '@angular/material/tooltip';
import {CatalogService} from '../../../core/catalog/catalog.service';
import {CatalogItem, ReferenceFieldDescriptor} from '../../../core/models/catalog.model';
import {ReferenceGateway} from '../application/reference.gateway';
import {ReferenceStore} from '../application/reference.store';
import {LookupOption, ReferenceRecordResponse} from '../domain/reference.model';
import {DatabaseTablesComponent} from '../../../shared/ui/database-tables.component';

@Component({
  selector: 'app-reference-page',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatIconModule,
    MatInputModule, MatPaginatorModule, MatProgressBarModule, MatSelectModule,
    MatSlideToggleModule, MatSortModule, MatTableModule, MatTooltipModule, DatabaseTablesComponent
  ],
  providers: [ReferenceStore],
  templateUrl: './reference-page.component.html',
  styleUrl: './reference-page.component.scss'
})
export class ReferencePageComponent {
  readonly store = inject(ReferenceStore);
  private readonly gateway = inject(ReferenceGateway);
  private readonly catalog = inject(CatalogService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  private readonly eduFilterFields: Readonly<Record<string, readonly string[]>> = {
    'edu-education-levels': ['educationSystemCode', 'isSelectable', 'activeFlag'],
    'edu-education-fields': ['parentEducationFieldsId', 'educationSystemCode', 'fieldNodeTypeCode', 'isSelectable', 'activeFlag'],
    'edu-education-field-levels': ['educationFieldsId', 'educationLevelsId', 'sourceId', 'activeFlag'],
    'edu-education-institutions': ['institutionTypeCode', 'educationSystemCode', 'institutionStatusCode', 'isDegreeGranting', 'activeFlag'],
    'edu-education-sources': ['sourceTypeCode', 'sourceYear', 'activeFlag'],
    'edu-education-source-mappings': ['sourceId', 'entityTypeCode', 'mappingTypeCode', 'matchStatusCode', 'activeFlag']
  };

  readonly searchControl = new FormControl('', {nonNullable: true});
  readonly activeControl = new FormControl<boolean | null>(null);
  readonly parentFilterControl = new FormControl<number | null>(null);
  readonly advancedFilterForm = new FormRecord<FormControl<unknown>>({});
  readonly advancedFilterFields = signal<readonly ReferenceFieldDescriptor[]>([]);
  readonly advancedFilterLookupOptions = signal<Partial<Record<string, readonly LookupOption[]>>>({});
  readonly form = new FormRecord<FormControl<unknown>>({});
  readonly editorVisible = signal(false);
  readonly hierarchyOptions = signal<Partial<Record<string, readonly LookupOption[]>>>({});
  readonly lookupOptions = signal<Partial<Record<string, readonly LookupOption[]>>>({});
  readonly parentFilterOptions = signal<readonly LookupOption[]>([]);
  readonly hierarchyValues = signal<Readonly<Record<string, number | null>>>({});

  readonly editableFields = computed(() => {
    const descriptor = this.store.descriptor();
    return (descriptor?.fields ?? []).filter(field =>
      !field.readOnly && !(field.type === 'LOOKUP' && field.apiName === descriptor?.parent?.apiField)
    );
  });
  readonly gridFields = computed(() => (this.store.descriptor()?.fields ?? []).filter(field => field.grid));
  readonly systemFields = computed(() => {
    const descriptor = this.store.descriptor();
    return (descriptor?.fields ?? []).filter(field =>
      field.readOnly && field.apiName !== descriptor?.idApiName
    );
  });
  readonly displayedColumns = computed(() => [
    ...this.gridFields().map(field => field.apiName),
    ...(this.store.descriptor()?.parent ? ['parentName'] : []),
    'actions'
  ]);
  readonly hierarchy = computed(() => this.buildHierarchy());
  readonly parentLabel = computed(() => this.store.descriptor()?.parent?.label ?? 'والد');
  readonly hasActiveField = computed(() =>
    (this.store.descriptor()?.fields ?? []).some(field => field.apiName === 'isActive')
  );
  readonly hierarchyComplete = computed(() =>
    this.hierarchy().every(item => this.hierarchyValues()[item.resource] !== null)
  );

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const resource = params.get('resource');
      if (resource) void this.initialize(resource);
    });

    this.searchControl.valueChanges.pipe(
      debounceTime(350), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)
    ).subscribe(text => void this.store.search({text, page: 0}));

    this.activeControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(active => void this.store.search({active, page: 0}));

    this.parentFilterControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(parentId => void this.store.search({parentId, page: 0}));
  }

  async create(): Promise<void> {
    this.store.clearSelection();
    this.buildForm(null);
    this.editorVisible.set(true);
    await Promise.all([this.initializeHierarchy([]), this.initializeLookupFields()]);
  }

  async edit(row: Readonly<Record<string, unknown>>): Promise<void> {
    const descriptor = this.store.descriptor();
    if (!descriptor) return;
    const id = Number(row[descriptor.idApiName]);
    const record = await this.store.select(id);
    if (!record) return;
    this.buildForm(record);
    this.editorVisible.set(true);
    await Promise.all([this.initializeHierarchy(record.ancestors), this.initializeLookupFields()]);
  }

  closeEditor(): void {
    this.editorVisible.set(false);
    this.store.clearSelection();
    this.form.reset();
  }

  async save(): Promise<void> {
    const descriptor = this.store.descriptor();
    if (!descriptor) return;
    if (!this.hierarchyComplete()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const values: Record<string, unknown> = {};
    for (const field of descriptor.fields.filter(item => !item.readOnly)) {
      if (field.type === 'LOOKUP' && field.apiName === descriptor.parent?.apiField) {
        const finalResource = this.hierarchy().at(-1)?.resource;
        values[field.apiName] = finalResource ? this.hierarchyValues()[finalResource] : null;
      } else {
        values[field.apiName] = this.form.controls[field.apiName]?.value ?? null;
      }
    }
    const selected = this.store.selected();
    if (selected?.values['recordVersion'] !== null && selected?.values['recordVersion'] !== undefined) {
      values['recordVersion'] = selected.values['recordVersion'];
    }
    if (await this.store.save(values)) this.closeEditor();
  }

  async remove(row: Readonly<Record<string, unknown>>): Promise<void> {
    const descriptor = this.store.descriptor();
    if (!descriptor) return;
    const id = Number(row[descriptor.idApiName]);
    const name = String(row[descriptor.nameApiName] ?? 'این رکورد');
    if (window.confirm(`«${name}» حذف شود؟`)) await this.store.remove(id);
  }

  onPage(event: PageEvent): void {
    void this.store.search({page: event.pageIndex, size: event.pageSize});
  }

  onSort(sort: Sort): void {
    void this.store.search({sortBy: sort.active || null, direction: sort.direction === 'desc' ? 'desc' : 'asc'});
  }

  async onHierarchyChange(index: number, resource: string, value: number | null): Promise<void> {
    const chain = this.hierarchy();
    const values = {...this.hierarchyValues(), [resource]: value};
    const options = {...this.hierarchyOptions()};
    for (let i = index + 1; i < chain.length; i++) {
      values[chain[i].resource] = null;
      options[chain[i].resource] = [];
    }
    this.hierarchyValues.set(values);
    this.hierarchyOptions.set(options);
    const next = chain[index + 1];
    if (next && value !== null) await this.loadOptions(next.resource, value);
  }

  cell(row: Readonly<Record<string, unknown>>, field: ReferenceFieldDescriptor): string {
    const value = row[field.apiName];
    if (field.type === 'BOOLEAN') {
      if (field.apiName === 'isActive') return value ? 'فعال' : 'غیرفعال';
      return value ? 'بله' : 'خیر';
    }
    if (field.type === 'SELECT') return field.options.find(option => Number(option.value) === Number(value))?.label ?? String(value ?? '');
    if (field.type === 'STRING_SELECT') return field.options.find(option => String(option.value) === String(value))?.label ?? String(value ?? '');
    if (field.type === 'LOOKUP') {
      if (value === null || value === undefined) return '—';
      return this.lookupOptions()[field.apiName]?.find(option => Number(option.value) === Number(value))?.label
        ?? String(value);
    }
    return value === null || value === undefined ? '—' : String(value);
  }

  isInvalid(field: ReferenceFieldDescriptor): boolean {
    const control = this.form.controls[field.apiName];
    return Boolean(control && control.invalid && control.touched);
  }

  auditValue(name: string): unknown {
    return this.store.selected()?.values[name] ?? '—';
  }

  private async initialize(resource: string): Promise<void> {
    await this.catalog.load();
    await this.store.initialize(resource);
    this.searchControl.setValue('', {emitEvent: false});
    this.activeControl.setValue(null, {emitEvent: false});
    this.parentFilterControl.setValue(null, {emitEvent: false});
    this.parentFilterOptions.set([]);
    this.lookupOptions.set({});
    await Promise.all([this.initializeParentFilter(), this.initializeGridLookupFields(), this.initializeAdvancedFilters(resource)]);
    this.editorVisible.set(false);
  }

  clearAdvancedFilters(): void {
    for (const control of Object.values(this.advancedFilterForm.controls)) {
      control.setValue(null, {emitEvent: false});
    }
    void this.store.search({filters: {}, page: 0});
  }

  private async initializeAdvancedFilters(resource: string): Promise<void> {
    for (const key of Object.keys(this.advancedFilterForm.controls)) {
      this.advancedFilterForm.removeControl(key);
    }
    this.advancedFilterFields.set([]);
    this.advancedFilterLookupOptions.set({});

    const descriptor = this.store.descriptor();
    const names = this.eduFilterFields[resource] ?? [];
    if (!descriptor || !names.length) return;

    const fields = names
      .map(name => descriptor.fields.find(field => field.apiName === name))
      .filter((field): field is ReferenceFieldDescriptor => Boolean(field));
    this.advancedFilterFields.set(fields);

    for (const field of fields) {
      const control = new FormControl<unknown>(null);
      this.advancedFilterForm.addControl(field.apiName, control);
      control.valueChanges.pipe(
        debounceTime(field.type === 'NUMBER' ? 350 : 0),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        const filters: Record<string, string | number | boolean> = {};
        for (const filterField of this.advancedFilterFields()) {
          const value = this.advancedFilterForm.controls[filterField.apiName]?.value;
          if (value !== null && value !== undefined && value !== '') {
            filters[filterField.apiName] = value as string | number | boolean;
          }
        }
        void this.store.search({filters, page: 0});
      });
    }

    const lookupEntries = await Promise.all(fields
      .filter(field => field.type === 'LOOKUP' && field.lookupResource)
      .map(async field => [field.apiName, await this.gateway.lookup(field.lookupResource!)] as const));
    this.advancedFilterLookupOptions.set(Object.fromEntries(lookupEntries));
  }

  private async initializeParentFilter(): Promise<void> {
    const parent = this.store.descriptor()?.parent;
    if (!parent) return;
    this.parentFilterOptions.set(await this.gateway.lookup(parent.resource));
  }

  private buildForm(record: ReferenceRecordResponse | null): void {
    for (const key of Object.keys(this.form.controls)) this.form.removeControl(key);
    const descriptor = this.store.descriptor();
    if (!descriptor) return;
    for (const field of descriptor.fields.filter(item =>
      !item.readOnly && !(item.type === 'LOOKUP' && item.apiName === descriptor.parent?.apiField)
    )) {
      const value = record?.values[field.apiName] ?? field.defaultValue ?? (field.type === 'BOOLEAN' ? false : null);
      const validators = [];
      if (field.required) validators.push(Validators.required);
      if (field.maxLength) validators.push(Validators.maxLength(field.maxLength));
      this.form.addControl(field.apiName, new FormControl<unknown>(value, {validators}));
    }
    this.wirePersianNormalization('persianName', 'normalizedPersianName');
    this.wirePersianNormalization('persianAffix', 'normalizedPersianAffix');
  }

  private wirePersianNormalization(sourceField: string, targetField: string): void {
    const source = this.form.controls[sourceField];
    const target = this.form.controls[targetField];
    if (!source || !target) return;
    source.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(value => {
      target.setValue(this.normalizePersianName(value), {emitEvent: false});
    });
    target.setValue(this.normalizePersianName(source.value), {emitEvent: false});
  }

  private normalizePersianName(value: unknown): string | null {
    if (value === null || value === undefined) return null;
    const normalized = String(value)
      .trim()
      .replace(/ي/g, 'ی')
      .replace(/ى/g, 'ی')
      .replace(/ك/g, 'ک')
      .replace(/ة/g, 'ه')
      .replace(/ۀ/g, 'ه')
      .replace(/ـ/g, '')
      .replace(/\u200c/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
    return normalized || null;
  }

  private async initializeLookupFields(): Promise<void> {
    const descriptor = this.store.descriptor();
    if (!descriptor) return;
    const fields = descriptor.fields.filter(field =>
      !field.readOnly && field.type === 'LOOKUP' && field.apiName !== descriptor.parent?.apiField && field.lookupResource
    );
    const entries = await Promise.all(fields.map(async field => [
      field.apiName,
      await this.gateway.lookup(field.lookupResource as string)
    ] as const));
    this.lookupOptions.update(current => ({...current, ...Object.fromEntries(entries)}));
  }

  private async initializeGridLookupFields(): Promise<void> {
    const descriptor = this.store.descriptor();
    if (!descriptor) return;
    const fields = descriptor.fields.filter(field =>
      field.grid && field.type === 'LOOKUP' && field.apiName !== descriptor.parent?.apiField && field.lookupResource
    );
    const entries = await Promise.all(fields.map(async field => [
      field.apiName,
      await this.gateway.lookup(field.lookupResource as string)
    ] as const));
    this.lookupOptions.update(current => ({...current, ...Object.fromEntries(entries)}));
  }

  private buildHierarchy(): readonly CatalogItem[] {
    const descriptor = this.store.descriptor();
    if (!descriptor?.parent) return [];
    const chain: CatalogItem[] = [];
    let resource: string | null = descriptor.parent.resource;
    while (resource) {
      const item = this.catalog.find(resource);
      if (!item) break;
      chain.unshift(item);
      resource = item.parentResource;
    }
    return chain;
  }

  private async initializeHierarchy(ancestors: readonly {resource: string; id: number}[]): Promise<void> {
    const chain = this.hierarchy();
    const values: Record<string, number | null> = {};
    const options: Record<string, readonly LookupOption[]> = {};
    this.hierarchyValues.set(values);
    this.hierarchyOptions.set(options);
    let parentId: number | null = null;
    for (const item of chain) {
      const loaded = await this.gateway.lookup(item.resource, parentId);
      options[item.resource] = loaded;
      const existing = ancestors.find(ancestor => ancestor.resource === item.resource)?.id ?? null;
      values[item.resource] = existing;
      parentId = existing;
      this.hierarchyOptions.set({...options});
      this.hierarchyValues.set({...values});
      if (existing === null) break;
    }
  }

  private async loadOptions(resource: string, parentId: number | null): Promise<void> {
    const loaded = await this.gateway.lookup(resource, parentId);
    this.hierarchyOptions.update(current => ({...current, [resource]: loaded}));
  }
}

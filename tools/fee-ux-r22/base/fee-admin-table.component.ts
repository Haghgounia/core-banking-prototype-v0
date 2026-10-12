import {HttpErrorResponse} from '@angular/common/http';
import {Component, computed, EventEmitter, inject, Input, OnChanges, Output, signal, SimpleChanges} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormControl, FormRecord, ReactiveFormsModule, Validators} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {DatabaseTablesComponent} from '../../shared/ui/database-tables.component';
import {PersianDateInputComponent} from '../../shared/ui/persian-date-input.component';
import {FeeColumnDescriptor, FeeSelectOption, FeeTableDescriptor} from './fee-admin.models';
import {FeeAdminService} from './fee-admin.service';
import {FEE_WORKSPACES} from './fee-workspaces';


/** Embedded, context-filtered mode shares the exact CRUD contracts of the legacy form. */
export interface FeeEmbeddedContext {
  readonly tableName: string;
  readonly filterColumn?: string;
  readonly filterValue?: string | number;
}

interface CalculationStrategyCard {
  readonly code: string;
  readonly title: string;
  readonly description: string;
  readonly icon: string;
}

@Component({
  selector: 'app-fee-admin-table',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule,
    MatSelectModule, MatProgressBarModule, DatabaseTablesComponent, PersianDateInputComponent],
  templateUrl: './fee-admin-table.component.html',
  styleUrl: './fee-admin-table.component.scss'
})
export class FeeAdminTableComponent implements OnChanges {
  @Input() embedded = false;
  @Input() context: FeeEmbeddedContext | null = null;
  @Input() selectable = false;
  @Input() chosenRowId: number | null = null;
  // Optional one-shot editor launch used by the R2.1 contextual catalog overview.
  @Input() quickAction: 'create' | 'edit' | null = null;
  @Input() quickRow: Readonly<Record<string, any>> | null = null;
  @Output() rowPicked = new EventEmitter<Readonly<Record<string, any>>>();
  @Output() rowWritten = new EventEmitter<Readonly<Record<string, any>>>();
  private initSerial = 0;
  private searchSerial = 0;

  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(FeeAdminService);

  readonly tableName = signal('');
  readonly descriptor = signal<FeeTableDescriptor | null>(null);
  readonly rows = signal<readonly Readonly<Record<string, any>>[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly size = 25;
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly editorOpen = signal(false);
  readonly showTechnicalDetails = signal(false);
  readonly currentWorkspace = computed(() => FEE_WORKSPACES.find(workspace => workspace.tables.includes(this.tableName())));
  readonly currentWorkspaceRoute = computed(() => this.currentWorkspace()?.key === 'reference'
    ? ['/fee', 'references'] : ['/fee', 'workspaces', this.currentWorkspace()?.key ?? 'catalog']);
  readonly editingId = signal<number | null>(null);
  readonly selectedRow = signal<Readonly<Record<string, any>> | null>(null);
  readonly filterColumn = signal<string | null>(null);
  readonly filterValue = signal<string | null>(null);
  readonly lookupOptions = signal<Record<string, readonly FeeSelectOption[]>>({});
  readonly searchControl = new FormControl('', {nonNullable: true});
  readonly form = new FormRecord<FormControl<unknown>>({});

  readonly calculationStrategies: readonly CalculationStrategyCard[] = [
    {code: 'FIXED', title: 'مبلغ ثابت', description: 'یک مبلغ ثابت برای هر بار اعمال کارمزد', icon: 'payments'},
    {code: 'PERCENTAGE', title: 'درصدی', description: 'نرخ درصدی روی مبلغ یا مبنای انتخاب‌شده', icon: 'percent'},
    {code: 'PERCENTAGE_WITH_FLOOR', title: 'درصدی با کف', description: 'درصدی با حداقل مبلغ کارمزد', icon: 'vertical_align_bottom'},
    {code: 'PERCENTAGE_WITH_CAP', title: 'درصدی با سقف', description: 'درصدی با حداکثر مبلغ کارمزد', icon: 'vertical_align_top'},
    {code: 'PERCENTAGE_FLOOR_CAP', title: 'درصدی با کف و سقف', description: 'درصدی با حداقل و حداکثر مبلغ', icon: 'height'},
    {code: 'FIXED_PLUS_PERCENTAGE', title: 'ثابت + درصد', description: 'جمع مبلغ ثابت و نرخ درصدی', icon: 'add_circle'},
    {code: 'PER_UNIT', title: 'به‌ازای واحد', description: 'مبلغ یا نرخ بر اساس تعداد/واحد', icon: 'tag'},
    {code: 'TIERED', title: 'پلکانی', description: 'محاسبه بر اساس پله‌های تعریف‌شده', icon: 'stairs'},
    {code: 'MARGINAL_TIERED', title: 'پلکانی مازاد', description: 'اعمال نرخ هر پله فقط به بخش مازاد', icon: 'stacked_line_chart'},
    {code: 'ANNUALIZED_PERCENTAGE', title: 'درصد سالانه', description: 'نرخ سالانه با Day Count و Proration', icon: 'calendar_month'},
    {code: 'COMPOSITE', title: 'ترکیبی', description: 'ترکیب چند Rule Component', icon: 'account_tree'}
  ];

  readonly editableColumns = computed(() => this.descriptor()?.editable ? this.descriptor()!.columns.filter(c =>
    !c.readOnly && !(this.embedded && this.filterColumn() === c.name)) : []);
  readonly auditColumns = computed(() => this.descriptor()?.columns.filter(c => c.readOnly && !c.primaryKey) ?? []);
  readonly gridColumns = computed(() => {
    const d = this.descriptor();
    if (!d) return [];
    const preferred = d.columns.filter(c => !c.readOnly && c.dataType !== 'CLOB');
    return [d.columns.find(c => c.primaryKey)!, ...preferred].filter((c, i, all) => c && all.findIndex(x => x.name === c.name) === i).slice(0, 8);
  });
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.size)));

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe(params => {
      if (this.context) return;
      this.tableName.set((params.get('table') ?? '').toUpperCase());
      this.page.set(0); this.editorOpen.set(false); this.editingId.set(null); this.selectedRow.set(null);
      void this.initialize();
    });
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
      if (this.context) return;
      this.filterColumn.set(params.get('filterColumn')); this.filterValue.set(params.get('filterValue'));
      if (this.descriptor()) void this.search(0);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['context'] || !this.context) return;
    const ctx = this.context;
    this.tableName.set(ctx.tableName.toUpperCase());
    this.filterColumn.set(ctx.filterColumn ?? null);
    this.filterValue.set(ctx.filterValue === undefined || ctx.filterValue === null ? null : String(ctx.filterValue));
    this.page.set(0);
    ++this.searchSerial; // invalidate any pending search for the old context
    this.searchControl.setValue('');
    this.descriptor.set(null);
    this.rows.set([]); this.total.set(0); this.lookupOptions.set({});
    this.editorOpen.set(false);
    this.editingId.set(null);
    this.selectedRow.set(null);
    void this.initialize();
  }

  hasUnsavedChanges(): boolean { return this.editorOpen() && this.form.dirty; }
  pick(row: Readonly<Record<string, any>>): void { this.rowPicked.emit(row); }
  rowId(row: Readonly<Record<string, any>>): number { return Number(row[this.descriptor()?.primaryKeyColumn ?? '']); }

  async initialize(): Promise<void> {
    if (!this.tableName()) return;
    const serial = ++this.initSerial;
    const targetTable = this.tableName();
    this.loading.set(true); this.error.set(null);
    try {
      const d = await this.service.descriptor(targetTable);
      if (serial !== this.initSerial) return;
      this.descriptor.set(d);
      this.buildForm(null);
      await this.loadLookups();
      if (serial !== this.initSerial) return;
      await this.search(0);
      if (serial !== this.initSerial || !this.embedded) return;
      if (this.quickAction === 'create') await this.create();
      else if (this.quickAction === 'edit' && this.quickRow) await this.open(this.quickRow);
    } catch (e) { if (serial === this.initSerial) this.error.set(this.message(e)); }
    finally { if (serial === this.initSerial) this.loading.set(false); }
  }

  async search(page = this.page()): Promise<void> {
    if (!this.descriptor()) return;
    const serial = ++this.searchSerial;
    const targetTable = this.tableName();
    this.loading.set(true); this.error.set(null);
    try {
      const result = await this.service.rows(targetTable, {text: this.searchControl.value, page, size: this.size,
        filterColumn: this.filterColumn() ?? undefined, filterValue: this.filterValue() ?? undefined});
      if (serial !== this.searchSerial || targetTable !== this.tableName()) return;
      this.rows.set(result.items); this.total.set(result.totalElements); this.page.set(result.page);
    } catch (e) { if (serial === this.searchSerial) this.error.set(this.message(e)); }
    finally { if (serial === this.searchSerial) this.loading.set(false); }
  }

  async create(): Promise<void> {
    if (!this.descriptor()?.editable || this.saving()) return;
    if (this.hasUnsavedChanges() && !window.confirm('ویرایش جاری ذخیره نشده است. فرم جدید باز شود؟')) return;
    this.editingId.set(null); this.selectedRow.set(null); this.buildForm(null); await this.loadLookups(); this.editorOpen.set(true);
  }

  async open(row: Readonly<Record<string, any>>): Promise<void> {
    const d = this.descriptor(); if (!d || this.saving()) return;
    if (this.hasUnsavedChanges() && !window.confirm('تغییرات ذخیره نشده است. رکورد دیگری باز شود؟')) return;
    this.selectedRow.set(row);
    if (d.editable) {
      this.editingId.set(Number(row[d.primaryKeyColumn])); this.buildForm(row); await this.loadLookups();
    }
    this.editorOpen.set(true);
  }

  closeEditor(): void { this.editorOpen.set(false); this.editingId.set(null); this.selectedRow.set(null); }
  toggleTechnicalDetails(): void { this.showTechnicalDetails.update(value => !value); }

  async save(): Promise<void> {
    if (!this.descriptor()?.editable) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true); this.error.set(null);
    try {
      const values = this.form.getRawValue(); const id = this.editingId();
      // Context columns must be immutable even when the form is edited via developer tools.
      if (this.embedded && this.filterColumn() && this.filterValue() !== null) {
        const fk = this.descriptor()!.columns.find(c => c.name === this.filterColumn());
        // A filter on the table PK is a read-scope, never a writeable FK.
        if (fk && !fk.primaryKey && !fk.readOnly) values[fk.name] = fk.dataType.startsWith('NUMBER') ? Number(this.filterValue()) : this.filterValue();
      }
      const saved = id === null ? await this.service.create(this.tableName(), values) : await this.service.update(this.tableName(), id, values);
      this.closeEditor(); await this.search(this.page());
      this.rowWritten.emit(saved);
    } catch (e) { this.error.set(this.message(e)); }
    finally { this.saving.set(false); }
  }

  async remove(row: Readonly<Record<string, any>>): Promise<void> {
    const d = this.descriptor(); if (!d?.editable) return;
    const id = Number(row[d.primaryKeyColumn]);
    if (!window.confirm('این رکورد حذف شود؟ وابستگی‌های FK در Oracle کنترل خواهند شد.')) return;
    try { await this.service.delete(this.tableName(), id); await this.search(this.page()); }
    catch (e) { this.error.set(this.message(e)); }
  }

  isReferenceTable(): boolean { return this.tableName() === 'FEE_REF_DOMAIN' || this.tableName() === 'FEE_REF_VALUE'; }
  canHardDelete(): boolean { return !this.embedded && !this.isReferenceTable(); }

  isCalculationRule(): boolean { return this.tableName() === 'FEE_CALCULATION_RULE'; }

  async createCalculation(strategyCode: string): Promise<void> {
    await this.create();
    this.form.controls['CALCULATION_STRATEGY_CODE']?.setValue(strategyCode);
  }

  currentCalculationStrategy(): string {
    return String(this.form.controls['CALCULATION_STRATEGY_CODE']?.value ?? '');
  }

  onSelectChanged(column: FeeColumnDescriptor, value: unknown): void {
    if (!this.isCalculationRule() || column.name !== 'CALCULATION_STRATEGY_CODE') return;
    this.form.controls['CALCULATION_STRATEGY_CODE']?.setValue(value);
    for (const name of ['FIXED_AMOUNT', 'RATE_VALUE', 'MIN_FEE_AMOUNT', 'MAX_FEE_AMOUNT', 'RATE_PERIOD_CODE', 'DAY_COUNT_BASIS_CODE', 'PRORATION_MODE_CODE', 'MIN_CHARGE_PERIOD_DAYS']) {
      const descriptor = this.descriptor()?.columns.find(c => c.name === name);
      if (descriptor && !this.calculationColumnVisible(descriptor)) this.form.controls[name]?.setValue(null);
    }
  }

  calculationColumnVisible(column: FeeColumnDescriptor): boolean {
    if (!this.isCalculationRule()) return true;
    const strategy = this.currentCalculationStrategy();
    const name = column.name;
    if (name === 'FIXED_AMOUNT') return ['FIXED', 'FIXED_PLUS_PERCENTAGE', 'PER_UNIT'].includes(strategy);
    if (name === 'RATE_VALUE') return ['PERCENTAGE', 'PERCENTAGE_WITH_FLOOR', 'PERCENTAGE_WITH_CAP', 'PERCENTAGE_FLOOR_CAP', 'FIXED_PLUS_PERCENTAGE', 'PER_UNIT', 'ANNUALIZED_PERCENTAGE'].includes(strategy);
    if (name === 'MIN_FEE_AMOUNT') return ['PERCENTAGE_WITH_FLOOR', 'PERCENTAGE_FLOOR_CAP', 'FIXED_PLUS_PERCENTAGE'].includes(strategy);
    if (name === 'MAX_FEE_AMOUNT') return ['PERCENTAGE_WITH_CAP', 'PERCENTAGE_FLOOR_CAP', 'FIXED_PLUS_PERCENTAGE'].includes(strategy);
    if (['RATE_PERIOD_CODE', 'DAY_COUNT_BASIS_CODE', 'PRORATION_MODE_CODE', 'MIN_CHARGE_PERIOD_DAYS'].includes(name)) return strategy === 'ANNUALIZED_PERCENTAGE';
    return true;
  }

  calculationFieldHint(column: FeeColumnDescriptor): string | null {
    if (!this.isCalculationRule()) return null;
    switch (column.name) {
      case 'CALCULATION_STRATEGY_CODE': return 'نوع محاسبه را انتخاب کنید؛ مبلغ ثابت، درصدی، پلکانی یا ترکیبی.';
      case 'FIXED_AMOUNT': return 'مبلغ ثابت به واحد ارز انتخاب‌شده.';
      case 'RATE_VALUE': return 'نرخ به‌صورت اعشاری ذخیره می‌شود؛ مثال: 0.002 یعنی 0.2٪.';
      case 'MIN_FEE_AMOUNT': return 'کف مبلغ کارمزد پس از محاسبه.';
      case 'MAX_FEE_AMOUNT': return 'سقف مبلغ کارمزد پس از محاسبه.';
      default: return null;
    }
  }

  control(column: FeeColumnDescriptor): FormControl<unknown> { return this.form.controls[column.name]; }
  isSelect(column: FeeColumnDescriptor): boolean { return column.foreignKey || column.options.length > 0; }
  options(column: FeeColumnDescriptor): readonly FeeSelectOption[] { return column.foreignKey ? this.lookupOptions()[column.name] ?? [] : column.options; }

  optionText(option: FeeSelectOption): string {
    const value = String(option.value);
    if (value === '1' || value === 'Y') return option.label && option.label !== value ? option.label : 'بله';
    if (value === '0' || value === 'N') return option.label && option.label !== value ? option.label : 'خیر';
    if (option.label && option.code && option.label !== option.code) return `${option.label} (${option.code})`;
    return option.label || option.code || value;
  }

  displayValue(value: unknown, column: FeeColumnDescriptor): string {
    if (value === null || value === undefined || value === '') return '—';
    const opts = this.options(column);
    if (opts.length) {
      const found = opts.find(o => String(o.value) === String(value)); if (found) return this.optionText(found);
    }
    if (column.dataType === 'DATE' || column.dataType.startsWith('TIMESTAMP')) return this.persianDate(String(value), column.dataType.startsWith('TIMESTAMP'));
    return String(value);
  }

  fieldValue(row: Readonly<Record<string, any>>, column: FeeColumnDescriptor): string { return this.displayValue(row[column.name], column); }
  editorTitle(): string { return this.editingId() === null ? 'ثبت رکورد جدید' : 'ویرایش رکورد'; }
  previous(): void { if (this.page() > 0) void this.search(this.page() - 1); }
  next(): void { if (this.page() + 1 < this.totalPages()) void this.search(this.page() + 1); }

  private buildForm(row: Readonly<Record<string, any>> | null): void {
    for (const key of Object.keys(this.form.controls)) this.form.removeControl(key);
    const d = this.descriptor(); if (!d?.editable) return;
    for (const column of d.columns.filter(c => !c.readOnly)) {
      let value = row?.[column.name] ?? this.defaultValue(column);
      if (this.filterColumn() === column.name && this.filterValue() !== null && row === null) value = column.dataType.startsWith('NUMBER') ? Number(this.filterValue()) : this.filterValue();
      const validators = !column.nullable && !this.hasDefault(column) ? [Validators.required] : [];
      this.form.addControl(column.name, new FormControl<unknown>(value, validators));
    }
    this.form.markAsPristine();
  }

  private async loadLookups(): Promise<void> {
    const d = this.descriptor(); if (!d) return;
    const lookups: Record<string, readonly FeeSelectOption[]> = {};
    await Promise.all(d.columns.filter(c => c.foreignKey).map(async column => {
      try { lookups[column.name] = await this.service.lookup(d.tableName, column.name); } catch { lookups[column.name] = []; }
    }));
    if (this.tableName() === d.tableName) this.lookupOptions.set(lookups);
  }

  private defaultValue(column: FeeColumnDescriptor): unknown {
    const raw = column.defaultValue?.trim(); if (!raw || raw.toUpperCase() === 'NULL') return null;
    if (raw.startsWith("'") && raw.endsWith("'")) return raw.slice(1, -1).trim();
    if (column.dataType.startsWith('NUMBER') && /^-?\d+(\.\d+)?$/.test(raw)) return Number(raw);
    return null;
  }

  private hasDefault(column: FeeColumnDescriptor): boolean { const v = column.defaultValue?.trim(); return Boolean(v && v.toUpperCase() !== 'NULL'); }

  private persianDate(value: string, withTime: boolean): string {
    try {
      const date = new Date(value.length === 10 ? `${value}T00:00:00` : value);
      if (Number.isNaN(date.getTime())) return value;
      const formatted = new Intl.DateTimeFormat('fa-IR-u-ca-persian', {year: 'numeric', month: '2-digit', day: '2-digit'}).format(date);
      if (!withTime) return formatted;
      const time = value.match(/T(\d{2}):(\d{2})/); return time ? `${formatted} ${time[1]}:${time[2]}` : formatted;
    } catch { return value; }
  }

  private message(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
      const body = error.error as {detail?: string};
      if (body.detail) return body.detail;
    }
    return error instanceof Error ? error.message : 'خطای پیش‌بینی‌نشده';
  }
}

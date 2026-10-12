import {HttpErrorResponse} from '@angular/common/http';
import {Component, computed, ElementRef, HostListener, inject, signal, ViewChild} from '@angular/core';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {FeeAdminService} from './fee-admin.service';
import {FeeAdminTableComponent, FeeEmbeddedContext} from './fee-admin-table.component';
import {FeeTablePage} from './fee-admin.models';

type FeeRow = Readonly<Record<string, any>>;
type Need = 'none' | 'definition' | 'version' | 'applicability' | 'calculation';
interface StudioTab {
  readonly key: string;
  readonly title: string;
  readonly table: string;
  readonly need: Need;
  readonly filterColumn?: string;
  readonly description: string;
  readonly selectable?: boolean;
}

/** The FEE UX-R2 studio never reads or writes FEE2 data. It uses only existing FEE CRUD APIs. */
const CATALOG_TABS: readonly StudioTab[] = [
  {key:'definition',title:'تعریف کارمزد',table:'FEE_DEFINITION',need:'none',description:'شناسنامه کارمزد؛ یک تعریف را در سمت راست برای کار روی نسخه‌های آن انتخاب کنید.'},
  {key:'version',title:'نسخه‌ها',table:'FEE_DEFINITION_VERSION',need:'definition',filterColumn:'FEE_DEFINITION_ID',description:'نسخه‌های همان تعریف، با شماره نسخه، وضعیت، سیاست و تاریخ اعتبار مستقل.'},
  {key:'feature',title:'ویژگی‌های پایه',table:'FEE_FEATURE',need:'none',description:'کاتالوگ مستقل ویژگی‌های کارمزدی؛ به یک تعریف مشخص محدود نیست.'},
  {key:'product',title:'ویژگی کارمزدی محصول',table:'FEE_PRODUCT_FEATURE',need:'version',filterColumn:'FEE_DEFINITION_VERSION_ID',description:'اتصال نسخه انتخاب‌شده به ویژگی محصول آزمایشی؛ دامنه Product Feature طبق مدل FEE حفظ می‌شود.'}
];
const RULE_TABS: readonly StudioTab[] = [
  {key:'applicability',title:'قواعد شمول',table:'FEE_APPLICABILITY_RULE',need:'version',filterColumn:'FEE_DEFINITION_VERSION_ID',description:'قواعد APPLY / EXCLUDE، اولویت و وضعیت اثرگذاری؛ برای تعریف شروط، ابتدا یک قاعده را با دکمه انتخاب مشخص کنید.',selectable:true},
  {key:'conditions',title:'شروط شمول',table:'FEE_APPLICABILITY_CONDITION',need:'applicability',filterColumn:'APPLICABILITY_RULE_ID',description:'هر شرط فقط به قاعده شمول انتخاب‌شده متصل می‌شود.'},
  {key:'calculation',title:'قواعد محاسبه',table:'FEE_CALCULATION_RULE',need:'version',filterColumn:'FEE_DEFINITION_VERSION_ID',description:'انتخاب استراتژی، مبنا، نرخ و مبلغ؛ برای ورود پله‌ها و ورودی‌ها قاعده را با دکمه انتخاب فعال کنید.',selectable:true},
  {key:'input',title:'ورودی‌های محاسبه',table:'FEE_INPUT_DEFINITION',need:'calculation',filterColumn:'CALCULATION_RULE_ID',description:'ورودی‌های مورد نیاز قاعده محاسبه انتخاب‌شده.'},
  {key:'tier',title:'پله‌های محاسبه',table:'FEE_CALCULATION_TIER',need:'calculation',filterColumn:'CALCULATION_RULE_ID',description:'برای روش‌های TIERED و MARGINAL_TIERED، هر پله را با بازه و استراتژی مخصوص آن نگهداری کنید.'},
  {key:'expression',title:'اجزای قاعده ترکیبی',table:'FEE_RULE_COMPONENT',need:'calculation',filterColumn:'CALCULATION_RULE_ID',description:'Nodeهای Expression Tree برای راهبرد COMPOSITE؛ ترتیب و والد اجزا مهم‌اند.'},
  {key:'charge',title:'مؤلفه‌های مبلغ',table:'FEE_CHARGE_COMPONENT',need:'version',filterColumn:'FEE_DEFINITION_VERSION_ID',description:'اجزای درآمد بانک، مالیات، هزینه عبوری و سایر مؤلفه‌های نسخه انتخابی.'}
];

@Component({
  selector:'app-fee-design-studio',
  standalone:true,
  imports:[RouterLink, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, FeeAdminTableComponent],
  templateUrl:'./fee-design-studio.component.html',
  styleUrl:'./fee-design-studio.component.scss'
})
export class FeeDesignStudioComponent {
  private readonly service = inject(FeeAdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  @ViewChild(FeeAdminTableComponent) editor?: FeeAdminTableComponent;
  @ViewChild('definitionSearchInput') private definitionSearchInput?: ElementRef<HTMLInputElement>;
  @ViewChild('versionSearchInput') private versionSearchInput?: ElementRef<HTMLInputElement>;

  readonly mode: 'catalog' | 'rules';
  readonly tabs: readonly StudioTab[];
  readonly activeTab = signal('');
  readonly selectedDefinition = signal<FeeRow | null>(null);
  readonly selectedVersion = signal<FeeRow | null>(null);
  readonly selectedApplicability = signal<FeeRow | null>(null);
  readonly selectedCalculation = signal<FeeRow | null>(null);
  readonly definitions = signal<readonly FeeRow[]>([]);
  readonly versions = signal<readonly FeeRow[]>([]);
  readonly definitionTotal = signal(0);
  readonly versionTotal = signal(0);
  readonly definitionPage = signal(0);
  readonly versionPage = signal(0);
  readonly loadingDefinitions = signal(false);
  readonly loadingVersions = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  // Only one side-panel list is visible by default. Open the existing CRUD editor on demand.
  readonly catalogManageAction = signal<'create' | 'edit' | null>(null);
  // R2.2 separates browsing, details and full-width editing in the catalog only.
  readonly catalogStage = signal<'browse' | 'detail'>('browse');
  readonly catalogWide = computed(() => this.mode === 'catalog' &&
    (this.activeTab() === 'definition' || this.activeTab() === 'version'));
  readonly defSearch = new FormControl('', {nonNullable:true});
  readonly versionSearch = new FormControl('', {nonNullable:true});
  readonly pageSize = 15;
  private definitionRequest = 0;
  private versionRequest = 0;

  readonly definitionId = computed(() => this.asId(this.selectedDefinition(), 'FEE_DEFINITION_ID'));
  readonly versionId = computed(() => this.asId(this.selectedVersion(), 'FEE_DEFINITION_VERSION_ID'));
  readonly definitionOutsidePage = computed(() => this.definitionId() !== null &&
    !this.definitions().some(row => this.asId(row,'FEE_DEFINITION_ID') === this.definitionId()));
  readonly versionOutsidePage = computed(() => this.versionId() !== null &&
    !this.versions().some(row => this.asId(row,'FEE_DEFINITION_VERSION_ID') === this.versionId()));
  readonly applicabilityId = computed(() => this.asId(this.selectedApplicability(), 'APPLICABILITY_RULE_ID'));
  readonly calculationId = computed(() => this.asId(this.selectedCalculation(), 'CALCULATION_RULE_ID'));
  readonly tab = computed(() => this.tabs.find(t => t.key === this.activeTab()) ?? this.tabs[0]);
  readonly missing = computed(() => {
    switch(this.tab().need) {
      case 'definition': return !this.definitionId() ? 'ابتدا یک کارمزد از فهرست انتخاب کنید.' : null;
      case 'version': return !this.versionId() ? 'ابتدا یک نسخه از فهرست نسخه‌های کارمزد انتخاب کنید.' : null;
      case 'applicability': return !this.applicabilityId() ? 'ابتدا یک قاعده شمول را در تب «قواعد شمول» انتخاب کنید.' : null;
      case 'calculation': return !this.calculationId() ? 'ابتدا یک قاعده محاسبه را در تب «قواعد محاسبه» انتخاب کنید.' : null;
      default: return null;
    }
  });
  readonly tableContext = computed<FeeEmbeddedContext | null>(() => {
    const t = this.tab();
    if (this.missing()) return null;
    if (t.need === 'none') return {tableName:t.table};
    const id = t.need === 'definition' ? this.definitionId() : t.need === 'version' ? this.versionId()
      : t.need === 'applicability' ? this.applicabilityId() : this.calculationId();
    if (!id || !t.filterColumn) return null;
    return {tableName:t.table,filterColumn:t.filterColumn,filterValue:id};
  });
  readonly catalogEditorContext = computed<FeeEmbeddedContext | null>(() => {
    if (this.mode !== 'catalog' || !this.catalogManageAction()) return null;
    if (this.activeTab() === 'definition') {
      if (this.catalogManageAction() === 'edit' && this.definitionId()) {
        return {tableName:'FEE_DEFINITION',filterColumn:'FEE_DEFINITION_ID',filterValue:this.definitionId()!};
      }
      return {tableName:'FEE_DEFINITION'};
    }
    if (this.activeTab() === 'version' && this.definitionId()) {
      return {tableName:'FEE_DEFINITION_VERSION',filterColumn:'FEE_DEFINITION_ID',filterValue:this.definitionId()!};
    }
    return null;
  });
  readonly catalogEditorRow = computed<FeeRow | null>(() => this.activeTab() === 'definition'
    ? this.selectedDefinition() : this.activeTab() === 'version' ? this.selectedVersion() : null);
  readonly selectedRowId = computed(() => this.tab().key === 'applicability' ? this.applicabilityId()
    : this.tab().key === 'calculation' ? this.calculationId() : null);
  readonly ruleSummary = computed(() => `${this.selectedApplicability()?.['RULE_CODE'] ?? '—'} / ${this.selectedCalculation()?.['RULE_CODE'] ?? '—'}`);
  readonly strategySummary = computed(() => String(this.selectedCalculation()?.['CALCULATION_STRATEGY_CODE'] ?? '—'));
  readonly legacyParams = computed(() => {
    const c = this.tableContext();
    return c?.filterColumn ? {filterColumn:c.filterColumn,filterValue:String(c.filterValue)} : {};
  });
  readonly navigationParams = computed(() => ({
    definitionId: this.definitionId(), versionId:this.versionId()
  }));

  constructor() {
    this.mode = this.route.snapshot.data['mode'] === 'rules' ? 'rules' : 'catalog';
    this.tabs = this.mode === 'rules' ? RULE_TABS : CATALOG_TABS;
    const requestedTab = this.route.snapshot.queryParamMap.get('tab');
    this.activeTab.set(this.tabs.some(t => t.key === requestedTab) ? requestedTab! : this.tabs[0].key);
    const requestedDefinition = this.positiveId(this.route.snapshot.queryParamMap.get('definitionId'));
    const requestedVersion = this.positiveId(this.route.snapshot.queryParamMap.get('versionId'));
    if (this.mode === 'catalog') {
      this.catalogStage.set(this.activeTab() === 'definition' && requestedDefinition
        || this.activeTab() === 'version' && requestedVersion ? 'detail' : 'browse');
    }
    void this.loadDefinitions(0, requestedDefinition, requestedVersion);
  }

  display(row: FeeRow, table:'definition'|'version'): string {
    if (table === 'definition') return String(row['NAME_FA'] ?? row['FEE_NAME_FA'] ?? row['FEE_CODE'] ?? row['DEFINITION_CODE'] ?? row['FEE_DEFINITION_ID'] ?? '—');
    return String(row['VERSION_NO'] ?? row['FEE_DEFINITION_VERSION_ID'] ?? '—');
  }
  subtitle(row: FeeRow, table:'definition'|'version'): string {
    if (table === 'definition') return String(row['FEE_CODE'] ?? row['DEFINITION_CODE'] ?? row['FEE_TYPE_CODE'] ?? `#${row['FEE_DEFINITION_ID']}`);
    const extra = row['STATUS_CODE'] ? ` · ${row['STATUS_CODE']}` : '';
    return `#${row['FEE_DEFINITION_VERSION_ID']}${extra}`;
  }
  asId(row: FeeRow | null, key: string): number | null {
    if (!row || row[key] === null || row[key] === undefined) return null;
    const v = Number(row[key]); return Number.isSafeInteger(v) && v > 0 ? v : null;
  }
  compactText(value: unknown, fallback = '—'): string {
    return value === null || value === undefined || String(value).trim() === '' ? fallback : String(value);
  }
  activationLabel(value: unknown): string {
    return String(value ?? '').toUpperCase() === 'Y' ? 'فعال' : String(value ?? '').toUpperCase() === 'N' ? 'غیرفعال' : 'نامشخص';
  }
  dateLabel(value: unknown): string {
    if (!value) return '—';
    const raw = String(value);
    const date = new Date(raw.length === 10 ? `${raw}T00:00:00` : raw);
    if (Number.isNaN(date.getTime())) return raw;
    return new Intl.DateTimeFormat('fa-IR-u-ca-persian', {year:'numeric',month:'2-digit',day:'2-digit'}).format(date);
  }
  focusDefinitionSearch(): void {
    if (this.mode === 'catalog' && (this.activeTab() !== 'definition' || this.catalogStage() !== 'browse')) {
      if (!this.canSwitch()) return;
      this.activeTab.set('definition'); this.catalogManageAction.set(null); this.catalogStage.set('browse');
      void this.syncUrl(); return;
    }
    this.definitionSearchInput?.nativeElement.scrollIntoView({behavior:'smooth',block:'center'});
    this.definitionSearchInput?.nativeElement.focus();
  }
  focusVersionSearch(): void {
    if (this.mode === 'catalog' && (this.activeTab() !== 'version' || this.catalogStage() !== 'browse')) {
      if (!this.definitionId() || !this.canSwitch()) return;
      this.activeTab.set('version'); this.catalogManageAction.set(null); this.catalogStage.set('browse');
      void this.syncUrl(); return;
    }
    this.versionSearchInput?.nativeElement.scrollIntoView({behavior:'smooth',block:'center'});
    this.versionSearchInput?.nativeElement.focus();
  }
  returnToCatalogList(): void {
    if (!this.canSwitch()) return;
    this.catalogManageAction.set(null);
    this.catalogStage.set('browse');
  }
  openCatalogEditor(action: 'create' | 'edit'): void {
    if (this.mode !== 'catalog' || !['definition','version'].includes(this.activeTab())) return;
    if (this.activeTab() === 'version' && !this.definitionId()) return;
    if (action === 'edit' && !this.catalogEditorRow()) return;
    if (!this.canSwitch()) return;
    this.catalogManageAction.set(action);
  }
  closeCatalogEditor(): void {
    if (this.canSwitch()) this.catalogManageAction.set(null);
  }
  private positiveId(value: unknown): number | null {
    if (value === null || value === '') return null;
    const n = Number(value); return Number.isSafeInteger(n) && n > 0 ? n : null;
  }
  private message(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
      const body = error.error as {detail?:string}; if (body.detail) return body.detail;
    }
    return error instanceof Error ? error.message : 'خطا در دریافت اطلاعات کارمزد';
  }

  async loadDefinitions(page = 0, restoreDefinition: number | null = null, restoreVersion: number | null = null): Promise<void> {
    const serial = ++this.definitionRequest;
    this.loadingDefinitions.set(true); this.error.set(null);
    try {
      const result: FeeTablePage = await this.service.rows('FEE_DEFINITION', {text:this.defSearch.value, page, size:this.pageSize});
      if (serial !== this.definitionRequest) return;
      this.definitions.set(result.items); this.definitionTotal.set(result.totalElements); this.definitionPage.set(result.page);
      if (restoreDefinition) {
        const row = result.items.find(x => this.asId(x,'FEE_DEFINITION_ID') === restoreDefinition)
          ?? await this.service.row('FEE_DEFINITION', restoreDefinition);
        if (serial !== this.definitionRequest) return;
        this.selectedDefinition.set(row);
        await this.loadVersions(0, restoreVersion);
      }
    } catch(e) { if (serial === this.definitionRequest) this.error.set(this.message(e)); }
    finally { if (serial === this.definitionRequest) this.loadingDefinitions.set(false); }
  }
  async loadVersions(page=0, restoreVersion: number | null = null): Promise<void> {
    const definitionId = this.definitionId();
    const serial = ++this.versionRequest;
    if (!definitionId) { this.versions.set([]); this.versionTotal.set(0);this.versionPage.set(0);this.loadingVersions.set(false);return; }
    this.loadingVersions.set(true);this.error.set(null);
    try {
      const result = await this.service.rows('FEE_DEFINITION_VERSION', {text:this.versionSearch.value,page,size:this.pageSize,
        filterColumn:'FEE_DEFINITION_ID', filterValue:definitionId});
      if (serial !== this.versionRequest || this.definitionId() !== definitionId) return;
      this.versions.set(result.items);this.versionTotal.set(result.totalElements);this.versionPage.set(result.page);
      if (restoreVersion) {
        const row = result.items.find(x => this.asId(x,'FEE_DEFINITION_VERSION_ID') === restoreVersion)
          ?? await this.service.row('FEE_DEFINITION_VERSION', restoreVersion);
        if (serial !== this.versionRequest) return;
        if (this.asId(row,'FEE_DEFINITION_ID') !== definitionId) throw new Error('نسخه انتخاب‌شده متعلق به تعریف جاری نیست.');
        this.selectedVersion.set(row);
      }
    } catch(e) { if (serial === this.versionRequest) this.error.set(this.message(e)); }
    finally { if (serial === this.versionRequest) this.loadingVersions.set(false); }
  }
  private canSwitch(): boolean {
    if (this.editor?.saving()) { window.alert('ثبت تغییرات هنوز کامل نشده است.'); return false; }
    if (this.editor?.hasUnsavedChanges()) return window.confirm('تغییرات ذخیره‌نشده این فرم از دست می‌رود. ادامه می‌دهید؟');
    return true;
  }
  canLeave(): boolean { return this.canSwitch(); }
  @HostListener('window:beforeunload', ['$event'])
  beforeUnload(event: BeforeUnloadEvent): void {
    if (this.editor?.hasUnsavedChanges()) event.preventDefault();
  }
  selectDefinition(row: FeeRow): void {
    if (this.definitionId() === this.asId(row,'FEE_DEFINITION_ID')) {
      if (this.mode === 'catalog') this.catalogStage.set('detail');
      return;
    }
    if (!this.canSwitch()) return;
    this.selectedDefinition.set(row);this.selectedVersion.set(null);this.clearRuleSelections();this.catalogManageAction.set(null);
    if (this.mode === 'catalog') this.catalogStage.set('detail');
    this.versions.set([]);this.versionTotal.set(0);this.versionPage.set(0);this.versionSearch.setValue('');
    this.notice.set(null);
    void this.syncUrl();void this.loadVersions();
  }
  selectVersion(row: FeeRow): void {
    if (this.versionId() === this.asId(row,'FEE_DEFINITION_VERSION_ID')) {
      if (this.mode === 'catalog') this.catalogStage.set('detail');
      return;
    }
    if (!this.canSwitch()) return;
    if (this.asId(row,'FEE_DEFINITION_ID') !== this.definitionId()) return;
    this.selectedVersion.set(row);this.clearRuleSelections();this.catalogManageAction.set(null);this.notice.set(null);
    if (this.mode === 'catalog') this.catalogStage.set('detail');
    void this.syncUrl();
  }
  chooseTab(key: string): void {
    if (!this.tabs.some(t => t.key === key) || key === this.activeTab() || !this.canSwitch()) return;
    this.activeTab.set(key);this.notice.set(null);this.catalogManageAction.set(null);
    if (this.mode === 'catalog') this.catalogStage.set(
      key === 'definition' && this.definitionId() || key === 'version' && this.versionId() ? 'detail' : 'browse');
    void this.syncUrl();
  }
  rowPicked(row: FeeRow): void {
    const tab = this.tab().key;
    if (tab === 'applicability' && this.applicabilityId() !== this.asId(row,'APPLICABILITY_RULE_ID')) {
      if (!this.canSwitch()) return;
      this.selectedApplicability.set(row);
    }
    if (tab === 'calculation' && this.calculationId() !== this.asId(row,'CALCULATION_RULE_ID')) {
      if (!this.canSwitch()) return;
      this.selectedCalculation.set(row);
    }
  }
  async rowWritten(row: FeeRow): Promise<void> {
    const t = this.tab().key;
    if (t === 'definition') {
      const id = this.asId(row,'FEE_DEFINITION_ID');
      if (id) { this.selectedDefinition.set(row);this.selectedVersion.set(null);this.clearRuleSelections();
        this.versions.set([]);this.versionSearch.setValue('');await this.loadDefinitions(0);await this.loadVersions();await this.syncUrl(); }
    } else if (t === 'version') {
      this.selectedVersion.set(row);this.clearRuleSelections();await this.loadVersions(0);await this.syncUrl();
    } else if (t === 'applicability' || t === 'calculation') {
      this.rowPicked(row);
    }
    this.catalogManageAction.set(null);
    if (this.mode === 'catalog') this.catalogStage.set('detail');
    this.notice.set('تغییرات ذخیره شد؛ ارتباطات و فهرست‌های وابسته به‌روزرسانی شدند.');
  }
  private clearRuleSelections(): void {this.selectedApplicability.set(null);this.selectedCalculation.set(null);}
  private async syncUrl(): Promise<void> {
    await this.router.navigate([], {relativeTo:this.route,
      queryParams:{...this.navigationParams(),tab:this.activeTab() === this.tabs[0].key ? null : this.activeTab()},
      queryParamsHandling:'merge',replaceUrl:true});
  }
  definitionPages(): number {return Math.max(1,Math.ceil(this.definitionTotal()/this.pageSize));}
  versionPages(): number {return Math.max(1,Math.ceil(this.versionTotal()/this.pageSize));}
  definitionPrevious(): void {if(this.definitionPage()>0) void this.loadDefinitions(this.definitionPage()-1);}
  definitionNext(): void {if(this.definitionPage()+1<this.definitionPages()) void this.loadDefinitions(this.definitionPage()+1);}
  versionPrevious(): void {if(this.versionPage()>0) void this.loadVersions(this.versionPage()-1);}
  versionNext(): void {if(this.versionPage()+1<this.versionPages()) void this.loadVersions(this.versionPage()+1);}
}

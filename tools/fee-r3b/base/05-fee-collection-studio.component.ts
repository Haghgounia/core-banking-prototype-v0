import {HttpErrorResponse} from '@angular/common/http';
import {Component, computed, HostListener, inject, signal, ViewChild} from '@angular/core';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {MatIconModule} from '@angular/material/icon';
import {FeeAdminService} from './fee-admin.service';
import {FeeAdminTableComponent, FeeEmbeddedContext} from './fee-admin-table.component';
import {FeeTablePage} from './fee-admin.models';

type FeeRow = Readonly<Record<string, any>>;
type Selector = 'definition' | 'version' | null;
interface CollectionTab {
  readonly key: string;
  readonly title: string;
  readonly table: string;
  readonly icon: string;
  readonly subtitle: string;
  readonly primaryColumn: string;
  readonly secondaryColumn: string;
  readonly editableInStudio: boolean;
}

/** A FEE-only, version-scoped workspace. No calls to FEE2 or Product Builder. */
const COLLECTION_TABS: readonly CollectionTab[] = [
  {key:'stages',title:'مراحل اخذ',table:'FEE_CHARGE_STAGE',icon:'flag',subtitle:'رویداد اخذ، شماره مرحله و ارتباط با قاعده محاسبه',primaryColumn:'NAME_FA',secondaryColumn:'STAGE_CODE',editableInStudio:false},
  {key:'timing',title:'زمان‌بندی',table:'FEE_TIMING_RULE',icon:'schedule',subtitle:'زمان اعمال، تناوب محاسبه و موعد وصول',primaryColumn:'RULE_CODE',secondaryColumn:'APPLICATION_TIMING_TYPE_CODE',editableInStudio:true},
  {key:'currency',title:'ارز و تبدیل',table:'FEE_CURRENCY_RULE',icon:'currency_exchange',subtitle:'ارز مبنا، ارز ثبت و کنترل تبدیل',primaryColumn:'RULE_CODE',secondaryColumn:'CURRENCY_MODALITY_TYPE_CODE',editableInStudio:true},
  {key:'collection',title:'روش وصول',table:'FEE_COLLECTION_RULE',icon:'payments',subtitle:'روش پرداخت، محل برداشت و شکست وصول',primaryColumn:'RULE_CODE',secondaryColumn:'PAYMENT_METHOD_CODE',editableInStudio:true},
  {key:'posting',title:'ثبت حسابداری',table:'FEE_POSTING_RULE',icon:'receipt_long',subtitle:'هدف ثبت، نقش حساب‌های بدهکار و بستانکار',primaryColumn:'RULE_CODE',secondaryColumn:'POSTING_MODALITY_TYPE_CODE',editableInStudio:true}
];

@Component({
  selector:'app-fee-collection-studio',
  standalone:true,
  imports:[ReactiveFormsModule, RouterLink, MatIconModule, FeeAdminTableComponent],
  templateUrl:'./fee-collection-studio.component.html',
  styleUrl:'./fee-collection-studio.component.scss'
})
export class FeeCollectionStudioComponent {
  private readonly service = inject(FeeAdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  @ViewChild(FeeAdminTableComponent) editor?: FeeAdminTableComponent;

  readonly tabs = COLLECTION_TABS;
  readonly activeTab = signal(COLLECTION_TABS[0].key);
  readonly tab = computed(() => this.tabs.find(t => t.key === this.activeTab()) ?? this.tabs[0]);
  readonly primaryKey = computed(() => ({
    FEE_CHARGE_STAGE:'CHARGE_STAGE_ID', FEE_TIMING_RULE:'TIMING_RULE_ID', FEE_CURRENCY_RULE:'CURRENCY_RULE_ID',
    FEE_COLLECTION_RULE:'COLLECTION_RULE_ID', FEE_POSTING_RULE:'POSTING_RULE_ID'
  } as Record<string,string>)[this.tab().table]);
  readonly selectedDefinition = signal<FeeRow | null>(null);
  readonly selectedVersion = signal<FeeRow | null>(null);
  readonly definitionId = computed(() => this.rowId(this.selectedDefinition(),'FEE_DEFINITION_ID'));
  readonly versionId = computed(() => this.rowId(this.selectedVersion(),'FEE_DEFINITION_VERSION_ID'));
  readonly selectionPanel = signal<Selector>('definition');
  readonly definitionSearch = new FormControl('',{nonNullable:true});
  readonly versionSearch = new FormControl('',{nonNullable:true});
  readonly definitions = signal<readonly FeeRow[]>([]);
  readonly versions = signal<readonly FeeRow[]>([]);
  readonly definitionTotal = signal(0);
  readonly versionTotal = signal(0);
  readonly definitionPage = signal(0);
  readonly versionPage = signal(0);
  readonly loadingDefinitions = signal(false);
  readonly loadingVersions = signal(false);
  readonly rows = signal<readonly FeeRow[]>([]);
  readonly rowTotal = signal(0);
  readonly rowPage = signal(0);
  readonly rowsLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  readonly editorMode = signal<'create' | 'edit' | null>(null);
  readonly editorRow = signal<FeeRow | null>(null);
  readonly pageSize = 12;
  private definitionRequest = 0;
  private versionRequest = 0;
  private rowRequest = 0;

  readonly context = computed<FeeEmbeddedContext | null>(() => this.versionId() ?
    {tableName:this.tab().table,filterColumn:'FEE_DEFINITION_VERSION_ID',filterValue:this.versionId()!} : null);
  readonly queryContext = computed(() => ({definitionId:this.definitionId(),versionId:this.versionId()}));

  constructor() {
    const initialTab = this.route.snapshot.queryParamMap.get('tab');
    if (this.tabs.some(t => t.key === initialTab)) this.activeTab.set(initialTab!);
    const defId = this.positiveId(this.route.snapshot.queryParamMap.get('definitionId'));
    const versionId = this.positiveId(this.route.snapshot.queryParamMap.get('versionId'));
    void this.loadDefinitions(0,defId,versionId);
  }

  display(row: FeeRow | null, column: string, fallback = '—'): string {
    if (!row) return fallback;
    const value = row[column];
    return value === undefined || value === null || String(value).trim() === '' ? fallback : String(value);
  }
  nameDefinition(row: FeeRow | null): string {return this.display(row,'NAME_FA','تعریف انتخاب نشده');}
  nameVersion(row: FeeRow | null): string {return row ? `نسخه ${this.display(row,'VERSION_NO')} · ${this.display(row,'STATUS_CODE')}` : 'نسخه انتخاب نشده';}
  rowId(row: FeeRow | null, column: string): number | null {return this.positiveId(row?.[column]);}
  activation(row: FeeRow): string {return row['IS_ACTIVE'] === 'Y' ? 'فعال' : row['IS_ACTIVE'] === 'N' ? 'غیرفعال' : '—';}
  totalPages(total: number): number {return Math.max(1,Math.ceil(total / this.pageSize));}
  definitionOutsidePage(): boolean {return !!this.definitionId() && !this.definitions().some(r => this.rowId(r,'FEE_DEFINITION_ID')===this.definitionId());}
  versionOutsidePage(): boolean {return !!this.versionId() && !this.versions().some(r => this.rowId(r,'FEE_DEFINITION_VERSION_ID')===this.versionId());}

  showSelector(panel: Exclude<Selector,null>): void {
    if (this.selectionPanel() === panel) {this.selectionPanel.set(null);return;}
    if (panel === 'version' && !this.definitionId()) return;
    if (!this.canLeave()) return;
    this.selectionPanel.set(panel);
  }

  async loadDefinitions(page=0, restoreDef:number | null=null, restoreVersion:number | null=null):Promise<void> {
    const serial=++this.definitionRequest;
    this.loadingDefinitions.set(true);this.error.set(null);
    try {
      const result=await this.service.rows('FEE_DEFINITION',{text:this.definitionSearch.value,page,size:this.pageSize});
      if(serial!==this.definitionRequest)return;
      this.definitions.set(result.items);this.definitionPage.set(result.page);this.definitionTotal.set(result.totalElements);
      if(restoreDef){
        const row=result.items.find(r=>this.rowId(r,'FEE_DEFINITION_ID')===restoreDef)
          ??await this.service.row('FEE_DEFINITION',restoreDef);
        if(serial!==this.definitionRequest)return;
        if(this.rowId(row,'FEE_DEFINITION_ID')!==restoreDef)throw new Error('شناسه تعریف کارمزد با پاسخ سرور مطابقت ندارد.');
        this.selectedDefinition.set(row);
        await this.loadVersions(0,restoreVersion);
      }
    }catch(e){if(serial===this.definitionRequest)this.error.set(this.errorText(e));}
    finally{if(serial===this.definitionRequest)this.loadingDefinitions.set(false);}
  }
  async loadVersions(page=0,restoreVersion:number | null=null):Promise<void>{
    const definition=this.definitionId();const serial=++this.versionRequest;
    if(!definition){this.versions.set([]);this.versionTotal.set(0);this.versionPage.set(0);this.loadingVersions.set(false);return;}
    this.loadingVersions.set(true);this.error.set(null);
    try{
      const result=await this.service.rows('FEE_DEFINITION_VERSION',{
        text:this.versionSearch.value,page,size:this.pageSize,filterColumn:'FEE_DEFINITION_ID',filterValue:definition
      });
      if(serial!==this.versionRequest || this.definitionId()!==definition)return;
      if(result.items.some(r=>this.rowId(r,'FEE_DEFINITION_ID')!==definition))throw new Error('پاسخ نسخه‌ها با تعریف انتخابی ناسازگار است.');
      this.versions.set(result.items);this.versionPage.set(result.page);this.versionTotal.set(result.totalElements);
      if(restoreVersion){
        const row=result.items.find(r=>this.rowId(r,'FEE_DEFINITION_VERSION_ID')===restoreVersion)
          ??await this.service.row('FEE_DEFINITION_VERSION',restoreVersion);
        if(serial!==this.versionRequest || this.definitionId()!==definition)return;
        if(this.rowId(row,'FEE_DEFINITION_ID')!==definition)throw new Error('نسخه درخواست‌شده متعلق به این تعریف کارمزد نیست.');
        this.selectedVersion.set(row);this.selectionPanel.set(null);
        await this.loadRows(0);
      } else if (!this.versionId()) this.selectionPanel.set('version');
    }catch(e){if(serial===this.versionRequest)this.error.set(this.errorText(e));}
    finally{if(serial===this.versionRequest)this.loadingVersions.set(false);}
  }
  selectDefinition(row:FeeRow):void{
    const id=this.rowId(row,'FEE_DEFINITION_ID');
    if(!id || !this.canLeave())return;
    if(id===this.definitionId()){this.selectionPanel.set('version');return;}
    this.selectedDefinition.set(row);this.selectedVersion.set(null);
    this.versionSearch.setValue('');this.versions.set([]);this.versionTotal.set(0);this.versionPage.set(0);
    this.rows.set([]);this.rowTotal.set(0);this.editorMode.set(null);this.selectionPanel.set('version');
    ++this.rowRequest;++this.versionRequest;this.rowsLoading.set(false);
    this.notice.set(null);void this.syncUrl();void this.loadVersions();
  }
  selectVersion(row:FeeRow):void{
    const definition=this.definitionId();const id=this.rowId(row,'FEE_DEFINITION_VERSION_ID');
    if(!definition || !id || this.rowId(row,'FEE_DEFINITION_ID')!==definition || !this.canLeave())return;
    this.selectedVersion.set(row);this.selectionPanel.set(null);this.editorMode.set(null);
    this.notice.set(null);void this.syncUrl();void this.loadRows(0);
  }
  switchTab(key:string):void{
    if(!this.tabs.some(t=>t.key===key) || key===this.activeTab() || !this.canLeave())return;
    this.activeTab.set(key);this.editorMode.set(null);this.editorRow.set(null);this.notice.set(null);
    void this.syncUrl();void this.loadRows(0);
  }
  async loadRows(page=0):Promise<void>{
    const version=this.versionId();const table=this.tab().table;const serial=++this.rowRequest;
    if(!version){this.rows.set([]);this.rowTotal.set(0);this.rowPage.set(0);this.rowsLoading.set(false);return;}
    this.rowsLoading.set(true);this.error.set(null);
    try{
      const result:FeeTablePage=await this.service.rows(table,{page,size:this.pageSize,
        filterColumn:'FEE_DEFINITION_VERSION_ID',filterValue:version});
      if(serial!==this.rowRequest || this.versionId()!==version || this.tab().table!==table)return;
      if(result.items.some(r=>this.rowId(r,'FEE_DEFINITION_VERSION_ID')!==version))
        throw new Error('نتایج این بخش متعلق به نسخه انتخاب‌شده نیستند؛ نمایش برای حفاظت از داده متوقف شد.');
      this.rows.set(result.items);this.rowTotal.set(result.totalElements);this.rowPage.set(result.page);
    }catch(e){if(serial===this.rowRequest){this.rows.set([]);this.rowTotal.set(0);this.error.set(this.errorText(e));}}
    finally{if(serial===this.rowRequest)this.rowsLoading.set(false);}
  }
  newRow():void{
    if(!this.versionId() || !this.tab().editableInStudio || !this.canLeave())return;
    this.editorRow.set(null);this.editorMode.set('create');
  }
  editRow(row:FeeRow):void{
    if(!this.versionId() || !this.tab().editableInStudio || !this.canLeave())return;
    if(this.rowId(row,'FEE_DEFINITION_VERSION_ID')!==this.versionId()){
      this.error.set('این رکورد متعلق به نسخه انتخاب‌شده نیست.');return;
    }
    this.editorRow.set(row);this.editorMode.set('edit');
  }
  closeEditor():void{
    if(!this.canLeave())return;
    this.editorMode.set(null);this.editorRow.set(null);
  }
  rowWritten(saved:FeeRow):void{
    if(this.rowId(saved,'FEE_DEFINITION_VERSION_ID')!==this.versionId()){
      this.error.set('نسخه رکورد ذخیره‌شده با نسخه جاری مطابقت ندارد؛ لطفاً نتیجه را بازبینی کنید.');return;
    }
    this.editorMode.set(null);this.editorRow.set(null);
    this.notice.set('رکورد در همان نسخه کارمزد ذخیره شد.');void this.loadRows(0);
  }
  canLeave():boolean{
    if(this.editor?.saving()){window.alert('ذخیره رکورد هنوز پایان نیافته است.');return false;}
    return !this.editor?.hasUnsavedChanges() || window.confirm('تغییرات ذخیره‌نشده از بین می‌رود. ادامه می‌دهید؟');
  }
  @HostListener('window:beforeunload',['$event'])
  beforeUnload(event:BeforeUnloadEvent):void{if(this.editor?.hasUnsavedChanges())event.preventDefault();}

  private positiveId(input:unknown):number|null{
    if(input===undefined || input===null || input==='')return null;
    const n=Number(input);return Number.isSafeInteger(n)&&n>0?n:null;
  }
  private async syncUrl():Promise<void>{
    await this.router.navigate([],{relativeTo:this.route,queryParams:{...this.queryContext(),tab:this.activeTab()===this.tabs[0].key?null:this.activeTab()},
      queryParamsHandling:'merge',replaceUrl:true});
  }
  private errorText(e:unknown):string{
    if(e instanceof HttpErrorResponse && e.error && typeof e.error==='object'){
      const detail=(e.error as {detail?:string}).detail;if(detail)return detail;
    }
    return e instanceof Error?e.message:'خطا در دریافت اطلاعات FEE';
  }
}

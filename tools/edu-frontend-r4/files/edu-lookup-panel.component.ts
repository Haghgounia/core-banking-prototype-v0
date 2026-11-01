import {Component, DestroyRef, inject, signal} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {debounceTime, distinctUntilChanged} from 'rxjs';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatButtonModule} from '@angular/material/button';
import {MatProgressBarModule} from '@angular/material/progress-bar';

type EduLookupType = 'fields' | 'levels' | 'institutions' | 'field-levels';
interface EduOption {value: number; code: string; label: string;}

@Component({
  selector: 'app-edu-lookup-panel', standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatProgressBarModule],
  template: `
    <section class="edu-lookup-panel" aria-label="انتخابگرهای اطلاعات تحصیلی">
      <div class="edu-lookup-header">
        <strong>آزمون انتخاب‌گرهای مرجع EDU</strong>
        <small>صرفاً خواندنی؛ انتخاب در این پنل اطلاعات فرم اصلی را تغییر نمی‌دهد.</small>
      </div>
      <div class="edu-lookup-inputs">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>نوع اطلاعات</mat-label>
          <mat-select [formControl]="typeControl">
            <mat-option value="fields">رشته تحصیلی</mat-option>
            <mat-option value="levels">مقطع تحصیلی</mat-option>
            <mat-option value="institutions">مؤسسه آموزشی</mat-option>
            <mat-option value="field-levels">ترکیب رشته و مقطع</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>جست‌وجوی نام یا کد</mat-label>
          <input matInput [formControl]="searchControl" placeholder="حداقل بخشی از نام یا کد">
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>نتیجه انتخاب</mat-label>
          <mat-select [formControl]="selectionControl">
            @for (item of results(); track item.value) {
              <mat-option [value]="item.value">{{item.label}} ({{item.code}})</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </div>
      @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
      @if (error()) { <p class="edu-lookup-error" role="alert">{{error()}}</p> }
      @if (!loading() && !error() && results().length === 0) { <p>نتیجه‌ای یافت نشد.</p> }
      @if (selectionControl.value !== null) { <p>شناسه انتخاب‌شده: <b>{{selectionControl.value}}</b></p> }
    </section>
  `,
  styles: [`
    .edu-lookup-panel {border:1px solid var(--mat-sys-outline-variant,#ddd);border-radius:12px;padding:16px;margin:12px 0;background:var(--mat-sys-surface-container-low,#fafafa)}
    .edu-lookup-header {display:flex;gap:12px;align-items:baseline;flex-wrap:wrap;margin-bottom:12px}
    .edu-lookup-header small {opacity:.75}
    .edu-lookup-inputs {display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:12px}
    .edu-lookup-error {color:#b3261e}
  `]
})
export class EduLookupPanelComponent {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  readonly typeControl = new FormControl<EduLookupType>('fields', {nonNullable:true});
  readonly searchControl = new FormControl('', {nonNullable:true});
  readonly selectionControl = new FormControl<number|null>(null);
  readonly results = signal<readonly EduOption[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');

  constructor() {
    this.typeControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.searchControl.setValue('', {emitEvent:false});
      this.request();
    });
    this.searchControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.request());
    this.request();
  }

  private requestId = 0;
  private request(): void {
    const requestId = ++this.requestId;
    const type = this.typeControl.value;
    const text = this.searchControl.value.trim();
    this.loading.set(true);
    this.error.set('');
    this.selectionControl.setValue(null);
    // Every request completes or is cancelled with component destruction.
    this.http.get<EduOption[]>(`/api/v1/education/lookups/${type}`, {
      params: new HttpParams().set('limit','100').set('text',text)
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: items => { if (requestId !== this.requestId) return;
        this.results.set(items); this.loading.set(false); },
      error: () => { if (requestId !== this.requestId) return;
        this.results.set([]); this.error.set('دریافت اطلاعات از EDU ناموفق بود.'); this.loading.set(false); }
    });
  }
}

import {DOCUMENT} from '@angular/common';
import {DestroyRef, Injectable, computed, inject, signal} from '@angular/core';

export type ThemePreference = 'light' | 'dark' | 'system';
export type EffectiveTheme = 'light' | 'dark';

const STORAGE_KEY = 'core-banking-prototype.theme';
const LEGACY_STORAGE_KEY = 'reference-data-prototype.theme';

@Injectable({providedIn: 'root'})
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);
  private readonly mediaQuery = this.document.defaultView?.matchMedia('(prefers-color-scheme: dark)') ?? null;

  readonly preference = signal<ThemePreference>(this.readPreference());
  readonly effectiveTheme = signal<EffectiveTheme>(this.resolveTheme(this.preference()));
  readonly icon = computed(() => this.effectiveTheme() === 'dark' ? 'dark_mode' : 'light_mode');
  readonly label = computed(() => {
    switch (this.preference()) {
      case 'light':
        return 'تم روشن';
      case 'dark':
        return 'تم تیره';
      default:
        return 'تم سیستم';
    }
  });

  constructor() {
    this.applyTheme();

    const listener = () => {
      if (this.preference() === 'system') {
        this.effectiveTheme.set(this.resolveTheme('system'));
        this.applyThemeClasses();
      }
    };

    this.mediaQuery?.addEventListener('change', listener);
    this.destroyRef.onDestroy(() => this.mediaQuery?.removeEventListener('change', listener));
  }

  setPreference(preference: ThemePreference): void {
    this.preference.set(preference);
    this.effectiveTheme.set(this.resolveTheme(preference));
    this.writePreference(preference);
    this.applyThemeClasses();
  }

  isSelected(preference: ThemePreference): boolean {
    return this.preference() === preference;
  }

  private applyTheme(): void {
    this.effectiveTheme.set(this.resolveTheme(this.preference()));
    this.applyThemeClasses();
  }

  private applyThemeClasses(): void {
    const root = this.document.documentElement;
    const theme = this.effectiveTheme();
    root.classList.toggle('theme-light', theme === 'light');
    root.classList.toggle('theme-dark', theme === 'dark');
    root.style.colorScheme = theme;
  }

  private resolveTheme(preference: ThemePreference): EffectiveTheme {
    if (preference !== 'system') {
      return preference;
    }
    return this.mediaQuery?.matches ? 'dark' : 'light';
  }

  private readPreference(): ThemePreference {
    try {
      const storage = this.document.defaultView?.localStorage;
      const value = storage?.getItem(STORAGE_KEY) ?? storage?.getItem(LEGACY_STORAGE_KEY);
      return value === 'light' || value === 'dark' || value === 'system' ? value : 'system';
    } catch {
      return 'system';
    }
  }

  private writePreference(preference: ThemePreference): void {
    try {
      this.document.defaultView?.localStorage.setItem(STORAGE_KEY, preference);
    } catch {
      // Local storage may be disabled by the browser. The selected theme still applies for this session.
    }
  }
}

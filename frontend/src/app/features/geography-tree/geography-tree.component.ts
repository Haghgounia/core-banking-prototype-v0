import {HttpClient} from '@angular/common/http';
import {Component, inject, signal} from '@angular/core';
import {firstValueFrom} from 'rxjs';
import {RouterLink} from '@angular/router';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatTooltipModule} from '@angular/material/tooltip';

interface GeographyTreeNode {
  readonly resource: string;
  readonly id: number;
  readonly code: string;
  readonly label: string;
  readonly hasChildren: boolean;
}

interface UiTreeNode extends GeographyTreeNode {
  readonly level: number;
  readonly expanded: boolean;
  readonly loading: boolean;
}

@Component({
  selector: 'app-geography-tree',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, MatTooltipModule],
  templateUrl: './geography-tree.component.html',
  styleUrl: './geography-tree.component.scss'
})
export class GeographyTreeComponent {
  private readonly http = inject(HttpClient);
  readonly nodes = signal<readonly UiTreeNode[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    void this.loadRoots();
  }

  async toggle(index: number): Promise<void> {
    const current = this.nodes()[index];
    if (!current?.hasChildren) return;

    if (current.expanded) {
      const next = [...this.nodes()];
      let end = index + 1;
      while (end < next.length && next[end].level > current.level) end++;
      next.splice(index + 1, end - index - 1);
      next[index] = {...current, expanded: false};
      this.nodes.set(next);
      return;
    }

    this.patch(index, {...current, loading: true});
    try {
      const children = await firstValueFrom(this.http.get<readonly GeographyTreeNode[]>(
        `/api/v1/geography/tree/${current.resource}/${current.id}/children`
      ));
      const rows: UiTreeNode[] = children.map(child => ({
        ...child,
        level: current.level + 1,
        expanded: false,
        loading: false
      }));
      const next = [...this.nodes()];
      next[index] = {...current, expanded: true, loading: false};
      next.splice(index + 1, 0, ...rows);
      this.nodes.set(next);
    } catch (error) {
      this.patch(index, {...current, loading: false});
      this.error.set(error instanceof Error ? error.message : 'خطا در بارگذاری درخت');
    }
  }

  icon(resource: string): string {
    return ({
      provinces: 'map',
      counties: 'apartment',
      districts: 'layers',
      cities: 'location_city',
      'rural-districts': 'holiday_village',
      villages: 'cottage'
    } as Record<string, string>)[resource] ?? 'circle';
  }

  private async loadRoots(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const roots = await firstValueFrom(this.http.get<readonly GeographyTreeNode[]>('/api/v1/geography/tree/roots'));
      this.nodes.set(roots.map(node => ({...node, level: 0, expanded: false, loading: false})));
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'خطا در بارگذاری درخت');
    } finally {
      this.loading.set(false);
    }
  }

  private patch(index: number, value: UiTreeNode): void {
    const next = [...this.nodes()];
    next[index] = value;
    this.nodes.set(next);
  }
}

import {Component} from '@angular/core';
import {MatIconModule} from '@angular/material/icon';
import {
  SYSTEM_ARCHITECTURE,
  SYSTEM_CAPABILITIES,
  SYSTEM_RELEASE,
  SYSTEM_TECHNOLOGY_GROUPS
} from './system-specification.data';

@Component({
  selector: 'app-system-specification',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './system-specification.component.html',
  styleUrl: './system-specification.component.scss'
})
export class SystemSpecificationComponent {
  readonly release = SYSTEM_RELEASE;
  readonly architecture = SYSTEM_ARCHITECTURE;
  readonly technologyGroups = SYSTEM_TECHNOLOGY_GROUPS;
  readonly capabilities = SYSTEM_CAPABILITIES;
}

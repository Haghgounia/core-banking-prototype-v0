import {ReferenceTableDescriptor} from '../../../core/models/catalog.model';
import {LookupOption, PageResponse, ReferenceRecordResponse, ReferenceSearchQuery} from '../domain/reference.model';

export abstract class ReferenceGateway {
  abstract descriptor(resource: string): Promise<ReferenceTableDescriptor>;
  abstract search(resource: string, query: ReferenceSearchQuery): Promise<PageResponse<Readonly<Record<string, unknown>>>>;
  abstract findById(resource: string, id: number): Promise<ReferenceRecordResponse>;
  abstract create(resource: string, values: Readonly<Record<string, unknown>>): Promise<ReferenceRecordResponse>;
  abstract update(resource: string, id: number, values: Readonly<Record<string, unknown>>): Promise<ReferenceRecordResponse>;
  abstract delete(resource: string, id: number): Promise<void>;
  abstract lookup(resource: string, parentId?: number | null, text?: string): Promise<readonly LookupOption[]>;
}

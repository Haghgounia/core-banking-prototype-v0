import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {catchError, throwError} from 'rxjs';

export interface ApiProblem {
  readonly title?: string;
  readonly detail?: string;
  readonly errorCode?: string;
  readonly fieldErrors?: Readonly<Record<string, string>>;
}

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      const problem = (error.error ?? {}) as ApiProblem;
      const normalized = new Error(problem.detail ?? problem.title ?? error.message ?? 'خطای ارتباط با سرور');
      Object.assign(normalized, {problem, status: error.status});
      return throwError(() => normalized);
    })
  );

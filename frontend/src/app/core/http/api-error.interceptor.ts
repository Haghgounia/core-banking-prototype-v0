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
      // Preserve HttpErrorResponse. CIF forms inspect ProblemDetail.fieldErrors and status directly;
      // converting the response to a plain Error hid actionable validation messages from users.
      return throwError(() => error);
    })
  );

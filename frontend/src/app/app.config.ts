import {ApplicationConfig, provideZonelessChangeDetection} from '@angular/core';
import {provideHttpClient, withInterceptors} from '@angular/common/http';
import {provideAnimationsAsync} from '@angular/platform-browser/animations/async';
import {provideRouter, withHashLocation} from '@angular/router';
import {routes} from './app.routes';
import {ReferenceGateway} from './features/reference-data/application/reference.gateway';
import {HttpReferenceGateway} from './features/reference-data/infrastructure/http-reference.gateway';
import {apiErrorInterceptor} from './core/http/api-error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withHashLocation()),
    provideHttpClient(withInterceptors([apiErrorInterceptor])),
    provideAnimationsAsync(),
    provideZonelessChangeDetection(),
    {provide: ReferenceGateway, useClass: HttpReferenceGateway}
  ]
};

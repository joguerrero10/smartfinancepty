import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { Preferences } from '@capacitor/preferences';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models';
import { environment } from '../../../environments/environment';

const SESSION_KEY = 'sf_session';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly api = `${environment.apiUrl}/api/v1/auth`;
  private readonly sessionSignal = signal<AuthResponse | null>(null);

  readonly currentUser = computed(() => this.sessionSignal());
  readonly isLoggedIn = computed(() => !!this.sessionSignal()?.accessToken);
  readonly accessToken = computed(() => this.sessionSignal()?.accessToken ?? null);

  constructor(private readonly http: HttpClient, private readonly router: Router) {
    this.restoreSession();
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/login`, request).pipe(
      tap(res => this.saveSession(res))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/register`, request).pipe(
      tap(res => this.saveSession(res))
    );
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.sessionSignal()?.refreshToken;
    return this.http.post<AuthResponse>(`${this.api}/refresh`, { refreshToken }).pipe(
      tap(res => this.saveSession(res))
    );
  }

  logout(): void {
    this.http.post(`${this.api}/logout`, {}).subscribe({ error: () => {} });
    this.clearSession();
    this.router.navigate(['/auth/login']);
  }

  private saveSession(response: AuthResponse): void {
    this.sessionSignal.set(response);
    Preferences.set({ key: SESSION_KEY, value: JSON.stringify(response) });
  }

  private clearSession(): void {
    this.sessionSignal.set(null);
    Preferences.remove({ key: SESSION_KEY });
  }

  private restoreSession(): void {
    Preferences.get({ key: SESSION_KEY }).then(({ value }) => {
      if (value) {
        this.sessionSignal.set(JSON.parse(value) as AuthResponse);
      }
    });
  }
}

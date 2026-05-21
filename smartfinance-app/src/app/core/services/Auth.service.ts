import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Preferences } from '@capacitor/preferences';
import { BehaviorSubject, firstValueFrom, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
} from '../models/AuthResponse.model';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private readonly API = `${environment.apiUrl}/api/v1/auth`;
  private readonly currentUserSubject =
    new BehaviorSubject<AuthResponse | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {}

  async initialize(): Promise<void> {
    await this.loadStoredUser();
  }

  get currentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }

  get isLoggedIn(): boolean {
    return !!this.currentUserSubject.value?.accessToken;
  }

  get accessToken(): string | null {
    return this.currentUserSubject.value?.accessToken ?? null;
  }
  // ── Auth endpoints ────────────────────────────────────────────────────────

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.API}/login`, request)
      .pipe(tap((response) => this.saveSession(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.API}/register`, request)
      .pipe(tap((response) => this.saveSession(response)));
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.currentUser?.refreshToken;
    return this.http
      .post<AuthResponse>(`${this.API}/refresh`, { refreshToken })
      .pipe(tap((response) => this.saveSession(response)));
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post(`${this.API}/logout`, {}));
    } catch {}
    await this.clearSession();
    this.router.navigate(['/auth/login']);
  }

  // ── Session management ────────────────────────────────────────────────────

  private async saveSession(response: AuthResponse): Promise<void> {
    this.currentUserSubject.next(response);
    await Preferences.set({
      key: 'auth_session',
      value: JSON.stringify(response),
    });
  }

  private async loadStoredUser(): Promise<void> {
    const { value } = await Preferences.get({ key: 'auth_session' });
    if (value) {
      const session: AuthResponse = JSON.parse(value);
      this.currentUserSubject.next(session);
    }
  }

  private async clearSession(): Promise<void> {
    this.currentUserSubject.next(null);
    await Preferences.remove({ key: 'auth_session' });
  }
}

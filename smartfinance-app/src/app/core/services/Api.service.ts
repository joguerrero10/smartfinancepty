import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AnalyticsData,
  Budget,
  Expense,
  ExpenseRequest,
  FileAttachment,
  Income,
  Notification,
  OcrResult,
  SavingsGoal,
} from '../models';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private readonly BASE = `${environment.apiUrl}/api/v1`;

  constructor(private readonly http: HttpClient) {}

  // ── Incomes ───────────────────────────────────────────────────────────────

  getIncomes(): Observable<Income[]> {
    return this.http.get<Income[]>(`${this.BASE}/incomes`);
  }

  createIncome(data: any): Observable<Income> {
    return this.http.post<Income>(`${this.BASE}/incomes`, data);
  }

  updateIncome(id: number, data: any): Observable<Income> {
    return this.http.put<Income>(`${this.BASE}/incomes/${id}`, data);
  }

  deleteIncome(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/incomes/${id}`);
  }

  // ── Expenses ──────────────────────────────────────────────────────────────

  getExpenses(): Observable<Expense[]> {
    return this.http.get<Expense[]>(`${this.BASE}/expenses`);
  }

  getExpenseById(id: number): Observable<Expense> {
    return this.http.get<Expense>(`${this.BASE}/expenses/${id}`);
  }

  createExpense(data: ExpenseRequest): Observable<Expense> {
    return this.http.post<Expense>(`${this.BASE}/expenses`, data);
  }

  updateExpense(id: number, data: ExpenseRequest): Observable<Expense> {
    return this.http.put<Expense>(`${this.BASE}/expenses/${id}`, data);
  }

  deleteExpense(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/expenses/${id}`);
  }

  getCategories(): Observable<any[]> {
    return this.http.get<any[]>(`${this.BASE}/expenses/categories`);
  }

  // ── Budgets ───────────────────────────────────────────────────────────────

  getBudgets(): Observable<Budget[]> {
    return this.http.get<Budget[]>(`${this.BASE}/budgets`);
  }

  getBudgetsByMonth(year: number, month: number): Observable<Budget[]> {
    return this.http.get<Budget[]>(`${this.BASE}/budgets/month`, {
      params: new HttpParams().set('year', year).set('month', month),
    });
  }

  createBudget(data: any): Observable<Budget> {
    return this.http.post<Budget>(`${this.BASE}/budgets`, data);
  }

  updateBudget(id: number, data: any): Observable<Budget> {
    return this.http.put<Budget>(`${this.BASE}/budgets/${id}`, data);
  }

  deleteBudget(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/budgets/${id}`);
  }

  // ── Savings Goals ─────────────────────────────────────────────────────────

  getSavingsGoals(): Observable<SavingsGoal[]> {
    return this.http.get<SavingsGoal[]>(`${this.BASE}/savings-goals`);
  }

  createSavingsGoal(data: any): Observable<SavingsGoal> {
    return this.http.post<SavingsGoal>(`${this.BASE}/savings-goals`, data);
  }

  updateSavingsGoal(id: number, data: any): Observable<SavingsGoal> {
    return this.http.put<SavingsGoal>(`${this.BASE}/savings-goals/${id}`, data);
  }

  deleteSavingsGoal(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/savings-goals/${id}`);
  }

  // ── Notifications ─────────────────────────────────────────────────────────

  getNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.BASE}/notifications`);
  }

  getUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(
      `${this.BASE}/notifications/unread/count`,
    );
  }

  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.BASE}/notifications/${id}/read`, {});
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.BASE}/notifications/read-all`, {});
  }

  // ── Analytics ─────────────────────────────────────────────────────────────

  getAnalytics(year?: number, month?: number): Observable<AnalyticsData> {
    if (year && month) {
      return this.http.get<AnalyticsData>(`${this.BASE}/analytics/month`, {
        params: new HttpParams().set('year', year).set('month', month),
      });
    }
    return this.http.get<AnalyticsData>(`${this.BASE}/analytics`);
  }

  // ── Files ─────────────────────────────────────────────────────────────────

  uploadFile(
    file: File,
    expenseId?: number,
    description?: string,
  ): Observable<FileAttachment> {
    const formData = new FormData();
    formData.append('file', file);
    if (expenseId) formData.append('expenseId', expenseId.toString());
    if (description) formData.append('description', description);
    return this.http.post<FileAttachment>(
      `${this.BASE}/files/upload`,
      formData,
    );
  }

  getFiles(): Observable<FileAttachment[]> {
    return this.http.get<FileAttachment[]>(`${this.BASE}/files`);
  }

  deleteFile(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/files/${id}`);
  }

  // ── OCR ───────────────────────────────────────────────────────────────────

  scanReceipt(
    file: File,
    autoCreate = false,
    categoryId?: number,
  ): Observable<OcrResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('autoCreate', autoCreate.toString());
    if (categoryId) formData.append('categoryId', categoryId.toString());
    return this.http.post<OcrResult>(`${this.BASE}/ocr/scan`, formData);
  }

  confirmOcrExpense(
    ocrResult: OcrResult,
    categoryId?: number,
    fileId?: number,
  ): Observable<Expense> {
    return this.http.post<Expense>(`${this.BASE}/ocr/confirm`, {
      ocrResult,
      categoryId,
      fileId,
    });
  }
}

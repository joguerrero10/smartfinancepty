import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AnalyticsData,
  Budget,
  BudgetRequest,
  Expense,
  ExpenseCategory,
  ExpenseRequest,
  FileAttachment,
  Income,
  IncomeRequest,
  Notification,
  OcrResult,
  SavingsGoal,
  SavingsGoalRequest,
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly base = `${environment.apiUrl}/api/v1`;

  constructor(private readonly http: HttpClient) {}

  // ── Incomes ───────────────────────────────────────────────────────────────

  getIncomes(): Observable<Income[]> {
    return this.http.get<Income[]>(`${this.base}/incomes`);
  }

  getIncomeById(id: number): Observable<Income> {
    return this.http.get<Income>(`${this.base}/incomes/${id}`);
  }

  createIncome(data: IncomeRequest): Observable<Income> {
    return this.http.post<Income>(`${this.base}/incomes`, data);
  }

  updateIncome(id: number, data: IncomeRequest): Observable<Income> {
    return this.http.put<Income>(`${this.base}/incomes/${id}`, data);
  }

  deleteIncome(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/incomes/${id}`);
  }

  // ── Expenses ──────────────────────────────────────────────────────────────

  getExpenses(): Observable<Expense[]> {
    return this.http.get<Expense[]>(`${this.base}/expenses`);
  }

  getExpenseById(id: number): Observable<Expense> {
    return this.http.get<Expense>(`${this.base}/expenses/${id}`);
  }

  createExpense(data: ExpenseRequest): Observable<Expense> {
    return this.http.post<Expense>(`${this.base}/expenses`, data);
  }

  updateExpense(id: number, data: ExpenseRequest): Observable<Expense> {
    return this.http.put<Expense>(`${this.base}/expenses/${id}`, data);
  }

  deleteExpense(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/expenses/${id}`);
  }

  getCategories(): Observable<ExpenseCategory[]> {
    return this.http.get<ExpenseCategory[]>(`${this.base}/expenses/categories`);
  }

  // ── Budgets ───────────────────────────────────────────────────────────────

  getBudgets(): Observable<Budget[]> {
    return this.http.get<Budget[]>(`${this.base}/budgets`);
  }

  getBudgetsByMonth(year: number, month: number): Observable<Budget[]> {
    const params = new HttpParams().set('year', year).set('month', month);
    return this.http.get<Budget[]>(`${this.base}/budgets/month`, { params });
  }

  createBudget(data: BudgetRequest): Observable<Budget> {
    return this.http.post<Budget>(`${this.base}/budgets`, data);
  }

  updateBudget(id: number, data: BudgetRequest): Observable<Budget> {
    return this.http.put<Budget>(`${this.base}/budgets/${id}`, data);
  }

  deleteBudget(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/budgets/${id}`);
  }

  // ── Savings Goals ─────────────────────────────────────────────────────────

  getSavingsGoals(): Observable<SavingsGoal[]> {
    return this.http.get<SavingsGoal[]>(`${this.base}/savings-goals`);
  }

  createSavingsGoal(data: SavingsGoalRequest): Observable<SavingsGoal> {
    return this.http.post<SavingsGoal>(`${this.base}/savings-goals`, data);
  }

  updateSavingsGoal(
    id: number,
    data: SavingsGoalRequest,
  ): Observable<SavingsGoal> {
    return this.http.put<SavingsGoal>(`${this.base}/savings-goals/${id}`, data);
  }

  deleteSavingsGoal(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/savings-goals/${id}`);
  }

  // ── Notifications ─────────────────────────────────────────────────────────

  getNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.base}/notifications`);
  }

  getUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(
      `${this.base}/notifications/unread/count`,
    );
  }

  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/notifications/${id}/read`, {});
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.base}/notifications/read-all`, {});
  }

  deleteNotification(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/notifications/${id}`);
  }

  // ── Analytics ─────────────────────────────────────────────────────────────

  getAnalytics(): Observable<AnalyticsData> {
    return this.http.get<AnalyticsData>(`${this.base}/analytics`);
  }

  getAnalyticsByMonth(year: number, month: number): Observable<AnalyticsData> {
    const params = new HttpParams().set('year', year).set('month', month);
    return this.http.get<AnalyticsData>(`${this.base}/analytics/month`, {
      params,
    });
  }

  getRecommendations(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/analytics/recommendations`);
  }

  // ── Files ─────────────────────────────────────────────────────────────────

  uploadFile(
    file: File,
    expenseId?: number,
    description?: string,
  ): Observable<FileAttachment> {
    const formData = new FormData();
    formData.append('file', file);
    if (expenseId) formData.append('expenseId', String(expenseId));
    if (description) formData.append('description', description);
    return this.http.post<FileAttachment>(
      `${this.base}/files/upload`,
      formData,
    );
  }

  getFiles(): Observable<FileAttachment[]> {
    return this.http.get<FileAttachment[]>(`${this.base}/files`);
  }

  getFilesByExpense(expenseId: number): Observable<FileAttachment[]> {
    return this.http.get<FileAttachment[]>(
      `${this.base}/files/expense/${expenseId}`,
    );
  }

  deleteFile(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/files/${id}`);
  }

  // ── OCR ───────────────────────────────────────────────────────────────────

  scanReceipt(
    file: File,
    autoCreate = false,
    categoryId?: number,
  ): Observable<OcrResult> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append('autoCreate', String(autoCreate));
    if (categoryId) formData.append('categoryId', String(categoryId));
    return this.http.post<OcrResult>(`${this.base}/ocr/scan`, formData);
  }

  confirmOcr(
    ocrResult: OcrResult,
    categoryId?: number,
    fileId?: number,
  ): Observable<Expense> {
    return this.http.post<Expense>(`${this.base}/ocr/confirm`, {
      ocrResult,
      categoryId,
      fileId,
    });
  }
}

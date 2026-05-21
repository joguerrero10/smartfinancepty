import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardData } from '../models';

interface GraphqlResponse<T> {
  data: T;
  errors?: { message: string }[];
}

@Injectable({ providedIn: 'root' })
export class GraphqlService {
  private readonly gql = `${environment.apiUrl}/graphql`;

  constructor(private readonly http: HttpClient) {}

  private query<T>(
    query: string,
    variables?: Record<string, unknown>,
  ): Observable<T> {
    return this.http
      .post<GraphqlResponse<T>>(this.gql, { query, variables })
      .pipe(
        map((res) => {
          if (res.errors?.length) {
            throw new Error(res.errors[0].message);
          }
          return res.data;
        }),
      );
  }

  getDashboard(): Observable<DashboardData> {
    return this.query<{ dashboard: DashboardData }>(
      `
      query {
        dashboard {
          totalGrossIncome totalNetIncome totalDeductions
          totalExpensesMonth totalFixedExpenses totalVariableExpenses
          balance savingsProjected savingsPercentage
          month year incomeCount expenseCount
          expensesByCategory {
            categoryId categoryName totalAmount percentage expenseCount color icon
          }
          recentExpenses {
            id description amount categoryName expenseType expenseDate
          }
          incomes {
            id name grossAmount netAmount totalDeductions incomeType frequency
          }
        }
      }
    `,
    ).pipe(map((data) => data.dashboard));
  }

  getDashboardByMonth(year: number, month: number): Observable<DashboardData> {
    return this.query<{ dashboardByMonth: DashboardData }>(
      `
      query DashboardByMonth($year: Int!, $month: Int!) {
        dashboardByMonth(year: $year, month: $month) {
          totalGrossIncome totalNetIncome totalDeductions
          totalExpensesMonth totalFixedExpenses totalVariableExpenses
          balance savingsProjected savingsPercentage month year
          expensesByCategory {
            categoryId categoryName totalAmount percentage expenseCount color icon
          }
          recentExpenses {
            id description amount categoryName expenseType expenseDate
          }
        }
      }
    `,
      { year, month },
    ).pipe(map((data) => data.dashboardByMonth));
  }

  getAnalytics(year: number, month: number): Observable<unknown> {
    return this.query<{ analytics: unknown }>(
      `
      query Analytics($year: Int!, $month: Int!) {
        analytics(year: $year, month: $month) {
          totalExpenses totalIncome savingsRate riskLevel riskMessage
          topCategories { categoryName totalAmount percentage trend trendPercentage }
          expenseTrend { label amount month year }
          prediction {
            predictedExpenses predictedSavings confidenceLevel basedOnMonths
          }
        }
      }
    `,
      { year, month },
    ).pipe(map((data) => data.analytics));
  }

  getRecommendations(): Observable<unknown[]> {
    return this.query<{ recommendations: unknown[] }>(
      `
      query {
        recommendations {
          id type title message priority potentialSavings categoryName actionable
        }
      }
    `,
    ).pipe(map((data) => data.recommendations));
  }
}

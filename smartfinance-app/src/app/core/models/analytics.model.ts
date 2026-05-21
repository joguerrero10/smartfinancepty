export type SeverityLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type trend = 'UP' | 'DOWN' | 'STABLE';

export interface AnalyticsData {
  analytics: AnalyticsResponse;
  recommendations: Recommendation[];
  comparison: ComparisonResponse;
}

export interface AnalyticsResponse {
  year: number;
  month: number;
  totalExpenses: number;
  totalIncome: number;
  savingsRate: number;
  topCategories: CategoryAnalytic[];
  expenseTrend: TrendPoint[];
  prediction: PredictionResult;
  riskLevel: SeverityLevel;
  riskMessage: string;
}

export interface CategoryAnalytic {
  categoryName: string;
  totalAmount: number;
  percentage: number;
  trend: trend;
  trendPercentage: number;
}

export interface TrendPoint {
  label: string;
  amount: number;
  month: number;
  year: number;
}

export interface PredictionResult {
  predictedExpenses: number;
  predictedSavings: number;
  confidenceLevel: SeverityLevel;
  basedOnMonths: number;
}

export interface Recommendation {
  id: string;
  type: string;
  title: string;
  message: string;
  priority: SeverityLevel;
  potentialSavings?: number;
  categoryName?: string;
  actionable: boolean;
}

export interface ComparisonResponse {
  currentMonth: MonthSummary;
  previousMonth: MonthSummary;
  expenseDifference: number;
  expenseChangePercentage: number;
  increased: boolean;
  categoriesIncreased: CategoryChange[];
  categoriesDecreased: CategoryChange[];
}

export interface MonthSummary {
  year: number;
  month: number;
  totalExpenses: number;
  totalIncome: number;
  balance: number;
}

export interface CategoryChange {
  categoryName: string;
  previousAmount: number;
  currentAmount: number;
  changePercentage: number;
  changeAmount: number;
}

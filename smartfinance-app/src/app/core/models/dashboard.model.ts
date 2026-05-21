export interface DashboardData {
  totalGrossIncome: number;
  totalNetIncome: number;
  totalDeductions: number;
  totalExpensesMonth: number;
  totalFixedExpenses: number;
  totalVariableExpenses: number;
  balance: number;
  savingsProjected: number;
  savingsPercentage: number;
  month: number;
  year: number;
  incomeCount: number;
  expenseCount: number;
  expensesByCategory: CategorySummary[];
  recentExpenses: RecentExpense[];
  incomes: IncomeSummary[];
}

export interface CategorySummary {
  categoryId: number;
  categoryName: string;
  totalAmount: number;
  percentage: number;
  expenseCount: number;
  color: string;
  icon: string;
}

export interface RecentExpense {
  id: number;
  description: string;
  amount: number;
  categoryName: string;
  expenseType: string;
  expenseDate: string;
}

export interface IncomeSummary {
  id: number;
  name: string;
  grossAmount: number;
  netAmount: number;
  totalDeductions: number;
  incomeType: string;
  frequency: string;
}

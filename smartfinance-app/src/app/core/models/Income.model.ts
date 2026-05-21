export interface Income {
  id: number;
  name: string;
  amount: number;
  netAmount: number;
  totalDeductions: number;
  incomeType: 'SALARY' | 'FREELANCE' | 'BUSINESS' | 'INVESTMENT' | 'OTHER';
  frequency: 'MONTHLY' | 'BIWEEKLY' | 'WEEKLY' | 'ANNUAL';
  deductions: Deduction[];
}

export interface Deduction {
  id?: number;
  name: string;
  deductionType: string;
  isPercentage: boolean;
  value: number;
}

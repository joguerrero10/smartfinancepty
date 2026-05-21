export interface SavingsGoal {
  id: number;
  name: string;
  fixedAmount?: number;
  percentage?: number;
  targetAmount: number;
  actualSavings: number;
  isAchieved: boolean;
  achievementPercentage: number;
  statusMessage: string;
}

import { Routes } from '@angular/router';
import { authGuard } from './core/guard/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'tabs/dashboard',
    pathMatch: 'full',
  },
  {
    path: 'auth',
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./pages/auth/login/login.page').then((m) => m.LoginPage),
      },
      // {
      //   path: 'register',
      //   loadComponent: () =>
      //     import('./pages/auth/register/register.page').then(
      //       (m) => m.RegisterPage,
      //     ),
      // },
      {
        path: '',
        redirectTo: 'login',
        pathMatch: 'full',
      },
    ],
  },
  {
    path: 'tabs',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/tabs/tabs.page').then((m) => m.TabsPage),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.page/dashboard.page.page').then(
            (m) => m.DashboardPagePage,
          ),
      },
      // {
      //   path: 'expenses',
      //   children: [
      //     {
      //       path: '',
      //       loadComponent: () =>
      //         import('./pages/expenses/list/expense-list.page').then(
      //           (m) => m.ExpenseListPage,
      //         ),
      //     },
      //     {
      //       path: 'new',
      //       loadComponent: () =>
      //         import('./pages/expenses/form/expense-form.page').then(
      //           (m) => m.ExpenseFormPage,
      //         ),
      //     },
      //     {
      //       path: ':id/edit',
      //       loadComponent: () =>
      //         import('./pages/expenses/form/expense-form.page').then(
      //           (m) => m.ExpenseFormPage,
      //         ),
      //     },
      //   ],
      // },
      // {
      //   path: 'analytics',
      //   loadComponent: () =>
      //     import('./pages/analytics/analytics.page').then(
      //       (m) => m.AnalyticsPage,
      //     ),
      // },
      // {
      //   path: 'budgets',
      //   loadComponent: () =>
      //     import('./pages/budgets/budgets.page').then((m) => m.BudgetsPage),
      // },
      {
        path: 'profile',
        loadComponent: () =>
          import('./pages/profile/profile.page').then((m) => m.ProfilePage),
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'tabs/dashboard',
  },
];

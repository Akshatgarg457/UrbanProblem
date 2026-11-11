import { Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import type { UserRole } from '../types'
import { useAuth } from '../context/AuthContext'
import { CitizenLayout } from '../components/layout/CitizenLayout'
import { DeptAdminLayout } from '../components/layout/DeptAdminLayout'
import { PublicLayout } from '../components/layout/PublicLayout'
import { SysAdminLayout } from '../components/layout/SysAdminLayout'
import { WorkerLayout } from '../components/layout/WorkerLayout'

// Public pages
import { LandingPage } from '../pages/LandingPage'
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { RegisterPage, WorkerRegisterPage } from '../pages/RegisterPage'
import { SignInPage } from '../pages/SignInPage'
import { NotificationsPage } from '../pages/shared/NotificationsPage'
import { HelpPage, SettingsPage } from '../pages/shared/WorkspaceUtilityPages'

// Citizen
import { CitizenDashboard } from '../pages/citizen/CitizenDashboard'
import { ComplaintDetail } from '../pages/citizen/ComplaintDetail'
import { MyComplaints } from '../pages/citizen/MyComplaints'
import { ReportComplaint } from '../pages/citizen/ReportComplaint'

// Shared
import { ProfilePage } from '../pages/ProfilePage'

// Department admin
import { DeptAnalytics } from '../pages/dept-admin/DeptAnalytics'
import { DeptComplaintMap } from '../pages/dept-admin/DeptComplaintMap'
import { DeptComplaints } from '../pages/dept-admin/DeptComplaints'
import { DeptDashboard } from '../pages/dept-admin/DeptDashboard'
import { DeptRecurrence } from '../pages/dept-admin/DeptRecurrence'
import { DeptReview } from '../pages/dept-admin/DeptReview'
import { DeptWorkers } from '../pages/dept-admin/DeptWorkers'

// Worker
import { WorkerDashboard } from '../pages/worker/WorkerDashboard'
import { WorkerTasks } from '../pages/worker/WorkerTasks'

// System admin
import { SysAdminAnalytics } from '../pages/system-admin/SysAdminAnalytics'
import { SysAdminComplaints } from '../pages/system-admin/SysAdminComplaints'
import { SysAdminDashboard } from '../pages/system-admin/SysAdminDashboard'
import { SysAdminDepartments } from '../pages/system-admin/SysAdminDepartments'
import { SysAdminRecurrence } from '../pages/system-admin/SysAdminRecurrence'
import { SysAdminUsers } from '../pages/system-admin/SysAdminUsers'

function ProtectedWorkspace({ role, children }: { role: UserRole; children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="auth-loading">Loading your workspace…</div>
  if (!user) return <Navigate to="/login" replace />
  if (user.role !== role) {
    const home = user.role === 'SYSTEM_ADMIN' ? '/dashboard/system-admin' : user.role === 'DEPARTMENT_ADMIN' ? '/dashboard/department-admin' : user.role === 'WORKER' ? '/dashboard/worker' : '/dashboard/citizen'
    return <Navigate to={home} replace />
  }
  return <>{children}</>
}

export function AppRoutes() {
  return (
    <Routes>
      {/* Public */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<LandingPage />} />
        <Route path="/signin" element={<SignInPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/register/worker" element={<WorkerRegisterPage />} />

      {/* Citizen workspace */}
      <Route path="/dashboard/citizen" element={<ProtectedWorkspace role="CITIZEN"><CitizenLayout /></ProtectedWorkspace>}>
        <Route index element={<CitizenDashboard />} />
        <Route path="report" element={<ReportComplaint />} />
        <Route path="complaints" element={<MyComplaints />} />
        <Route path="complaints/:id" element={<ComplaintDetail />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="help" element={<HelpPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      {/* Department admin workspace */}
      <Route path="/dashboard/department-admin" element={<ProtectedWorkspace role="DEPARTMENT_ADMIN"><DeptAdminLayout /></ProtectedWorkspace>}>
        <Route index element={<DeptDashboard />} />
        <Route path="complaints" element={<DeptComplaints />} />
        <Route path="complaints/:id" element={<ComplaintDetail />} />
        <Route path="map" element={<DeptComplaintMap />} />
        <Route path="review" element={<DeptReview />} />
        <Route path="workers" element={<DeptWorkers />} />
        <Route path="recurrence" element={<DeptRecurrence />} />
        <Route path="analytics" element={<DeptAnalytics />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="help" element={<HelpPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      {/* Worker workspace */}
      <Route path="/dashboard/worker" element={<ProtectedWorkspace role="WORKER"><WorkerLayout /></ProtectedWorkspace>}>
        <Route index element={<WorkerDashboard />} />
        <Route path="tasks" element={<WorkerTasks />} />
        <Route path="tasks/:id" element={<ComplaintDetail />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="help" element={<HelpPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      {/* System admin workspace */}
      <Route path="/dashboard/system-admin" element={<ProtectedWorkspace role="SYSTEM_ADMIN"><SysAdminLayout /></ProtectedWorkspace>}>
        <Route index element={<SysAdminDashboard />} />
        <Route path="users" element={<SysAdminUsers />} />
        <Route path="departments" element={<SysAdminDepartments />} />
        <Route path="complaints" element={<SysAdminComplaints />} />
        <Route path="complaints/:id" element={<ComplaintDetail />} />
        <Route path="review" element={<DeptReview />} />
        <Route path="recurrence" element={<SysAdminRecurrence />} />
        <Route path="analytics" element={<SysAdminAnalytics />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="help" element={<HelpPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      {/* Catch-all redirect */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

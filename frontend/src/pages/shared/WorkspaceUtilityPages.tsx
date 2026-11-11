import { MotionPage } from '../../components/MotionPage'
import { PageHeader, SectionCard } from '../../components/ui'
import { ThemeToggle } from '../../components/ThemeToggle'

export function HelpPage() {
  return <MotionPage><div className="dashboard-content"><PageHeader title="Help & Support" description="Guidance for using CommonGround." />
    <div className="utility-grid"><SectionCard title="Reporting an issue"><p>Choose the closest category, describe the problem clearly, add its location and attach photos when useful. You can follow updates from My Complaints.</p></SectionCard><SectionCard title="Following a complaint"><p>Open a complaint to review its current status, submitted details and available status history. Status changes are made by the responsible staff.</p></SectionCard><SectionCard title="Need assistance?"><p>If you need help with an existing report, keep its complaint reference available when contacting your local support team.</p></SectionCard><SectionCard title="Account & security"><p>Keep your password safe and update it from your profile. If you forget it, answer your security question to reset access. Worker accounts require administrator approval before signing in.</p></SectionCard></div>
  </div></MotionPage>
}
export function SettingsPage() {
  return <MotionPage><div className="dashboard-content"><PageHeader title="Settings" description="Manage your local application preferences." />
    <SectionCard title="Appearance"><div className="settings-row"><div><strong>Color theme</strong><p>Switch between light and dark appearance. Your choice is saved in this browser.</p></div><ThemeToggle /></div></SectionCard>
    <SectionCard title="Account security"><div className="settings-row"><div><strong>Sign-in session</strong><p>Your account uses a secure backend-issued session token. Sign out when using a shared device.</p></div><span className="settings-status">Session protected</span></div></SectionCard>
  </div></MotionPage>
}

export default function KpiCard({ label, value, sub, tone = 'default', icon }) {
  return (
    <div className={`kpi-card kpi-tone-${tone}`}>
      <div className="kpi-top">
        <span className="kpi-label">{label}</span>
        {icon && <span className={`kpi-icon kpi-icon-${tone}`}>{icon}</span>}
      </div>
      <div className={`kpi-value ${tone === 'danger' ? 'danger' : tone === 'primary' ? 'primary' : ''}`}>
        {value}
      </div>
      {sub && <div className="kpi-sub">{sub}</div>}
    </div>
  );
}

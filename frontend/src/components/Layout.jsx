import { useCallback, useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { healthApi, inventoryApi, productionApi } from '../api/endpoints';
import {
  IconBox,
  IconClipboard,
  IconDashboard,
  IconLayers,
  IconTouch,
  IconUsers,
  IconWarehouse,
} from './ui/Icons';

const UI_MODE_KEY = 'factoryflow_ui_mode';

const NAV_ITEMS = [
  {
    to: '/',
    label: '대시보드',
    icon: IconDashboard,
    title: '생산·재고 대시보드',
    subtitle: '금일 생산 현황, 진행 중 작업지시, 안전재고 경고를 실시간으로 모니터링합니다.',
  },
  {
    to: '/items',
    label: '품목 관리',
    icon: IconBox,
    title: '기준정보 · 품목 관리',
    subtitle: '원재료(원단/부자재)와 완제품(박스) 마스터 정보를 관리합니다.',
  },
  {
    to: '/partners',
    label: '거래처 관리',
    icon: IconUsers,
    title: '기준정보 · 거래처 관리',
    subtitle: '원재료 매입처와 완제품 납품처 정보를 관리합니다.',
  },
  {
    to: '/boms',
    label: 'BOM 관리',
    icon: IconLayers,
    title: '기준정보 · BOM(자재명세서)',
    subtitle: '완제품 1단위 생산에 필요한 원재료 소요량을 설정합니다.',
  },
  {
    to: '/inventories',
    label: '재고 · 입출고',
    icon: IconWarehouse,
    showLowStockBadge: true,
    title: '재고 및 수불 이력 관리',
    subtitle: '현재고, 안전재고 미달 여부, 입출고/조정 및 전체 이력을 관리합니다.',
  },
  {
    to: '/production',
    label: '작업지시 · 실적',
    icon: IconClipboard,
    showPausedBadge: true,
    title: '생산 · 작업지시 및 실적 관리',
    subtitle: '작업지시 발행, 상태 전환(일시정지/재개), 생산실적 등록(BOM 재고 자동 연동)을 처리합니다.',
  },
];

export default function Layout() {
  const location = useLocation();
  const [serverStatus, setServerStatus] = useState('checking');
  const [lowStockCount, setLowStockCount] = useState(0);
  const [pausedCount, setPausedCount] = useState(0);
  const [fieldMode, setFieldMode] = useState(() => {
    try {
      return window.localStorage.getItem(UI_MODE_KEY) === 'field';
    } catch {
      return false;
    }
  });

  useEffect(() => {
    document.documentElement.dataset.uiMode = fieldMode ? 'field' : 'standard';
    try {
      window.localStorage.setItem(UI_MODE_KEY, fieldMode ? 'field' : 'standard');
    } catch {
      // ignore storage errors
    }
  }, [fieldMode]);

  const refreshSystemSignals = useCallback(async () => {
    try {
      await healthApi.check();
      setServerStatus('up');
      const [lowRes, pausedRes] = await Promise.all([
        inventoryApi.list({ belowSafetyOnly: true }),
        productionApi.listWorkOrders({ status: 'PAUSED' }),
      ]);
      setLowStockCount((lowRes.data || []).length);
      setPausedCount((pausedRes.data || []).length);
    } catch {
      setServerStatus('down');
    }
  }, []);

  useEffect(() => {
    refreshSystemSignals();
  }, [location.pathname, refreshSystemSignals]);

  useEffect(() => {
    const onDataChanged = () => refreshSystemSignals();
    window.addEventListener('factoryflow:data-changed', onDataChanged);
    const timer = window.setInterval(refreshSystemSignals, 30000);
    return () => {
      window.removeEventListener('factoryflow:data-changed', onDataChanged);
      window.clearInterval(timer);
    };
  }, [refreshSystemSignals]);

  const currentNav =
    NAV_ITEMS.find((item) => item.to === location.pathname) || NAV_ITEMS[0];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <h1 className="brand-title">FactoryFlow</h1>
          <div className="brand-sub">골판지 상자 제조 Mini MES</div>
        </div>

        <nav className="nav-list">
          {NAV_ITEMS.map((item) => {
            const NavIcon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
              >
                <span className="nav-link-main">
                  <NavIcon size={17} />
                  <span>{item.label}</span>
                </span>
                {item.showLowStockBadge && lowStockCount > 0 && (
                  <span
                    className="nav-alert-badge"
                    title={`안전재고 미만 ${lowStockCount}건`}
                  >
                    {lowStockCount}
                  </span>
                )}
                {item.showPausedBadge && pausedCount > 0 && (
                  <span
                    className="nav-alert-badge warning"
                    title={`일시정지 작업지시 ${pausedCount}건`}
                  >
                    {pausedCount}
                  </span>
                )}
              </NavLink>
            );
          })}
        </nav>

        <div className="sidebar-footer">
          <span
            className={`health-dot ${serverStatus === 'up' ? 'up' : 'down'}`}
          />
          {serverStatus === 'up'
            ? 'API 서버 연결 정상'
            : serverStatus === 'checking'
            ? 'API 연결 확인 중...'
            : 'API 서버 연결 안 됨'}
        </div>
      </aside>

      <div className="main-area">
        <header className="topbar">
          <div>
            <h2 className="topbar-title">{currentNav.title}</h2>
            <p className="topbar-subtitle">{currentNav.subtitle}</p>
          </div>

          <div className="topbar-actions">
            <button
              type="button"
              className={`btn ${fieldMode ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setFieldMode((prev) => !prev)}
              title="태블릿·터치스크린 및 공장 현장 가독성을 위해 버튼 크기와 글꼴 대비를 키웁니다"
            >
              <IconTouch size={16} />
              <span>{fieldMode ? '현장 터치 모드 ON' : '현장 터치 모드'}</span>
            </button>
          </div>
        </header>

        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { healthApi } from '../api/endpoints';

const NAV_ITEMS = [
  { to: '/', label: '대시보드', title: '생산·재고 대시보드', subtitle: '금일 생산 현황, 진행 중 작업지시, 안전재고 경고를 한눈에 확인합니다.' },
  { to: '/items', label: '품목 관리', title: '기준정보 · 품목 관리', subtitle: '원재료(원단/부자재)와 완제품(박스) 마스터 정보를 관리합니다.' },
  { to: '/partners', label: '거래처 관리', title: '기준정보 · 거래처 관리', subtitle: '원재료 매입처와 완제품 납품처 정보를 관리합니다.' },
  { to: '/boms', label: 'BOM 관리', title: '기준정보 · BOM(자재명세서)', subtitle: '완제품 1단위 생산에 필요한 원재료 소요량을 설정합니다.' },
  { to: '/inventories', label: '재고 · 입출고', title: '재고 및 수불 이력 관리', subtitle: '현재고, 안전재고 미달 여부, 입출고/조정 및 전체 이력을 관리합니다.' },
  { to: '/production', label: '작업지시 · 실적', title: '생산 · 작업지시 및 실적 관리', subtitle: '작업지시 발행, 상태 전환, 생산실적 등록(BOM 원재료 자동 차감·완제품 증가)을 처리합니다.' },
];

export default function Layout() {
  const location = useLocation();
  const [serverStatus, setServerStatus] = useState('checking');

  useEffect(() => {
    let active = true;
    healthApi
      .check()
      .then(() => {
        if (active) setServerStatus('up');
      })
      .catch(() => {
        if (active) setServerStatus('down');
      });
    return () => {
      active = false;
    };
  }, [location.pathname]);

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
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
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
        </header>

        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

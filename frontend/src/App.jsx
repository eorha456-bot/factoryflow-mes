import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ToastProvider } from './context/ToastContext';
import Layout from './components/Layout';
import DashboardPage from './pages/DashboardPage';
import ItemsPage from './pages/ItemsPage';
import PartnersPage from './pages/PartnersPage';
import BomPage from './pages/BomPage';
import InventoryPage from './pages/InventoryPage';
import ProductionPage from './pages/ProductionPage';

export default function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/items" element={<ItemsPage />} />
            <Route path="/partners" element={<PartnersPage />} />
            <Route path="/boms" element={<BomPage />} />
            <Route path="/inventories" element={<InventoryPage />} />
            <Route path="/production" element={<ProductionPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ToastProvider>
  );
}

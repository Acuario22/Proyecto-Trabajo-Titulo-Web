import { Routes } from '@angular/router';
import { PanelAdministrativoComponent } from './pages/panel-administrativo/panel-administrativo';
import { TrazabilidadReporteComponent } from './pages/trazabilidad-reporte/trazabilidad-reporte';
import { GestionarReporteComponent } from './pages/gestionar-reporte/gestionar-reporte';

export const routes: Routes = [
    // Ruta base que redirige al panel administrativo
    { path: '', redirectTo: 'panel-administrativo', pathMatch: 'full' },

    // Rutas
    { path: 'panel-administrativo', component: PanelAdministrativoComponent },
    { path: 'trazabilidad-reporte', component: TrazabilidadReporteComponent },
    { path: 'gestionar-reporte', component: GestionarReporteComponent },
    { path: 'gestionar-reporte/:id', component: GestionarReporteComponent },
];



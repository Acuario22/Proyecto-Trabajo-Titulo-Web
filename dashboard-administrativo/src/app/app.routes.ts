import { Routes } from '@angular/router';
import { PanelAdministrativoComponent } from './pages/panel-administrativo/panel-administrativo';
import { TrazabilidadReporteComponent } from './pages/trazabilidad-reporte/trazabilidad-reporte';
import { GestionarReporteComponent } from './pages/gestionar-reporte/gestionar-reporte';
import { RevisarReporteComponent } from './pages/revisar-reporte/revisar-reporte';

export const routes: Routes = [
    // Ruta base que redirige al panel administrativo
    { path: '', redirectTo: 'panel-administrativo', pathMatch: 'full' },

    // Rutas
    { path: 'panel-administrativo', component: PanelAdministrativoComponent },
    { path: 'trazabilidad-reporte', component: TrazabilidadReporteComponent },
    { path: 'trazabilidad-reporte/:id', component: TrazabilidadReporteComponent },
    { path: 'seguimiento', redirectTo: 'trazabilidad-reporte', pathMatch: 'full' },
    { path: 'seguimiento/:id', component: TrazabilidadReporteComponent },
    { path: 'gestionar-reporte', component: GestionarReporteComponent },
    { path: 'gestionar-reporte/:id', component: GestionarReporteComponent },
    { path: 'gestionar/:id', component: GestionarReporteComponent },
    { path: 'revisar/:id', component: RevisarReporteComponent },
    { path: 'revisar-reporte/:id', component: RevisarReporteComponent },
];



import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import {
  ReporteService,
  Reporte,
  CategoriaConteo,
  DashboardMetricas
} from '../../core/services/reporte.service';

import { HeaderComponent } from '../../shared/components/header/header';

// Constantes y tipos para Urgencia y Sentimiento (mantenidos para retrocompatibilidad con vistas hijas)
export const URGENCIAS = ['Baja', 'Media', 'Alta'] as const;
export type Urgencia = typeof URGENCIAS[number];

export const SENTIMIENTOS = ['Enojado', 'Neutral', 'Frustrado'] as const;
export type Sentimiento = typeof SENTIMIENTOS[number];

// Re-exportamos el modelo unificado de Reporte
export type { Reporte };

@Component({
  selector: 'app-panel-administrativo',
  standalone: true,
  imports: [CommonModule, HeaderComponent],
  templateUrl: './panel-administrativo.html',
  styleUrl: './panel-administrativo.css'
})
export class PanelAdministrativoComponent implements OnInit {
  private router = inject(Router);
  private reporteService = inject(ReporteService);
  private cdr = inject(ChangeDetectorRef);

  // Listas oficiales de Urgencias y Sentimientos
  readonly urgencias = URGENCIAS;
  readonly sentimientos = SENTIMIENTOS;

  // Pestaña seleccionada actualmente ('Todos' por defecto)
  categoriaSeleccionada: string = 'Todos';

  // Categorías cargadas dinámicamente desde el backend
  categorias: CategoriaConteo[] = [];
  cargandoCategorias: boolean = true;

  // Total acumulado para la pestaña "Todos"
  totalReportesGeneral: number = 0;

  // Métricas para las tarjetas KPIs
  metricas: DashboardMetricas = {
    totalReportes: 0,
    urgenciaAlta: 0,
    finalizados: 0
  };
  cargandoMetricas: boolean = true;

  // Lista de reportes obtenidos de la base de datos
  reportes: Reporte[] = [];
  cargandoReportes: boolean = true;
  errorReportes: string | null = null;

  ngOnInit(): void {
    this.cargarCategorias();
    this.cargarMetricas();
    this.cargarReportes(this.categoriaSeleccionada);
  }

  /**
   * Carga la lista de categorías con sus respectivos conteos desde el backend.
   */
  cargarCategorias(): void {
    this.cargandoCategorias = true;
    this.reporteService.obtenerCategorias().subscribe({
      next: (data) => {
        this.categorias = data ?? [];
        this.cargandoCategorias = false;
        // Si aún no tenemos métricas, calculamos el total sumando las categorías
        if (this.totalReportesGeneral === 0 && this.categorias.length > 0) {
          this.totalReportesGeneral = this.categorias.reduce((sum, c) => sum + (c.conteo || 0), 0);
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error al cargar categorías desde el backend:', err);
        this.cargandoCategorias = false;
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Carga las métricas KPI superiores (total, urgencia alta, finalizados).
   */
  cargarMetricas(): void {
    this.cargandoMetricas = true;
    this.reporteService.obtenerMetricas().subscribe({
      next: (data) => {
        if (data) {
          this.metricas = data;
          this.totalReportesGeneral = data.totalReportes;
        }
        this.cargandoMetricas = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error al cargar métricas del backend:', err);
        this.cargandoMetricas = false;
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Carga los reportes desde la base de datos según la categoría solicitada:
   * - Si es 'Todos', consulta el endpoint de todos los reportes (/api/reportes/todos).
   * - Si es una categoría específica, consulta el endpoint con filtro (/api/reportes?categoria=...).
   */
  cargarReportes(categoria: string): void {
    this.cargandoReportes = true;
    this.errorReportes = null;

    const peticion$ = (categoria === 'Todos' || !categoria)
      ? this.reporteService.obtenerTodos()
      : this.reporteService.obtenerPorCategoria(categoria);

    peticion$.subscribe({
      next: (data) => {
        this.reportes = data ?? [];
        this.cargandoReportes = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error(`Error al cargar reportes para la categoría "${categoria}":`, err);
        this.errorReportes = 'No fue posible conectar con el servidor para obtener los reportes.';
        this.cargandoReportes = false;
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Cambiar de pestaña activa y disparar la consulta al endpoint correspondiente.
   */
  seleccionarCategoria(categoria: string): void {
    if (this.categoriaSeleccionada === categoria && !this.cargandoReportes) {
      return;
    }
    this.categoriaSeleccionada = categoria;
    this.cargarReportes(categoria);
  }

  /**
   * Helpers visuales para badges y estilos dinámicos
   */
  obtenerClaseUrgencia(urgencia?: string): string {
    const u = (urgencia || '').toLowerCase().trim();
    if (u.includes('alta')) return 'alta';
    if (u.includes('media')) return 'media';
    if (u.includes('baja')) return 'baja';
    return 'baja';
  }

  obtenerClaseSentimiento(sentimiento?: string): string {
    const s = (sentimiento || '').toLowerCase().trim();
    if (s.includes('enojado')) return 'enojado';
    if (s.includes('frustrado')) return 'frustrado';
    if (s.includes('neutral') || s.includes('neutro')) return 'neutral';
    if (s.includes('positivo') || s.includes('feliz')) return 'positivo';
    return 'neutral';
  }

  obtenerClaseEstado(estado?: string): string {
    const e = (estado || '').toLowerCase().trim();
    if (e.includes('finalizado') || e.includes('resuelto') || e.includes('completado')) {
      return 'estado-finalizado';
    }
    if (e.includes('proceso') || e.includes('iniciado')) {
      return 'estado-proceso';
    }
    return 'estado-pendiente';
  }

  obtenerClaseRiesgo(riesgo?: string): string {
    const r = (riesgo || '').toUpperCase().trim();
    switch (r) {
      case 'CRITICO':
        return 'riesgo-critico';
      case 'ALTO':
        return 'riesgo-alto';
      case 'MEDIO':
        return 'riesgo-medio';
      case 'BAJO':
        return 'riesgo-bajo';
      default:
        return 'riesgo-ninguno';
    }
  }

  /**
   * Determina si el reporte se encuentra en Fase 1 ("No iniciado").
   */
  esNoIniciado(estado?: string): boolean {
    if (!estado) return true;
    const e = estado.trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    return e === 'no iniciado' || e === 'pendiente';
  }

  /**
   * Clasifica la fase para estados posteriores:
   * - 'proceso': "Iniciado" o "En Proceso" (icono de reloj azul)
   * - 'revision': "En revisión" (icono de lupa ámbar)
   * - 'finalizado': "Finalizado" (icono de check verde)
   */
  obtenerTipoFase(estado?: string): 'proceso' | 'revision' | 'finalizado' {
    if (!estado) return 'proceso';
    const e = estado.trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    if (e.includes('finalizado') || e.includes('resuelto') || e.includes('completado')) {
      return 'finalizado';
    }
    if (e.includes('revision')) {
      return 'revision';
    }
    return 'proceso';
  }

  // Acción para el botón de seguimiento (navega a /seguimiento/:id)
  verSeguimiento(id: number | string, codigo?: string): void {
    this.router.navigate(['/seguimiento', id], { queryParams: { codigo } });
  }

  // Acción para el botón de gestión (navega a /gestionar/:id)
  gestionarReporte(id: number | string, codigo?: string): void {
    this.router.navigate(['/gestionar', id], { queryParams: { codigo } });
  }

  // Acción para el botón de revisión y cierre (navega a /revisar/:id)
  irARevision(id: number | string, codigo?: string): void {
    this.router.navigate(['/revisar', id], { queryParams: { codigo } });
  }
}
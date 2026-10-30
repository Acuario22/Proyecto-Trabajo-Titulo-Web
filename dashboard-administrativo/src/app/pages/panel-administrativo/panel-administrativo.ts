import { Component, inject, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef, HostListener, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription, Subject, timer, interval, forkJoin } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import {
  ReporteService,
  Reporte,
  CategoriaConteo,
  DashboardMetricas
} from '../../core/services/reporte.service';

import { HeaderComponent } from '../../shared/components/header/header';
import { ReporteDetalleModalComponent } from '../../shared/components/reporte-detalle-modal/reporte-detalle-modal';

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
  imports: [CommonModule, HeaderComponent, ReporteDetalleModalComponent],
  templateUrl: './panel-administrativo.html',
  styleUrl: './panel-administrativo.css'
})
export class PanelAdministrativoComponent implements OnInit, OnDestroy, AfterViewInit {
  private router = inject(Router);
  private reporteService = inject(ReporteService);
  private cdr = inject(ChangeDetectorRef);
  private subReportes?: Subscription;

  @ViewChild('tabsWrapper') tabsWrapperRef?: ElementRef<HTMLElement>;
  @ViewChild('statusDropdownRef') statusDropdownRef?: ElementRef<HTMLElement>;
  @ViewChild('refreshDropdownRef') refreshDropdownRef?: ElementRef<HTMLElement>;

  // Estado del menú desplegable de estados
  dropdownEstadoAbierto: boolean = false;
  // Estado del menú desplegable de frecuencia de recarga
  dropdownRecargaAbierto: boolean = false;

  // Variables para la pastilla deslizante (Sliding Pill Indicator tipo toggle)
  indicadorLeft: number = 0;
  indicadorWidth: number = 0;
  indicadorVisible: boolean = false;

  // Listas oficiales de Urgencias y Sentimientos
  readonly urgencias = URGENCIAS;
  readonly sentimientos = SENTIMIENTOS;

  // Pestaña seleccionada actualmente ('Todos' por defecto)
  categoriaSeleccionada: string = 'Todos';

  // Estado seleccionado para filtrado (null = 'Todos los estados', 1 = No iniciado, 2 = Iniciado, 3 = En revisión, 4 = Finalizado)
  estadoSeleccionado: number | null = null;

  // Catálogo de estados disponibles para la botonera de filtrado
  readonly estadosFiltro = [
    { id: null, nombre: 'Todos los estados', corto: 'Todos' },
    { id: 1, nombre: 'No iniciado', corto: 'No iniciado' },
    { id: 2, nombre: 'Iniciado', corto: 'Iniciado' },
    { id: 3, nombre: 'En revisión', corto: 'En revisión' },
    { id: 4, nombre: 'Finalizado', corto: 'Finalizado' }
  ];

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

  // ==========================================
  // CONFIGURACIÓN Y ESTADO DE PAGINACIÓN
  // ==========================================
  paginaActual: number = 1;
  tamanoPagina: number = 10;
  readonly opcionesTamanoPagina: number[] = [10, 20, 40];

  /**
   * Calcula el número total de páginas según la cantidad de reportes y el tamaño de página.
   */
  get totalPaginas(): number {
    if (!this.reportes || this.reportes.length === 0) return 1;
    return Math.ceil(this.reportes.length / this.tamanoPagina);
  }

  /**
   * Retorna únicamente los reportes correspondientes a la página activa.
   */
  get reportesPaginados(): Reporte[] {
    if (!this.reportes || this.reportes.length === 0) return [];
    const inicio = (this.paginaActual - 1) * this.tamanoPagina;
    return this.reportes.slice(inicio, inicio + this.tamanoPagina);
  }

  /**
   * Índice inicial visible en el texto descriptivo de la paginación (1-based).
   */
  get rangoInicio(): number {
    if (!this.reportes || this.reportes.length === 0) return 0;
    return (this.paginaActual - 1) * this.tamanoPagina + 1;
  }

  /**
   * Índice final visible en el texto descriptivo de la paginación.
   */
  get rangoFin(): number {
    if (!this.reportes || this.reportes.length === 0) return 0;
    return Math.min(this.paginaActual * this.tamanoPagina, this.reportes.length);
  }

  /**
   * Cambia a una página específica dentro del rango disponible.
   */
  cambiarPagina(nuevaPagina: number): void {
    if (nuevaPagina >= 1 && nuevaPagina <= this.totalPaginas && nuevaPagina !== this.paginaActual) {
      this.paginaActual = nuevaPagina;
      this.cdr.markForCheck();
    }
  }

  /**
   * Modifica el tamaño de la página (10, 20 o 40) y reinicia la visualización a la primera página.
   */
  cambiarTamanoPagina(nuevoTamano: number): void {
    this.tamanoPagina = Number(nuevoTamano);
    this.paginaActual = 1;
    this.cdr.markForCheck();
  }

  /**
   * Manejador del cambio de tamaño de página desde el selector HTML.
   */
  onCambiarTamanoPagina(event: Event): void {
    const select = event.target as HTMLSelectElement;
    if (select) {
      this.cambiarTamanoPagina(parseInt(select.value, 10));
    }
  }

  /**
   * Retorna una lista secuencial de páginas para renderizar los botones numéricos.
   */
  obtenerPaginas(): number[] {
    const paginas: number[] = [];
    for (let i = 1; i <= this.totalPaginas; i++) {
      paginas.push(i);
    }
    return paginas;
  }

  // Estado y reporte activo para el Modal de "Más información"
  modalDetalleAbierto: boolean = false;
  reporteSeleccionadoParaModal: Reporte | null = null;

  // Estado y configuración para Actualización Dinámica (Auto-refresh)
  tiempoRelativo: string = 'hace unos momentos';
  ultimaActualizacion: Date = new Date();
  intervaloSeleccionado: number = 60; // 1 minuto por defecto
  actualizando: boolean = false; // Indicador de carga en segundo plano

  // Opciones disponibles para el selector de intervalo
  readonly opcionesIntervalo = [
    { valor: 30, etiqueta: 'Cada 30 seg', corto: '30 seg' },
    { valor: 60, etiqueta: 'Cada 1 min', corto: '1 min' },
    { valor: 300, etiqueta: 'Cada 5 min', corto: '5 min' },
    { valor: 600, etiqueta: 'Cada 10 min', corto: '10 min' }
  ];

  // Control de ciclo de vida con RxJS para prevenir memory leaks
  private destroy$ = new Subject<void>();
  private autoRefreshSub?: Subscription;
  private timerRelativoSub?: Subscription;

  ngOnInit(): void {
    this.cargarCategorias();
    this.cargarMetricas();
    this.cargarReportes(this.categoriaSeleccionada);
    this.iniciarTemporizadorRelativo();
    this.cambiarIntervalo(this.intervaloSeleccionado);
  }

  /**
   * Carga la lista de categorías con sus respectivos conteos desde el backend.
   */
  cargarCategorias(): void {
    this.cargandoCategorias = true;
    this.reporteService.obtenerCategorias()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.categorias = data ?? [];
          this.cargandoCategorias = false;
          // Si aún no tenemos métricas, calculamos el total sumando las categorías
          if (this.totalReportesGeneral === 0 && this.categorias.length > 0) {
            this.totalReportesGeneral = this.categorias.reduce((sum, c) => sum + (c.conteo || 0), 0);
          }
          this.cdr.markForCheck();
          this.actualizarIndicador();
        },
        error: (err) => {
          console.error('Error al cargar categorías desde el backend:', err);
          this.cargandoCategorias = false;
          this.cdr.markForCheck();
        }
      });
  }

  ngAfterViewInit(): void {
    this.actualizarIndicador();
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.actualizarIndicador();
  }

  /**
   * Carga las métricas KPI superiores (total, urgencia alta, finalizados).
   */
  cargarMetricas(): void {
    this.cargandoMetricas = true;
    this.reporteService.obtenerMetricas()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
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

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.autoRefreshSub?.unsubscribe();
    this.timerRelativoSub?.unsubscribe();
    this.subReportes?.unsubscribe();
    document.body.style.overflow = '';
  }

  /**
   * Inicializa el temporizador de tiempo relativo (actualiza la etiqueta cada 30 segundos).
   */
  private iniciarTemporizadorRelativo(): void {
    this.timerRelativoSub?.unsubscribe();
    this.timerRelativoSub = timer(30000, 30000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.tiempoRelativo = this.calcularTiempoRelativo(this.ultimaActualizacion);
        this.cdr.markForCheck();
      });
  }

  /**
   * Calcula la representación amigable en lenguaje natural del tiempo transcurrido
   * desde la última sincronización con la base de datos.
   */
  calcularTiempoRelativo(fecha: Date): string {
    if (!fecha) return 'hace unos momentos';
    const ahora = new Date();
    const diffMs = ahora.getTime() - fecha.getTime();
    const diffSeg = Math.floor(diffMs / 1000);

    if (diffSeg < 60) {
      return 'hace unos momentos';
    }
    const diffMin = Math.floor(diffSeg / 60);
    if (diffMin === 1) {
      return 'hace 1 min';
    }
    if (diffMin < 60) {
      return `hace ${diffMin} min`;
    }
    const diffHoras = Math.floor(diffMin / 60);
    if (diffHoras === 1) {
      return 'hace 1 h';
    }
    return `hace ${diffHoras} h`;
  }

  /**
   * Cambia la frecuencia del temporizador de actualización automática.
   * - segundos === 0: Desactiva la recarga automática (modo manual).
   * - segundos > 0: Reinicia el temporizador con la nueva frecuencia periódica.
   */
  cambiarIntervalo(segundos: number): void {
    this.intervaloSeleccionado = Number(segundos);
    this.autoRefreshSub?.unsubscribe();

    if (this.intervaloSeleccionado > 0) {
      const ms = this.intervaloSeleccionado * 1000;
      this.autoRefreshSub = interval(ms)
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => {
          this.refrescarDatos(true);
        });
    }
    this.cdr.markForCheck();
  }

  /**
   * Abre o cierra el menú desplegable de frecuencia de recarga.
   */
  toggleDropdownRecarga(event: MouseEvent): void {
    event.stopPropagation();
    this.dropdownRecargaAbierto = !this.dropdownRecargaAbierto;
    if (this.dropdownRecargaAbierto) {
      this.dropdownEstadoAbierto = false;
    }
  }

  /**
   * Selecciona un nuevo intervalo de actualización desde el menú desplegable.
   */
  seleccionarIntervalo(segundos: number): void {
    this.dropdownRecargaAbierto = false;
    this.cambiarIntervalo(segundos);
  }

  /**
   * Helper que retorna la etiqueta completa del intervalo actual.
   */
  obtenerEtiquetaIntervaloActual(): string {
    const encontrado = this.opcionesIntervalo.find(o => o.valor === this.intervaloSeleccionado);
    return encontrado ? encontrado.etiqueta : 'Cada 1 min';
  }

  /**
   * Helper que retorna el nombre corto para el botón disparador del desplegable de recarga.
   */
  obtenerEtiquetaIntervaloCorto(): string {
    const encontrado = this.opcionesIntervalo.find(o => o.valor === this.intervaloSeleccionado);
    return encontrado ? encontrado.corto : '1 min';
  }

  /**
   * Manejador del evento change en el selector HTML de intervalos.
   */
  onCambiarIntervaloSelect(event: Event): void {
    const select = event.target as HTMLSelectElement;
    if (select) {
      this.cambiarIntervalo(parseInt(select.value, 10));
    }
  }

  /**
   * Fuerza la actualización manual e inmediata del dashboard al presionar el botón circular.
   */
  actualizarManualmente(): void {
    this.refrescarDatos(true);
  }

  /**
   * Actualiza concurrentemente los reportes, categorías y métricas sin bloquear la vista.
   */
  refrescarDatos(esSegundoPlano: boolean = true): void {
    if (this.actualizando) {
      return;
    }

    this.actualizando = true;
    if (!esSegundoPlano && this.reportes.length === 0) {
      this.cargandoReportes = true;
    }
    this.cdr.markForCheck();

    forkJoin({
      categorias: this.reporteService.obtenerCategorias(),
      metricas: this.reporteService.obtenerMetricas(),
      reportes: this.reporteService.obtenerReportesFiltrados(this.categoriaSeleccionada, this.estadoSeleccionado)
    }).pipe(
      takeUntil(this.destroy$),
      finalize(() => {
        this.actualizando = false;
        this.cargandoReportes = false;
        this.cargandoCategorias = false;
        this.cargandoMetricas = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: ({ categorias, metricas, reportes }) => {
        if (categorias) {
          this.categorias = categorias;
        }
        if (metricas) {
          this.metricas = metricas;
          this.totalReportesGeneral = metricas.totalReportes;
        }
        if (reportes) {
          this.reportes = reportes;
          this.errorReportes = null;
        }
        this.ultimaActualizacion = new Date();
        this.tiempoRelativo = 'hace unos momentos';
        this.actualizarIndicador();
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error al actualizar datos en segundo plano:', err);
        if (this.reportes.length === 0) {
          this.errorReportes = 'No fue posible conectar con el servidor para obtener los reportes.';
        }
      }
    });
  }

  /**
   * Carga los reportes desde la base de datos según la categoría y el estado solicitados:
   * Consulta el endpoint con filtros combinados (/api/reportes?categoria=...&estado=...).
   */
  cargarReportes(categoria: string = this.categoriaSeleccionada, idEstado: number | null = this.estadoSeleccionado): void {
    this.cargandoReportes = true;
    this.errorReportes = null;
    this.subReportes?.unsubscribe();

    this.subReportes = this.reporteService.obtenerReportesFiltrados(categoria, idEstado)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.reportes = data ?? [];
          if (this.paginaActual > this.totalPaginas) {
            this.paginaActual = 1;
          }
          this.cargandoReportes = false;
          this.ultimaActualizacion = new Date();
          this.tiempoRelativo = 'hace unos momentos';
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error(`Error al cargar reportes filtrados (categoria: ${categoria}, estado: ${idEstado}):`, err);
          this.errorReportes = 'No fue posible conectar con el servidor para obtener los reportes.';
          this.cargandoReportes = false;
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Cambiar de pestaña activa con animación de desplazamiento tipo toggle switch.
   */
  seleccionarCategoria(categoria: string, event?: MouseEvent): void {
    if (event?.currentTarget) {
      const btn = event.currentTarget as HTMLElement;
      this.indicadorLeft = btn.offsetLeft;
      this.indicadorWidth = btn.offsetWidth;
      this.indicadorVisible = true;
    }

    if (this.categoriaSeleccionada === categoria && !this.cargandoReportes) {
      return;
    }
    this.categoriaSeleccionada = categoria;
    this.paginaActual = 1;
    this.cargarReportes(categoria, this.estadoSeleccionado);

    if (!event) {
      this.actualizarIndicador();
    }
  }

  /**
   * Abre o cierra el menú desplegable de estados.
   */
  toggleDropdownEstado(event: MouseEvent): void {
    event.stopPropagation();
    this.dropdownEstadoAbierto = !this.dropdownEstadoAbierto;
    if (this.dropdownEstadoAbierto) {
      this.dropdownRecargaAbierto = false;
    }
  }

  /**
   * Cierra los desplegables si el usuario hace clic fuera de ellos.
   */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.dropdownEstadoAbierto && this.statusDropdownRef) {
      if (!this.statusDropdownRef.nativeElement.contains(event.target as Node)) {
        this.dropdownEstadoAbierto = false;
        this.cdr.markForCheck();
      }
    }
    if (this.dropdownRecargaAbierto && this.refreshDropdownRef) {
      if (!this.refreshDropdownRef.nativeElement.contains(event.target as Node)) {
        this.dropdownRecargaAbierto = false;
        this.cdr.markForCheck();
      }
    }
  }

  /**
   * Filtra los reportes según el estado seleccionado (1 a 4 o null para todos).
   */
  seleccionarEstado(idEstado: number | null): void {
    this.dropdownEstadoAbierto = false;
    if (this.estadoSeleccionado === idEstado && !this.cargandoReportes) {
      return;
    }
    this.estadoSeleccionado = idEstado;
    this.paginaActual = 1;
    this.cargarReportes(this.categoriaSeleccionada, idEstado);
  }

  /**
   * Helper que retorna el nombre legible del estado actual filtrado.
   */
  obtenerNombreEstadoActual(): string {
    if (this.estadoSeleccionado === null) return 'Todos';
    const encontrado = this.estadosFiltro.find(e => e.id === this.estadoSeleccionado);
    return encontrado ? encontrado.nombre : 'Todos';
  }

  /**
   * Helper que retorna el nombre corto para el botón disparador del desplegable.
   */
  obtenerNombreEstadoCortoActual(): string {
    if (this.estadoSeleccionado === null) return 'Todos';
    const encontrado = this.estadosFiltro.find(e => e.id === this.estadoSeleccionado);
    return encontrado ? encontrado.corto : 'Todos';
  }

  /**
   * Restablece ambos filtros (categoría a 'Todos' y estado a null).
   */
  restablecerFiltros(): void {
    this.categoriaSeleccionada = 'Todos';
    this.estadoSeleccionado = null;
    this.paginaActual = 1;
    this.actualizarIndicador();
    this.cargarReportes('Todos', null);
  }

  /**
   * Mide y reposiciona suavemente la pastilla indicadora deslizante.
   */
  actualizarIndicador(targetBtn?: HTMLElement): void {
    setTimeout(() => {
      const wrapper = this.tabsWrapperRef?.nativeElement;
      if (!wrapper) return;
      const btn = targetBtn || wrapper.querySelector<HTMLElement>('.tab-button.active');
      if (btn) {
        this.indicadorLeft = btn.offsetLeft;
        this.indicadorWidth = btn.offsetWidth;
        this.indicadorVisible = true;
        this.cdr.markForCheck();
      }
    }, 20);
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
    const e = (estado || '').toLowerCase().trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    if (e.includes('finalizado') || e.includes('resuelto') || e.includes('completado')) {
      return 'estado-finalizado';
    }
    if (e.includes('revision')) {
      return 'estado-revision';
    }
    if (e.includes('no iniciado') || e.includes('pendiente')) {
      return 'estado-pendiente';
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

  /**
   * Abre el modal de Más Información con los datos completos del reporte.
   */
  abrirModalDetalle(reporte: Reporte, event?: MouseEvent): void {
    if (event) {
      event.stopPropagation();
    }
    this.reporteSeleccionadoParaModal = reporte;
    this.modalDetalleAbierto = true;
    document.body.style.overflow = 'hidden';
    this.cdr.markForCheck();
  }

  /**
   * Cierra el modal de Más Información y restituye el scroll.
   */
  cerrarModalDetalle(): void {
    this.modalDetalleAbierto = false;
    this.reporteSeleccionadoParaModal = null;
    document.body.style.overflow = '';
    this.cdr.markForCheck();
  }

  /**
   * Cierra el modal al presionar la tecla Escape.
   */
  @HostListener('window:keydown.escape')
  onEscapeModal(): void {
    if (this.modalDetalleAbierto) {
      this.cerrarModalDetalle();
    }
  }
}
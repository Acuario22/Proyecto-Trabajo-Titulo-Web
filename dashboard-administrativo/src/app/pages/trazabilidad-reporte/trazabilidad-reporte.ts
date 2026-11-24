import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Urgencia, Sentimiento } from '../panel-administrativo/panel-administrativo';
import { ReporteService } from '../../core/services/reporte.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { HeaderComponent } from '../../shared/components/header/header';

export interface PasoLineaTiempo {
  numero: number;
  titulo: string;
  fecha?: string;
  estado: 'completado' | 'en-proceso' | 'pendiente';
  descripcion: string;
}

export interface DetalleReporte {
  codigo: string;
  titulo: string;
  descripcion: string;
  direccion?: string;
  fechaIngreso: string;
  urgencia: Urgencia;
  sentimiento: Sentimiento;
  estadoActual: string;
  detalleEstadoActual: string;
}

@Component({
  selector: 'app-trazabilidad-reporte',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent],
  templateUrl: './trazabilidad-reporte.html',
  styleUrl: './trazabilidad-reporte.css'
})
export class TrazabilidadReporteComponent implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private reporteService = inject(ReporteService);
  private notificacionService = inject(NotificacionService);
  private cdr = inject(ChangeDetectorRef);

  idReporte: number | string | null = null;
  cargando: boolean = true;

  reporte: DetalleReporte = {
    codigo: 'RPT-2026-0001',
    titulo: 'Reporte de Pavimentación',
    descripcion: 'Cargando detalle del reporte...',
    direccion: 'Cargando dirección...',
    fechaIngreso: '--/--/----',
    urgencia: 'Baja',
    sentimiento: 'Neutral',
    estadoActual: 'En Proceso',
    detalleEstadoActual: 'Consultando estado en el sistema...'
  };

  pasos: PasoLineaTiempo[] = [
    {
      numero: 1,
      titulo: 'Iniciado',
      fecha: undefined,
      estado: 'completado',
      descripcion: 'Reporte recibido y registrado en el sistema.'
    },
    {
      numero: 2,
      titulo: 'En Proceso',
      fecha: undefined,
      estado: 'en-proceso',
      descripcion: 'Cuadrilla asignada y gestión técnica en curso.'
    },
    {
      numero: 3,
      titulo: 'Finalizado',
      estado: 'pendiente',
      descripcion: 'Pendiente de ejecución y cierre.'
    }
  ];

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const codigoParam = this.route.snapshot.queryParamMap.get('codigo') || this.route.snapshot.paramMap.get('codigo');
    const param = codigoParam || idParam || '1';
    this.idReporte = idParam || codigoParam;

    this.cargarTrazabilidad(param);
  }

  cargarTrazabilidad(identificador: string): void {
    this.cargando = true;
    this.reporteService.obtenerTrazabilidad(identificador).subscribe({
      next: (data) => {
        if (data) {
          this.reporte = {
            codigo: data.codigo || this.reporte.codigo,
            titulo: data.titulo || this.reporte.titulo,
            descripcion: data.descripcion || this.reporte.descripcion,
            direccion: data.direccion || this.reporte.direccion || 'Dirección registrada en terreno',
            fechaIngreso: data.fechaIngreso || this.reporte.fechaIngreso,
            urgencia: (data.urgencia as Urgencia) || this.reporte.urgencia,
            sentimiento: (data.sentimiento as Sentimiento) || this.reporte.sentimiento,
            estadoActual: data.estadoActual || this.reporte.estadoActual,
            detalleEstadoActual: data.detalleEstadoActual || this.reporte.detalleEstadoActual
          };

          if (data.pasos && Array.isArray(data.pasos)) {
            this.pasos = data.pasos;
          }
        }
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.warn('No se pudo cargar trazabilidad del backend, manteniendo vista inicial:', err);
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  calcularPorcentajeProgreso(): number {
    if (!this.pasos || this.pasos.length === 0) return 0;
    const completados = this.pasos.filter(p => p.estado === 'completado').length;
    const enProceso = this.pasos.filter(p => p.estado === 'en-proceso').length;
    if (completados === this.pasos.length) return 100;
    if (completados === 0 && enProceso > 0) return 33;
    if (completados === 1 && enProceso > 0) return 66;
    if (completados === 2 && enProceso > 0) return 85;
    return Math.round(((completados + enProceso * 0.5) / this.pasos.length) * 100);
  }

  obtenerClaseUrgencia(urgencia: string): string {
    const u = (urgencia || '').toLowerCase().trim();
    if (u.includes('alta') || u.includes('critica') || u.includes('urgente')) return 'alta';
    if (u.includes('media') || u.includes('moderada')) return 'media';
    return 'baja';
  }

  obtenerClaseSentimiento(sentimiento: string): string {
    const s = (sentimiento || '').toLowerCase().trim();
    if (s.includes('enojado') || s.includes('negativo')) return 'negativo';
    if (s.includes('frustrado')) return 'frustrado';
    if (s.includes('positivo')) return 'positivo';
    return 'neutral';
  }

  esEnRevision(): boolean {
    if (!this.reporte || !this.reporte.estadoActual) return false;
    const e = this.reporte.estadoActual.trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    return e.includes('revision');
  }

  esFinalizado(): boolean {
    if (!this.reporte || !this.reporte.estadoActual) return false;
    const e = this.reporte.estadoActual.trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    return e.includes('finalizado');
  }

  irARevision(): void {
    const id = this.idReporte || this.reporte.codigo;
    this.router.navigate(['/revisar', id]);
  }

  verEnMapa(): void {
    const direccion = this.reporte.direccion?.trim();
    if (direccion && direccion.toLowerCase() !== 'no especificada') {
      const dir = encodeURIComponent(direccion);
      window.open(`https://www.google.com/maps/search/?api=1&query=${dir}`, '_blank');
    } else {
      this.notificacionService.info(
        'Sin dirección',
        'Este reporte no registra una dirección específica.'
      );
    }
  }

  volverAlPanel(): void {
    this.router.navigate(['/panel-administrativo']);
  }
}


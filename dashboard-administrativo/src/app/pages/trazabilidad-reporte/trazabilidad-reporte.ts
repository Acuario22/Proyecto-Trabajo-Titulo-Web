import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Urgencia, Sentimiento, URGENCIAS, SENTIMIENTOS } from '../panel-administrativo/panel-administrativo';
import { ReporteService } from '../../core/services/reporte.service';
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
  private cdr = inject(ChangeDetectorRef);

  readonly urgencias = URGENCIAS;
  readonly sentimientos = SENTIMIENTOS;

  idReporte: number | string | null = null;
  cargando: boolean = true;

  reporte: DetalleReporte = {
    codigo: 'RPT-2024-0001',
    titulo: 'Reporte de Pavimentación',
    descripcion: 'Cargando detalle del reporte...',
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
            fechaIngreso: data.fechaIngreso || this.reporte.fechaIngreso,
            urgencia: data.urgencia || this.reporte.urgencia,
            sentimiento: data.sentimiento || this.reporte.sentimiento,
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

  esEnRevision(): boolean {
    if (!this.reporte || !this.reporte.estadoActual) return false;
    const e = this.reporte.estadoActual.trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    return e.includes('revision');
  }

  irARevision(): void {
    const id = this.idReporte || this.reporte.codigo;
    this.router.navigate(['/revisar', id]);
  }

  volverAlPanel(): void {
    this.router.navigate(['/panel-administrativo']);
  }
}

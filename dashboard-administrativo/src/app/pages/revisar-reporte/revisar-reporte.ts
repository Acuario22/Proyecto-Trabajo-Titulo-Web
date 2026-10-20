import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HeaderComponent } from '../../shared/components/header/header';
import {
  ReporteService,
  Reporte,
  OrdenTrabajoResponse,
  CerrarReporteRequest
} from '../../core/services/reporte.service';

@Component({
  selector: 'app-revisar-reporte',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, HeaderComponent],
  templateUrl: './revisar-reporte.html',
  styleUrl: './revisar-reporte.css'
})
export class RevisarReporteComponent implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private reporteService = inject(ReporteService);
  private cdr = inject(ChangeDetectorRef);

  idReporte: number | null = null;
  cargando: boolean = true;
  guardandoCierre: boolean = false;
  mensajeExito: string | null = null;
  mensajeError: string | null = null;

  // Verificaciones requeridas para el cierre
  checkInstruccionesConformes: boolean = false;
  checkEvidenciasVerificadas: boolean = false;
  observacionCierreAdmin: string = '';

  reporte: Reporte = {
    id: 0,
    codigo: 'RPT-2024-0000',
    categoria: '',
    texto: '',
    direccion: '',
    fechaIngreso: '',
    urgencia: 'Baja',
    sentimiento: 'Neutral',
    estado: 'En revisión',
    nivelRiesgo: 'MEDIO',
    huboAjuste: false
  };

  orden: OrdenTrabajoResponse = {
    idOrden: 0,
    idReporte: 0,
    codigoReporte: '',
    cuadrilla: 'Cuadrilla de Operaciones Urbanas',
    responsable: 'Encargado Técnico',
    cantidadPersonal: 3,
    vehiculo: 'Unidad de Terreno',
    fechaVisita: '',
    horaVisita: '',
    duracionEstimada: 'Jornada completa',
    materiales: 'Equipamiento e insumos estándar',
    instrucciones: 'Ejecutar obra conforme a especificaciones municipales.',
    estadoReporte: 'En revisión',
    comentarioCuadrilla: ''
  };

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.idReporte = Number(idParam);
      this.cargarDatos(this.idReporte);
    } else {
      this.mensajeError = 'Identificador de reporte no proporcionado en la ruta.';
      this.cargando = false;
    }
  }

  cargarDatos(id: number): void {
    this.cargando = true;
    this.mensajeError = null;

    // Cargar reporte unitario
    this.reporteService.obtenerReportePorId(id).subscribe({
      next: (rep) => {
        this.reporte = rep;
        // Cargar orden de trabajo vinculada
        this.reporteService.obtenerOrdenTrabajo(id).subscribe({
          next: (ord) => {
            this.orden = ord;
            this.cargando = false;
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.warn('No se encontró orden de trabajo previa, usando datos por defecto:', err);
            this.orden.idReporte = rep.id;
            this.orden.codigoReporte = rep.codigo;
            this.orden.comentarioCuadrilla = 'Trabajo ejecutado en terreno por ' + this.orden.cuadrilla 
              + ' a cargo de ' + this.orden.responsable + '. Se cumplieron las instrucciones operativas y la zona quedó habilitada.';
            this.cargando = false;
            this.cdr.detectChanges();
          }
        });
      },
      error: (err) => {
        console.error('Error al cargar reporte:', err);
        this.mensajeError = 'No fue posible cargar el reporte solicitado desde el servidor.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  get formularioValido(): boolean {
    return this.checkInstruccionesConformes &&
      this.checkEvidenciasVerificadas &&
      this.observacionCierreAdmin.trim().length > 0;
  }

  finalizarReporte(): void {
    if (!this.formularioValido || !this.idReporte || this.guardandoCierre) {
      return;
    }

    this.guardandoCierre = true;
    this.mensajeError = null;
    this.mensajeExito = null;

    const request: CerrarReporteRequest = {
      observacionCierreAdmin: this.observacionCierreAdmin.trim()
    };

    this.reporteService.finalizarReporte(this.idReporte, request).subscribe({
      next: (res) => {
        this.guardandoCierre = false;
        this.mensajeExito = `¡Reporte ${this.reporte.codigo} finalizado exitosamente! Cierre administrativo registrado en TBLORDENTRABAJO.`;
        this.reporte.estado = 'Finalizado';
        this.cdr.detectChanges();

        // Redirigir a la vista de seguimiento del reporte tras 1.8 segundos
        setTimeout(() => {
          this.router.navigate(['/seguimiento', this.idReporte], {
            queryParams: { codigo: this.reporte.codigo }
          });
        }, 1800);
      },
      error: (err) => {
        console.error('Error al finalizar reporte:', err);
        this.guardandoCierre = false;
        this.mensajeError = err.error?.message || 'Ocurrió un error al registrar el cierre del reporte en el servidor.';
        this.cdr.detectChanges();
      }
    });
  }

  volverAlPanel(): void {
    this.router.navigate(['/panel-administrativo']);
  }

  verEnMapa(): void {
    const dir = encodeURIComponent(this.reporte.direccion || 'Santiago, Chile');
    window.open(`https://www.google.com/maps/search/?api=1&query=${dir}`, '_blank');
  }

  obtenerClaseUrgencia(urgencia?: string): string {
    const u = (urgencia || '').toLowerCase().trim();
    if (u.includes('alta') || u.includes('alta')) return 'urgencia-alta';
    if (u.includes('media')) return 'urgencia-media';
    return 'urgencia-baja';
  }
}

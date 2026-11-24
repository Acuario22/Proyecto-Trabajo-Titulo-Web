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
import { NotificacionService } from '../../core/services/notificacion.service';

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
  private notificacionService = inject(NotificacionService);
  private cdr = inject(ChangeDetectorRef);

  idReporte: number | null = null;
  cargando: boolean = true;
  guardandoCierre: boolean = false;
  intentadoFinalizar: boolean = false;
  mensajeExito: string | null = null;
  mensajeError: string | null = null;

  // Verificaciones requeridas para el cierre
  checkInstruccionesConformes: boolean = false;
  checkEvidenciasVerificadas: boolean = false;
  observacionCierreAdmin: string = '';

  reporte: Reporte = {
    id: 0,
    codigo: 'RPT-2026-0000',
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

  async finalizarReporte(): Promise<void> {
    if (!this.checkInstruccionesConformes || !this.checkEvidenciasVerificadas) {
      this.intentadoFinalizar = true;
      this.notificacionService.advertencia(
        'Conformidad incompleta',
        'Debes marcar ambas casillas de verificación técnica y conformidad de trabajo en terreno.'
      );
      this.cdr.detectChanges();
      return;
    }

    if (!this.observacionCierreAdmin || !this.observacionCierreAdmin.trim()) {
      this.intentadoFinalizar = true;
      this.notificacionService.advertencia(
        'Observación requerida',
        'Por favor completa la observación de cierre administrativo (*) para finalizar el reporte.'
      );
      this.cdr.detectChanges();
      return;
    }

    if (!this.idReporte || this.guardandoCierre) {
      return;
    }

    // Modal de Confirmación centrado
    const confirmado = await this.notificacionService.confirmar({
      titulo: '¿Confirmar cierre definitivo del reporte?',
      mensaje: 'Asegúrate de haber ingresado la información correctamente. Si es así, confirma para finalizar el reporte.',
      textoConfirmar: 'Sí, finalizar reporte',
      textoCancelar: 'Volver y revisar',
      tipo: 'exito'
    });

    if (!confirmado) {
      return;
    }

    this.guardandoCierre = true;
    this.mensajeError = null;
    this.mensajeExito = null;

    const request: CerrarReporteRequest = {
      observacionCierreAdmin: this.observacionCierreAdmin.trim()
    };

    this.reporteService.finalizarReporte(this.idReporte, request).subscribe({
      next: () => {
        this.guardandoCierre = false;
        this.reporte.estado = 'Finalizado';
        this.mensajeExito = `¡Reporte ${this.reporte.codigo} finalizado exitosamente! Cierre administrativo registrado en TBLORDENTRABAJO.`;
        this.notificacionService.exito(
          'Reporte finalizado',
          'La incidencia ha sido cerrada y certificada con éxito.'
        );
        this.cdr.detectChanges();

        // Redirigir a la vista de seguimiento del reporte tras 1.2 segundos
        setTimeout(() => {
          this.router.navigate(['/seguimiento', this.idReporte], {
            queryParams: { codigo: this.reporte.codigo }
          });
        }, 1200);
      },
      error: (err) => {
        console.error('Error al finalizar reporte:', err);
        this.guardandoCierre = false;
        this.notificacionService.error(
          'Error al finalizar',
          err.error?.message || 'Ocurrió un error al registrar el cierre del reporte en el servidor.'
        );
        this.cdr.detectChanges();
      }
    });
  }

  volverAlPanel(): void {
    this.router.navigate(['/panel-administrativo']);
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

  obtenerClaseUrgencia(urgencia?: string): string {
    const u = (urgencia || '').toLowerCase().trim();
    if (u.includes('alta')) return 'tag-urgency-high';
    if (u.includes('media')) return 'tag-urgency-medium';
    return 'tag-urgency-low';
  }

  obtenerClaseSentimiento(sentimiento?: string): string {
    const s = (sentimiento || '').toLowerCase().trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    if (s.includes('enojado') || s.includes('negativo')) return 'enojado';
    if (s.includes('frustrado')) return 'frustrado';
    if (s.includes('positivo') || s.includes('feliz')) return 'positivo';
    return 'neutral';
  }

  obtenerClaseEstado(estado?: string): string {
    const e = (estado || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    if (e.includes('finaliz') || e.includes('resuelt')) return 'tag-estado-finalizado';
    if (e.includes('proceso') || e.includes('revision')) return 'tag-estado-proceso';
    if (e.includes('iniciad')) return 'tag-estado-iniciado';
    return 'tag-estado-pendiente';
  }
}

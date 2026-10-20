import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import {
  ReporteService,
  Reporte,
  CrearOrdenTrabajoRequest
} from '../../core/services/reporte.service';

export interface OrdenTrabajoFormModel {
  cuadrilla: string;
  responsable: string;
  cantidadPersonal: number | null;
  vehiculo: string;
  fechaVisita: string;
  horaVisita: string;
  duracionEstimada: string;
  materiales: string;
  instrucciones: string;
}

@Component({
  selector: 'app-gestionar-reporte',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './gestionar-reporte.html',
  styleUrl: './gestionar-reporte.css'
})
export class GestionarReporteComponent implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private reporteService = inject(ReporteService);
  private cdr = inject(ChangeDetectorRef);

  idReporte: number | null = null;
  cargandoReporte: boolean = true;
  guardandoOrden: boolean = false;
  mensajeExito: string | null = null;
  mensajeError: string | null = null;

  reporte: Reporte = {
    id: 0,
    codigo: 'Cargando...',
    categoria: '',
    texto: '',
    direccion: '',
    fechaIngreso: '',
    urgencia: 'Media',
    sentimiento: 'Neutral',
    estado: 'No Iniciado',
    nivelRiesgo: 'NINGUNO',
    huboAjuste: false
  };

  form: OrdenTrabajoFormModel = {
    cuadrilla: '',
    responsable: '',
    cantidadPersonal: null,
    vehiculo: '',
    fechaVisita: '',
    horaVisita: '',
    duracionEstimada: '',
    materiales: '',
    instrucciones: ''
  };

  // Opciones fijas solicitadas para cuadrillas especializadas
  readonly cuadrillas = [
    'Cuadrilla de Obras Viales (Pavimentación)',
    'Cuadrilla Sanitaria y Redes (Alcantarillado)',
    'Cuadrilla Eléctrica y Alumbrado (Iluminación)',
    'Cuadrilla de Emergencia Municipal'
  ];

  // Opciones fijas solicitadas para duración estimada
  readonly duraciones = [
    '1 a 2 horas (Intervención rápida)',
    'Media jornada (4 horas)',
    'Jornada completa (8 horas)',
    '2 a 3 días',
    'Más de 3 días (Obra mayor)'
  ];

  ngOnInit(): void {
    const paramId = this.route.snapshot.paramMap.get('id') || this.route.snapshot.queryParamMap.get('id');
    const codigoParam = this.route.snapshot.queryParamMap.get('codigo');

    let numericId: number | null = null;
    if (paramId && !isNaN(Number(paramId))) {
      numericId = Number(paramId);
    } else if (codigoParam) {
      numericId = this.parsearIdentificador(codigoParam);
    }

    if (numericId) {
      this.idReporte = numericId;
      this.cargarDatosReporte(numericId);
      this.cargarOrdenExistente(numericId);
    } else {
      this.mensajeError = 'No se especificó un ID de reporte válido para gestionar.';
      this.cargandoReporte = false;
    }
  }

  cargarDatosReporte(id: number): void {
    this.cargandoReporte = true;
    this.reporteService.obtenerReportePorId(id).subscribe({
      next: (rep) => {
        this.reporte = rep;
        this.cargandoReporte = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error al cargar reporte:', err);
        this.mensajeError = 'No fue posible cargar la información del reporte desde el servidor.';
        this.cargandoReporte = false;
        this.cdr.markForCheck();
      }
    });
  }

  cargarOrdenExistente(id: number): void {
    this.reporteService.obtenerOrdenTrabajo(id).subscribe({
      next: (orden) => {
        if (orden) {
          this.form = {
            cuadrilla: orden.cuadrilla || '',
            responsable: orden.responsable || '',
            cantidadPersonal: orden.cantidadPersonal || null,
            vehiculo: orden.vehiculo || '',
            fechaVisita: orden.fechaVisita || '',
            horaVisita: orden.horaVisita ? orden.horaVisita.substring(0, 5) : '',
            duracionEstimada: orden.duracionEstimada || '',
            materiales: orden.materiales || '',
            instrucciones: orden.instrucciones || ''
          };
          this.cdr.markForCheck();
        }
      },
      error: () => {
        // Si responde 404, el reporte aún no tiene orden asignada. El formulario queda vacío listo para crear.
      }
    });
  }

  crearOrdenTrabajo(): void {
    if (!this.idReporte) {
      alert('Error: no se tiene el identificador del reporte.');
      return;
    }

    if (
      !this.form.cuadrilla ||
      !this.form.responsable ||
      !this.form.cantidadPersonal ||
      !this.form.fechaVisita ||
      !this.form.horaVisita ||
      !this.form.duracionEstimada ||
      !this.form.instrucciones
    ) {
      alert('Por favor completa todos los campos obligatorios (*) para crear la orden de trabajo.');
      return;
    }

    if (this.form.cantidadPersonal < 1) {
      alert('La cantidad de personal debe ser de al menos 1 operario.');
      return;
    }

    const request: CrearOrdenTrabajoRequest = {
      cuadrilla: this.form.cuadrilla,
      responsable: this.form.responsable,
      cantidadPersonal: this.form.cantidadPersonal,
      vehiculo: this.form.vehiculo ? this.form.vehiculo.trim() : undefined,
      fechaVisita: this.form.fechaVisita,
      horaVisita: this.form.horaVisita.length === 5 ? this.form.horaVisita : this.form.horaVisita.substring(0, 5),
      duracionEstimada: this.form.duracionEstimada,
      materiales: this.form.materiales ? this.form.materiales.trim() : undefined,
      instrucciones: this.form.instrucciones
    };

    this.guardandoOrden = true;
    this.reporteService.crearOrdenTrabajo(this.idReporte, request).subscribe({
      next: () => {
        this.guardandoOrden = false;
        this.reporte.estado = 'Iniciado';
        this.mensajeExito = '¡Orden de trabajo creada con éxito! El reporte ahora se encuentra en estado Iniciado.';
        this.cdr.markForCheck();

        setTimeout(() => {
          this.volverAlPanel();
        }, 1800);
      },
      error: (err) => {
        console.error('Error al registrar orden de trabajo:', err);
        this.guardandoOrden = false;
        alert('Ocurrió un error al registrar la orden de trabajo en la base de datos.');
        this.cdr.markForCheck();
      }
    });
  }

  obtenerClaseUrgencia(urgencia?: string): string {
    const u = (urgencia || '').toLowerCase();
    if (u.includes('alta')) return 'tag-urgency-high';
    if (u.includes('media')) return 'tag-urgency-medium';
    if (u.includes('baja')) return 'tag-urgency-low';
    return 'tag-urgency-low';
  }

  volverAlPanel(): void {
    this.router.navigate(['/panel-administrativo']);
  }

  cancelar(): void {
    this.volverAlPanel();
  }

  verEnMapa(): void {
    if (this.reporte.direccion && this.reporte.direccion.trim()) {
      const direccionCodificada = encodeURIComponent(this.reporte.direccion.trim());
      window.open(`https://www.google.com/maps/search/?api=1&query=${direccionCodificada}`, '_blank');
    } else {
      alert('Este reporte no registra una dirección en la base de datos.');
    }
  }

  private parsearIdentificador(str: string): number {
    try {
      if (str.toUpperCase().startsWith('RPT-')) {
        const partes = str.split('-');
        return parseInt(partes[partes.length - 1], 10);
      }
      return parseInt(str, 10);
    } catch {
      return 1;
    }
  }
}

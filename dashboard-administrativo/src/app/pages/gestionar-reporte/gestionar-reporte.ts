import { Component, inject, OnInit, ChangeDetectorRef, ViewChild, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import {
  ReporteService,
  Reporte,
  CrearOrdenTrabajoRequest
} from '../../core/services/reporte.service';
import { NotificacionService } from '../../core/services/notificacion.service';

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

import { HeaderComponent } from '../../shared/components/header/header';

@Component({
  selector: 'app-gestionar-reporte',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, HeaderComponent],
  templateUrl: './gestionar-reporte.html',
  styleUrl: './gestionar-reporte.css'
})
export class GestionarReporteComponent implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private reporteService = inject(ReporteService);
  private notificacionService = inject(NotificacionService);
  private cdr = inject(ChangeDetectorRef);

  @ViewChild('cuadrillaDropdownRef') cuadrillaDropdownRef?: ElementRef<HTMLElement>;
  @ViewChild('duracionDropdownRef') duracionDropdownRef?: ElementRef<HTMLElement>;
  @ViewChild('calendarioDropdownRef') calendarioDropdownRef?: ElementRef<HTMLElement>;
  @ViewChild('horaDropdownRef') horaDropdownRef?: ElementRef<HTMLElement>;

  dropdownCuadrillaAbierto: boolean = false;
  dropdownDuracionAbierto: boolean = false;
  calendarioAbierto: boolean = false;
  selectorHoraAbierto: boolean = false;
  mesVisualizado: Date = new Date();

  readonly diasSemana = ['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá', 'Do'];
  readonly horasPresets = ['08:00', '09:00', '10:30', '12:00', '14:00', '15:30', '17:00'];
  readonly horasDisponibles = ['07', '08', '09', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20'];
  readonly minutosDisponibles = ['00', '15', '30', '45'];

  idReporte: number | null = null;
  cargandoReporte: boolean = true;
  guardandoOrden: boolean = false;
  intentadoGuardar: boolean = false;
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
        // SEGURIDAD: Si el reporte ya no se encuentra en estado "No iniciado",
        // redirigir automáticamente a la vista de seguimiento/trazabilidad.
        if (!this.esEstadoNoIniciado(rep.estado)) {
          console.warn(`Seguridad: El reporte ${rep.codigo} (ID: ${rep.id}) ya está en estado "${rep.estado}". Redirigiendo a seguimiento.`);
          this.router.navigate(['/seguimiento', rep.id], {
            queryParams: { codigo: rep.codigo }
          });
          return;
        }

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

  esEstadoNoIniciado(estado?: string): boolean {
    if (!estado) return true;
    const e = estado.trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    return e === 'no iniciado' || e === 'pendiente';
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

  async crearOrdenTrabajo(): Promise<void> {
    if (!this.idReporte) {
      this.notificacionService.error('Error de identificador', 'No se tiene el identificador del reporte.');
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
      this.intentadoGuardar = true;
      this.notificacionService.advertencia(
        'Campos obligatorios incompletos',
        'Por favor completa todos los campos marcados con (*) para crear la orden de trabajo.'
      );
      this.cdr.markForCheck();
      return;
    }

    if (this.form.cantidadPersonal < 1) {
      this.intentadoGuardar = true;
      this.notificacionService.advertencia(
        'Personal insuficiente',
        'La cantidad de personal debe ser de al menos 1 operario.'
      );
      this.cdr.markForCheck();
      return;
    }

    // Modal de Confirmación previo al registro
    const confirmado = await this.notificacionService.confirmar({
      titulo: '¿Confirmar creación de Orden de Trabajo?',
      mensaje: 'Asegúrate de haber ingresado la información correctamente. Si es así, confirma para crear la orden de trabajo.',
      textoConfirmar: 'Sí, crear orden',
      textoCancelar: 'Volver y revisar',
      tipo: 'primario'
    });

    if (!confirmado) {
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
        this.notificacionService.exito(
          'Orden de trabajo creada',
          'El reporte pasó al estado Iniciado correctamente.'
        );
        this.cdr.markForCheck();

        setTimeout(() => {
          this.volverAlPanel();
        }, 1200);
      },
      error: (err) => {
        console.error('Error al registrar orden de trabajo:', err);
        this.guardandoOrden = false;
        this.notificacionService.error(
          'Error al registrar orden',
          err.error?.message || 'Ocurrió un error al registrar la orden de trabajo en la base de datos.'
        );
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

  toggleDropdownCuadrilla(event: MouseEvent): void {
    event.stopPropagation();
    this.dropdownCuadrillaAbierto = !this.dropdownCuadrillaAbierto;
    if (this.dropdownCuadrillaAbierto) {
      this.dropdownDuracionAbierto = false;
    }
    this.cdr.markForCheck();
  }

  toggleDropdownDuracion(event: MouseEvent): void {
    event.stopPropagation();
    this.dropdownDuracionAbierto = !this.dropdownDuracionAbierto;
    if (this.dropdownDuracionAbierto) {
      this.dropdownCuadrillaAbierto = false;
    }
    this.cdr.markForCheck();
  }

  seleccionarCuadrilla(cuadrilla: string): void {
    this.form.cuadrilla = cuadrilla;
    this.dropdownCuadrillaAbierto = false;
    this.cdr.markForCheck();
  }

  seleccionarDuracion(duracion: string): void {
    this.form.duracionEstimada = duracion;
    this.dropdownDuracionAbierto = false;
    this.cdr.markForCheck();
  }

  get nombreMesVisualizado(): string {
    const meses = [
      'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
      'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];
    return `${meses[this.mesVisualizado.getMonth()]} ${this.mesVisualizado.getFullYear()}`;
  }

  get diasCalendario(): Array<{ fechaStr: string; dia: number; esMesActual: boolean; esHoy: boolean; esSeleccionado: boolean }> {
    const year = this.mesVisualizado.getFullYear();
    const month = this.mesVisualizado.getMonth();

    const hoy = new Date();
    const hoyStr = this.formatearFechaISO(hoy);

    const primerDia = new Date(year, month, 1);
    let diaSemanaInicio = primerDia.getDay() - 1;
    if (diaSemanaInicio === -1) diaSemanaInicio = 6;

    const ultimoDia = new Date(year, month + 1, 0);
    const diasEnMes = ultimoDia.getDate();
    const ultimoDiaMesAnterior = new Date(year, month, 0).getDate();

    const dias = [];

    for (let i = diaSemanaInicio - 1; i >= 0; i--) {
      const d = ultimoDiaMesAnterior - i;
      const fecha = new Date(year, month - 1, d);
      const fechaStr = this.formatearFechaISO(fecha);
      dias.push({
        fechaStr,
        dia: d,
        esMesActual: false,
        esHoy: fechaStr === hoyStr,
        esSeleccionado: this.form.fechaVisita === fechaStr
      });
    }

    for (let d = 1; d <= diasEnMes; d++) {
      const fecha = new Date(year, month, d);
      const fechaStr = this.formatearFechaISO(fecha);
      dias.push({
        fechaStr,
        dia: d,
        esMesActual: true,
        esHoy: fechaStr === hoyStr,
        esSeleccionado: this.form.fechaVisita === fechaStr
      });
    }

    const celdasRestantes = (7 - (dias.length % 7)) % 7;
    for (let d = 1; d <= celdasRestantes; d++) {
      const fecha = new Date(year, month + 1, d);
      const fechaStr = this.formatearFechaISO(fecha);
      dias.push({
        fechaStr,
        dia: d,
        esMesActual: false,
        esHoy: fechaStr === hoyStr,
        esSeleccionado: this.form.fechaVisita === fechaStr
      });
    }

    return dias;
  }

  formatearFechaISO(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  formatearFechaLegible(fechaISO?: string): string {
    if (!fechaISO) return '';
    try {
      const partes = fechaISO.split('-');
      if (partes.length === 3) {
        return `${partes[2]}/${partes[1]}/${partes[0]}`;
      }
      return fechaISO;
    } catch {
      return fechaISO || '';
    }
  }

  cambiarMes(delta: number, event?: MouseEvent): void {
    if (event) event.stopPropagation();
    this.mesVisualizado = new Date(this.mesVisualizado.getFullYear(), this.mesVisualizado.getMonth() + delta, 1);
    this.cdr.markForCheck();
  }

  toggleCalendario(event: MouseEvent): void {
    event.stopPropagation();
    this.calendarioAbierto = !this.calendarioAbierto;
    if (this.calendarioAbierto) {
      this.selectorHoraAbierto = false;
      this.dropdownCuadrillaAbierto = false;
      this.dropdownDuracionAbierto = false;
      if (this.form.fechaVisita) {
        const partes = this.form.fechaVisita.split('-');
        if (partes.length === 3) {
          this.mesVisualizado = new Date(Number(partes[0]), Number(partes[1]) - 1, 1);
        }
      } else {
        this.mesVisualizado = new Date();
      }
    }
    this.cdr.markForCheck();
  }

  seleccionarFecha(fechaStr: string, event?: MouseEvent): void {
    if (event) event.stopPropagation();
    this.form.fechaVisita = fechaStr;
    this.calendarioAbierto = false;
    this.cdr.markForCheck();
  }

  seleccionarHoy(event?: MouseEvent): void {
    if (event) event.stopPropagation();
    const hoy = new Date();
    this.form.fechaVisita = this.formatearFechaISO(hoy);
    this.mesVisualizado = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    this.calendarioAbierto = false;
    this.cdr.markForCheck();
  }

  limpiarFecha(event?: MouseEvent): void {
    if (event) event.stopPropagation();
    this.form.fechaVisita = '';
    this.cdr.markForCheck();
  }

  toggleSelectorHora(event: MouseEvent): void {
    event.stopPropagation();
    this.selectorHoraAbierto = !this.selectorHoraAbierto;
    if (this.selectorHoraAbierto) {
      this.calendarioAbierto = false;
      this.dropdownCuadrillaAbierto = false;
      this.dropdownDuracionAbierto = false;
    }
    this.cdr.markForCheck();
  }

  seleccionarPresetHora(hora: string, event?: MouseEvent): void {
    if (event) event.stopPropagation();
    this.form.horaVisita = hora;
    this.selectorHoraAbierto = false;
    this.cdr.markForCheck();
  }

  seleccionarHoraParte(parteHora: string, event?: MouseEvent): void {
    if (event) event.stopPropagation();
    const actualMinuto = this.obtenerMinutoActual();
    this.form.horaVisita = `${parteHora}:${actualMinuto}`;
    this.cdr.markForCheck();
  }

  seleccionarMinutoParte(parteMinuto: string, event?: MouseEvent): void {
    if (event) event.stopPropagation();
    const actualHora = this.obtenerHoraActual();
    this.form.horaVisita = `${actualHora}:${parteMinuto}`;
    this.selectorHoraAbierto = false;
    this.cdr.markForCheck();
  }

  obtenerHoraActual(): string {
    if (this.form.horaVisita && this.form.horaVisita.includes(':')) {
      return this.form.horaVisita.split(':')[0];
    }
    return '09';
  }

  obtenerMinutoActual(): string {
    if (this.form.horaVisita && this.form.horaVisita.includes(':')) {
      return this.form.horaVisita.split(':')[1];
    }
    return '00';
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as Node;
    let cambio = false;

    if (this.dropdownCuadrillaAbierto && this.cuadrillaDropdownRef) {
      if (!this.cuadrillaDropdownRef.nativeElement.contains(target)) {
        this.dropdownCuadrillaAbierto = false;
        cambio = true;
      }
    }

    if (this.dropdownDuracionAbierto && this.duracionDropdownRef) {
      if (!this.duracionDropdownRef.nativeElement.contains(target)) {
        this.dropdownDuracionAbierto = false;
        cambio = true;
      }
    }

    if (this.calendarioAbierto && this.calendarioDropdownRef) {
      if (!this.calendarioDropdownRef.nativeElement.contains(target)) {
        this.calendarioAbierto = false;
        cambio = true;
      }
    }

    if (this.selectorHoraAbierto && this.horaDropdownRef) {
      if (!this.horaDropdownRef.nativeElement.contains(target)) {
        this.selectorHoraAbierto = false;
        cambio = true;
      }
    }

    if (cambio) {
      this.cdr.markForCheck();
    }
  }

  volverAlPanel(): void {
    this.router.navigate(['/panel-administrativo']);
  }

  cancelar(): void {
    this.volverAlPanel();
  }

  verEnMapa(): void {
    const direccion = this.reporte.direccion?.trim();
    if (direccion && direccion.toLowerCase() !== 'no especificada') {
      const direccionCodificada = encodeURIComponent(direccion);
      window.open(`https://www.google.com/maps/search/?api=1&query=${direccionCodificada}`, '_blank');
    } else {
      this.notificacionService.info(
        'Sin dirección',
        'Este reporte no registra una dirección específica.'
      );
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
